package net.minecraft.world.level.block;

import java.util.Random;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.BlockParticleOption;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.world.entity.item.FallingBlockEntity;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.Material;

public class FallingBlock extends Block {
	public FallingBlock(Block.Properties properties) {
		super(properties);
	}

	@Override
	public void onPlace(BlockState blockState, Level level, BlockPos blockPos, BlockState blockState2, boolean bl) {
		level.getBlockTicks().scheduleTick(blockPos, this, this.getTickDelay(level));
	}

	@Override
	public BlockState updateShape(
		BlockState blockState, Direction direction, BlockState blockState2, LevelAccessor levelAccessor, BlockPos blockPos, BlockPos blockPos2
	) {
		levelAccessor.getBlockTicks().scheduleTick(blockPos, this, this.getTickDelay(levelAccessor));
		return super.updateShape(blockState, direction, blockState2, levelAccessor, blockPos, blockPos2);
	}

	@Override
	public void tick(BlockState blockState, Level level, BlockPos blockPos, Random random) {
		if (!level.isClientSide) {
			this.checkSlide(level, blockPos);
		}
	}

	private void checkSlide(Level level, BlockPos blockPos) {
		if (isFree(level.getBlockState(blockPos.below())) && blockPos.getY() >= 0) {
			if (!level.isClientSide) {
				FallingBlockEntity fallingBlockEntity = new FallingBlockEntity(
					level, (double)blockPos.getX() + 0.5, (double)blockPos.getY(), (double)blockPos.getZ() + 0.5, level.getBlockState(blockPos)
				);
				this.falling(fallingBlockEntity);
				level.addFreshEntity(fallingBlockEntity);
			}
		}
	}

	protected void falling(FallingBlockEntity fallingBlockEntity) {
	}

	@Override
	public int getTickDelay(LevelReader levelReader) {
		return 2;
	}

	public static boolean isFree(BlockState blockState) {
		Block block = blockState.getBlock();
		Material material = blockState.getMaterial();
		return blockState.isAir() || block == Blocks.FIRE || material.isLiquid() || material.isReplaceable();
	}

	public void onLand(Level level, BlockPos blockPos, BlockState blockState, BlockState blockState2) {
	}

	public void onBroken(Level level, BlockPos blockPos) {
	}

	@Environment(EnvType.CLIENT)
	@Override
	public void animateTick(BlockState blockState, Level level, BlockPos blockPos, Random random) {
		if (random.nextInt(16) == 0) {
			BlockPos blockPos2 = blockPos.below();
			if (isFree(level.getBlockState(blockPos2))) {
				double d = (double)((float)blockPos.getX() + random.nextFloat());
				double e = (double)blockPos.getY() - 0.05;
				double f = (double)((float)blockPos.getZ() + random.nextFloat());
				level.addParticle(new BlockParticleOption(ParticleTypes.FALLING_DUST, blockState), d, e, f, 0.0, 0.0, 0.0);
			}
		}
	}

	@Environment(EnvType.CLIENT)
	public int getDustColor(BlockState blockState) {
		return -16777216;
	}
}
