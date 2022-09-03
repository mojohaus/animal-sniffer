package localhost;

import java.util.Optional;

public class TestWithNested {
    Optional<String> f;

    Optional<String> test() {
        return null;
    }

    public class Nested {
        Optional<String> f;

        Optional<String> test() {
            return null;
        }
    }
}
