package dev.tyceg.endlessstronghold;

import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import dev.tyceg.endlessstronghold.gen.Heights;

/**
 * Generator settings, read from the world preset JSON's {@code "settings"}.
 * Every field is optional and falls back to the default.
 *
 * <ul>
 * <li>{@code hub_y} (default 30): the floor of each region's hub, where the
 * main corridors start and where players spawn.</li>
 * <li>{@code lowest_y} (default -24): the lowest any piece may reach, like
 * vanilla's lowest height; at least 12 below {@code hub_y}.</li>
 * </ul>
 *
 * The region size is fixed: changing it would move every region.
 */
public record GeneratorSettings(int hubY, int lowestY) {
	public static final GeneratorSettings DEFAULT = new GeneratorSettings(Heights.DEFAULT.hubY(), Heights.DEFAULT.lowestY());

	public static final Codec<GeneratorSettings> CODEC = RecordCodecBuilder.<GeneratorSettings>create(i -> i.group(
		Codec.intRange(-40, 300).optionalFieldOf("hub_y", DEFAULT.hubY()).forGetter(GeneratorSettings::hubY),
		Codec.intRange(-64, 288).optionalFieldOf("lowest_y", DEFAULT.lowestY()).forGetter(GeneratorSettings::lowestY)
	).apply(i, GeneratorSettings::new)).validate(s -> s.lowestY() < s.hubY() - 12
		? DataResult.success(s)
		: DataResult.error(() -> "lowest_y (" + s.lowestY() + ") must be at least 12 below hub_y (" + s.hubY() + ")"));

	public Heights heights() {
		return new Heights(hubY, lowestY);
	}
}
