package net.minecraft.advancements.critereon;

import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonSyntaxException;
import javax.annotation.Nullable;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Registry;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.GsonHelper;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;

public class PlacedBlockTrigger extends SimpleCriterionTrigger<PlacedBlockTrigger.TriggerInstance> {
	private static final ResourceLocation ID = new ResourceLocation("placed_block");

	@Override
	public ResourceLocation getId() {
		return ID;
	}

	public PlacedBlockTrigger.TriggerInstance createInstance(JsonObject jsonObject, JsonDeserializationContext jsonDeserializationContext) {
		Block block = deserializeBlock(jsonObject);
		StatePropertiesPredicate statePropertiesPredicate = StatePropertiesPredicate.fromJson(jsonObject.get("state"));
		if (block != null) {
			statePropertiesPredicate.checkState(block.getStateDefinition(), string -> {
				throw new JsonSyntaxException("Block " + block + " has no property " + string + ":");
			});
		}

		LocationPredicate locationPredicate = LocationPredicate.fromJson(jsonObject.get("location"));
		ItemPredicate itemPredicate = ItemPredicate.fromJson(jsonObject.get("item"));
		return new PlacedBlockTrigger.TriggerInstance(block, statePropertiesPredicate, locationPredicate, itemPredicate);
	}

	@Nullable
	private static Block deserializeBlock(JsonObject jsonObject) {
		if (jsonObject.has("block")) {
			ResourceLocation resourceLocation = new ResourceLocation(GsonHelper.getAsString(jsonObject, "block"));
			return (Block)Registry.BLOCK.getOptional(resourceLocation).orElseThrow(() -> new JsonSyntaxException("Unknown block type '" + resourceLocation + "'"));
		} else {
			return null;
		}
	}

	public void trigger(ServerPlayer serverPlayer, BlockPos blockPos, ItemStack itemStack) {
		BlockState blockState = serverPlayer.getLevel().getBlockState(blockPos);
		this.trigger(serverPlayer.getAdvancements(), triggerInstance -> triggerInstance.matches(blockState, blockPos, serverPlayer.getLevel(), itemStack));
	}

	public static class TriggerInstance extends AbstractCriterionTriggerInstance {
		private final Block block;
		private final StatePropertiesPredicate state;
		private final LocationPredicate location;
		private final ItemPredicate item;

		public TriggerInstance(
			@Nullable Block block, StatePropertiesPredicate statePropertiesPredicate, LocationPredicate locationPredicate, ItemPredicate itemPredicate
		) {
			super(PlacedBlockTrigger.ID);
			this.block = block;
			this.state = statePropertiesPredicate;
			this.location = locationPredicate;
			this.item = itemPredicate;
		}

		public static PlacedBlockTrigger.TriggerInstance placedBlock(Block block) {
			return new PlacedBlockTrigger.TriggerInstance(block, StatePropertiesPredicate.ANY, LocationPredicate.ANY, ItemPredicate.ANY);
		}

		public boolean matches(BlockState blockState, BlockPos blockPos, ServerLevel serverLevel, ItemStack itemStack) {
			if (this.block != null && blockState.getBlock() != this.block) {
				return false;
			} else if (!this.state.matches(blockState)) {
				return false;
			} else if (!this.location.matches(serverLevel, (float)blockPos.getX(), (float)blockPos.getY(), (float)blockPos.getZ())) {
				return false;
			} else {
				return this.item.matches(itemStack);
			}
		}

		@Override
		public JsonElement serializeToJson() {
			JsonObject jsonObject = new JsonObject();
			if (this.block != null) {
				jsonObject.addProperty("block", Registry.BLOCK.getKey(this.block).toString());
			}

			jsonObject.add("state", this.state.serializeToJson());
			jsonObject.add("location", this.location.serializeToJson());
			jsonObject.add("item", this.item.serializeToJson());
			return jsonObject;
		}
	}
}
