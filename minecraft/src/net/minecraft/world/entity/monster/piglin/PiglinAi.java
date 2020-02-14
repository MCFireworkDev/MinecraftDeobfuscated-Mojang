package net.minecraft.world.entity.monster.piglin;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Lists;
import com.mojang.datafixers.Dynamic;
import com.mojang.datafixers.util.Pair;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import net.minecraft.core.BlockPos;
import net.minecraft.core.SerializableUUID;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.IntRange;
import net.minecraft.util.TimeUtil;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntitySelector;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.Brain;
import net.minecraft.world.entity.ai.behavior.AdmireHeldItem;
import net.minecraft.world.entity.ai.behavior.BackUpIfTooClose;
import net.minecraft.world.entity.ai.behavior.BehaviorUtils;
import net.minecraft.world.entity.ai.behavior.CopyMemoryWithExpiry;
import net.minecraft.world.entity.ai.behavior.CrossbowAttack;
import net.minecraft.world.entity.ai.behavior.DismountOrSkipMounting;
import net.minecraft.world.entity.ai.behavior.DoNothing;
import net.minecraft.world.entity.ai.behavior.EraseMemoryIf;
import net.minecraft.world.entity.ai.behavior.GoToCelebrateLocation;
import net.minecraft.world.entity.ai.behavior.GoToWantedItem;
import net.minecraft.world.entity.ai.behavior.InteractWith;
import net.minecraft.world.entity.ai.behavior.InteractWithDoor;
import net.minecraft.world.entity.ai.behavior.LookAtTargetSink;
import net.minecraft.world.entity.ai.behavior.MeleeAttack;
import net.minecraft.world.entity.ai.behavior.Mount;
import net.minecraft.world.entity.ai.behavior.MoveToTargetSink;
import net.minecraft.world.entity.ai.behavior.RandomStroll;
import net.minecraft.world.entity.ai.behavior.RememberIfHoglinWasKilled;
import net.minecraft.world.entity.ai.behavior.RunIf;
import net.minecraft.world.entity.ai.behavior.RunOne;
import net.minecraft.world.entity.ai.behavior.RunSometimes;
import net.minecraft.world.entity.ai.behavior.SetEntityLookTarget;
import net.minecraft.world.entity.ai.behavior.SetLookAndInteract;
import net.minecraft.world.entity.ai.behavior.SetWalkTargetAwayFrom;
import net.minecraft.world.entity.ai.behavior.SetWalkTargetFromAttackTargetIfTargetOutOfReach;
import net.minecraft.world.entity.ai.behavior.SetWalkTargetFromLookTarget;
import net.minecraft.world.entity.ai.behavior.StartAdmiringItemIfSeen;
import net.minecraft.world.entity.ai.behavior.StartAttacking;
import net.minecraft.world.entity.ai.behavior.StartCelebratingIfTargetDead;
import net.minecraft.world.entity.ai.behavior.StartHuntingHoglin;
import net.minecraft.world.entity.ai.behavior.StopAdmiringIfItemTooFarAway;
import net.minecraft.world.entity.ai.behavior.StopAttackingIfTargetInvalid;
import net.minecraft.world.entity.ai.behavior.StopBeingAngryIfTargetDead;
import net.minecraft.world.entity.ai.behavior.StopHoldingItemIfNoLongerAdmiring;
import net.minecraft.world.entity.ai.memory.MemoryModuleType;
import net.minecraft.world.entity.ai.util.RandomPos;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.monster.WitherSkeleton;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.schedule.Activity;
import net.minecraft.world.item.ArmorItem;
import net.minecraft.world.item.ArmorMaterials;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.TieredItem;
import net.minecraft.world.item.Tiers;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.storage.loot.BuiltInLootTables;
import net.minecraft.world.level.storage.loot.LootContext;
import net.minecraft.world.level.storage.loot.LootTable;
import net.minecraft.world.level.storage.loot.parameters.LootContextParamSets;
import net.minecraft.world.level.storage.loot.parameters.LootContextParams;
import net.minecraft.world.phys.Vec3;

public class PiglinAi {
	private static final IntRange TIME_BETWEEN_HUNTS = TimeUtil.rangeOfSeconds(30, 120);
	private static final IntRange RIDE_START_INTERVAL = TimeUtil.rangeOfSeconds(5, 20);
	private static final IntRange RIDE_DURATION = TimeUtil.rangeOfSeconds(10, 30);
	private static final IntRange RETREAT_DURATION = TimeUtil.rangeOfSeconds(5, 20);
	private static final Set FOOD_ITEMS = ImmutableSet.of(Items.PORKCHOP, Items.COOKED_PORKCHOP);
	private static final Set<Pair<Integer, Item>> BARTER_RESPONSE_ITEMS_WEIGHTED = ImmutableSet.of(
		Pair.of(1, Items.SHROOMLIGHT),
		Pair.of(1, Items.OBSIDIAN),
		Pair.of(2, Items.QUARTZ),
		Pair.of(2, Items.GLOWSTONE_DUST),
		Pair.of(2, Items.MAGMA_CREAM),
		Pair.of(2, Items.ENDER_PEARL),
		Pair.of(2, Items.WARPED_FUNGI),
		Pair.of(5, Items.GRAVEL),
		Pair.of(5, Items.SOUL_SAND),
		Pair.of(5, Items.FIRE_CHARGE),
		Pair.of(10, Items.COOKED_PORKCHOP),
		Pair.of(10, Items.LEATHER),
		Pair.of(10, Items.NETHER_BRICK),
		Pair.of(10, Items.RED_MUSHROOM),
		Pair.of(10, Items.BROWN_MUSHROOM),
		Pair.of(10, Items.FLINT),
		Pair.of(10, Items.ROTTEN_FLESH),
		Pair.of(10, Items.CRIMSON_FUNGI)
	);
	private static final Set<Item> PICK_UP_ITEMS = ImmutableSet.of(Items.PORKCHOP, Items.COOKED_PORKCHOP, Items.GOLD_NUGGET);
	private static final Set<Item> LOVED_ITEMS_IN_ADDITION_TO_GOLD_TIER_AND_GOLD_MATERIAL = ImmutableSet.of(
		Items.GOLD_INGOT,
		Items.GOLDEN_APPLE,
		Items.GOLDEN_HORSE_ARMOR,
		Items.GOLDEN_CARROT,
		Items.GOLD_BLOCK,
		Items.GOLD_ORE,
		Items.ENCHANTED_GOLDEN_APPLE,
		Items.GOLDEN_HORSE_ARMOR
	);

	protected static Brain<?> makeBrain(Piglin piglin, Dynamic<?> dynamic) {
		Brain<Piglin> brain = new Brain<>(Piglin.MEMORY_TYPES, Piglin.SENSOR_TYPES, dynamic);
		initCoreActivity(piglin, brain);
		initIdleActivity(piglin, brain);
		initAdmireItemActivity(piglin, brain);
		initFightActivity(piglin, brain);
		initCelebrateActivity(piglin, brain);
		initRetreatActivity(piglin, brain);
		initRidePiglinActivity(piglin, brain);
		brain.setCoreActivities(ImmutableSet.of(Activity.CORE));
		brain.setDefaultActivity(Activity.IDLE);
		brain.useDefaultActivity();
		initMemories(piglin.level, brain);
		return brain;
	}

	private static void initMemories(Level level, Brain<Piglin> brain) {
		int i = TIME_BETWEEN_HUNTS.randomValue(level.random);
		brain.setMemoryWithExpiry(MemoryModuleType.HUNTED_RECENTLY, true, level.getGameTime(), (long)i);
	}

	private static void initCoreActivity(Piglin piglin, Brain<Piglin> brain) {
		brain.addActivity(
			Activity.CORE,
			0,
			ImmutableList.of(
				new LookAtTargetSink(45, 90),
				new MoveToTargetSink(200),
				new InteractWithDoor(),
				new StopHoldingItemIfNoLongerAdmiring<>(),
				new StartAdmiringItemIfSeen(120),
				new StartCelebratingIfTargetDead(300),
				new StopBeingAngryIfTargetDead()
			)
		);
	}

	private static void initIdleActivity(Piglin piglin, Brain<Piglin> brain) {
		float f = piglin.getMovementSpeed();
		brain.addActivity(
			Activity.IDLE,
			10,
			ImmutableList.of(
				new SetEntityLookTarget(PiglinAi::isPlayerHoldingLovedItem, 14.0F),
				new StartAttacking<>(Piglin::isAdult, PiglinAi::findNearestValidAttackTarget),
				new StartHuntingHoglin(),
				avoidZombifiedPiglin(f),
				avoidSoulFire(f),
				babySometimesRideBabyHoglin(),
				createIdleLookBehaviors(),
				createIdleMovementBehaviors(f),
				new SetLookAndInteract(EntityType.PLAYER, 4)
			)
		);
	}

	private static void initFightActivity(Piglin piglin, Brain<Piglin> brain) {
		float f = piglin.getMovementSpeed();
		brain.addActivityAndRemoveMemoryWhenStopped(
			Activity.FIGHT,
			10,
			ImmutableList.of(
				new StopAttackingIfTargetInvalid<>(livingEntity -> !isNearestValidAttackTarget(piglin, livingEntity)),
				new RunIf(PiglinAi::hasCrossbow, new BackUpIfTooClose<>(5, 0.75F)),
				new SetWalkTargetFromAttackTargetIfTargetOutOfReach(f * 1.2F),
				new MeleeAttack(1.5, 20),
				new CrossbowAttack(),
				new RememberIfHoglinWasKilled()
			),
			MemoryModuleType.ATTACK_TARGET
		);
	}

	private static void initCelebrateActivity(Piglin piglin, Brain<Piglin> brain) {
		float f = piglin.getMovementSpeed();
		brain.addActivityAndRemoveMemoryWhenStopped(
			Activity.CELEBRATE,
			10,
			ImmutableList.of(
				avoidZombifiedPiglin(f),
				avoidSoulFire(f),
				new SetEntityLookTarget(PiglinAi::isPlayerHoldingLovedItem, 14.0F),
				new StartAttacking(Piglin::isAdult, PiglinAi::findNearestValidAttackTarget),
				new GoToCelebrateLocation(2),
				new RunOne(
					ImmutableList.of(Pair.of(new SetEntityLookTarget(EntityType.PIGLIN, 8.0F), 1), Pair.of(new RandomStroll(f, 2, 1), 1), Pair.of(new DoNothing(10, 20), 1))
				)
			),
			MemoryModuleType.CELEBRATE_LOCATION
		);
	}

	private static void initAdmireItemActivity(Piglin piglin, Brain<Piglin> brain) {
		float f = piglin.getMovementSpeed();
		brain.addActivityAndRemoveMemoryWhenStopped(
			Activity.ADMIRE_ITEM,
			10,
			ImmutableList.of(new GoToWantedItem<>(Piglin::isOffHandEmpty, 9, true), new AdmireHeldItem(f * 0.5F), new StopAdmiringIfItemTooFarAway(9)),
			MemoryModuleType.ADMIRING_ITEM
		);
	}

	private static void initRetreatActivity(Piglin piglin, Brain<Piglin> brain) {
		float f = piglin.getMovementSpeed() * 1.3F;
		brain.addActivityAndRemoveMemoryWhenStopped(
			Activity.AVOID,
			10,
			ImmutableList.of(
				SetWalkTargetAwayFrom.entity(MemoryModuleType.AVOID_TARGET, f, 6, false),
				createIdleLookBehaviors(),
				createIdleMovementBehaviors(piglin.getMovementSpeed()),
				new EraseMemoryIf(PiglinAi::wantsToStopFleeing, MemoryModuleType.AVOID_TARGET)
			),
			MemoryModuleType.AVOID_TARGET
		);
	}

	private static void initRidePiglinActivity(Piglin piglin, Brain<Piglin> brain) {
		brain.addActivityAndRemoveMemoryWhenStopped(
			Activity.RIDE,
			10,
			ImmutableList.of(
				new Mount<>(),
				new SetEntityLookTarget(PiglinAi::isPlayerHoldingLovedItem, 8.0F),
				new RunIf(Piglin::isRiding, createIdleLookBehaviors()),
				new DismountOrSkipMounting(8, PiglinAi::wantsToStopRiding)
			),
			MemoryModuleType.RIDE_TARGET
		);
	}

	private static RunOne<Piglin> createIdleLookBehaviors() {
		return new RunOne<>(
			ImmutableList.of(
				Pair.of(new SetEntityLookTarget(EntityType.PLAYER, 8.0F), 1),
				Pair.of(new SetEntityLookTarget(EntityType.PIGLIN, 8.0F), 1),
				Pair.of(new SetEntityLookTarget(8.0F), 1),
				Pair.of(new DoNothing(30, 60), 1)
			)
		);
	}

	private static RunOne<Piglin> createIdleMovementBehaviors(float f) {
		return new RunOne<>(
			ImmutableList.of(
				Pair.of(new RandomStroll(f), 2),
				Pair.of(InteractWith.of(EntityType.PIGLIN, 8, MemoryModuleType.INTERACTION_TARGET, f, 2), 2),
				Pair.of(new RunIf<>(PiglinAi::doesntSeeAnyPlayerHoldingLovedItem, new SetWalkTargetFromLookTarget(f, 3)), 2),
				Pair.of(new DoNothing(30, 60), 1)
			)
		);
	}

	private static SetWalkTargetAwayFrom<BlockPos> avoidSoulFire(float f) {
		return SetWalkTargetAwayFrom.pos(MemoryModuleType.NEAREST_SOUL_FIRE, f * 1.5F, 8, false);
	}

	private static SetWalkTargetAwayFrom<?> avoidZombifiedPiglin(float f) {
		return SetWalkTargetAwayFrom.entity(MemoryModuleType.NEAREST_VISIBLE_ZOMBIFIED_PIGLIN, f * 1.5F, 10, false);
	}

	protected static void updateActivity(Piglin piglin) {
		Brain<Piglin> brain = piglin.getBrain();
		Activity activity = (Activity)brain.getActiveNonCoreActivity().orElse(null);
		brain.setActiveActivityToFirstValid(ImmutableList.of(Activity.ADMIRE_ITEM, Activity.FIGHT, Activity.AVOID, Activity.CELEBRATE, Activity.RIDE, Activity.IDLE));
		Activity activity2 = (Activity)brain.getActiveNonCoreActivity().orElse(null);
		if (activity != activity2) {
			playActivitySound(piglin);
		}

		piglin.setAggressive(brain.hasMemoryValue(MemoryModuleType.ATTACK_TARGET));
		if (!brain.hasMemoryValue(MemoryModuleType.RIDE_TARGET)) {
			piglin.stopRiding();
		}

		if (piglin.isRiding() && seesPlayerHoldingWantedItem(piglin)) {
			piglin.stopRiding();
			piglin.getBrain().eraseMemory(MemoryModuleType.RIDE_TARGET);
		}
	}

	protected static void pickUpItem(Piglin piglin, ItemEntity itemEntity) {
		piglin.take(itemEntity, 1);
		ItemStack itemStack = itemEntity.getItem();
		ItemStack itemStack2 = itemStack.split(1);
		if (isLovedItem(itemStack2.getItem())) {
			piglin.setItemInHand(InteractionHand.OFF_HAND, itemStack2);
			admireGoldItem(piglin);
			if (piglin.isAdult() && isBarterCurrency(itemStack2.getItem())) {
				throwBarterItem(piglin);
			}
		} else {
			piglin.equipItemIfPossible(itemStack2);
		}

		if (isFood(itemStack2.getItem()) && !hasEatenRecently(piglin)) {
			eat(piglin);
		}

		if (itemStack.isEmpty()) {
			itemEntity.remove();
		} else {
			itemEntity.setItem(itemStack);
		}

		stopWalking(piglin);
	}

	public static void stopHoldingOffHandItem(Piglin piglin) {
		ItemStack itemStack = piglin.getItemInHand(InteractionHand.OFF_HAND);
		if (!itemStack.isEmpty()) {
			piglin.setItemInHand(InteractionHand.OFF_HAND, ItemStack.EMPTY);
			if (!isBarterCurrency(itemStack.getItem())) {
				if (!piglin.equipItemIfPossible(itemStack)) {
					ItemStack itemStack2 = piglin.addToInventory(itemStack);
					throwItemStack(piglin, itemStack2, getRandomNearbyPos(piglin));
				}
			}
		}
	}

	private static void throwBarterItem(Piglin piglin) {
		Optional<Player> optional = piglin.getBrain().getMemory(MemoryModuleType.NEAREST_VISIBLE_PLAYER);
		if (optional.isPresent()) {
			throwBarterItemAtPlayer(piglin, (Player)optional.get());
		} else {
			throwBarterItemAtRandomPos(piglin);
		}
	}

	private static void throwBarterItemAtRandomPos(Piglin piglin) {
		throwItemStack(piglin, getBarterResponseItem(piglin), getRandomNearbyPos(piglin));
	}

	private static void throwBarterItemAtPlayer(Piglin piglin, Player player) {
		throwItemStack(piglin, getBarterResponseItem(piglin), player.position());
	}

	private static void throwItemStack(Piglin piglin, ItemStack itemStack, Vec3 vec3) {
		piglin.swing(InteractionHand.OFF_HAND);
		if (!itemStack.isEmpty()) {
			BehaviorUtils.throwItem(piglin, itemStack, vec3.add(0.0, 1.0, 0.0));
		}
	}

	private static ItemStack getBarterResponseItem(Piglin piglin) {
		LootTable lootTable = piglin.level.getServer().getLootTables().get(BuiltInLootTables.PIGLIN_BARTERING);
		List<ItemStack> list = lootTable.getRandomItems(
			new LootContext.Builder((ServerLevel)piglin.level)
				.withParameter(LootContextParams.THIS_ENTITY, piglin)
				.withRandom(piglin.level.random)
				.create(LootContextParamSets.PIGLIN_BARTER)
		);
		return list.isEmpty() ? ItemStack.EMPTY : (ItemStack)list.get(0);
	}

	protected static boolean wantsToPickUp(Piglin piglin, ItemStack itemStack) {
		Item item = itemStack.getItem();
		if (piglin.isOffHandEmpty() && !wasHitByPlayer(piglin) && (!isFood(item) || !hasEatenRecently(piglin))) {
			return PICK_UP_ITEMS.contains(item) || isLovedItem(item) || isBetterThanCurrentItem(piglin, itemStack);
		} else {
			return false;
		}
	}

	private static boolean isBetterThanCurrentItem(Piglin piglin, ItemStack itemStack) {
		EquipmentSlot equipmentSlot = Mob.getEquipmentSlotForItem(itemStack);
		ItemStack itemStack2 = piglin.getItemBySlot(equipmentSlot);
		return piglin.canReplaceCurrentItem(itemStack, itemStack2, equipmentSlot);
	}

	public static boolean isLovedItem(Item item) {
		return LOVED_ITEMS_IN_ADDITION_TO_GOLD_TIER_AND_GOLD_MATERIAL.contains(item)
			|| item instanceof TieredItem && ((TieredItem)item).getTier() == Tiers.GOLD
			|| item instanceof ArmorItem && ((ArmorItem)item).getMaterial() == ArmorMaterials.GOLD;
	}

	private static boolean wantsToStopRiding(Piglin piglin, Entity entity) {
		if (!(entity instanceof Mob)) {
			return false;
		} else {
			Mob mob = (Mob)entity;
			return !mob.isBaby() || !mob.isAlive() || wasHurtRecently(piglin) || wasHurtRecently(mob) || mob instanceof Piglin && mob.getVehicle() == null;
		}
	}

	private static boolean isNearestValidAttackTarget(Piglin piglin, LivingEntity livingEntity) {
		return findNearestValidAttackTarget(piglin).filter(livingEntity2 -> livingEntity2 == livingEntity).isPresent();
	}

	private static Optional<? extends LivingEntity> findNearestValidAttackTarget(Piglin piglin) {
		Brain<Piglin> brain = piglin.getBrain();
		Optional<LivingEntity> optional = BehaviorUtils.getLivingEntityFromUUIDMemory(piglin, MemoryModuleType.ANGRY_AT);
		if (optional.isPresent() && isAttackAllowed((LivingEntity)optional.get())) {
			return optional;
		} else {
			Optional<WitherSkeleton> optional2 = brain.getMemory(MemoryModuleType.NEAREST_VISIBLE_WITHER_SKELETON);
			if (optional2.isPresent() && seesAdultPiglins(piglin)) {
				return optional2;
			} else {
				Optional<Player> optional3 = brain.getMemory(MemoryModuleType.NEAREST_TARGETABLE_PLAYER_NOT_WEARING_GOLD);
				return optional3.isPresent() && isAttackAllowed((LivingEntity)optional3.get()) ? optional3 : Optional.empty();
			}
		}
	}

	public static void angerNearbyPiglinsThatSee(Player player) {
		if (isAttackAllowed(player)) {
			List<Piglin> list = player.level.getEntitiesOfClass(Piglin.class, player.getBoundingBox().inflate(16.0));
			list.stream().filter(PiglinAi::isIdle).filter(piglin -> BehaviorUtils.canSee(piglin, player)).forEach(piglin -> setAngerTarget(piglin, player));
		}
	}

	public static boolean mobInteract(Piglin piglin, Player player, InteractionHand interactionHand) {
		ItemStack itemStack = player.getItemInHand(interactionHand);
		Item item = itemStack.getItem();
		if (!isAdmiringItem(piglin) && piglin.isAdult() && isBarterCurrency(item) && !wasHitByPlayer(piglin)) {
			itemStack.shrink(1);
			piglin.setItemInHand(InteractionHand.OFF_HAND, new ItemStack(item, 1));
			admireGoldItem(piglin);
			throwBarterItemAtPlayer(piglin, player);
			return true;
		} else {
			return false;
		}
	}

	protected static void wasHurtBy(Piglin piglin, LivingEntity livingEntity) {
		piglin.playHurtSound();
		if (!(livingEntity instanceof Piglin)) {
			stopHoldingOffHandItem(piglin);
			Brain<Piglin> brain = piglin.getBrain();
			brain.eraseMemory(MemoryModuleType.CELEBRATE_LOCATION);
			brain.eraseMemory(MemoryModuleType.ADMIRING_ITEM);
			if (livingEntity instanceof Player) {
				brain.setMemoryWithExpiry(MemoryModuleType.WAS_HIT_BY_PLAYER, (Player)livingEntity, piglin.level.getGameTime(), 400L);
			}

			if (piglin.isBaby()) {
				brain.setMemoryWithExpiry(MemoryModuleType.AVOID_TARGET, livingEntity, piglin.level.getGameTime(), 100L);
			} else if (livingEntity.getType() == EntityType.HOGLIN && hoglinsOutnumberPiglins(piglin)) {
				setAvoidTargetAndDontHuntForAWhile(piglin, livingEntity);
				broadcastRetreat(piglin, livingEntity);
			} else {
				maybeRetaliate(piglin, livingEntity);
			}
		}
	}

	private static void maybeRetaliate(Piglin piglin, LivingEntity livingEntity) {
		if (!piglin.getBrain().isActive(Activity.AVOID) || livingEntity.getType() != EntityType.HOGLIN) {
			if (isAttackAllowed(livingEntity)) {
				if (!BehaviorUtils.isOtherTargetMuchFurtherAwayThanCurrentAttackTarget(piglin, livingEntity, 4.0)) {
					setAngerTarget(piglin, livingEntity);
					broadcastAngerTarget(piglin, livingEntity);
				}
			}
		}
	}

	private static void playActivitySound(Piglin piglin) {
		piglin.getBrain().getActiveNonCoreActivity().ifPresent(activity -> {
			if (activity == Activity.FIGHT) {
				piglin.playAngrySound();
			} else if (activity == Activity.AVOID || piglin.isConverting()) {
				piglin.playRetreatSound();
			} else if (activity == Activity.ADMIRE_ITEM) {
				piglin.playAdmiringSound();
			} else if (activity == Activity.CELEBRATE) {
				piglin.playCelebrateSound();
			} else if (seesPlayerHoldingLovedItem(piglin)) {
				piglin.playJealousSound();
			} else if (seesZombifiedPiglin(piglin) || seesSoulFire(piglin)) {
				piglin.playRetreatSound();
			}
		});
	}

	protected static void maybePlayActivitySound(Piglin piglin) {
		if ((double)piglin.level.random.nextFloat() < 0.0125) {
			playActivitySound(piglin);
		}
	}

	private static boolean seesAdultPiglins(Piglin piglin) {
		Brain<Piglin> brain = piglin.getBrain();
		return !brain.hasMemoryValue(MemoryModuleType.NEAREST_VISIBLE_ADULT_PIGLINS)
			? false
			: ((List)brain.getMemory(MemoryModuleType.NEAREST_VISIBLE_ADULT_PIGLINS).get()).stream().anyMatch(Piglin::isAdult);
	}

	public static boolean hasAnyoneNearbyHuntedRecently(Piglin piglin) {
		return piglin.getBrain().hasMemoryValue(MemoryModuleType.HUNTED_RECENTLY)
			|| getVisibleAdultPiglins(piglin).stream().anyMatch(piglinx -> piglinx.getBrain().hasMemoryValue(MemoryModuleType.HUNTED_RECENTLY));
	}

	private static List<Piglin> getVisibleAdultPiglins(Piglin piglin) {
		return (List<Piglin>)(piglin.getBrain().hasMemoryValue(MemoryModuleType.NEAREST_VISIBLE_ADULT_PIGLINS)
			? (List)piglin.getBrain().getMemory(MemoryModuleType.NEAREST_VISIBLE_ADULT_PIGLINS).get()
			: Lists.<Piglin>newArrayList());
	}

	public static boolean isWearingGold(LivingEntity livingEntity) {
		for(ItemStack itemStack : livingEntity.getArmorSlots()) {
			Item item = itemStack.getItem();
			if (item instanceof ArmorItem && ((ArmorItem)item).getMaterial() == ArmorMaterials.GOLD) {
				return true;
			}
		}

		return false;
	}

	private static void stopWalking(Piglin piglin) {
		piglin.getBrain().eraseMemory(MemoryModuleType.WALK_TARGET);
		piglin.getNavigation().stop();
	}

	private static RunSometimes<Piglin> babySometimesRideBabyHoglin() {
		return new RunSometimes<>(
			new CopyMemoryWithExpiry<>(Piglin::isBaby, MemoryModuleType.NEAREST_VISIBLE_BABY_HOGLIN, MemoryModuleType.RIDE_TARGET, RIDE_DURATION), RIDE_START_INTERVAL
		);
	}

	public static void broadcastAngerTarget(Piglin piglin, LivingEntity livingEntity) {
		getVisibleAdultPiglins(piglin).forEach(piglinx -> setAngerTargetIfCloserThanCurrent(piglinx, livingEntity));
	}

	public static void broadcastDontKillAnyMoreHoglinsForAWhile(Piglin piglin) {
		getVisibleAdultPiglins(piglin).forEach(piglinx -> dontKillAnyMoreHoglinsForAWhile(piglinx));
	}

	public static void setAngerTarget(Piglin piglin, LivingEntity livingEntity) {
		piglin.getBrain().eraseMemory(MemoryModuleType.CANT_REACH_WALK_TARGET_SINCE);
		piglin.getBrain().setMemoryWithExpiry(MemoryModuleType.ANGRY_AT, new SerializableUUID(livingEntity.getUUID()), piglin.level.getGameTime(), 600L);
		if (livingEntity.getType() == EntityType.HOGLIN) {
			dontKillAnyMoreHoglinsForAWhile(piglin);
		}
	}

	private static void setAngerTargetIfCloserThanCurrent(Piglin piglin, LivingEntity livingEntity) {
		Optional<LivingEntity> optional = getAngerTarget(piglin);
		LivingEntity livingEntity2 = BehaviorUtils.getNearestTarget(piglin, optional, livingEntity);
		setAngerTarget(piglin, livingEntity2);
	}

	private static Optional<LivingEntity> getAngerTarget(Piglin piglin) {
		return BehaviorUtils.getLivingEntityFromUUIDMemory(piglin, MemoryModuleType.ANGRY_AT);
	}

	private static void broadcastRetreat(Piglin piglin, LivingEntity livingEntity) {
		getVisibleAdultPiglins(piglin).forEach(piglinx -> retreatFromNearestTarget(piglinx, livingEntity));
	}

	private static void retreatFromNearestTarget(Piglin piglin, LivingEntity livingEntity) {
		Brain<Piglin> brain = piglin.getBrain();
		LivingEntity livingEntity2 = BehaviorUtils.getNearestTarget(piglin, brain.getMemory(MemoryModuleType.AVOID_TARGET), livingEntity);
		livingEntity2 = BehaviorUtils.getNearestTarget(piglin, brain.getMemory(MemoryModuleType.ATTACK_TARGET), livingEntity2);
		setAvoidTargetAndDontHuntForAWhile(piglin, livingEntity2);
	}

	private static boolean wantsToStopFleeing(Piglin piglin) {
		return piglin.isAdult() && piglinsEqualOrOutnumberHoglins(piglin);
	}

	private static boolean piglinsEqualOrOutnumberHoglins(Piglin piglin) {
		return !hoglinsOutnumberPiglins(piglin);
	}

	private static boolean hoglinsOutnumberPiglins(Piglin piglin) {
		int i = piglin.getBrain().getMemory(MemoryModuleType.VISIBLE_ADULT_PIGLIN_COUNT).orElse(0) + 1;
		int j = piglin.getBrain().getMemory(MemoryModuleType.VISIBLE_ADULT_HOGLIN_COUNT).orElse(0);
		return j > i;
	}

	private static void setAvoidTargetAndDontHuntForAWhile(Piglin piglin, LivingEntity livingEntity) {
		piglin.getBrain().eraseMemory(MemoryModuleType.ANGRY_AT);
		piglin.getBrain().eraseMemory(MemoryModuleType.ATTACK_TARGET);
		piglin.getBrain()
			.setMemoryWithExpiry(MemoryModuleType.AVOID_TARGET, livingEntity, piglin.level.getGameTime(), (long)RETREAT_DURATION.randomValue(piglin.level.random));
		dontKillAnyMoreHoglinsForAWhile(piglin);
	}

	public static void dontKillAnyMoreHoglinsForAWhile(Piglin piglin) {
		piglin.getBrain()
			.setMemoryWithExpiry(MemoryModuleType.HUNTED_RECENTLY, true, piglin.level.getGameTime(), (long)TIME_BETWEEN_HUNTS.randomValue(piglin.level.random));
	}

	private static boolean seesPlayerHoldingWantedItem(Piglin piglin) {
		return piglin.getBrain().hasMemoryValue(MemoryModuleType.NEAREST_PLAYER_HOLDING_WANTED_ITEM);
	}

	private static void eat(Piglin piglin) {
		piglin.getBrain().setMemoryWithExpiry(MemoryModuleType.ATE_RECENTLY, true, piglin.level.getGameTime(), 200L);
	}

	private static Vec3 getRandomNearbyPos(Piglin piglin) {
		Vec3 vec3 = RandomPos.getLandPos(piglin, 4, 2);
		return vec3 == null ? piglin.position() : vec3;
	}

	private static boolean hasEatenRecently(Piglin piglin) {
		return piglin.getBrain().hasMemoryValue(MemoryModuleType.ATE_RECENTLY);
	}

	static boolean isIdle(Piglin piglin) {
		return piglin.getBrain().isActive(Activity.IDLE);
	}

	private static boolean hasCrossbow(LivingEntity livingEntity) {
		return livingEntity.isHolding(Items.CROSSBOW);
	}

	private static void admireGoldItem(LivingEntity livingEntity) {
		livingEntity.getBrain().setMemoryWithExpiry(MemoryModuleType.ADMIRING_ITEM, true, livingEntity.level.getGameTime(), 120L);
	}

	private static boolean isAdmiringItem(Piglin piglin) {
		return piglin.getBrain().hasMemoryValue(MemoryModuleType.ADMIRING_ITEM);
	}

	private static boolean isBarterCurrency(Item item) {
		return item == Items.GOLD_INGOT;
	}

	private static boolean isFood(Item item) {
		return FOOD_ITEMS.contains(item);
	}

	private static boolean isAttackAllowed(LivingEntity livingEntity) {
		return EntitySelector.ATTACK_ALLOWED.test(livingEntity);
	}

	private static boolean seesSoulFire(Piglin piglin) {
		return piglin.getBrain().hasMemoryValue(MemoryModuleType.NEAREST_SOUL_FIRE);
	}

	private static boolean seesZombifiedPiglin(Piglin piglin) {
		return piglin.getBrain().hasMemoryValue(MemoryModuleType.NEAREST_VISIBLE_ZOMBIFIED_PIGLIN);
	}

	private static boolean seesPlayerHoldingLovedItem(LivingEntity livingEntity) {
		return livingEntity.getBrain().hasMemoryValue(MemoryModuleType.NEAREST_PLAYER_HOLDING_WANTED_ITEM);
	}

	private static boolean doesntSeeAnyPlayerHoldingLovedItem(LivingEntity livingEntity) {
		return !seesPlayerHoldingLovedItem(livingEntity);
	}

	public static boolean isPlayerHoldingLovedItem(LivingEntity livingEntity) {
		return livingEntity.getType() == EntityType.PLAYER && livingEntity.isHolding(PiglinAi::isLovedItem);
	}

	private static boolean wasHitByPlayer(Piglin piglin) {
		return piglin.getBrain().hasMemoryValue(MemoryModuleType.WAS_HIT_BY_PLAYER);
	}

	private static boolean wasHurtRecently(LivingEntity livingEntity) {
		return livingEntity.getBrain().hasMemoryValue(MemoryModuleType.HURT_BY);
	}

	protected static boolean seesPlayer(Piglin piglin) {
		return piglin.getBrain().hasMemoryValue(MemoryModuleType.NEAREST_PLAYERS);
	}
}
