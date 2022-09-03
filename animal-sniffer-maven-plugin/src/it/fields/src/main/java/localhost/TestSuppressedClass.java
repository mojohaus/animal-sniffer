package localhost;

import java.util.Optional;
import org.codehaus.mojo.animal_sniffer.IgnoreJRERequirement;

@IgnoreJRERequirement
public class TestSuppressedClass {
    Optional<String> f;

    Optional<String> test() {
        return null;
    }
}
