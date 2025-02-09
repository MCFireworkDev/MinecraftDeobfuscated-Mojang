package net.minecraft.world.level.block;

import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import com.mojang.serialization.codecs.RecordCodecBuilder.Instance;
import java.util.Optional;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.BlockTags;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.feature.ConfiguredFeature;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;

public class MushroomBlock extends BushBlock implements BonemealableBlock {
	public static final MapCodec<MushroomBlock> CODEC = RecordCodecBuilder.mapCodec(
		instance -> instance.group(
					ResourceKey.codec(Registries.CONFIGURED_FEATURE).fieldOf("feature").forGetter(mushroomBlock -> mushroomBlock.feature), propertiesCodec()
				)
				.apply(instance, MushroomBlock::new)
	);
	protected static final float AABB_OFFSET = 3.0F;
	protected static final VoxelShape SHAPE = Block.box(5.0, 0.0, 5.0, 11.0, 6.0, 11.0);
	private final ResourceKey<ConfiguredFeature<?, ?>> feature;

	@Override
	public MapCodec<MushroomBlock> codec() {
		return CODEC;
	}

	public MushroomBlock(ResourceKey<ConfiguredFeature<?, ?>> resourceKey, BlockBehaviour.Properties properties) {
		super(properties);
		this.feature = resourceKey;
	}

	@Override
	public VoxelShape getShape(BlockState blockState, BlockGetter blockGetter, BlockPos blockPos, CollisionContext collisionContext) {
		return SHAPE;
	}

	@Override
	public void randomTick(BlockState blockState, ServerLevel serverLevel, BlockPos blockPos, RandomSource randomSource) {
		if (randomSource.nextInt(25) == 0) {
			int i = 5;
			int j = 4;

			for(BlockPos blockPos2 : BlockPos.betweenClosed(blockPos.offset(-4, -1, -4), blockPos.offset(4, 1, 4))) {
				if (serverLevel.getBlockState(blockPos2).is(this)) {
					if (--i <= 0) {
						return;
					}
				}
			}

			BlockPos blockPos3 = blockPos.offset(randomSource.nextInt(3) - 1, randomSource.nextInt(2) - randomSource.nextInt(2), randomSource.nextInt(3) - 1);

			for(int k = 0; k < 4; ++k) {
				if (serverLevel.isEmptyBlock(blockPos3) && blockState.canSurvive(serverLevel, blockPos3)) {
					blockPos = blockPos3;
				}

				blockPos3 = blockPos.offset(randomSource.nextInt(3) - 1, randomSource.nextInt(2) - randomSource.nextInt(2), randomSource.nextInt(3) - 1);
			}

			if (serverLevel.isEmptyBlock(blockPos3) && blockState.canSurvive(serverLevel, blockPos3)) {
				serverLevel.setBlock(blockPos3, blockState, 2);
			}
		}
	}

	@Override
	protected boolean mayPlaceOn(BlockState blockState, BlockGetter blockGetter, BlockPos blockPos) {
		return blockState.isSolidRender(blockGetter, blockPos);
	}

	@Override
	public boolean canSurvive(BlockState blockState, LevelReader levelReader, BlockPos blockPos) {
		BlockPos blockPos2 = blockPos.below();
		BlockState blockState2 = levelReader.getBlockState(blockPos2);
		if (blockState2.is(BlockTags.MUSHROOM_GROW_BLOCK)) {
			return true;
		} else {
			return levelReader.getRawBrightness(blockPos, 0) < 13 && this.mayPlaceOn(blockState2, levelReader, blockPos2);
		}
	}

	public boolean growMushroom(ServerLevel serverLevel, BlockPos blockPos, BlockState blockState, RandomSource randomSource) {
		Optional<? extends Holder<ConfiguredFeature<?, ?>>> optional = serverLevel.registryAccess()
			.registryOrThrow(Registries.CONFIGURED_FEATURE)
			.getHolder(this.feature);
		if (optional.isEmpty()) {
			return false;
		} else {
			serverLevel.removeBlock(blockPos, false);
			if (((ConfiguredFeature)((Holder)optional.get()).value()).place(serverLevel, serverLevel.getChunkSource().getGenerator(), randomSource, blockPos)) {
				return true;
			} else {
				serverLevel.setBlock(blockPos, blockState, 3);
				return false;
			}
		}
	}

	@Override
	public boolean isValidBonemealTarget(LevelReader levelReader, BlockPos blockPos, BlockState blockState) {
		return true;
	}

	@Override
	public boolean isBonemealSuccess(Level level, RandomSource randomSource, BlockPos blockPos, BlockState blockState) {
		return (double)randomSource.nextFloat() < 0.4;
	}

	@Override
	public void performBonemeal(ServerLevel serverLevel, RandomSource randomSource, BlockPos blockPos, BlockState blockState) {
		this.growMushroom(serverLevel, blockPos, blockState, randomSource);
	}
}
