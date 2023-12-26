package org.codehaus.mojo.animal_sniffer;

import java.io.IOException;
import java.io.InputStream;
import java.io.InvalidClassException;
import java.io.ObjectInputStream;
import java.io.ObjectStreamClass;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

/**
 * {@link ObjectInputStream} subclass which only permits loading classes which are needed
 * by signature files. All other classes are rejected for security reasons.
 */
public class SignatureObjectInputStream extends ObjectInputStream {
    private static final Set<String> ALLOWED_CLASS_NAMES = Collections.unmodifiableSet(new HashSet<>(Arrays.asList(
        Clazz.class.getName(),
        String[].class.getName()
    )));

    public SignatureObjectInputStream(InputStream in) throws IOException {
        super(in);
    }

    // Impose restrictions on allowed classes, see https://wiki.sei.cmu.edu/confluence/display/java/SER12-J.+Prevent+deserialization+of+untrusted+data
    @Override
    protected Class<?> resolveClass(ObjectStreamClass desc) throws IOException, ClassNotFoundException {
        String className = desc.getName();

        if (ALLOWED_CLASS_NAMES.contains(className)) {
            return super.resolveClass(desc);
        }

        Class<?> c;
        try {
            // Should be safe because default implementation uses `initialize=false`, and this is guaranteed by the Javadoc
            c = super.resolveClass(desc);
        } catch (ClassNotFoundException classNotFoundException) {
            // To be safe throw InvalidClassException instead because all allowed classes should exist on classpath
            throw new InvalidClassException(className, "Class not found, probably disallowed class");
        }

        // Also allow Set classes because Clazz has field of type Set
        if (isAllowedSetClass(c)) {
            return c;
        }

        throw new InvalidClassException(className, "Disallowed class for signature data");
    }

    /**
     * Check if the class is an allowed implementation of {@link Set}.
     */
    private static boolean isAllowedSetClass(Class<?> c) {
        return Set.class.isAssignableFrom(c) && c.getName().startsWith("java.util.");
    }
}
