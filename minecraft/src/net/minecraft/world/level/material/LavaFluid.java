package net.minecraft.world.level.material;

import java.util.Optional;
import javax.annotation.Nullable;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.FluidTags;
import net.minecraft.util.RandomSource;
import net.minecraft.voting.rules.Rules;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.GameRules;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.BaseFireBlock;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.LiquidBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;

public abstract class LavaFluid extends FlowingFluid {
	public static final float MIN_LEVEL_CUTOFF = 0.44444445F;

	@Override
	public Fluid getFlowing() {
		return Fluids.FLOWING_LAVA;
	}

	@Override
	public Fluid getSource() {
		return Fluids.LAVA;
	}

	@Override
	public Item getBucket() {
		return Items.LAVA_BUCKET;
	}

	@Override
	public void animateTick(Level level, BlockPos blockPos, FluidState fluidState, RandomSource randomSource) {
		BlockPos blockPos2 = blockPos.above();
		if (level.getBlockState(blockPos2).isAir() && !level.getBlockState(blockPos2).isSolidRender(level, blockPos2)) {
			if (randomSource.nextInt(100) == 0) {
				double d = (double)blockPos.getX() + randomSource.nextDouble();
				double e = (double)blockPos.getY() + 1.0;
				double f = (double)blockPos.getZ() + randomSource.nextDouble();
				level.addParticle(ParticleTypes.LAVA, d, e, f, 0.0, 0.0, 0.0);
				level.playLocalSound(
					d, e, f, SoundEvents.LAVA_POP, SoundSource.BLOCKS, 0.2F + randomSource.nextFloat() * 0.2F, 0.9F + randomSource.nextFloat() * 0.15F, false
				);
			}

			if (randomSource.nextInt(200) == 0) {
				level.playLocalSound(
					(double)blockPos.getX(),
					(double)blockPos.getY(),
					(double)blockPos.getZ(),
					SoundEvents.LAVA_AMBIENT,
					SoundSource.BLOCKS,
					0.2F + randomSource.nextFloat() * 0.2F,
					0.9F + randomSource.nextFloat() * 0.15F,
					false
				);
			}
		}
	}

	@Override
	public void randomTick(Level level, BlockPos blockPos, FluidState fluidState, RandomSource randomSource) {
		if (level.getGameRules().getBoolean(GameRules.RULE_DOFIRETICK)) {
			int i = randomSource.nextInt(3);
			if (i > 0) {
				BlockPos blockPos2 = blockPos;

				for(int j = 0; j < i; ++j) {
					blockPos2 = blockPos2.offset(randomSource.nextInt(3) - 1, 1, randomSource.nextInt(3) - 1);
					if (!level.isLoaded(blockPos2)) {
						return;
					}

					BlockState blockState = level.getBlockState(blockPos2);
					if (blockState.isAir()) {
						if (this.hasFlammableNeighbours(level, blockPos2)) {
							level.setBlockAndUpdate(blockPos2, BaseFireBlock.getState(level, blockPos2));
							return;
						}
					} else if (blockState.getMaterial().blocksMotion()) {
						return;
					}
				}
			} else {
				for(int k = 0; k < 3; ++k) {
					BlockPos blockPos3 = blockPos.offset(randomSource.nextInt(3) - 1, 0, randomSource.nextInt(3) - 1);
					if (!level.isLoaded(blockPos3)) {
						return;
					}

					if (level.isEmptyBlock(blockPos3.above()) && this.isFlammable(level, blockPos3)) {
						level.setBlockAndUpdate(blockPos3.above(), BaseFireBlock.getState(level, blockPos3));
					}
				}
			}
		}
	}

	private boolean hasFlammableNeighbours(LevelReader levelReader, BlockPos blockPos) {
		for(Direction direction : Direction.values()) {
			if (this.isFlammable(levelReader, blockPos.relative(direction))) {
				return true;
			}
		}

		return false;
	}

	private boolean isFlammable(LevelReader levelReader, BlockPos blockPos) {
		return blockPos.getY() >= levelReader.getMinBuildHeight() && blockPos.getY() < levelReader.getMaxBuildHeight() && !levelReader.hasChunkAt(blockPos)
			? false
			: levelReader.getBlockState(blockPos).ignitedByLava();
	}

	@Nullable
	@Override
	public ParticleOptions getDripParticle() {
		return ParticleTypes.DRIPPING_LAVA;
	}

	@Override
	protected void beforeDestroyingBlock(LevelAccessor levelAccessor, BlockPos blockPos, BlockState blockState) {
		this.fizz(levelAccessor, blockPos);
	}

	@Override
	public int getSlopeFindDistance(LevelReader levelReader) {
		return levelReader.dimensionType().ultraWarm() ? 4 : 2;
	}

	@Override
	public BlockState createLegacyBlock(FluidState fluidState) {
		return Blocks.LAVA.defaultBlockState().setValue(LiquidBlock.LEVEL, Integer.valueOf(getLegacyLevel(fluidState)));
	}

	@Override
	public boolean isSame(Fluid fluid) {
		return fluid == Fluids.LAVA || fluid == Fluids.FLOWING_LAVA;
	}

	@Override
	public int getDropOff(LevelReader levelReader) {
		if (Rules.UNCONTROLABLE_LAVE.get()) {
			return 0;
		} else {
			return levelReader.dimensionType().ultraWarm() ? 1 : 2;
		}
	}

	@Override
	public boolean canBeReplacedWith(FluidState fluidState, BlockGetter blockGetter, BlockPos blockPos, Fluid fluid, Direction direction) {
		return fluidState.getHeight(blockGetter, blockPos) >= 0.44444445F && fluid.is(FluidTags.WATER);
	}

	@Override
	public int getTickDelay(LevelReader levelReader) {
		int i = Rules.LAVA_SPREAD_TICK_DELAY.get();
		if (i != 30) {
			return i;
		} else {
			return levelReader.dimensionType().ultraWarm() ? 10 : 30;
		}
	}

	@Override
	public int getSpreadDelay(Level level, BlockPos blockPos, FluidState fluidState, FluidState fluidState2) {
		int i = this.getTickDelay(level);
		if (!fluidState.isEmpty()
			&& !fluidState2.isEmpty()
			&& !fluidState.getValue(FALLING)
			&& !fluidState2.getValue(FALLING)
			&& fluidState2.getHeight(level, blockPos) > fluidState.getHeight(level, blockPos)
			&& level.getRandom().nextInt(4) != 0) {
			i *= 4;
		}

		return i;
	}

	private void fizz(LevelAccessor levelAccessor, BlockPos blockPos) {
		levelAccessor.levelEvent(1501, blockPos, 0);
	}

	@Override
	protected boolean canConvertToSource(Level level) {
		return level.getGameRules().getBoolean(GameRules.RULE_LAVA_SOURCE_CONVERSION);
	}

	@Override
	protected void spreadTo(LevelAccessor levelAccessor, BlockPos blockPos, BlockState blockState, Direction direction, FluidState fluidState) {
		if (direction == Direction.DOWN) {
			FluidState fluidState2 = levelAccessor.getFluidState(blockPos);
			if (this.is(FluidTags.LAVA) && fluidState2.is(FluidTags.WATER)) {
				if (blockState.getBlock() instanceof LiquidBlock) {
					levelAccessor.setBlock(blockPos, Rules.STONE_GEN_REPLACE.get().defaultBlockState(), 3);
				}

				this.fizz(levelAccessor, blockPos);
				return;
			}
		}

		super.spreadTo(levelAccessor, blockPos, blockState, direction, fluidState);
	}

	@Override
	protected boolean isRandomlyTicking() {
		return true;
	}

	@Override
	protected float getExplosionResistance() {
		return 100.0F;
	}

	@Override
	public Optional<SoundEvent> getPickupSound() {
		return Optional.of(SoundEvents.BUCKET_FILL_LAVA);
	}

	public static class Flowing extends LavaFluid {
		@Override
		protected void createFluidStateDefinition(StateDefinition.Builder<Fluid, FluidState> builder) {
			super.createFluidStateDefinition(builder);
			builder.add(LEVEL);
		}

		@Override
		public int getAmount(FluidState fluidState) {
			return fluidState.getValue(LEVEL);
		}

		@Override
		public boolean isSource(FluidState fluidState) {
			return false;
		}
	}

	public static class Source extends LavaFluid {
		@Override
		public int getAmount(FluidState fluidState) {
			return 8;
		}

		@Override
		public boolean isSource(FluidState fluidState) {
			return true;
		}
	}
}
