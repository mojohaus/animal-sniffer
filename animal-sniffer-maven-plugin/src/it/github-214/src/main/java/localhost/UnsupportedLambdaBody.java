package localhost;

import java.util.Base64;

public class UnsupportedLambdaBody {
    public static Runnable make() {
        return () -> Base64.getEncoder();
    }
}
