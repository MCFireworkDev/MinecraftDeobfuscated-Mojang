package net.minecraft.client.gui.font;

import com.google.common.collect.Lists;
import com.google.common.collect.Sets;
import com.mojang.blaze3d.font.GlyphInfo;
import com.mojang.blaze3d.font.GlyphProvider;
import com.mojang.blaze3d.font.SheetGlyphInfo;
import it.unimi.dsi.fastutil.ints.Int2ObjectFunction;
import it.unimi.dsi.fastutil.ints.Int2ObjectMap;
import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.ints.IntArrayList;
import it.unimi.dsi.fastutil.ints.IntList;
import it.unimi.dsi.fastutil.ints.IntOpenHashSet;
import it.unimi.dsi.fastutil.ints.IntSet;
import java.util.List;
import java.util.Set;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.gui.font.glyphs.BakedGlyph;
import net.minecraft.client.gui.font.glyphs.SpecialGlyphs;
import net.minecraft.client.renderer.texture.TextureManager;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;

@Environment(EnvType.CLIENT)
public class FontSet implements AutoCloseable {
	private static final RandomSource RANDOM = RandomSource.create();
	private static final float LARGE_FORWARD_ADVANCE = 32.0F;
	private final TextureManager textureManager;
	private final ResourceLocation name;
	private BakedGlyph missingGlyph;
	private BakedGlyph whiteGlyph;
	private final List<GlyphProvider> providers = Lists.<GlyphProvider>newArrayList();
	private final CodepointMap<BakedGlyph> glyphs = new CodepointMap<>(i -> new BakedGlyph[i], i -> new BakedGlyph[i][]);
	private final CodepointMap<FontSet.GlyphInfoFilter> glyphInfos = new CodepointMap(i -> new FontSet.GlyphInfoFilter[i], i -> new FontSet.GlyphInfoFilter[i][]);
	private final Int2ObjectMap<IntList> glyphsByWidth = new Int2ObjectOpenHashMap<>();
	private final List<FontTexture> textures = Lists.<FontTexture>newArrayList();

	public FontSet(TextureManager textureManager, ResourceLocation resourceLocation) {
		this.textureManager = textureManager;
		this.name = resourceLocation;
	}

	public void reload(List<GlyphProvider> list) {
		this.closeProviders();
		this.closeTextures();
		this.glyphs.clear();
		this.glyphInfos.clear();
		this.glyphsByWidth.clear();
		this.missingGlyph = SpecialGlyphs.MISSING.bake(this::stitch);
		this.whiteGlyph = SpecialGlyphs.WHITE.bake(this::stitch);
		IntSet intSet = new IntOpenHashSet();

		for(GlyphProvider glyphProvider : list) {
			intSet.addAll(glyphProvider.getSupportedGlyphs());
		}

		Set<GlyphProvider> set = Sets.<GlyphProvider>newHashSet();
		intSet.forEach(i -> {
			for(GlyphProvider glyphProviderxx : list) {
				GlyphInfo glyphInfo = glyphProviderxx.getGlyph(i);
				if (glyphInfo != null) {
					set.add(glyphProviderxx);
					if (glyphInfo != SpecialGlyphs.MISSING) {
						this.glyphsByWidth.computeIfAbsent(Mth.ceil(glyphInfo.getAdvance(false)), (Int2ObjectFunction<? extends IntList>)(ix -> new IntArrayList())).add(i);
					}
					break;
				}
			}
		});
		list.stream().filter(set::contains).forEach(this.providers::add);
	}

	public void close() {
		this.closeProviders();
		this.closeTextures();
	}

	private void closeProviders() {
		for(GlyphProvider glyphProvider : this.providers) {
			glyphProvider.close();
		}

		this.providers.clear();
	}

	private void closeTextures() {
		for(FontTexture fontTexture : this.textures) {
			fontTexture.close();
		}

		this.textures.clear();
	}

	private static boolean hasFishyAdvance(GlyphInfo glyphInfo) {
		float f = glyphInfo.getAdvance(false);
		if (!(f < 0.0F) && !(f > 32.0F)) {
			float g = glyphInfo.getAdvance(true);
			return g < 0.0F || g > 32.0F;
		} else {
			return true;
		}
	}

	private FontSet.GlyphInfoFilter computeGlyphInfo(int i) {
		GlyphInfo glyphInfo = null;

		for(GlyphProvider glyphProvider : this.providers) {
			GlyphInfo glyphInfo2 = glyphProvider.getGlyph(i);
			if (glyphInfo2 != null) {
				if (glyphInfo == null) {
					glyphInfo = glyphInfo2;
				}

				if (!hasFishyAdvance(glyphInfo2)) {
					return new FontSet.GlyphInfoFilter(glyphInfo, glyphInfo2);
				}
			}
		}

		return glyphInfo != null ? new FontSet.GlyphInfoFilter(glyphInfo, SpecialGlyphs.MISSING) : FontSet.GlyphInfoFilter.MISSING;
	}

	public GlyphInfo getGlyphInfo(int i, boolean bl) {
		return ((FontSet.GlyphInfoFilter)this.glyphInfos.computeIfAbsent(i, this::computeGlyphInfo)).select(bl);
	}

	private BakedGlyph computeBakedGlyph(int i) {
		for(GlyphProvider glyphProvider : this.providers) {
			GlyphInfo glyphInfo = glyphProvider.getGlyph(i);
			if (glyphInfo != null) {
				return glyphInfo.bake(this::stitch);
			}
		}

		return this.missingGlyph;
	}

	public BakedGlyph getGlyph(int i) {
		return this.glyphs.computeIfAbsent(i, this::computeBakedGlyph);
	}

	private BakedGlyph stitch(SheetGlyphInfo sheetGlyphInfo) {
		for(FontTexture fontTexture : this.textures) {
			BakedGlyph bakedGlyph = fontTexture.add(sheetGlyphInfo);
			if (bakedGlyph != null) {
				return bakedGlyph;
			}
		}

		ResourceLocation resourceLocation = this.name.withSuffix("/" + this.textures.size());
		boolean bl = sheetGlyphInfo.isColored();
		GlyphRenderTypes glyphRenderTypes = bl
			? GlyphRenderTypes.createForColorTexture(resourceLocation)
			: GlyphRenderTypes.createForIntensityTexture(resourceLocation);
		FontTexture fontTexture2 = new FontTexture(glyphRenderTypes, bl);
		this.textures.add(fontTexture2);
		this.textureManager.register(resourceLocation, fontTexture2);
		BakedGlyph bakedGlyph2 = fontTexture2.add(sheetGlyphInfo);
		return bakedGlyph2 == null ? this.missingGlyph : bakedGlyph2;
	}

	public BakedGlyph getRandomGlyph(GlyphInfo glyphInfo) {
		IntList intList = this.glyphsByWidth.get(Mth.ceil(glyphInfo.getAdvance(false)));
		return intList != null && !intList.isEmpty() ? this.getGlyph(intList.getInt(RANDOM.nextInt(intList.size()))) : this.missingGlyph;
	}

	public BakedGlyph whiteGlyph() {
		return this.whiteGlyph;
	}

	@Environment(EnvType.CLIENT)
	static record GlyphInfoFilter(GlyphInfo glyphInfo, GlyphInfo glyphInfoNotFishy) {
		static final FontSet.GlyphInfoFilter MISSING = new FontSet.GlyphInfoFilter(SpecialGlyphs.MISSING, SpecialGlyphs.MISSING);

		GlyphInfo select(boolean bl) {
			return bl ? this.glyphInfoNotFishy : this.glyphInfo;
		}
	}
}
