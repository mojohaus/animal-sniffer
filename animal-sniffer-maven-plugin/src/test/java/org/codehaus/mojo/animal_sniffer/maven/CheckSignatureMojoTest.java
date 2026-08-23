package org.codehaus.mojo.animal_sniffer.maven;

/*
 * The MIT License
 *
 * Copyright (c) 2026 codehaus.org.
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

import java.util.Arrays;

import org.apache.maven.model.Dependency;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;

public class CheckSignatureMojoTest {
    @Test
    public void parsesUnclassifiedCoordinates() {
        // Original short-form three-part command-line coordinates remain supported
        CheckSignatureMojo mojo = new CheckSignatureMojo();

        mojo.setSignature("test:api:1.0");

        assertEquals("test", mojo.signature.getGroupId());
        assertEquals("api", mojo.signature.getArtifactId());
        assertNull(mojo.signature.getClassifier());
        assertEquals("1.0", mojo.signature.getVersion());
    }

    @Test
    public void parsesClassifiedCoordinates() {
        // Full coordinates select a classified signature
        CheckSignatureMojo mojo = new CheckSignatureMojo();

        mojo.setSignature("test:api:signature:test-classifier:2.0");

        assertEquals("test", mojo.signature.getGroupId());
        assertEquals("api", mojo.signature.getArtifactId());
        assertEquals("test-classifier", mojo.signature.getClassifier());
        assertEquals("2.0", mojo.signature.getVersion());
    }

    @Test
    public void matchesClassifierForVersionInference() {
        // Version inference should not cross classified artifact variants
        Signature signature = new Signature();
        signature.setGroupId("test");
        signature.setArtifactId("api");
        signature.setClassifier("test-classifier");
        Dependency unclassified = dependency(null, "1.0");
        Dependency classified = dependency("test-classifier", "2.0");

        Dependency match =
                CheckSignatureMojo.findMatchingDependency(signature, Arrays.asList(unclassified, classified));

        assertSame(classified, match);
    }

    private static Dependency dependency(String classifier, String version) {
        Dependency dependency = new Dependency();
        dependency.setGroupId("test");
        dependency.setArtifactId("api");
        dependency.setClassifier(classifier);
        dependency.setType("signature");
        dependency.setVersion(version);
        return dependency;
    }
}
