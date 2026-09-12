package localhost;

import java.util.Base64;
import org.codehaus.mojo.animal_sniffer.IgnoreJRERequirement;

public class SuppressedSyntheticAccessor {
    @IgnoreJRERequirement
    private static void encode(Base64.Encoder encoder) {
    }

    public static class Nested {
        @IgnoreJRERequirement
        public static void use() {
            encode(null);
        }
    }
}
