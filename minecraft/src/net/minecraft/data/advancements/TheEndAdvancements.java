package net.minecraft.data.advancements;

import java.util.function.Consumer;
import net.minecraft.advancements.Advancement;
import net.minecraft.advancements.AdvancementRewards;
import net.minecraft.advancements.FrameType;
import net.minecraft.advancements.critereon.ChangeDimensionTrigger;
import net.minecraft.advancements.critereon.DistancePredicate;
import net.minecraft.advancements.critereon.EnterBlockTrigger;
import net.minecraft.advancements.critereon.EntityPredicate;
import net.minecraft.advancements.critereon.InventoryChangeTrigger;
import net.minecraft.advancements.critereon.KilledTrigger;
import net.minecraft.advancements.critereon.LevitationTrigger;
import net.minecraft.advancements.critereon.LocationPredicate;
import net.minecraft.advancements.critereon.LocationTrigger;
import net.minecraft.advancements.critereon.MinMaxBounds;
import net.minecraft.advancements.critereon.SummonedEntityTrigger;
import net.minecraft.network.chat.TranslatableComponent;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.dimension.DimensionType;
import net.minecraft.world.level.levelgen.feature.Feature;

public class TheEndAdvancements implements Consumer<Consumer<Advancement>> {
	public void accept(Consumer<Advancement> consumer) {
		Advancement advancement = Advancement.Builder.advancement()
			.display(
				Blocks.END_STONE,
				new TranslatableComponent("advancements.end.root.title"),
				new TranslatableComponent("advancements.end.root.description"),
				new ResourceLocation("textures/gui/advancements/backgrounds/end.png"),
				FrameType.TASK,
				false,
				false,
				false
			)
			.addCriterion("entered_end", ChangeDimensionTrigger.TriggerInstance.changedDimensionTo(DimensionType.THE_END))
			.save(consumer, "end/root");
		Advancement advancement2 = Advancement.Builder.advancement()
			.parent(advancement)
			.display(
				Blocks.DRAGON_HEAD,
				new TranslatableComponent("advancements.end.kill_dragon.title"),
				new TranslatableComponent("advancements.end.kill_dragon.description"),
				null,
				FrameType.TASK,
				true,
				true,
				false
			)
			.addCriterion("killed_dragon", KilledTrigger.TriggerInstance.playerKilledEntity(EntityPredicate.Builder.entity().of(EntityType.ENDER_DRAGON)))
			.save(consumer, "end/kill_dragon");
		Advancement advancement3 = Advancement.Builder.advancement()
			.parent(advancement2)
			.display(
				Items.ENDER_PEARL,
				new TranslatableComponent("advancements.end.enter_end_gateway.title"),
				new TranslatableComponent("advancements.end.enter_end_gateway.description"),
				null,
				FrameType.TASK,
				true,
				true,
				false
			)
			.addCriterion("entered_end_gateway", EnterBlockTrigger.TriggerInstance.entersBlock(Blocks.END_GATEWAY))
			.save(consumer, "end/enter_end_gateway");
		Advancement advancement4 = Advancement.Builder.advancement()
			.parent(advancement2)
			.display(
				Items.END_CRYSTAL,
				new TranslatableComponent("advancements.end.respawn_dragon.title"),
				new TranslatableComponent("advancements.end.respawn_dragon.description"),
				null,
				FrameType.GOAL,
				true,
				true,
				false
			)
			.addCriterion("summoned_dragon", SummonedEntityTrigger.TriggerInstance.summonedEntity(EntityPredicate.Builder.entity().of(EntityType.ENDER_DRAGON)))
			.save(consumer, "end/respawn_dragon");
		Advancement advancement5 = Advancement.Builder.advancement()
			.parent(advancement3)
			.display(
				Blocks.PURPUR_BLOCK,
				new TranslatableComponent("advancements.end.find_end_city.title"),
				new TranslatableComponent("advancements.end.find_end_city.description"),
				null,
				FrameType.TASK,
				true,
				true,
				false
			)
			.addCriterion("in_city", LocationTrigger.TriggerInstance.located(LocationPredicate.inFeature(Feature.END_CITY)))
			.save(consumer, "end/find_end_city");
		Advancement advancement6 = Advancement.Builder.advancement()
			.parent(advancement2)
			.display(
				Items.DRAGON_BREATH,
				new TranslatableComponent("advancements.end.dragon_breath.title"),
				new TranslatableComponent("advancements.end.dragon_breath.description"),
				null,
				FrameType.GOAL,
				true,
				true,
				false
			)
			.addCriterion("dragon_breath", InventoryChangeTrigger.TriggerInstance.hasItem(Items.DRAGON_BREATH))
			.save(consumer, "end/dragon_breath");
		Advancement advancement7 = Advancement.Builder.advancement()
			.parent(advancement5)
			.display(
				Items.SHULKER_SHELL,
				new TranslatableComponent("advancements.end.levitate.title"),
				new TranslatableComponent("advancements.end.levitate.description"),
				null,
				FrameType.CHALLENGE,
				true,
				true,
				false
			)
			.rewards(AdvancementRewards.Builder.experience(50))
			.addCriterion("levitated", LevitationTrigger.TriggerInstance.levitated(DistancePredicate.vertical(MinMaxBounds.Floats.atLeast(50.0F))))
			.save(consumer, "end/levitate");
		Advancement advancement8 = Advancement.Builder.advancement()
			.parent(advancement5)
			.display(
				Items.ELYTRA,
				new TranslatableComponent("advancements.end.elytra.title"),
				new TranslatableComponent("advancements.end.elytra.description"),
				null,
				FrameType.GOAL,
				true,
				true,
				false
			)
			.addCriterion("elytra", InventoryChangeTrigger.TriggerInstance.hasItem(Items.ELYTRA))
			.save(consumer, "end/elytra");
		Advancement advancement9 = Advancement.Builder.advancement()
			.parent(advancement2)
			.display(
				Blocks.DRAGON_EGG,
				new TranslatableComponent("advancements.end.dragon_egg.title"),
				new TranslatableComponent("advancements.end.dragon_egg.description"),
				null,
				FrameType.GOAL,
				true,
				true,
				false
			)
			.addCriterion("dragon_egg", InventoryChangeTrigger.TriggerInstance.hasItem(Blocks.DRAGON_EGG))
			.save(consumer, "end/dragon_egg");
	}
}
