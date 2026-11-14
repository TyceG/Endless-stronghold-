package dev.tyceg.endlessstronghold;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

/**
 * Generator settings, read from the world preset JSON. Every field is optional.
 * Reserved for Phase 4: since the stronghold grows like vanilla's, it has no
 * fixed floors, and the generator doesn't read these yet. They stay so worlds
 * that saved them still load.
 */
public record GeneratorSettings(int floors, int baseY) {
	public static final GeneratorSettings DEFAULT = new GeneratorSettings(4, 0);

	public static final Codec<GeneratorSettings> CODEC = RecordCodecBuilder.create(i -> i.group(
		Codec.intRange(1, 4).optionalFieldOf("floors", DEFAULT.floors()).forGetter(GeneratorSettings::floors),
		Codec.intRange(-64, 240).optionalFieldOf("base_y", DEFAULT.baseY()).forGetter(GeneratorSettings::baseY)
	).apply(i, GeneratorSettings::new));
}
