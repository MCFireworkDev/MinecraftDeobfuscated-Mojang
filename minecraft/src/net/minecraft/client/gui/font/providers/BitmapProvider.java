package net.minecraft.client.gui.font.providers;

import com.google.common.collect.Lists;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import com.mojang.blaze3d.font.GlyphProvider;
import com.mojang.blaze3d.font.RawGlyph;
import com.mojang.blaze3d.platform.NativeImage;
import it.unimi.dsi.fastutil.ints.Int2ObjectMap;
import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.ints.IntSet;
import it.unimi.dsi.fastutil.ints.IntSets;
import java.io.IOException;
import java.util.List;
import javax.annotation.Nullable;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.Resource;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.util.GsonHelper;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

@Environment(EnvType.CLIENT)
public class BitmapProvider implements GlyphProvider {
	private static final Logger LOGGER = LogManager.getLogger();
	private final NativeImage image;
	private final Int2ObjectMap<BitmapProvider.Glyph> glyphs;

	private BitmapProvider(NativeImage nativeImage, Int2ObjectMap<BitmapProvider.Glyph> int2ObjectMap) {
		this.image = nativeImage;
		this.glyphs = int2ObjectMap;
	}

	@Override
	public void close() {
		this.image.close();
	}

	@Nullable
	@Override
	public RawGlyph getGlyph(int i) {
		return this.glyphs.get(i);
	}

	@Override
	public IntSet getSupportedGlyphs() {
		return IntSets.unmodifiable(this.glyphs.keySet());
	}

	@Environment(EnvType.CLIENT)
	public static class Builder implements GlyphProviderBuilder {
		private final ResourceLocation texture;
		private final List<int[]> chars;
		private final int height;
		private final int ascent;

		public Builder(ResourceLocation resourceLocation, int i, int j, List<int[]> list) {
			this.texture = new ResourceLocation(resourceLocation.getNamespace(), "textures/" + resourceLocation.getPath());
			this.chars = list;
			this.height = i;
			this.ascent = j;
		}

		public static BitmapProvider.Builder fromJson(JsonObject jsonObject) {
			int i = GsonHelper.getAsInt(jsonObject, "height", 8);
			int j = GsonHelper.getAsInt(jsonObject, "ascent");
			if (j > i) {
				throw new JsonParseException("Ascent " + j + " higher than height " + i);
			} else {
				List<int[]> list = Lists.<int[]>newArrayList();
				JsonArray jsonArray = GsonHelper.getAsJsonArray(jsonObject, "chars");

				for(int k = 0; k < jsonArray.size(); ++k) {
					String string = GsonHelper.convertToString(jsonArray.get(k), "chars[" + k + "]");
					int[] is = string.codePoints().toArray();
					if (k > 0) {
						int l = ((int[])list.get(0)).length;
						if (is.length != l) {
							throw new JsonParseException("Elements of chars have to be the same length (found: " + is.length + ", expected: " + l + "), pad with space or \\u0000");
						}
					}

					list.add(is);
				}

				if (!list.isEmpty() && ((int[])list.get(0)).length != 0) {
					return new BitmapProvider.Builder(new ResourceLocation(GsonHelper.getAsString(jsonObject, "file")), i, j, list);
				} else {
					throw new JsonParseException("Expected to find data in chars, found none.");
				}
			}
		}

		@Nullable
		@Override
		public GlyphProvider create(ResourceManager resourceManager) {
			try {
				Resource resource = resourceManager.getResource(this.texture);
				Throwable var3 = null;

				BitmapProvider var31;
				try {
					NativeImage nativeImage = NativeImage.read(NativeImage.Format.RGBA, resource.getInputStream());
					int i = nativeImage.getWidth();
					int j = nativeImage.getHeight();
					int k = i / ((int[])this.chars.get(0)).length;
					int l = j / this.chars.size();
					float f = (float)this.height / (float)l;
					Int2ObjectMap<BitmapProvider.Glyph> int2ObjectMap = new Int2ObjectOpenHashMap<>();

					for(int m = 0; m < this.chars.size(); ++m) {
						int n = 0;

						for(int o : (int[])this.chars.get(m)) {
							int p = n++;
							if (o != 0 && o != 32) {
								int q = this.getActualGlyphWidth(nativeImage, k, l, p, m);
								BitmapProvider.Glyph glyph = int2ObjectMap.put(
									o, new BitmapProvider.Glyph(f, nativeImage, p * k, m * l, k, l, (int)(0.5 + (double)((float)q * f)) + 1, this.ascent)
								);
								if (glyph != null) {
									BitmapProvider.LOGGER.warn("Codepoint '{}' declared multiple times in {}", Integer.toHexString(o), this.texture);
								}
							}
						}
					}

					var31 = new BitmapProvider(nativeImage, int2ObjectMap);
				} catch (Throwable var28) {
					var3 = var28;
					throw var28;
				} finally {
					if (resource != null) {
						if (var3 != null) {
							try {
								resource.close();
							} catch (Throwable var27) {
								var3.addSuppressed(var27);
							}
						} else {
							resource.close();
						}
					}
				}

				return var31;
			} catch (IOException var30) {
				throw new RuntimeException(var30.getMessage());
			}
		}

		private int getActualGlyphWidth(NativeImage nativeImage, int i, int j, int k, int l) {
			int m;
			for(m = i - 1; m >= 0; --m) {
				int n = k * i + m;

				for(int o = 0; o < j; ++o) {
					int p = l * j + o;
					if (nativeImage.getLuminanceOrAlpha(n, p) != 0) {
						return m + 1;
					}
				}
			}

			return m + 1;
		}
	}

	@Environment(EnvType.CLIENT)
	static final class Glyph implements RawGlyph {
		private final float scale;
		private final NativeImage image;
		private final int offsetX;
		private final int offsetY;
		private final int width;
		private final int height;
		private final int advance;
		private final int ascent;

		private Glyph(float f, NativeImage nativeImage, int i, int j, int k, int l, int m, int n) {
			this.scale = f;
			this.image = nativeImage;
			this.offsetX = i;
			this.offsetY = j;
			this.width = k;
			this.height = l;
			this.advance = m;
			this.ascent = n;
		}

		@Override
		public float getOversample() {
			return 1.0F / this.scale;
		}

		@Override
		public int getPixelWidth() {
			return this.width;
		}

		@Override
		public int getPixelHeight() {
			return this.height;
		}

		@Override
		public float getAdvance() {
			return (float)this.advance;
		}

		@Override
		public float getBearingY() {
			return RawGlyph.super.getBearingY() + 7.0F - (float)this.ascent;
		}

		@Override
		public void upload(int i, int j) {
			this.image.upload(0, i, j, this.offsetX, this.offsetY, this.width, this.height, false, false);
		}

		@Override
		public boolean isColored() {
			return this.image.format().components() > 1;
		}
	}
}
