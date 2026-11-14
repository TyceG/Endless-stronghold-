package dev.tyceg.endlessstronghold;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

/**
 * Generator settings, read from the world preset JSON. Every field is optional
 * and falls back to the defaults in the design doc.
 */
public record GeneratorSettings(int floors, int baseY) {
	public static final GeneratorSettings DEFAULT = new GeneratorSettings(4, 0);

	public static final Codec<GeneratorSettings> CODEC = RecordCodecBuilder.create(i -> i.group(
		Codec.intRange(1, 4).optionalFieldOf("floors", DEFAULT.floors()).forGetter(GeneratorSettings::floors),
		Codec.intRange(-64, 240).optionalFieldOf("base_y", DEFAULT.baseY()).forGetter(GeneratorSettings::baseY)
	).apply(i, GeneratorSettings::new));
}
