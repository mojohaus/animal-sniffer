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

import javax.xml.parsers.DocumentBuilderFactory;

import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;

import org.junit.jupiter.api.Test;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

/**
 * Checks the generated plugin descriptor, where a copy-pasted {@code property} would show up as two parameters
 * sharing one user property.
 */
class BuildSignaturesMojoDescriptorTest {
    @Test
    void everyParameterHasItsOwnUserProperty() throws Exception {
        Map<String, String> expressionToParameter = new HashMap<>();
        for (Map.Entry<String, String> e : buildMojoExpressions().entrySet()) {
            String previous = expressionToParameter.put(e.getValue(), e.getKey());
            assertNull(
                    previous,
                    "parameters " + previous + " and " + e.getKey() + " share the expression " + e.getValue());
        }
    }

    @Test
    void includeModuleClassesHasItsOwnProperty() throws Exception {
        Map<String, String> expressions = buildMojoExpressions();
        assertEquals("${includeModuleClasses}", expressions.get("includeModuleClasses"));
        assertEquals("${includeJavaHome}", expressions.get("includeJavaHome"));
    }

    private static Map<String, String> buildMojoExpressions() throws Exception {
        Document doc;
        try (InputStream in =
                BuildSignaturesMojoDescriptorTest.class.getResourceAsStream("/META-INF/maven/plugin.xml")) {
            assertNotNull(in, "plugin descriptor not generated");
            doc = DocumentBuilderFactory.newInstance().newDocumentBuilder().parse(in);
        }
        NodeList mojos = doc.getElementsByTagName("mojo");
        for (int i = 0; i < mojos.getLength(); i++) {
            Element mojo = (Element) mojos.item(i);
            if (!"build".equals(text(mojo, "goal"))) {
                continue;
            }
            Map<String, String> result = new HashMap<>();
            Element configuration =
                    (Element) mojo.getElementsByTagName("configuration").item(0);
            NodeList children = configuration.getChildNodes();
            for (int j = 0; j < children.getLength(); j++) {
                if (children.item(j) instanceof Element) {
                    Element c = (Element) children.item(j);
                    String value = c.getTextContent().trim();
                    if (value.startsWith("${")) {
                        result.put(c.getTagName(), value);
                    }
                }
            }
            return result;
        }
        throw new AssertionError("build goal not found in plugin descriptor");
    }

    private static String text(Element parent, String name) {
        return parent.getElementsByTagName(name).item(0).getTextContent().trim();
    }
}
