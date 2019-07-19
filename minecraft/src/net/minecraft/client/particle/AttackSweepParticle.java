package net.minecraft.client.particle;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.core.particles.SimpleParticleType;
import net.minecraft.world.level.Level;

@Environment(EnvType.CLIENT)
public class AttackSweepParticle extends TextureSheetParticle {
	private final SpriteSet sprites;

	private AttackSweepParticle(Level level, double d, double e, double f, double g, SpriteSet spriteSet) {
		super(level, d, e, f, 0.0, 0.0, 0.0);
		this.sprites = spriteSet;
		this.lifetime = 4;
		float h = this.random.nextFloat() * 0.6F + 0.4F;
		this.rCol = h;
		this.gCol = h;
		this.bCol = h;
		this.quadSize = 1.0F - (float)g * 0.5F;
		this.setSpriteFromAge(spriteSet);
	}

	@Override
	public int getLightColor(float f) {
		return 15728880;
	}

	@Override
	public void tick() {
		this.xo = this.x;
		this.yo = this.y;
		this.zo = this.z;
		if (this.age++ >= this.lifetime) {
			this.remove();
		} else {
			this.setSpriteFromAge(this.sprites);
		}
	}

	@Override
	public ParticleRenderType getRenderType() {
		return ParticleRenderType.PARTICLE_SHEET_LIT;
	}

	@Environment(EnvType.CLIENT)
	public static class Provider implements ParticleProvider<SimpleParticleType> {
		private final SpriteSet sprites;

		public Provider(SpriteSet spriteSet) {
			this.sprites = spriteSet;
		}

		public Particle createParticle(SimpleParticleType simpleParticleType, Level level, double d, double e, double f, double g, double h, double i) {
			return new AttackSweepParticle(level, d, e, f, g, this.sprites);
		}
	}
}
