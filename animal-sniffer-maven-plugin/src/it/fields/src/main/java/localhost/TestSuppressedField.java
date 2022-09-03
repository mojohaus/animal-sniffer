package localhost;

import java.util.Optional;
import org.codehaus.mojo.animal_sniffer.IgnoreJRERequirement;

public class TestSuppressedField {
    Optional<String> f1;
    @IgnoreJRERequirement
    Optional<String> f2;
    Optional<String> f3;

    // Also include method to make sure field does not influence method detection
    // in any way
    Optional<String> test() {
        return null;
    }
}
