package net.minecraft.world.level.block;

import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.ItemInteractionResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.phys.BlockHitResult;

public class RedStoneOreBlock extends Block {
	public static final MapCodec<RedStoneOreBlock> CODEC = simpleCodec(RedStoneOreBlock::new);
	public static final BooleanProperty LIT = RedstoneTorchBlock.LIT;

	@Override
	public MapCodec<RedStoneOreBlock> codec() {
		return CODEC;
	}

	public RedStoneOreBlock(BlockBehaviour.Properties properties) {
		super(properties);
		this.registerDefaultState(this.defaultBlockState().setValue(LIT, Boolean.valueOf(false)));
	}

	@Override
	public void attack(BlockState blockState, Level level, BlockPos blockPos, Player player) {
		interact(blockState, level, blockPos);
		super.attack(blockState, level, blockPos, player);
	}

	@Override
	public void stepOn(Level level, BlockPos blockPos, BlockState blockState, Entity entity) {
		if (!entity.isSteppingCarefully()) {
			interact(blockState, level, blockPos);
		}

		super.stepOn(level, blockPos, blockState, entity);
	}

	@Override
	public ItemInteractionResult useItemOn(
		ItemStack itemStack, BlockState blockState, Level level, BlockPos blockPos, Player player, InteractionHand interactionHand, BlockHitResult blockHitResult
	) {
		if (level.isClientSide) {
			spawnParticles(level, blockPos);
		} else {
			interact(blockState, level, blockPos);
		}

		return itemStack.getItem() instanceof BlockItem && new BlockPlaceContext(player, interactionHand, itemStack, blockHitResult).canPlace()
			? ItemInteractionResult.SKIP_DEFAULT_BLOCK_INTERACTION
			: ItemInteractionResult.SUCCESS;
	}

	private static void interact(BlockState blockState, Level level, BlockPos blockPos) {
		spawnParticles(level, blockPos);
		if (!blockState.getValue(LIT)) {
			level.setBlock(blockPos, blockState.setValue(LIT, Boolean.valueOf(true)), 3);
		}
	}

	@Override
	public boolean isRandomlyTicking(BlockState blockState) {
		return blockState.getValue(LIT);
	}

	@Override
	public void randomTick(BlockState blockState, ServerLevel serverLevel, BlockPos blockPos, RandomSource randomSource) {
		if (blockState.getValue(LIT)) {
			serverLevel.setBlock(blockPos, blockState.setValue(LIT, Boolean.valueOf(false)), 3);
		}
	}

	@Override
	public void spawnAfterBreak(BlockState blockState, ServerLevel serverLevel, BlockPos blockPos, ItemStack itemStack, boolean bl) {
		super.spawnAfterBreak(blockState, serverLevel, blockPos, itemStack, bl);
		if (bl && EnchantmentHelper.getItemEnchantmentLevel(Enchantments.SILK_TOUCH, itemStack) == 0) {
			int i = 1 + serverLevel.random.nextInt(5);
			this.popExperience(serverLevel, blockPos, i);
		}
	}

	@Override
	public void animateTick(BlockState blockState, Level level, BlockPos blockPos, RandomSource randomSource) {
		if (blockState.getValue(LIT)) {
			spawnParticles(level, blockPos);
		}
	}

	private static void spawnParticles(Level level, BlockPos blockPos) {
		double d = 0.5625;
		RandomSource randomSource = level.random;

		for(Direction direction : Direction.values()) {
			BlockPos blockPos2 = blockPos.relative(direction);
			if (!level.getBlockState(blockPos2).isSolidRender(level, blockPos2)) {
				Direction.Axis axis = direction.getAxis();
				double e = axis == Direction.Axis.X ? 0.5 + 0.5625 * (double)direction.getStepX() : (double)randomSource.nextFloat();
				double f = axis == Direction.Axis.Y ? 0.5 + 0.5625 * (double)direction.getStepY() : (double)randomSource.nextFloat();
				double g = axis == Direction.Axis.Z ? 0.5 + 0.5625 * (double)direction.getStepZ() : (double)randomSource.nextFloat();
				level.addParticle(DustParticleOptions.REDSTONE, (double)blockPos.getX() + e, (double)blockPos.getY() + f, (double)blockPos.getZ() + g, 0.0, 0.0, 0.0);
			}
		}
	}

	@Override
	protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
		builder.add(LIT);
	}
}
