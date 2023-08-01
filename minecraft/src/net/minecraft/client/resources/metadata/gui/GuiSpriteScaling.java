package net.minecraft.client.resources.metadata.gui;

import com.mojang.datafixers.util.Either;
import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import com.mojang.serialization.codecs.RecordCodecBuilder.Instance;
import java.lang.runtime.ObjectMethods;
import java.util.OptionalInt;
import java.util.function.Function;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.util.ExtraCodecs;
import net.minecraft.util.StringRepresentable;

@Environment(EnvType.CLIENT)
public interface GuiSpriteScaling {
	Codec<GuiSpriteScaling> CODEC = GuiSpriteScaling.Type.CODEC.dispatch(GuiSpriteScaling::type, GuiSpriteScaling.Type::codec);
	GuiSpriteScaling DEFAULT = new GuiSpriteScaling.Stretch();

	GuiSpriteScaling.Type type();

	@Environment(EnvType.CLIENT)
	public static record NineSlice(int width, int height, GuiSpriteScaling.NineSlice.Border border) implements GuiSpriteScaling {
		public static final Codec<GuiSpriteScaling.NineSlice> CODEC = RecordCodecBuilder.create(
			instance -> instance.group(
						ExtraCodecs.POSITIVE_INT.fieldOf("width").forGetter(GuiSpriteScaling.NineSlice::width),
						ExtraCodecs.POSITIVE_INT.fieldOf("height").forGetter(GuiSpriteScaling.NineSlice::height),
						GuiSpriteScaling.NineSlice.Border.CODEC.fieldOf("border").forGetter(GuiSpriteScaling.NineSlice::border)
					)
					.apply(instance, GuiSpriteScaling.NineSlice::new)
		);

		@Override
		public GuiSpriteScaling.Type type() {
			return GuiSpriteScaling.Type.NINE_SLICE;
		}

		@Environment(EnvType.CLIENT)
		public static record Border(int left, int top, int right, int bottom) {
			private static final Codec<GuiSpriteScaling.NineSlice.Border> VALUE_CODEC = ExtraCodecs.POSITIVE_INT
				.flatComapMap(integer -> new GuiSpriteScaling.NineSlice.Border(integer, integer, integer, integer), border -> {
					OptionalInt optionalInt = border.unpackValue();
					return optionalInt.isPresent() ? DataResult.success(optionalInt.getAsInt()) : DataResult.error(() -> "Border has different side sizes");
				});
			private static final Codec<GuiSpriteScaling.NineSlice.Border> RECORD_CODEC = RecordCodecBuilder.create(
				instance -> instance.group(
							ExtraCodecs.POSITIVE_INT.fieldOf("left").forGetter(GuiSpriteScaling.NineSlice.Border::left),
							ExtraCodecs.POSITIVE_INT.fieldOf("top").forGetter(GuiSpriteScaling.NineSlice.Border::top),
							ExtraCodecs.POSITIVE_INT.fieldOf("right").forGetter(GuiSpriteScaling.NineSlice.Border::right),
							ExtraCodecs.POSITIVE_INT.fieldOf("bottom").forGetter(GuiSpriteScaling.NineSlice.Border::bottom)
						)
						.apply(instance, GuiSpriteScaling.NineSlice.Border::new)
			);
			static final Codec<GuiSpriteScaling.NineSlice.Border> CODEC = Codec.either(VALUE_CODEC, RECORD_CODEC)
				.xmap(
					either -> either.map(Function.identity(), Function.identity()), border -> border.unpackValue().isPresent() ? Either.left(border) : Either.right(border)
				);

			private OptionalInt unpackValue() {
				return this.left() == this.top() && this.top() == this.right() && this.right() == this.bottom() ? OptionalInt.of(this.left()) : OptionalInt.empty();
			}
		}
	}

	@Environment(EnvType.CLIENT)
	public static final class Stretch extends Record implements GuiSpriteScaling {
		public static final Codec<GuiSpriteScaling.Stretch> CODEC = Codec.unit(GuiSpriteScaling.Stretch::new);

		@Override
		public GuiSpriteScaling.Type type() {
			return GuiSpriteScaling.Type.STRETCH;
		}

		public final String toString() {
			return ObjectMethods.bootstrap<"toString",GuiSpriteScaling.Stretch,"">(this);
		}

		public final int hashCode() {
			return ObjectMethods.bootstrap<"hashCode",GuiSpriteScaling.Stretch,"">(this);
		}

		public final boolean equals(Object object) {
			return ObjectMethods.bootstrap<"equals",GuiSpriteScaling.Stretch,"">(this, object);
		}
	}

	@Environment(EnvType.CLIENT)
	public static record Tile(int width, int height) implements GuiSpriteScaling {
		public static final Codec<GuiSpriteScaling.Tile> CODEC = RecordCodecBuilder.create(
			instance -> instance.group(
						ExtraCodecs.POSITIVE_INT.fieldOf("width").forGetter(GuiSpriteScaling.Tile::width),
						ExtraCodecs.POSITIVE_INT.fieldOf("height").forGetter(GuiSpriteScaling.Tile::height)
					)
					.apply(instance, GuiSpriteScaling.Tile::new)
		);

		@Override
		public GuiSpriteScaling.Type type() {
			return GuiSpriteScaling.Type.TILE;
		}
	}

	@Environment(EnvType.CLIENT)
	public static enum Type implements StringRepresentable {
		STRETCH("stretch", GuiSpriteScaling.Stretch.CODEC),
		TILE("tile", GuiSpriteScaling.Tile.CODEC),
		NINE_SLICE("nine_slice", GuiSpriteScaling.NineSlice.CODEC);

		public static final Codec<GuiSpriteScaling.Type> CODEC = StringRepresentable.fromEnum(GuiSpriteScaling.Type::values);
		private final String key;
		private final Codec<? extends GuiSpriteScaling> codec;

		private Type(String string2, Codec<? extends GuiSpriteScaling> codec) {
			this.key = string2;
			this.codec = codec;
		}

		@Override
		public String getSerializedName() {
			return this.key;
		}

		public Codec<? extends GuiSpriteScaling> codec() {
			return this.codec;
		}
	}
}
