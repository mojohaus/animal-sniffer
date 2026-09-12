package localhost;

import java.util.Base64;

public abstract class AbstractParameter {
    public abstract void encode(Base64.Encoder encoder);
}
