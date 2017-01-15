package localhost;

import java.util.Optional;
import java.util.function.Predicate;

public class Bad {
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
}
