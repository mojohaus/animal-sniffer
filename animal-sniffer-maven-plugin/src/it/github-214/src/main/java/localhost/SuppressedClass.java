package localhost;

import java.util.Base64;
import org.codehaus.mojo.animal_sniffer.IgnoreJRERequirement;

@IgnoreJRERequirement
public abstract class SuppressedClass {
    public SuppressedClass(Base64.Encoder encoder) {
    }

    public void encode(Base64.Encoder encoder, Base64.Decoder[][] decoders) {
    }

    public abstract void decode(Base64.Decoder... decoders);
}
