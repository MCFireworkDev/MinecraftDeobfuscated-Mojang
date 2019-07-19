package net.minecraft.client.particle;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.world.level.Level;

@Environment(EnvType.CLIENT)
public abstract class TextureSheetParticle extends SingleQuadParticle {
	protected TextureAtlasSprite sprite;

	protected TextureSheetParticle(Level level, double d, double e, double f) {
		super(level, d, e, f);
	}

	protected TextureSheetParticle(Level level, double d, double e, double f, double g, double h, double i) {
		super(level, d, e, f, g, h, i);
	}

	protected void setSprite(TextureAtlasSprite textureAtlasSprite) {
		this.sprite = textureAtlasSprite;
	}

	@Override
	protected float getU0() {
		return this.sprite.getU0();
	}

	@Override
	protected float getU1() {
		return this.sprite.getU1();
	}

	@Override
	protected float getV0() {
		return this.sprite.getV0();
	}

	@Override
	protected float getV1() {
		return this.sprite.getV1();
	}

	public void pickSprite(SpriteSet spriteSet) {
		this.setSprite(spriteSet.get(this.random));
	}

	public void setSpriteFromAge(SpriteSet spriteSet) {
		this.setSprite(spriteSet.get(this.age, this.lifetime));
	}
}
