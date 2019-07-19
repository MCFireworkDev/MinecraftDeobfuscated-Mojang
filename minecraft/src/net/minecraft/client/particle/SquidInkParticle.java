package net.minecraft.client.particle;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.SimpleParticleType;
import net.minecraft.world.level.Level;

@Environment(EnvType.CLIENT)
public class SquidInkParticle extends SimpleAnimatedParticle {
	private SquidInkParticle(Level level, double d, double e, double f, double g, double h, double i, SpriteSet spriteSet) {
		super(level, d, e, f, spriteSet, 0.0F);
		this.quadSize = 0.5F;
		this.setAlpha(1.0F);
		this.setColor(0.0F, 0.0F, 0.0F);
		this.lifetime = (int)((double)(this.quadSize * 12.0F) / (Math.random() * 0.8F + 0.2F));
		this.setSpriteFromAge(spriteSet);
		this.hasPhysics = false;
		this.xd = g;
		this.yd = h;
		this.zd = i;
		this.setBaseAirFriction(0.0F);
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
			if (this.age > this.lifetime / 2) {
				this.setAlpha(1.0F - ((float)this.age - (float)(this.lifetime / 2)) / (float)this.lifetime);
			}

			this.move(this.xd, this.yd, this.zd);
			if (this.level.getBlockState(new BlockPos(this.x, this.y, this.z)).isAir()) {
				this.yd -= 0.008F;
			}

			this.xd *= 0.92F;
			this.yd *= 0.92F;
			this.zd *= 0.92F;
			if (this.onGround) {
				this.xd *= 0.7F;
				this.zd *= 0.7F;
			}
		}
	}

	@Environment(EnvType.CLIENT)
	public static class Provider implements ParticleProvider<SimpleParticleType> {
		private final SpriteSet sprites;

		public Provider(SpriteSet spriteSet) {
			this.sprites = spriteSet;
		}

		public Particle createParticle(SimpleParticleType simpleParticleType, Level level, double d, double e, double f, double g, double h, double i) {
			return new SquidInkParticle(level, d, e, f, g, h, i, this.sprites);
		}
	}
}
