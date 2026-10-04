package localhost;

import java.util.Base64;
import org.codehaus.mojo.animal_sniffer.IgnoreJRERequirement;

public class SuppressedLambdaCapture {
    @IgnoreJRERequirement
    public static Runnable make(Base64.Encoder encoder) {
        return () -> System.out.println(encoder);
    }
}
