/*
 * The MIT License
 *
 * Copyright 2012 Codehaus.
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 */

package org.codehaus.mojo.animal_sniffer;

import java.io.File;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Signature polymorphic methods are called with the descriptor of the call site (#67).
 */
class SignaturePolymorphicTest {

    private static final String METHOD_HANDLE = "java/lang/invoke/MethodHandle";

    @Test
    void methodHandleCallsAreCheckedByName() throws Exception {
        List<String> errors = check(methodHandle(
                "invoke([Ljava/lang/Object;)Ljava/lang/Object;", "invokeExact([Ljava/lang/Object;)Ljava/lang/Object;"));

        assertEquals(Collections.emptyList(), errors);
    }

    @Test
    void methodHandleInheritsTheMethodsOfItsSuperClass() throws Exception {
        Map<String, Clazz> classes = new HashMap<>();
        classes.put(METHOD_HANDLE, new Clazz(METHOD_HANDLE, Collections.<String>emptySet(), "my/Base", new String[0]));
        classes.put(
                "my/Base",
                new Clazz(
                        "my/Base",
                        new HashSet<>(Arrays.asList(
                                "invoke([Ljava/lang/Object;)Ljava/lang/Object;",
                                "invokeExact([Ljava/lang/Object;)Ljava/lang/Object;")),
                        "java/lang/Object",
                        new String[0]));

        assertEquals(Collections.emptyList(), check(classes));
    }

    @Test
    void missingMethodIsStillReported() throws Exception {
        List<String> errors = check(methodHandle("invoke([Ljava/lang/Object;)Ljava/lang/Object;"));

        assertEquals(1, errors.size(), errors.toString());
        assertTrue(errors.get(0).contains("MethodHandle.invokeExact"), errors.get(0));
    }

    @Test
    void declarationsWithoutObjectArrayDoNotSatisfyTheCall() throws Exception {
        // the same names, but without the single Object[] parameter that makes a method signature polymorphic
        List<String> errors = check(methodHandle("invoke(I)V", "invokeExact(Ljava/lang/String;)Ljava/lang/Integer;"));

        assertEquals(2, errors.size(), errors.toString());
    }

    @Test
    void recognizesTheSignaturePolymorphicMethods() {
        assertTrue(SignatureChecker.isSignaturePolymorphic(METHOD_HANDLE, "invokeExact"));
        assertTrue(SignatureChecker.isSignaturePolymorphic(METHOD_HANDLE, "invoke"));
        assertTrue(SignatureChecker.isSignaturePolymorphic("java/lang/invoke/VarHandle", "compareAndSet"));
        assertTrue(SignatureChecker.isSignaturePolymorphic("java/lang/invoke/VarHandle", "getAndBitwiseXorRelease"));
        assertFalse(SignatureChecker.isSignaturePolymorphic(METHOD_HANDLE, "bindTo"));
        assertFalse(SignatureChecker.isSignaturePolymorphic("java/lang/invoke/VarHandle", "toString"));
        assertFalse(SignatureChecker.isSignaturePolymorphic("my/Handle", "invoke"));
    }

    private static Map<String, Clazz> methodHandle(String... signatures) {
        Map<String, Clazz> classes = new HashMap<>();
        classes.put(
                METHOD_HANDLE,
                new Clazz(METHOD_HANDLE, new HashSet<>(Arrays.asList(signatures)), "java/lang/Object", new String[0]));
        return classes;
    }

    private static List<String> check(Map<String, Clazz> classes) throws Exception {
        final List<String> errors = new ArrayList<>();
        Set<String> ignored = new HashSet<>(Arrays.asList(
                "java.lang.Object",
                "java.lang.String",
                "java.lang.Integer",
                "java.lang.Throwable",
                "org.codehaus.mojo.animal_sniffer.*"));
        SignatureChecker checker = new SignatureChecker(classes, ignored, new SignatureCheckerTest.TestLogger() {
            @Override
            public void error(String message) {
                errors.add(message);
            }
        });
        checker.setSourcePath(Collections.<File>emptyList());
        checker.process(classFileOf(SignaturePolymorphicCaller.class));
        return errors;
    }

    private static File classFileOf(Class<?> type) throws URISyntaxException {
        return new File(type.getResource(type.getSimpleName() + ".class").toURI());
    }
}
