package net.minecraft.world.item;

import javax.annotation.Nullable;
import net.minecraft.advancements.CriteriaTriggers;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.stats.Stats;
import net.minecraft.tags.FluidTags;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.BucketPickup;
import net.minecraft.world.level.block.LiquidBlockContainer;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.gameevent.GameEvent;
import net.minecraft.world.level.material.FlowingFluid;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.Fluids;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;

public class BucketItem extends Item implements DispensibleContainerItem {
	private final Fluid content;

	public BucketItem(Fluid fluid, Item.Properties properties) {
		super(properties);
		this.content = fluid;
	}

	@Override
	public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand interactionHand) {
		ItemStack itemStack = player.getItemInHand(interactionHand);
		BlockHitResult blockHitResult = getPlayerPOVHitResult(level, player, this.content == Fluids.EMPTY ? ClipContext.Fluid.SOURCE_ONLY : ClipContext.Fluid.NONE);
		if (blockHitResult.getType() == HitResult.Type.MISS) {
			return InteractionResultHolder.pass(itemStack);
		} else if (blockHitResult.getType() != HitResult.Type.BLOCK) {
			return InteractionResultHolder.pass(itemStack);
		} else {
			BlockPos blockPos = blockHitResult.getBlockPos();
			Direction direction = blockHitResult.getDirection();
			BlockPos blockPos2 = blockPos.relative(direction);
			if (!level.mayInteract(player, blockPos) || !player.mayUseItemAt(blockPos2, direction, itemStack)) {
				return InteractionResultHolder.fail(itemStack);
			} else if (this.content == Fluids.EMPTY) {
				BlockState blockState = level.getBlockState(blockPos);
				Block itemStack2 = blockState.getBlock();
				if (itemStack2 instanceof BucketPickup bucketPickup) {
					ItemStack itemStack2x = bucketPickup.pickupBlock(player, level, blockPos, blockState);
					if (!itemStack2x.isEmpty()) {
						player.awardStat(Stats.ITEM_USED.get(this));
						bucketPickup.getPickupSound().ifPresent(soundEvent -> player.playSound(soundEvent, 1.0F, 1.0F));
						level.gameEvent(player, GameEvent.FLUID_PICKUP, blockPos);
						ItemStack itemStack3 = ItemUtils.createFilledResult(itemStack, player, itemStack2x);
						if (!level.isClientSide) {
							CriteriaTriggers.FILLED_BUCKET.trigger((ServerPlayer)player, itemStack2x);
						}

						return InteractionResultHolder.sidedSuccess(itemStack3, level.isClientSide());
					}
				}

				return InteractionResultHolder.fail(itemStack);
			} else {
				BlockState blockState = level.getBlockState(blockPos);
				BlockPos blockPos3 = blockState.getBlock() instanceof LiquidBlockContainer && this.content == Fluids.WATER ? blockPos : blockPos2;
				if (this.emptyContents(player, level, blockPos3, blockHitResult)) {
					this.checkExtraContent(player, level, itemStack, blockPos3);
					if (player instanceof ServerPlayer) {
						CriteriaTriggers.PLACED_BLOCK.trigger((ServerPlayer)player, blockPos3, itemStack);
					}

					player.awardStat(Stats.ITEM_USED.get(this));
					return InteractionResultHolder.sidedSuccess(getEmptySuccessItem(itemStack, player), level.isClientSide());
				} else {
					return InteractionResultHolder.fail(itemStack);
				}
			}
		}
	}

	public static ItemStack getEmptySuccessItem(ItemStack itemStack, Player player) {
		return !player.getAbilities().instabuild ? new ItemStack(Items.BUCKET) : itemStack;
	}

	@Override
	public void checkExtraContent(@Nullable Player player, Level level, ItemStack itemStack, BlockPos blockPos) {
	}

	@Override
	public boolean emptyContents(@Nullable Player player, Level level, BlockPos blockPos, @Nullable BlockHitResult blockHitResult) {
		Fluid blockState = this.content;
		if (!(blockState instanceof FlowingFluid)) {
			return false;
		} else {
			FlowingFluid flowingFluid;
			Block block;
			boolean bl;
			boolean var10000;
			label82: {
				flowingFluid = (FlowingFluid)blockState;
				blockState = level.getBlockState(blockPos);
				block = blockState.getBlock();
				bl = blockState.canBeReplaced(this.content);
				label70:
				if (!blockState.isAir() && !bl) {
					if (block instanceof LiquidBlockContainer liquidBlockContainer && liquidBlockContainer.canPlaceLiquid(player, level, blockPos, blockState, this.content)) {
						break label70;
					}

					var10000 = false;
					break label82;
				}

				var10000 = true;
			}

			boolean bl2 = var10000;
			if (!bl2) {
				return blockHitResult != null && this.emptyContents(player, level, blockHitResult.getBlockPos().relative(blockHitResult.getDirection()), null);
			} else if (level.dimensionType().ultraWarm() && this.content.is(FluidTags.WATER)) {
				int i = blockPos.getX();
				int j = blockPos.getY();
				int k = blockPos.getZ();
				level.playSound(
					player, blockPos, SoundEvents.FIRE_EXTINGUISH, SoundSource.BLOCKS, 0.5F, 2.6F + (level.random.nextFloat() - level.random.nextFloat()) * 0.8F
				);

				for(int l = 0; l < 8; ++l) {
					level.addParticle(ParticleTypes.LARGE_SMOKE, (double)i + Math.random(), (double)j + Math.random(), (double)k + Math.random(), 0.0, 0.0, 0.0);
				}

				return true;
			} else {
				if (block instanceof LiquidBlockContainer liquidBlockContainer && this.content == Fluids.WATER) {
					liquidBlockContainer.placeLiquid(level, blockPos, blockState, flowingFluid.getSource(false));
					this.playEmptySound(player, level, blockPos);
					return true;
				}

				if (!level.isClientSide && bl && !blockState.liquid()) {
					level.destroyBlock(blockPos, true);
				}

				if (!level.setBlock(blockPos, this.content.defaultFluidState().createLegacyBlock(), 11) && !blockState.getFluidState().isSource()) {
					return false;
				} else {
					this.playEmptySound(player, level, blockPos);
					return true;
				}
			}
		}
	}

	protected void playEmptySound(@Nullable Player player, LevelAccessor levelAccessor, BlockPos blockPos) {
		SoundEvent soundEvent = this.content.is(FluidTags.LAVA) ? SoundEvents.BUCKET_EMPTY_LAVA : SoundEvents.BUCKET_EMPTY;
		levelAccessor.playSound(player, blockPos, soundEvent, SoundSource.BLOCKS, 1.0F, 1.0F);
		levelAccessor.gameEvent(player, GameEvent.FLUID_PLACE, blockPos);
	}
}
