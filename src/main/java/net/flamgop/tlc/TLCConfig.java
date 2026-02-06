package net.flamgop.tlc;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

public record TLCConfig(
    double maxLoadDistance,
    boolean teleport
) {
    public static final Codec<TLCConfig> CODEC = RecordCodecBuilder.create(instance ->
            instance.group(
                    Codec.DOUBLE.fieldOf("max_load_distance").forGetter(TLCConfig::maxLoadDistance),
                    Codec.BOOL.fieldOf("teleport_when_too_far").forGetter(TLCConfig::teleport)
            ).apply(instance, TLCConfig::new)
    );

    public TLCConfig() {
        this(100, true);
    }
}
