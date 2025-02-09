package net.minecraft.world.entity.animal;

import net.minecraft.core.BlockPos;
import net.minecraft.tags.FluidTags;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.MobType;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.pathfinder.BlockPathTypes;

public abstract class WaterAnimal extends PathfinderMob {
	protected WaterAnimal(EntityType<? extends WaterAnimal> entityType, Level level) {
		super(entityType, level);
		this.setPathfindingMalus(BlockPathTypes.WATER, 0.0F);
	}

	@Override
	public MobType getMobType() {
		return MobType.WATER;
	}

	@Override
	public boolean checkSpawnObstruction(LevelReader levelReader) {
		return levelReader.isUnobstructed(this);
	}

	@Override
	public int getAmbientSoundInterval() {
		return 120;
	}

	@Override
	public int getExperienceReward() {
		return 1 + this.level().random.nextInt(3);
	}

	protected void handleAirSupply(int i) {
		if (this.isAlive() && !this.isInWaterOrBubble()) {
			this.setAirSupply(i - 1);
			if (this.getAirSupply() == -20) {
				this.setAirSupply(0);
				this.hurt(this.damageSources().drown(), 2.0F);
			}
		} else {
			this.setAirSupply(300);
		}
	}

	@Override
	public void baseTick() {
		int i = this.getAirSupply();
		super.baseTick();
		this.handleAirSupply(i);
	}

	@Override
	public boolean isPushedByFluid() {
		return false;
	}

	@Override
	public boolean canBeLeashed(Player player) {
		return false;
	}

	public static boolean checkSurfaceWaterAnimalSpawnRules(
		EntityType<? extends WaterAnimal> entityType, LevelAccessor levelAccessor, MobSpawnType mobSpawnType, BlockPos blockPos, RandomSource randomSource
	) {
		int i = levelAccessor.getSeaLevel();
		int j = i - 13;
		return blockPos.getY() >= j
			&& blockPos.getY() <= i
			&& levelAccessor.getFluidState(blockPos.below()).is(FluidTags.WATER)
			&& levelAccessor.getBlockState(blockPos.above()).is(Blocks.WATER);
	}
}
