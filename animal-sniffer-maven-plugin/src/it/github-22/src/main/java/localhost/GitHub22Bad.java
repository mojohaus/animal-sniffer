package localhost;

import java.io.Serializable;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Optional;
import java.util.function.Predicate;

public class GitHub22Bad {
    public Predicate<Optional<?>> methodReference() {
        return Optional::isPresent;
    }

    public Predicate<Optional<?>> methodReferenceSerializable() {
        return (Serializable & Predicate<Optional<?>>) Optional::isPresent;
    }

    public Predicate<Optional<?>> lambda() {
        return o -> o.isPresent();
    }

    public Predicate<Optional<?>> lambdaSerializable() {
        return (Serializable & Predicate<Optional<?>>) o -> o.isPresent();
    }

    public void callMethodWhichReturnsTypeFromNewerAPI() {
        lambda();
    }

    public Predicate[][][] arrayReturnType() {
        return new Predicate[][][]{{{(Predicate<Optional<?>>) Optional::isPresent}}};
    }

    public void callArray() {
        arrayReturnType();
    }

    public void exceptionMulticatch() {
        try {
            Method method = GitHub22Bad.class.getDeclaredMethod("emptyMethod");
            method.invoke(null);
        } catch (NoSuchMethodException | IllegalAccessException | InvocationTargetException ignore) {
        }
    }

    public void exceptionClassNotAvailableInJava6() {
        try {
            Method method = GitHub22Bad.class.getDeclaredMethod("emptyMethod");
            method.invoke(null);
        } catch (ReflectiveOperationException ignore) {
        }
    }
}
