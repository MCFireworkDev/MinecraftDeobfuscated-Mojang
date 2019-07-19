package net.minecraft.data.advancements;

import java.util.function.Consumer;
import net.minecraft.advancements.Advancement;
import net.minecraft.advancements.AdvancementRewards;
import net.minecraft.advancements.FrameType;
import net.minecraft.advancements.RequirementsStrategy;
import net.minecraft.advancements.critereon.ChanneledLightningTrigger;
import net.minecraft.advancements.critereon.DamagePredicate;
import net.minecraft.advancements.critereon.DamageSourcePredicate;
import net.minecraft.advancements.critereon.DistancePredicate;
import net.minecraft.advancements.critereon.EntityEquipmentPredicate;
import net.minecraft.advancements.critereon.EntityPredicate;
import net.minecraft.advancements.critereon.KilledByCrossbowTrigger;
import net.minecraft.advancements.critereon.KilledTrigger;
import net.minecraft.advancements.critereon.LocationPredicate;
import net.minecraft.advancements.critereon.LocationTrigger;
import net.minecraft.advancements.critereon.MinMaxBounds;
import net.minecraft.advancements.critereon.PlayerHurtEntityTrigger;
import net.minecraft.advancements.critereon.ShotCrossbowTrigger;
import net.minecraft.advancements.critereon.SummonedEntityTrigger;
import net.minecraft.advancements.critereon.TradeTrigger;
import net.minecraft.advancements.critereon.UsedTotemTrigger;
import net.minecraft.core.Registry;
import net.minecraft.network.chat.TranslatableComponent;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.EntityTypeTags;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.raid.Raid;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.biome.Biomes;
import net.minecraft.world.level.block.Blocks;

public class AdventureAdvancements implements Consumer<Consumer<Advancement>> {
	private static final Biome[] EXPLORABLE_BIOMES = new Biome[]{
		Biomes.BIRCH_FOREST_HILLS,
		Biomes.RIVER,
		Biomes.SWAMP,
		Biomes.DESERT,
		Biomes.WOODED_HILLS,
		Biomes.GIANT_TREE_TAIGA_HILLS,
		Biomes.SNOWY_TAIGA,
		Biomes.BADLANDS,
		Biomes.FOREST,
		Biomes.STONE_SHORE,
		Biomes.SNOWY_TUNDRA,
		Biomes.TAIGA_HILLS,
		Biomes.SNOWY_MOUNTAINS,
		Biomes.WOODED_BADLANDS_PLATEAU,
		Biomes.SAVANNA,
		Biomes.PLAINS,
		Biomes.FROZEN_RIVER,
		Biomes.GIANT_TREE_TAIGA,
		Biomes.SNOWY_BEACH,
		Biomes.JUNGLE_HILLS,
		Biomes.JUNGLE_EDGE,
		Biomes.MUSHROOM_FIELD_SHORE,
		Biomes.MOUNTAINS,
		Biomes.DESERT_HILLS,
		Biomes.JUNGLE,
		Biomes.BEACH,
		Biomes.SAVANNA_PLATEAU,
		Biomes.SNOWY_TAIGA_HILLS,
		Biomes.BADLANDS_PLATEAU,
		Biomes.DARK_FOREST,
		Biomes.TAIGA,
		Biomes.BIRCH_FOREST,
		Biomes.MUSHROOM_FIELDS,
		Biomes.WOODED_MOUNTAINS,
		Biomes.WARM_OCEAN,
		Biomes.LUKEWARM_OCEAN,
		Biomes.COLD_OCEAN,
		Biomes.DEEP_LUKEWARM_OCEAN,
		Biomes.DEEP_COLD_OCEAN,
		Biomes.DEEP_FROZEN_OCEAN,
		Biomes.BAMBOO_JUNGLE,
		Biomes.BAMBOO_JUNGLE_HILLS
	};
	private static final EntityType<?>[] MOBS_TO_KILL = new EntityType[]{
		EntityType.CAVE_SPIDER,
		EntityType.SPIDER,
		EntityType.ZOMBIE_PIGMAN,
		EntityType.ENDERMAN,
		EntityType.BLAZE,
		EntityType.CREEPER,
		EntityType.EVOKER,
		EntityType.GHAST,
		EntityType.GUARDIAN,
		EntityType.HUSK,
		EntityType.MAGMA_CUBE,
		EntityType.SHULKER,
		EntityType.SILVERFISH,
		EntityType.SKELETON,
		EntityType.SLIME,
		EntityType.STRAY,
		EntityType.VINDICATOR,
		EntityType.WITCH,
		EntityType.WITHER_SKELETON,
		EntityType.ZOMBIE,
		EntityType.ZOMBIE_VILLAGER,
		EntityType.PHANTOM,
		EntityType.DROWNED,
		EntityType.PILLAGER,
		EntityType.RAVAGER
	};

	public void accept(Consumer<Advancement> consumer) {
		Advancement advancement = Advancement.Builder.advancement()
			.display(
				Items.MAP,
				new TranslatableComponent("advancements.adventure.root.title"),
				new TranslatableComponent("advancements.adventure.root.description"),
				new ResourceLocation("textures/gui/advancements/backgrounds/adventure.png"),
				FrameType.TASK,
				false,
				false,
				false
			)
			.requirements(RequirementsStrategy.OR)
			.addCriterion("killed_something", KilledTrigger.TriggerInstance.playerKilledEntity())
			.addCriterion("killed_by_something", KilledTrigger.TriggerInstance.entityKilledPlayer())
			.save(consumer, "adventure/root");
		Advancement advancement2 = Advancement.Builder.advancement()
			.parent(advancement)
			.display(
				Blocks.RED_BED,
				new TranslatableComponent("advancements.adventure.sleep_in_bed.title"),
				new TranslatableComponent("advancements.adventure.sleep_in_bed.description"),
				null,
				FrameType.TASK,
				true,
				true,
				false
			)
			.addCriterion("slept_in_bed", LocationTrigger.TriggerInstance.sleptInBed())
			.save(consumer, "adventure/sleep_in_bed");
		Advancement advancement3 = this.addBiomes(Advancement.Builder.advancement())
			.parent(advancement2)
			.display(
				Items.DIAMOND_BOOTS,
				new TranslatableComponent("advancements.adventure.adventuring_time.title"),
				new TranslatableComponent("advancements.adventure.adventuring_time.description"),
				null,
				FrameType.CHALLENGE,
				true,
				true,
				false
			)
			.rewards(AdvancementRewards.Builder.experience(500))
			.save(consumer, "adventure/adventuring_time");
		Advancement advancement4 = Advancement.Builder.advancement()
			.parent(advancement)
			.display(
				Items.EMERALD,
				new TranslatableComponent("advancements.adventure.trade.title"),
				new TranslatableComponent("advancements.adventure.trade.description"),
				null,
				FrameType.TASK,
				true,
				true,
				false
			)
			.addCriterion("traded", TradeTrigger.TriggerInstance.tradedWithVillager())
			.save(consumer, "adventure/trade");
		Advancement advancement5 = this.addMobsToKill(Advancement.Builder.advancement())
			.parent(advancement)
			.display(
				Items.IRON_SWORD,
				new TranslatableComponent("advancements.adventure.kill_a_mob.title"),
				new TranslatableComponent("advancements.adventure.kill_a_mob.description"),
				null,
				FrameType.TASK,
				true,
				true,
				false
			)
			.requirements(RequirementsStrategy.OR)
			.save(consumer, "adventure/kill_a_mob");
		Advancement advancement6 = this.addMobsToKill(Advancement.Builder.advancement())
			.parent(advancement5)
			.display(
				Items.DIAMOND_SWORD,
				new TranslatableComponent("advancements.adventure.kill_all_mobs.title"),
				new TranslatableComponent("advancements.adventure.kill_all_mobs.description"),
				null,
				FrameType.CHALLENGE,
				true,
				true,
				false
			)
			.rewards(AdvancementRewards.Builder.experience(100))
			.save(consumer, "adventure/kill_all_mobs");
		Advancement advancement7 = Advancement.Builder.advancement()
			.parent(advancement5)
			.display(
				Items.BOW,
				new TranslatableComponent("advancements.adventure.shoot_arrow.title"),
				new TranslatableComponent("advancements.adventure.shoot_arrow.description"),
				null,
				FrameType.TASK,
				true,
				true,
				false
			)
			.addCriterion(
				"shot_arrow",
				PlayerHurtEntityTrigger.TriggerInstance.playerHurtEntity(
					DamagePredicate.Builder.damageInstance()
						.type(DamageSourcePredicate.Builder.damageType().isProjectile(true).direct(EntityPredicate.Builder.entity().of(EntityType.ARROW)))
				)
			)
			.save(consumer, "adventure/shoot_arrow");
		Advancement advancement8 = Advancement.Builder.advancement()
			.parent(advancement5)
			.display(
				Items.TRIDENT,
				new TranslatableComponent("advancements.adventure.throw_trident.title"),
				new TranslatableComponent("advancements.adventure.throw_trident.description"),
				null,
				FrameType.TASK,
				true,
				true,
				false
			)
			.addCriterion(
				"shot_trident",
				PlayerHurtEntityTrigger.TriggerInstance.playerHurtEntity(
					DamagePredicate.Builder.damageInstance()
						.type(DamageSourcePredicate.Builder.damageType().isProjectile(true).direct(EntityPredicate.Builder.entity().of(EntityType.TRIDENT)))
				)
			)
			.save(consumer, "adventure/throw_trident");
		Advancement advancement9 = Advancement.Builder.advancement()
			.parent(advancement8)
			.display(
				Items.TRIDENT,
				new TranslatableComponent("advancements.adventure.very_very_frightening.title"),
				new TranslatableComponent("advancements.adventure.very_very_frightening.description"),
				null,
				FrameType.TASK,
				true,
				true,
				false
			)
			.addCriterion(
				"struck_villager", ChanneledLightningTrigger.TriggerInstance.channeledLightning(EntityPredicate.Builder.entity().of(EntityType.VILLAGER).build())
			)
			.save(consumer, "adventure/very_very_frightening");
		Advancement advancement10 = Advancement.Builder.advancement()
			.parent(advancement4)
			.display(
				Blocks.CARVED_PUMPKIN,
				new TranslatableComponent("advancements.adventure.summon_iron_golem.title"),
				new TranslatableComponent("advancements.adventure.summon_iron_golem.description"),
				null,
				FrameType.GOAL,
				true,
				true,
				false
			)
			.addCriterion("summoned_golem", SummonedEntityTrigger.TriggerInstance.summonedEntity(EntityPredicate.Builder.entity().of(EntityType.IRON_GOLEM)))
			.save(consumer, "adventure/summon_iron_golem");
		Advancement advancement11 = Advancement.Builder.advancement()
			.parent(advancement7)
			.display(
				Items.ARROW,
				new TranslatableComponent("advancements.adventure.sniper_duel.title"),
				new TranslatableComponent("advancements.adventure.sniper_duel.description"),
				null,
				FrameType.CHALLENGE,
				true,
				true,
				false
			)
			.rewards(AdvancementRewards.Builder.experience(50))
			.addCriterion(
				"killed_skeleton",
				KilledTrigger.TriggerInstance.playerKilledEntity(
					EntityPredicate.Builder.entity().of(EntityType.SKELETON).distance(DistancePredicate.horizontal(MinMaxBounds.Floats.atLeast(50.0F))),
					DamageSourcePredicate.Builder.damageType().isProjectile(true)
				)
			)
			.save(consumer, "adventure/sniper_duel");
		Advancement advancement12 = Advancement.Builder.advancement()
			.parent(advancement5)
			.display(
				Items.TOTEM_OF_UNDYING,
				new TranslatableComponent("advancements.adventure.totem_of_undying.title"),
				new TranslatableComponent("advancements.adventure.totem_of_undying.description"),
				null,
				FrameType.GOAL,
				true,
				true,
				false
			)
			.addCriterion("used_totem", UsedTotemTrigger.TriggerInstance.usedTotem(Items.TOTEM_OF_UNDYING))
			.save(consumer, "adventure/totem_of_undying");
		Advancement advancement13 = Advancement.Builder.advancement()
			.parent(advancement)
			.display(
				Items.CROSSBOW,
				new TranslatableComponent("advancements.adventure.ol_betsy.title"),
				new TranslatableComponent("advancements.adventure.ol_betsy.description"),
				null,
				FrameType.TASK,
				true,
				true,
				false
			)
			.addCriterion("shot_crossbow", ShotCrossbowTrigger.TriggerInstance.shotCrossbow(Items.CROSSBOW))
			.save(consumer, "adventure/ol_betsy");
		Advancement advancement14 = Advancement.Builder.advancement()
			.parent(advancement13)
			.display(
				Items.CROSSBOW,
				new TranslatableComponent("advancements.adventure.whos_the_pillager_now.title"),
				new TranslatableComponent("advancements.adventure.whos_the_pillager_now.description"),
				null,
				FrameType.TASK,
				true,
				true,
				false
			)
			.addCriterion("kill_pillager", KilledByCrossbowTrigger.TriggerInstance.crossbowKilled(EntityPredicate.Builder.entity().of(EntityType.PILLAGER)))
			.save(consumer, "adventure/whos_the_pillager_now");
		Advancement advancement15 = Advancement.Builder.advancement()
			.parent(advancement13)
			.display(
				Items.CROSSBOW,
				new TranslatableComponent("advancements.adventure.two_birds_one_arrow.title"),
				new TranslatableComponent("advancements.adventure.two_birds_one_arrow.description"),
				null,
				FrameType.CHALLENGE,
				true,
				true,
				false
			)
			.rewards(AdvancementRewards.Builder.experience(65))
			.addCriterion(
				"two_birds",
				KilledByCrossbowTrigger.TriggerInstance.crossbowKilled(
					EntityPredicate.Builder.entity().of(EntityType.PHANTOM), EntityPredicate.Builder.entity().of(EntityType.PHANTOM)
				)
			)
			.save(consumer, "adventure/two_birds_one_arrow");
		Advancement advancement16 = Advancement.Builder.advancement()
			.parent(advancement13)
			.display(
				Items.CROSSBOW,
				new TranslatableComponent("advancements.adventure.arbalistic.title"),
				new TranslatableComponent("advancements.adventure.arbalistic.description"),
				null,
				FrameType.CHALLENGE,
				true,
				true,
				true
			)
			.rewards(AdvancementRewards.Builder.experience(85))
			.addCriterion("arbalistic", KilledByCrossbowTrigger.TriggerInstance.crossbowKilled(MinMaxBounds.Ints.exactly(5)))
			.save(consumer, "adventure/arbalistic");
		Advancement advancement17 = Advancement.Builder.advancement()
			.parent(advancement)
			.display(
				Raid.getLeaderBannerInstance(),
				new TranslatableComponent("advancements.adventure.voluntary_exile.title"),
				new TranslatableComponent("advancements.adventure.voluntary_exile.description"),
				null,
				FrameType.TASK,
				true,
				true,
				true
			)
			.addCriterion(
				"voluntary_exile",
				KilledTrigger.TriggerInstance.playerKilledEntity(EntityPredicate.Builder.entity().of(EntityTypeTags.RAIDERS).equipment(EntityEquipmentPredicate.CAPTAIN))
			)
			.save(consumer, "adventure/voluntary_exile");
		Advancement advancement18 = Advancement.Builder.advancement()
			.parent(advancement17)
			.display(
				Raid.getLeaderBannerInstance(),
				new TranslatableComponent("advancements.adventure.hero_of_the_village.title"),
				new TranslatableComponent("advancements.adventure.hero_of_the_village.description"),
				null,
				FrameType.CHALLENGE,
				true,
				true,
				true
			)
			.rewards(AdvancementRewards.Builder.experience(100))
			.addCriterion("hero_of_the_village", LocationTrigger.TriggerInstance.raidWon())
			.save(consumer, "adventure/hero_of_the_village");
	}

	private Advancement.Builder addMobsToKill(Advancement.Builder builder) {
		for(EntityType<?> entityType : MOBS_TO_KILL) {
			builder.addCriterion(
				Registry.ENTITY_TYPE.getKey(entityType).toString(), KilledTrigger.TriggerInstance.playerKilledEntity(EntityPredicate.Builder.entity().of(entityType))
			);
		}

		return builder;
	}

	private Advancement.Builder addBiomes(Advancement.Builder builder) {
		for(Biome biome : EXPLORABLE_BIOMES) {
			builder.addCriterion(Registry.BIOME.getKey(biome).toString(), LocationTrigger.TriggerInstance.located(LocationPredicate.inBiome(biome)));
		}

		return builder;
	}
}
