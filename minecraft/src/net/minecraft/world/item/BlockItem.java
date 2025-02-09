package net.minecraft.world.item;

import java.util.List;
import java.util.Map;
import javax.annotation.Nullable;
import net.minecraft.advancements.CriteriaTriggers;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.flag.FeatureFlagSet;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.ShulkerBoxBlock;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.Property;
import net.minecraft.world.level.gameevent.GameEvent;
import net.minecraft.world.phys.shapes.CollisionContext;

public class BlockItem extends Item {
	public static final String BLOCK_ENTITY_TAG = "BlockEntityTag";
	public static final String BLOCK_STATE_TAG = "BlockStateTag";
	@Deprecated
	private final Block block;

	public BlockItem(Block block, Item.Properties properties) {
		super(properties);
		this.block = block;
	}

	@Override
	public InteractionResult useOn(UseOnContext useOnContext) {
		InteractionResult interactionResult = this.place(new BlockPlaceContext(useOnContext));
		if (!interactionResult.consumesAction() && this.isEdible()) {
			InteractionResult interactionResult2 = this.use(useOnContext.getLevel(), useOnContext.getPlayer(), useOnContext.getHand()).getResult();
			return interactionResult2 == InteractionResult.CONSUME ? InteractionResult.CONSUME_PARTIAL : interactionResult2;
		} else {
			return interactionResult;
		}
	}

	public InteractionResult place(BlockPlaceContext blockPlaceContext) {
		if (!this.getBlock().isEnabled(blockPlaceContext.getLevel().enabledFeatures())) {
			return InteractionResult.FAIL;
		} else if (!blockPlaceContext.canPlace()) {
			return InteractionResult.FAIL;
		} else {
			BlockPlaceContext blockPlaceContext2 = this.updatePlacementContext(blockPlaceContext);
			if (blockPlaceContext2 == null) {
				return InteractionResult.FAIL;
			} else {
				BlockState blockState = this.getPlacementState(blockPlaceContext2);
				if (blockState == null) {
					return InteractionResult.FAIL;
				} else if (!this.placeBlock(blockPlaceContext2, blockState)) {
					return InteractionResult.FAIL;
				} else {
					BlockPos blockPos = blockPlaceContext2.getClickedPos();
					Level level = blockPlaceContext2.getLevel();
					Player player = blockPlaceContext2.getPlayer();
					ItemStack itemStack = blockPlaceContext2.getItemInHand();
					BlockState blockState2 = level.getBlockState(blockPos);
					if (blockState2.is(blockState.getBlock())) {
						blockState2 = this.updateBlockStateFromTag(blockPos, level, itemStack, blockState2);
						this.updateCustomBlockEntityTag(blockPos, level, player, itemStack, blockState2);
						blockState2.getBlock().setPlacedBy(level, blockPos, blockState2, player, itemStack);
						if (player instanceof ServerPlayer) {
							CriteriaTriggers.PLACED_BLOCK.trigger((ServerPlayer)player, blockPos, itemStack);
						}
					}

					SoundType soundType = blockState2.getSoundType();
					level.playSound(player, blockPos, this.getPlaceSound(blockState2), SoundSource.BLOCKS, (soundType.getVolume() + 1.0F) / 2.0F, soundType.getPitch() * 0.8F);
					level.gameEvent(GameEvent.BLOCK_PLACE, blockPos, GameEvent.Context.of(player, blockState2));
					if (player == null || !player.getAbilities().instabuild) {
						itemStack.shrink(1);
					}

					return InteractionResult.sidedSuccess(level.isClientSide);
				}
			}
		}
	}

	protected SoundEvent getPlaceSound(BlockState blockState) {
		return blockState.getSoundType().getPlaceSound();
	}

	@Nullable
	public BlockPlaceContext updatePlacementContext(BlockPlaceContext blockPlaceContext) {
		return blockPlaceContext;
	}

	protected boolean updateCustomBlockEntityTag(BlockPos blockPos, Level level, @Nullable Player player, ItemStack itemStack, BlockState blockState) {
		return updateCustomBlockEntityTag(level, player, blockPos, itemStack);
	}

	@Nullable
	protected BlockState getPlacementState(BlockPlaceContext blockPlaceContext) {
		BlockState blockState = this.getBlock().getStateForPlacement(blockPlaceContext);
		return blockState != null && this.canPlace(blockPlaceContext, blockState) ? blockState : null;
	}

	private BlockState updateBlockStateFromTag(BlockPos blockPos, Level level, ItemStack itemStack, BlockState blockState) {
		BlockState blockState2 = blockState;
		CompoundTag compoundTag = itemStack.getTag();
		if (compoundTag != null) {
			CompoundTag compoundTag2 = compoundTag.getCompound("BlockStateTag");
			StateDefinition<Block, BlockState> stateDefinition = blockState.getBlock().getStateDefinition();

			for(String string : compoundTag2.getAllKeys()) {
				Property<?> property = stateDefinition.getProperty(string);
				if (property != null) {
					String string2 = compoundTag2.get(string).getAsString();
					blockState2 = updateState(blockState2, property, string2);
				}
			}
		}

		if (blockState2 != blockState) {
			level.setBlock(blockPos, blockState2, 2);
		}

		return blockState2;
	}

	private static <T extends Comparable<T>> BlockState updateState(BlockState blockState, Property<T> property, String string) {
		return (BlockState)property.getValue(string).map(comparable -> blockState.setValue(property, comparable)).orElse(blockState);
	}

	protected boolean canPlace(BlockPlaceContext blockPlaceContext, BlockState blockState) {
		Player player = blockPlaceContext.getPlayer();
		CollisionContext collisionContext = player == null ? CollisionContext.empty() : CollisionContext.of(player);
		return (!this.mustSurvive() || blockState.canSurvive(blockPlaceContext.getLevel(), blockPlaceContext.getClickedPos()))
			&& blockPlaceContext.getLevel().isUnobstructed(blockState, blockPlaceContext.getClickedPos(), collisionContext);
	}

	protected boolean mustSurvive() {
		return true;
	}

	protected boolean placeBlock(BlockPlaceContext blockPlaceContext, BlockState blockState) {
		return blockPlaceContext.getLevel().setBlock(blockPlaceContext.getClickedPos(), blockState, 11);
	}

	public static boolean updateCustomBlockEntityTag(Level level, @Nullable Player player, BlockPos blockPos, ItemStack itemStack) {
		MinecraftServer minecraftServer = level.getServer();
		if (minecraftServer == null) {
			return false;
		} else {
			CompoundTag compoundTag = getBlockEntityData(itemStack);
			if (compoundTag != null) {
				BlockEntity blockEntity = level.getBlockEntity(blockPos);
				if (blockEntity != null) {
					if (!level.isClientSide && blockEntity.onlyOpCanSetNbt() && (player == null || !player.canUseGameMasterBlocks())) {
						return false;
					}

					CompoundTag compoundTag2 = blockEntity.saveWithoutMetadata();
					CompoundTag compoundTag3 = compoundTag2.copy();
					compoundTag2.merge(compoundTag);
					if (!compoundTag2.equals(compoundTag3)) {
						blockEntity.load(compoundTag2);
						blockEntity.setChanged();
						return true;
					}
				}
			}

			return false;
		}
	}

	@Override
	public String getDescriptionId() {
		return this.getBlock().getDescriptionId();
	}

	@Override
	public void appendHoverText(ItemStack itemStack, @Nullable Level level, List<Component> list, TooltipFlag tooltipFlag) {
		super.appendHoverText(itemStack, level, list, tooltipFlag);
		this.getBlock().appendHoverText(itemStack, level, list, tooltipFlag);
	}

	public Block getBlock() {
		return this.block;
	}

	public void registerBlocks(Map<Block, Item> map, Item item) {
		map.put(this.getBlock(), item);
	}

	@Override
	public boolean canFitInsideContainerItems() {
		return !(this.block instanceof ShulkerBoxBlock);
	}

	@Override
	public void onDestroyed(ItemEntity itemEntity) {
		if (this.block instanceof ShulkerBoxBlock) {
			ItemStack itemStack = itemEntity.getItem();
			CompoundTag compoundTag = getBlockEntityData(itemStack);
			if (compoundTag != null && compoundTag.contains("Items", 9)) {
				ListTag listTag = compoundTag.getList("Items", 10);
				ItemUtils.onContainerDestroyed(itemEntity, listTag.stream().map(CompoundTag.class::cast).map(ItemStack::of));
			}
		}
	}

	@Nullable
	public static CompoundTag getBlockEntityData(ItemStack itemStack) {
		return itemStack.getTagElement("BlockEntityTag");
	}

	public static void setBlockEntityData(ItemStack itemStack, BlockEntityType<?> blockEntityType, CompoundTag compoundTag) {
		if (compoundTag.isEmpty()) {
			itemStack.removeTagKey("BlockEntityTag");
		} else {
			BlockEntity.addEntityType(compoundTag, blockEntityType);
			itemStack.addTagElement("BlockEntityTag", compoundTag);
		}
	}

	@Override
	public FeatureFlagSet requiredFeatures() {
		return this.getBlock().requiredFeatures();
	}
}
