package localhost;

import java.util.Optional;

public class Test {
    Optional<String> f;

    // Also include method to make sure field does not influence method detection
    // in any way
    Optional<String> test() {
        return null;
    }
}
