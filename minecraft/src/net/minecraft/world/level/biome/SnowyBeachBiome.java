package net.minecraft.world.level.biome;

import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.level.levelgen.surfacebuilders.SurfaceBuilder;

public final class SnowyBeachBiome extends Biome {
	public SnowyBeachBiome() {
		super(
			new Biome.BiomeBuilder()
				.surfaceBuilder(SurfaceBuilder.DEFAULT, SurfaceBuilder.CONFIG_DESERT)
				.precipitation(Biome.Precipitation.SNOW)
				.biomeCategory(Biome.BiomeCategory.BEACH)
				.depth(0.0F)
				.scale(0.025F)
				.temperature(0.05F)
				.downfall(0.3F)
				.specialEffects(
					new BiomeSpecialEffects.Builder()
						.waterColor(4020182)
						.waterFogColor(329011)
						.fogColor(12638463)
						.ambientMoodSound(AmbientMoodSettings.LEGACY_CAVE_SETTINGS)
						.build()
				)
				.parent(null)
		);
		this.addStructureStart(BiomeDefaultFeatures.MINESHAFT);
		this.addStructureStart(BiomeDefaultFeatures.BURIED_TREASURE);
		this.addStructureStart(BiomeDefaultFeatures.SHIPWRECH_BEACHED);
		this.addStructureStart(BiomeDefaultFeatures.RUINED_PORTAL_STANDARD);
		BiomeDefaultFeatures.addDefaultCarvers(this);
		BiomeDefaultFeatures.addDefaultLakes(this);
		BiomeDefaultFeatures.addDefaultMonsterRoom(this);
		BiomeDefaultFeatures.addDefaultUndergroundVariety(this);
		BiomeDefaultFeatures.addDefaultOres(this);
		BiomeDefaultFeatures.addDefaultSoftDisks(this);
		BiomeDefaultFeatures.addDefaultFlowers(this);
		BiomeDefaultFeatures.addDefaultGrass(this);
		BiomeDefaultFeatures.addDefaultMushrooms(this);
		BiomeDefaultFeatures.addDefaultExtraVegetation(this);
		BiomeDefaultFeatures.addDefaultSprings(this);
		BiomeDefaultFeatures.addSurfaceFreezing(this);
		this.addSpawn(MobCategory.AMBIENT, new Biome.SpawnerData(EntityType.BAT, 10, 8, 8));
		this.addSpawn(MobCategory.MONSTER, new Biome.SpawnerData(EntityType.SPIDER, 100, 4, 4));
		this.addSpawn(MobCategory.MONSTER, new Biome.SpawnerData(EntityType.ZOMBIE, 95, 4, 4));
		this.addSpawn(MobCategory.MONSTER, new Biome.SpawnerData(EntityType.ZOMBIE_VILLAGER, 5, 1, 1));
		this.addSpawn(MobCategory.MONSTER, new Biome.SpawnerData(EntityType.SKELETON, 100, 4, 4));
		this.addSpawn(MobCategory.MONSTER, new Biome.SpawnerData(EntityType.CREEPER, 100, 4, 4));
		this.addSpawn(MobCategory.MONSTER, new Biome.SpawnerData(EntityType.SLIME, 100, 4, 4));
		this.addSpawn(MobCategory.MONSTER, new Biome.SpawnerData(EntityType.ENDERMAN, 10, 1, 4));
		this.addSpawn(MobCategory.MONSTER, new Biome.SpawnerData(EntityType.WITCH, 5, 1, 1));
	}
}
