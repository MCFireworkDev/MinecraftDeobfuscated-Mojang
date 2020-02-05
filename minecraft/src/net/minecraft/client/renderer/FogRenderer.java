package net.minecraft.client.renderer;

import com.mojang.blaze3d.platform.GlStateManager;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.math.Vector3f;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.Util;
import net.minecraft.client.Camera;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.BlockPos;
import net.minecraft.tags.FluidTags;
import net.minecraft.util.Mth;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.biome.Biomes;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.level.material.Fluids;
import net.minecraft.world.phys.Vec3;

@Environment(EnvType.CLIENT)
public class FogRenderer {
	private static float fogRed;
	private static float fogGreen;
	private static float fogBlue;
	private static int targetBiomeFog = -1;
	private static int previousBiomeFog = -1;
	private static long biomeChangedTime = -1L;

	public static void setupColor(Camera camera, float f, ClientLevel clientLevel, int i, float g) {
		FluidState fluidState = camera.getFluidInCamera();
		if (fluidState.is(FluidTags.WATER)) {
			long l = Util.getMillis();
			int j = clientLevel.getBiome(new BlockPos(camera.getPosition())).getWaterFogColor();
			if (biomeChangedTime < 0L) {
				targetBiomeFog = j;
				previousBiomeFog = j;
				biomeChangedTime = l;
			}

			int k = targetBiomeFog >> 16 & 0xFF;
			int m = targetBiomeFog >> 8 & 0xFF;
			int n = targetBiomeFog & 0xFF;
			int o = previousBiomeFog >> 16 & 0xFF;
			int p = previousBiomeFog >> 8 & 0xFF;
			int q = previousBiomeFog & 0xFF;
			float h = Mth.clamp((float)(l - biomeChangedTime) / 5000.0F, 0.0F, 1.0F);
			float r = Mth.lerp(h, (float)o, (float)k);
			float s = Mth.lerp(h, (float)p, (float)m);
			float t = Mth.lerp(h, (float)q, (float)n);
			fogRed = r / 255.0F;
			fogGreen = s / 255.0F;
			fogBlue = t / 255.0F;
			if (targetBiomeFog != j) {
				targetBiomeFog = j;
				previousBiomeFog = Mth.floor(r) << 16 | Mth.floor(s) << 8 | Mth.floor(t);
				biomeChangedTime = l;
			}
		} else if (fluidState.is(FluidTags.LAVA)) {
			fogRed = 0.6F;
			fogGreen = 0.1F;
			fogBlue = 0.0F;
			biomeChangedTime = -1L;
		} else {
			float u = 0.25F + 0.75F * (float)i / 32.0F;
			u = 1.0F - (float)Math.pow((double)u, 0.25);
			Vec3 vec3 = clientLevel.getSkyColor(camera.getBlockPosition(), f);
			float v = (float)vec3.x;
			float w = (float)vec3.y;
			float x = (float)vec3.z;
			int n = clientLevel.getBiome(camera.getBlockPosition()).getFogColor();
			float y = Mth.cos(clientLevel.getTimeOfDay(f) * (float) (Math.PI * 2)) * 2.0F + 0.5F;
			Vec3 vec32 = clientLevel.getDimension().getBrightnessDependentFogColor(n, Mth.clamp(y, 0.0F, 1.0F));
			fogRed = (float)vec32.x;
			fogGreen = (float)vec32.y;
			fogBlue = (float)vec32.z;
			if (i >= 4) {
				float z = Mth.sin(clientLevel.getSunAngle(f)) > 0.0F ? -1.0F : 1.0F;
				Vector3f vector3f = new Vector3f(z, 0.0F, 0.0F);
				float r = camera.getLookVector().dot(vector3f);
				if (r < 0.0F) {
					r = 0.0F;
				}

				if (r > 0.0F) {
					float[] fs = clientLevel.dimension.getSunriseColor(clientLevel.getTimeOfDay(f), f);
					if (fs != null) {
						r *= fs[3];
						fogRed = fogRed * (1.0F - r) + fs[0] * r;
						fogGreen = fogGreen * (1.0F - r) + fs[1] * r;
						fogBlue = fogBlue * (1.0F - r) + fs[2] * r;
					}
				}
			}

			fogRed += (v - fogRed) * u;
			fogGreen += (w - fogGreen) * u;
			fogBlue += (x - fogBlue) * u;
			float z = clientLevel.getRainLevel(f);
			if (z > 0.0F) {
				float h = 1.0F - z * 0.5F;
				float r = 1.0F - z * 0.4F;
				fogRed *= h;
				fogGreen *= h;
				fogBlue *= r;
			}

			float h = clientLevel.getThunderLevel(f);
			if (h > 0.0F) {
				float r = 1.0F - h * 0.5F;
				fogRed *= r;
				fogGreen *= r;
				fogBlue *= r;
			}

			biomeChangedTime = -1L;
		}

		double d = camera.getPosition().y * clientLevel.dimension.getClearColorScale();
		if (camera.getEntity() instanceof LivingEntity && ((LivingEntity)camera.getEntity()).hasEffect(MobEffects.BLINDNESS)) {
			int j = ((LivingEntity)camera.getEntity()).getEffect(MobEffects.BLINDNESS).getDuration();
			if (j < 20) {
				d *= (double)(1.0F - (float)j / 20.0F);
			} else {
				d = 0.0;
			}
		}

		if (d < 1.0) {
			if (d < 0.0) {
				d = 0.0;
			}

			d *= d;
			fogRed = (float)((double)fogRed * d);
			fogGreen = (float)((double)fogGreen * d);
			fogBlue = (float)((double)fogBlue * d);
		}

		if (g > 0.0F) {
			fogRed = fogRed * (1.0F - g) + fogRed * 0.7F * g;
			fogGreen = fogGreen * (1.0F - g) + fogGreen * 0.6F * g;
			fogBlue = fogBlue * (1.0F - g) + fogBlue * 0.6F * g;
		}

		if (fluidState.is(FluidTags.WATER)) {
			float v = 0.0F;
			if (camera.getEntity() instanceof LocalPlayer) {
				LocalPlayer localPlayer = (LocalPlayer)camera.getEntity();
				v = localPlayer.getWaterVision();
			}

			float w = Math.min(1.0F / fogRed, Math.min(1.0F / fogGreen, 1.0F / fogBlue));
			fogRed = fogRed * (1.0F - v) + fogRed * w * v;
			fogGreen = fogGreen * (1.0F - v) + fogGreen * w * v;
			fogBlue = fogBlue * (1.0F - v) + fogBlue * w * v;
		} else if (camera.getEntity() instanceof LivingEntity && ((LivingEntity)camera.getEntity()).hasEffect(MobEffects.NIGHT_VISION)) {
			float v = GameRenderer.getNightVisionScale((LivingEntity)camera.getEntity(), f);
			float w = Math.min(1.0F / fogRed, Math.min(1.0F / fogGreen, 1.0F / fogBlue));
			fogRed = fogRed * (1.0F - v) + fogRed * w * v;
			fogGreen = fogGreen * (1.0F - v) + fogGreen * w * v;
			fogBlue = fogBlue * (1.0F - v) + fogBlue * w * v;
		}

		RenderSystem.clearColor(fogRed, fogGreen, fogBlue, 0.0F);
	}

	public static void setupNoFog() {
		RenderSystem.fogDensity(0.0F);
		RenderSystem.fogMode(GlStateManager.FogMode.EXP2);
	}

	public static void setupFog(Camera camera, FogRenderer.FogMode fogMode, float f, boolean bl) {
		FluidState fluidState = camera.getFluidInCamera();
		Entity entity = camera.getEntity();
		boolean bl2 = fluidState.getType() != Fluids.EMPTY;
		if (bl2) {
			float g = 1.0F;
			if (fluidState.is(FluidTags.WATER)) {
				g = 0.05F;
				if (entity instanceof LocalPlayer) {
					LocalPlayer localPlayer = (LocalPlayer)entity;
					g -= localPlayer.getWaterVision() * localPlayer.getWaterVision() * 0.03F;
					Biome biome = localPlayer.level.getBiome(new BlockPos(localPlayer));
					if (biome == Biomes.SWAMP || biome == Biomes.SWAMP_HILLS) {
						g += 0.005F;
					}
				}
			} else if (fluidState.is(FluidTags.LAVA)) {
				g = 2.0F;
			}

			RenderSystem.fogDensity(g);
			RenderSystem.fogMode(GlStateManager.FogMode.EXP2);
		} else {
			float g;
			float j;
			if (entity instanceof LivingEntity && ((LivingEntity)entity).hasEffect(MobEffects.BLINDNESS)) {
				int i = ((LivingEntity)entity).getEffect(MobEffects.BLINDNESS).getDuration();
				float h = Mth.lerp(Math.min(1.0F, (float)i / 20.0F), f, 5.0F);
				if (fogMode == FogRenderer.FogMode.FOG_SKY) {
					g = 0.0F;
					j = h * 0.8F;
				} else {
					g = h * 0.25F;
					j = h;
				}
			} else if (bl) {
				g = f * 0.05F;
				j = Math.min(f, 192.0F) * 0.5F;
			} else if (fogMode == FogRenderer.FogMode.FOG_SKY) {
				g = 0.0F;
				j = f;
			} else {
				g = f * 0.75F;
				j = f;
			}

			RenderSystem.fogStart(g);
			RenderSystem.fogEnd(j);
			RenderSystem.fogMode(GlStateManager.FogMode.LINEAR);
			RenderSystem.setupNvFogDistance();
		}
	}

	public static void levelFogColor() {
		RenderSystem.fog(2918, fogRed, fogGreen, fogBlue, 1.0F);
	}

	@Environment(EnvType.CLIENT)
	public static enum FogMode {
		FOG_SKY,
		FOG_TERRAIN;
	}
}
