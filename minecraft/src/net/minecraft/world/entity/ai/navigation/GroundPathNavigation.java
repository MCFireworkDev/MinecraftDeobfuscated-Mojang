package net.minecraft.world.entity.ai.navigation;

import net.minecraft.core.BlockPos;
import net.minecraft.core.SectionPos;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.level.pathfinder.BlockPathTypes;
import net.minecraft.world.level.pathfinder.Node;
import net.minecraft.world.level.pathfinder.Path;
import net.minecraft.world.level.pathfinder.PathFinder;
import net.minecraft.world.level.pathfinder.WalkNodeEvaluator;
import net.minecraft.world.phys.Vec3;

public class GroundPathNavigation extends PathNavigation {
	private boolean avoidSun;

	public GroundPathNavigation(Mob mob, Level level) {
		super(mob, level);
	}

	@Override
	protected PathFinder createPathFinder(int i) {
		this.nodeEvaluator = new WalkNodeEvaluator();
		this.nodeEvaluator.setCanPassDoors(true);
		return new PathFinder(this.nodeEvaluator, i);
	}

	@Override
	protected boolean canUpdatePath() {
		return this.mob.onGround() || this.mob.isInLiquid() || this.mob.isPassenger();
	}

	@Override
	protected Vec3 getTempMobPos() {
		return new Vec3(this.mob.getX(), (double)this.getSurfaceY(), this.mob.getZ());
	}

	@Override
	public Path createPath(BlockPos blockPos, int i) {
		LevelChunk levelChunk = this.level
			.getChunkSource()
			.getChunkNow(SectionPos.blockToSectionCoord(blockPos.getX()), SectionPos.blockToSectionCoord(blockPos.getZ()));
		if (levelChunk == null) {
			return null;
		} else {
			if (levelChunk.getBlockState(blockPos).isAir()) {
				BlockPos blockPos2 = blockPos.below();

				while(blockPos2.getY() > this.level.getMinBuildHeight() && levelChunk.getBlockState(blockPos2).isAir()) {
					blockPos2 = blockPos2.below();
				}

				if (blockPos2.getY() > this.level.getMinBuildHeight()) {
					return super.createPath(blockPos2.above(), i);
				}

				while(blockPos2.getY() < this.level.getMaxBuildHeight() && levelChunk.getBlockState(blockPos2).isAir()) {
					blockPos2 = blockPos2.above();
				}

				blockPos = blockPos2;
			}

			if (!levelChunk.getBlockState(blockPos).isSolid()) {
				return super.createPath(blockPos, i);
			} else {
				BlockPos blockPos2 = blockPos.above();

				while(blockPos2.getY() < this.level.getMaxBuildHeight() && levelChunk.getBlockState(blockPos2).isSolid()) {
					blockPos2 = blockPos2.above();
				}

				return super.createPath(blockPos2, i);
			}
		}
	}

	@Override
	public Path createPath(Entity entity, int i) {
		return this.createPath(entity.blockPosition(), i);
	}

	private int getSurfaceY() {
		if (this.mob.isInWater() && this.canFloat()) {
			int i = this.mob.getBlockY();
			BlockState blockState = this.level.getBlockState(BlockPos.containing(this.mob.getX(), (double)i, this.mob.getZ()));
			int j = 0;

			while(blockState.is(Blocks.WATER)) {
				blockState = this.level.getBlockState(BlockPos.containing(this.mob.getX(), (double)(++i), this.mob.getZ()));
				if (++j > 16) {
					return this.mob.getBlockY();
				}
			}

			return i;
		} else {
			return Mth.floor(this.mob.getY() + 0.5);
		}
	}

	@Override
	protected void trimPath() {
		super.trimPath();
		if (this.avoidSun) {
			if (this.level.canSeeSky(BlockPos.containing(this.mob.getX(), this.mob.getY() + 0.5, this.mob.getZ()))) {
				return;
			}

			for(int i = 0; i < this.path.getNodeCount(); ++i) {
				Node node = this.path.getNode(i);
				if (this.level.canSeeSky(new BlockPos(node.x, node.y, node.z))) {
					this.path.truncateNodes(i);
					return;
				}
			}
		}
	}

	protected boolean hasValidPathType(BlockPathTypes blockPathTypes) {
		if (blockPathTypes == BlockPathTypes.WATER) {
			return false;
		} else if (blockPathTypes == BlockPathTypes.LAVA) {
			return false;
		} else {
			return blockPathTypes != BlockPathTypes.OPEN;
		}
	}

	public void setCanOpenDoors(boolean bl) {
		this.nodeEvaluator.setCanOpenDoors(bl);
	}

	public boolean canPassDoors() {
		return this.nodeEvaluator.canPassDoors();
	}

	public void setCanPassDoors(boolean bl) {
		this.nodeEvaluator.setCanPassDoors(bl);
	}

	public boolean canOpenDoors() {
		return this.nodeEvaluator.canPassDoors();
	}

	public void setAvoidSun(boolean bl) {
		this.avoidSun = bl;
	}

	public void setCanWalkOverFences(boolean bl) {
		this.nodeEvaluator.setCanWalkOverFences(bl);
	}
}
