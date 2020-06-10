package net.minecraft.data.advancements;

import java.util.function.Consumer;
import net.minecraft.advancements.Advancement;
import net.minecraft.advancements.AdvancementRewards;
import net.minecraft.advancements.FrameType;
import net.minecraft.advancements.RequirementsStrategy;
import net.minecraft.advancements.critereon.BlockPredicate;
import net.minecraft.advancements.critereon.BrewedPotionTrigger;
import net.minecraft.advancements.critereon.ChangeDimensionTrigger;
import net.minecraft.advancements.critereon.ConstructBeaconTrigger;
import net.minecraft.advancements.critereon.DamageSourcePredicate;
import net.minecraft.advancements.critereon.DistancePredicate;
import net.minecraft.advancements.critereon.EffectsChangedTrigger;
import net.minecraft.advancements.critereon.EntityEquipmentPredicate;
import net.minecraft.advancements.critereon.EntityFlagsPredicate;
import net.minecraft.advancements.critereon.EntityPredicate;
import net.minecraft.advancements.critereon.InventoryChangeTrigger;
import net.minecraft.advancements.critereon.ItemDurabilityTrigger;
import net.minecraft.advancements.critereon.ItemPickedUpByEntityTrigger;
import net.minecraft.advancements.critereon.ItemPredicate;
import net.minecraft.advancements.critereon.ItemUsedOnBlockTrigger;
import net.minecraft.advancements.critereon.KilledTrigger;
import net.minecraft.advancements.critereon.LocationPredicate;
import net.minecraft.advancements.critereon.LocationTrigger;
import net.minecraft.advancements.critereon.LootTableTrigger;
import net.minecraft.advancements.critereon.MinMaxBounds;
import net.minecraft.advancements.critereon.MobEffectsPredicate;
import net.minecraft.advancements.critereon.NetherTravelTrigger;
import net.minecraft.advancements.critereon.StatePropertiesPredicate;
import net.minecraft.advancements.critereon.SummonedEntityTrigger;
import net.minecraft.network.chat.TranslatableComponent;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.ItemTags;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.biome.Biomes;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.RespawnAnchorBlock;
import net.minecraft.world.level.levelgen.feature.StructureFeature;
import net.minecraft.world.level.storage.loot.LootContext;
import net.minecraft.world.level.storage.loot.predicates.LootItemEntityPropertyCondition;

public class NetherAdvancements implements Consumer<Consumer<Advancement>> {
	private static final Biome[] EXPLORABLE_BIOMES = new Biome[]{
		Biomes.NETHER_WASTES, Biomes.SOUL_SAND_VALLEY, Biomes.WARPED_FOREST, Biomes.CRIMSON_FOREST, Biomes.BASALT_DELTAS
	};

	public void accept(Consumer<Advancement> consumer) {
		Advancement advancement = Advancement.Builder.advancement()
			.display(
				Blocks.RED_NETHER_BRICKS,
				new TranslatableComponent("advancements.nether.root.title"),
				new TranslatableComponent("advancements.nether.root.description"),
				new ResourceLocation("textures/gui/advancements/backgrounds/nether.png"),
				FrameType.TASK,
				false,
				false,
				false
			)
			.addCriterion("entered_nether", ChangeDimensionTrigger.TriggerInstance.changedDimensionTo(Level.NETHER))
			.save(consumer, "nether/root");
		Advancement advancement2 = Advancement.Builder.advancement()
			.parent(advancement)
			.display(
				Items.FIRE_CHARGE,
				new TranslatableComponent("advancements.nether.return_to_sender.title"),
				new TranslatableComponent("advancements.nether.return_to_sender.description"),
				null,
				FrameType.CHALLENGE,
				true,
				true,
				false
			)
			.rewards(AdvancementRewards.Builder.experience(50))
			.addCriterion(
				"killed_ghast",
				KilledTrigger.TriggerInstance.playerKilledEntity(
					EntityPredicate.Builder.entity().of(EntityType.GHAST),
					DamageSourcePredicate.Builder.damageType().isProjectile(true).direct(EntityPredicate.Builder.entity().of(EntityType.FIREBALL))
				)
			)
			.save(consumer, "nether/return_to_sender");
		Advancement advancement3 = Advancement.Builder.advancement()
			.parent(advancement)
			.display(
				Blocks.NETHER_BRICKS,
				new TranslatableComponent("advancements.nether.find_fortress.title"),
				new TranslatableComponent("advancements.nether.find_fortress.description"),
				null,
				FrameType.TASK,
				true,
				true,
				false
			)
			.addCriterion("fortress", LocationTrigger.TriggerInstance.located(LocationPredicate.inFeature(StructureFeature.NETHER_BRIDGE)))
			.save(consumer, "nether/find_fortress");
		Advancement.Builder.advancement()
			.parent(advancement)
			.display(
				Items.MAP,
				new TranslatableComponent("advancements.nether.fast_travel.title"),
				new TranslatableComponent("advancements.nether.fast_travel.description"),
				null,
				FrameType.CHALLENGE,
				true,
				true,
				false
			)
			.rewards(AdvancementRewards.Builder.experience(100))
			.addCriterion("travelled", NetherTravelTrigger.TriggerInstance.travelledThroughNether(DistancePredicate.horizontal(MinMaxBounds.Floats.atLeast(7000.0F))))
			.save(consumer, "nether/fast_travel");
		Advancement.Builder.advancement()
			.parent(advancement2)
			.display(
				Items.GHAST_TEAR,
				new TranslatableComponent("advancements.nether.uneasy_alliance.title"),
				new TranslatableComponent("advancements.nether.uneasy_alliance.description"),
				null,
				FrameType.CHALLENGE,
				true,
				true,
				false
			)
			.rewards(AdvancementRewards.Builder.experience(100))
			.addCriterion(
				"killed_ghast",
				KilledTrigger.TriggerInstance.playerKilledEntity(
					EntityPredicate.Builder.entity().of(EntityType.GHAST).located(LocationPredicate.inDimension(Level.OVERWORLD))
				)
			)
			.save(consumer, "nether/uneasy_alliance");
		Advancement advancement4 = Advancement.Builder.advancement()
			.parent(advancement3)
			.display(
				Blocks.WITHER_SKELETON_SKULL,
				new TranslatableComponent("advancements.nether.get_wither_skull.title"),
				new TranslatableComponent("advancements.nether.get_wither_skull.description"),
				null,
				FrameType.TASK,
				true,
				true,
				false
			)
			.addCriterion("wither_skull", InventoryChangeTrigger.TriggerInstance.hasItems(Blocks.WITHER_SKELETON_SKULL))
			.save(consumer, "nether/get_wither_skull");
		Advancement advancement5 = Advancement.Builder.advancement()
			.parent(advancement4)
			.display(
				Items.NETHER_STAR,
				new TranslatableComponent("advancements.nether.summon_wither.title"),
				new TranslatableComponent("advancements.nether.summon_wither.description"),
				null,
				FrameType.TASK,
				true,
				true,
				false
			)
			.addCriterion("summoned", SummonedEntityTrigger.TriggerInstance.summonedEntity(EntityPredicate.Builder.entity().of(EntityType.WITHER)))
			.save(consumer, "nether/summon_wither");
		Advancement advancement6 = Advancement.Builder.advancement()
			.parent(advancement3)
			.display(
				Items.BLAZE_ROD,
				new TranslatableComponent("advancements.nether.obtain_blaze_rod.title"),
				new TranslatableComponent("advancements.nether.obtain_blaze_rod.description"),
				null,
				FrameType.TASK,
				true,
				true,
				false
			)
			.addCriterion("blaze_rod", InventoryChangeTrigger.TriggerInstance.hasItems(Items.BLAZE_ROD))
			.save(consumer, "nether/obtain_blaze_rod");
		Advancement advancement7 = Advancement.Builder.advancement()
			.parent(advancement5)
			.display(
				Blocks.BEACON,
				new TranslatableComponent("advancements.nether.create_beacon.title"),
				new TranslatableComponent("advancements.nether.create_beacon.description"),
				null,
				FrameType.TASK,
				true,
				true,
				false
			)
			.addCriterion("beacon", ConstructBeaconTrigger.TriggerInstance.constructedBeacon(MinMaxBounds.Ints.atLeast(1)))
			.save(consumer, "nether/create_beacon");
		Advancement.Builder.advancement()
			.parent(advancement7)
			.display(
				Blocks.BEACON,
				new TranslatableComponent("advancements.nether.create_full_beacon.title"),
				new TranslatableComponent("advancements.nether.create_full_beacon.description"),
				null,
				FrameType.GOAL,
				true,
				true,
				false
			)
			.addCriterion("beacon", ConstructBeaconTrigger.TriggerInstance.constructedBeacon(MinMaxBounds.Ints.exactly(4)))
			.save(consumer, "nether/create_full_beacon");
		Advancement advancement8 = Advancement.Builder.advancement()
			.parent(advancement6)
			.display(
				Items.POTION,
				new TranslatableComponent("advancements.nether.brew_potion.title"),
				new TranslatableComponent("advancements.nether.brew_potion.description"),
				null,
				FrameType.TASK,
				true,
				true,
				false
			)
			.addCriterion("potion", BrewedPotionTrigger.TriggerInstance.brewedPotion())
			.save(consumer, "nether/brew_potion");
		Advancement advancement9 = Advancement.Builder.advancement()
			.parent(advancement8)
			.display(
				Items.MILK_BUCKET,
				new TranslatableComponent("advancements.nether.all_potions.title"),
				new TranslatableComponent("advancements.nether.all_potions.description"),
				null,
				FrameType.CHALLENGE,
				true,
				true,
				false
			)
			.rewards(AdvancementRewards.Builder.experience(100))
			.addCriterion(
				"all_effects",
				EffectsChangedTrigger.TriggerInstance.hasEffects(
					MobEffectsPredicate.effects()
						.and(MobEffects.MOVEMENT_SPEED)
						.and(MobEffects.MOVEMENT_SLOWDOWN)
						.and(MobEffects.DAMAGE_BOOST)
						.and(MobEffects.JUMP)
						.and(MobEffects.REGENERATION)
						.and(MobEffects.FIRE_RESISTANCE)
						.and(MobEffects.WATER_BREATHING)
						.and(MobEffects.INVISIBILITY)
						.and(MobEffects.NIGHT_VISION)
						.and(MobEffects.WEAKNESS)
						.and(MobEffects.POISON)
						.and(MobEffects.SLOW_FALLING)
						.and(MobEffects.DAMAGE_RESISTANCE)
				)
			)
			.save(consumer, "nether/all_potions");
		Advancement.Builder.advancement()
			.parent(advancement9)
			.display(
				Items.BUCKET,
				new TranslatableComponent("advancements.nether.all_effects.title"),
				new TranslatableComponent("advancements.nether.all_effects.description"),
				null,
				FrameType.CHALLENGE,
				true,
				true,
				true
			)
			.rewards(AdvancementRewards.Builder.experience(1000))
			.addCriterion(
				"all_effects",
				EffectsChangedTrigger.TriggerInstance.hasEffects(
					MobEffectsPredicate.effects()
						.and(MobEffects.MOVEMENT_SPEED)
						.and(MobEffects.MOVEMENT_SLOWDOWN)
						.and(MobEffects.DAMAGE_BOOST)
						.and(MobEffects.JUMP)
						.and(MobEffects.REGENERATION)
						.and(MobEffects.FIRE_RESISTANCE)
						.and(MobEffects.WATER_BREATHING)
						.and(MobEffects.INVISIBILITY)
						.and(MobEffects.NIGHT_VISION)
						.and(MobEffects.WEAKNESS)
						.and(MobEffects.POISON)
						.and(MobEffects.WITHER)
						.and(MobEffects.DIG_SPEED)
						.and(MobEffects.DIG_SLOWDOWN)
						.and(MobEffects.LEVITATION)
						.and(MobEffects.GLOWING)
						.and(MobEffects.ABSORPTION)
						.and(MobEffects.HUNGER)
						.and(MobEffects.CONFUSION)
						.and(MobEffects.DAMAGE_RESISTANCE)
						.and(MobEffects.SLOW_FALLING)
						.and(MobEffects.CONDUIT_POWER)
						.and(MobEffects.DOLPHINS_GRACE)
						.and(MobEffects.BLINDNESS)
						.and(MobEffects.BAD_OMEN)
						.and(MobEffects.HERO_OF_THE_VILLAGE)
				)
			)
			.save(consumer, "nether/all_effects");
		Advancement advancement10 = Advancement.Builder.advancement()
			.parent(advancement)
			.display(
				Items.ANCIENT_DEBRIS,
				new TranslatableComponent("advancements.nether.obtain_ancient_debris.title"),
				new TranslatableComponent("advancements.nether.obtain_ancient_debris.description"),
				null,
				FrameType.TASK,
				true,
				true,
				false
			)
			.addCriterion("ancient_debris", InventoryChangeTrigger.TriggerInstance.hasItems(Items.ANCIENT_DEBRIS))
			.save(consumer, "nether/obtain_ancient_debris");
		Advancement.Builder.advancement()
			.parent(advancement10)
			.display(
				Items.NETHERITE_CHESTPLATE,
				new TranslatableComponent("advancements.nether.netherite_armor.title"),
				new TranslatableComponent("advancements.nether.netherite_armor.description"),
				null,
				FrameType.CHALLENGE,
				true,
				true,
				false
			)
			.rewards(AdvancementRewards.Builder.experience(100))
			.addCriterion(
				"netherite_armor",
				InventoryChangeTrigger.TriggerInstance.hasItems(Items.NETHERITE_HELMET, Items.NETHERITE_CHESTPLATE, Items.NETHERITE_LEGGINGS, Items.NETHERITE_BOOTS)
			)
			.save(consumer, "nether/netherite_armor");
		Advancement.Builder.advancement()
			.parent(advancement10)
			.display(
				Items.LODESTONE,
				new TranslatableComponent("advancements.nether.use_lodestone.title"),
				new TranslatableComponent("advancements.nether.use_lodestone.description"),
				null,
				FrameType.TASK,
				true,
				true,
				false
			)
			.addCriterion(
				"use_lodestone",
				ItemUsedOnBlockTrigger.TriggerInstance.itemUsedOnBlock(
					LocationPredicate.Builder.location().setBlock(BlockPredicate.Builder.block().of(Blocks.LODESTONE).build()),
					ItemPredicate.Builder.item().of(Items.COMPASS)
				)
			)
			.save(consumer, "nether/use_lodestone");
		Advancement advancement11 = Advancement.Builder.advancement()
			.parent(advancement)
			.display(
				Items.CRYING_OBSIDIAN,
				new TranslatableComponent("advancements.nether.obtain_crying_obsidian.title"),
				new TranslatableComponent("advancements.nether.obtain_crying_obsidian.description"),
				null,
				FrameType.TASK,
				true,
				true,
				false
			)
			.addCriterion("crying_obsidian", InventoryChangeTrigger.TriggerInstance.hasItems(Items.CRYING_OBSIDIAN))
			.save(consumer, "nether/obtain_crying_obsidian");
		Advancement.Builder.advancement()
			.parent(advancement11)
			.display(
				Items.RESPAWN_ANCHOR,
				new TranslatableComponent("advancements.nether.charge_respawn_anchor.title"),
				new TranslatableComponent("advancements.nether.charge_respawn_anchor.description"),
				null,
				FrameType.TASK,
				true,
				true,
				false
			)
			.addCriterion(
				"charge_respawn_anchor",
				ItemUsedOnBlockTrigger.TriggerInstance.itemUsedOnBlock(
					LocationPredicate.Builder.location()
						.setBlock(
							BlockPredicate.Builder.block()
								.of(Blocks.RESPAWN_ANCHOR)
								.setProperties(StatePropertiesPredicate.Builder.properties().hasProperty(RespawnAnchorBlock.CHARGE, 4).build())
								.build()
						),
					ItemPredicate.Builder.item().of(Blocks.GLOWSTONE)
				)
			)
			.save(consumer, "nether/charge_respawn_anchor");
		Advancement advancement12 = Advancement.Builder.advancement()
			.parent(advancement)
			.display(
				Items.WARPED_FUNGUS_ON_A_STICK,
				new TranslatableComponent("advancements.nether.ride_strider.title"),
				new TranslatableComponent("advancements.nether.ride_strider.description"),
				null,
				FrameType.TASK,
				true,
				true,
				false
			)
			.addCriterion(
				"used_warped_fungus_on_a_stick",
				ItemDurabilityTrigger.TriggerInstance.changedDurability(
					EntityPredicate.Composite.wrap(EntityPredicate.Builder.entity().vehicle(EntityPredicate.Builder.entity().of(EntityType.STRIDER).build()).build()),
					ItemPredicate.Builder.item().of(Items.WARPED_FUNGUS_ON_A_STICK).build(),
					MinMaxBounds.Ints.ANY
				)
			)
			.save(consumer, "nether/ride_strider");
		AdventureAdvancements.addBiomes(Advancement.Builder.advancement(), EXPLORABLE_BIOMES)
			.parent(advancement12)
			.display(
				Items.NETHERITE_BOOTS,
				new TranslatableComponent("advancements.nether.explore_nether.title"),
				new TranslatableComponent("advancements.nether.explore_nether.description"),
				null,
				FrameType.CHALLENGE,
				true,
				true,
				false
			)
			.rewards(AdvancementRewards.Builder.experience(500))
			.save(consumer, "nether/explore_nether");
		Advancement advancement13 = Advancement.Builder.advancement()
			.parent(advancement)
			.display(
				Items.POLISHED_BLACKSTONE_BRICKS,
				new TranslatableComponent("advancements.nether.find_bastion.title"),
				new TranslatableComponent("advancements.nether.find_bastion.description"),
				null,
				FrameType.TASK,
				true,
				true,
				false
			)
			.addCriterion("bastion", LocationTrigger.TriggerInstance.located(LocationPredicate.inFeature(StructureFeature.BASTION_REMNANT)))
			.save(consumer, "nether/find_bastion");
		Advancement.Builder.advancement()
			.parent(advancement13)
			.display(
				Blocks.CHEST,
				new TranslatableComponent("advancements.nether.loot_bastion.title"),
				new TranslatableComponent("advancements.nether.loot_bastion.description"),
				null,
				FrameType.TASK,
				true,
				true,
				false
			)
			.requirements(RequirementsStrategy.OR)
			.addCriterion("loot_bastion_other", LootTableTrigger.TriggerInstance.lootTableUsed(new ResourceLocation("minecraft:chests/bastion_other")))
			.addCriterion("loot_bastion_treasure", LootTableTrigger.TriggerInstance.lootTableUsed(new ResourceLocation("minecraft:chests/bastion_treasure")))
			.addCriterion("loot_bastion_hoglin_stable", LootTableTrigger.TriggerInstance.lootTableUsed(new ResourceLocation("minecraft:chests/bastion_hoglin_stable")))
			.addCriterion("loot_bastion_bridge", LootTableTrigger.TriggerInstance.lootTableUsed(new ResourceLocation("minecraft:chests/bastion_bridge")))
			.save(consumer, "nether/loot_bastion");
		Advancement.Builder.advancement()
			.parent(advancement)
			.display(
				Items.GOLD_INGOT,
				new TranslatableComponent("advancements.nether.distract_piglin.title"),
				new TranslatableComponent("advancements.nether.distract_piglin.description"),
				null,
				FrameType.TASK,
				true,
				true,
				false
			)
			.addCriterion(
				"distract_piglin",
				ItemPickedUpByEntityTrigger.TriggerInstance.itemPickedUpByEntity(
					EntityPredicate.Composite.create(
						LootItemEntityPropertyCondition.hasProperties(
								LootContext.EntityTarget.THIS,
								EntityPredicate.Builder.entity()
									.equipment(EntityEquipmentPredicate.Builder.equipment().head(ItemPredicate.Builder.item().of(Items.GOLDEN_HELMET).build()).build())
							)
							.invert()
							.build(),
						LootItemEntityPropertyCondition.hasProperties(
								LootContext.EntityTarget.THIS,
								EntityPredicate.Builder.entity()
									.equipment(EntityEquipmentPredicate.Builder.equipment().chest(ItemPredicate.Builder.item().of(Items.GOLDEN_CHESTPLATE).build()).build())
							)
							.invert()
							.build(),
						LootItemEntityPropertyCondition.hasProperties(
								LootContext.EntityTarget.THIS,
								EntityPredicate.Builder.entity()
									.equipment(EntityEquipmentPredicate.Builder.equipment().legs(ItemPredicate.Builder.item().of(Items.GOLDEN_LEGGINGS).build()).build())
							)
							.invert()
							.build(),
						LootItemEntityPropertyCondition.hasProperties(
								LootContext.EntityTarget.THIS,
								EntityPredicate.Builder.entity()
									.equipment(EntityEquipmentPredicate.Builder.equipment().feet(ItemPredicate.Builder.item().of(Items.GOLDEN_BOOTS).build()).build())
							)
							.invert()
							.build()
					),
					ItemPredicate.Builder.item().of(ItemTags.PIGLIN_LOVED),
					EntityPredicate.Composite.wrap(
						EntityPredicate.Builder.entity().of(EntityType.PIGLIN).flags(EntityFlagsPredicate.Builder.flags().setIsBaby(false).build()).build()
					)
				)
			)
			.save(consumer, "nether/distract_piglin");
	}
}
