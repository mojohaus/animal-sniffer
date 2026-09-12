package localhost;

import java.util.Base64;
import org.codehaus.mojo.animal_sniffer.IgnoreJRERequirement;

public abstract class SuppressedMethods {
    @IgnoreJRERequirement
    public SuppressedMethods(Base64.Encoder encoder) {
    }

    @IgnoreJRERequirement
    public void encode(Base64.Encoder encoder, Base64.Decoder[][] decoders) {
    }

    @IgnoreJRERequirement
    public abstract void decode(Base64.Decoder... decoders);
}
