package localhost;

import java.util.Optional;
import org.codehaus.mojo.animal_sniffer.IgnoreJRERequirement;

public class TestWithNestedSuppressedClass {
    @IgnoreJRERequirement
    public class Nested {
        Optional<String> f;

        Optional<String> test() {
            return null;
        }
    }
}
