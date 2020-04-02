package net.minecraft.world.entity.monster.hoglin;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import com.mojang.datafixers.Dynamic;
import com.mojang.datafixers.util.Pair;
import java.util.List;
import java.util.Optional;
import net.minecraft.core.BlockPos;
import net.minecraft.util.IntRange;
import net.minecraft.util.TimeUtil;
import net.minecraft.world.entity.AgableMob;
import net.minecraft.world.entity.EntitySelector;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.ai.Brain;
import net.minecraft.world.entity.ai.behavior.AnimalMakeLove;
import net.minecraft.world.entity.ai.behavior.BecomePassiveIfMemoryPresent;
import net.minecraft.world.entity.ai.behavior.BehaviorUtils;
import net.minecraft.world.entity.ai.behavior.DoNothing;
import net.minecraft.world.entity.ai.behavior.EraseMemoryIf;
import net.minecraft.world.entity.ai.behavior.LookAtTargetSink;
import net.minecraft.world.entity.ai.behavior.MeleeAttack;
import net.minecraft.world.entity.ai.behavior.MoveToTargetSink;
import net.minecraft.world.entity.ai.behavior.RandomStroll;
import net.minecraft.world.entity.ai.behavior.RunIf;
import net.minecraft.world.entity.ai.behavior.RunOne;
import net.minecraft.world.entity.ai.behavior.RunSometimes;
import net.minecraft.world.entity.ai.behavior.SetEntityLookTarget;
import net.minecraft.world.entity.ai.behavior.SetWalkTargetAwayFrom;
import net.minecraft.world.entity.ai.behavior.SetWalkTargetFromAttackTargetIfTargetOutOfReach;
import net.minecraft.world.entity.ai.behavior.SetWalkTargetFromLookTarget;
import net.minecraft.world.entity.ai.behavior.StartAttacking;
import net.minecraft.world.entity.ai.behavior.StopAttackingIfTargetInvalid;
import net.minecraft.world.entity.ai.memory.MemoryModuleType;
import net.minecraft.world.entity.schedule.Activity;

public class HoglinAi {
	private static final IntRange RETREAT_DURATION = TimeUtil.rangeOfSeconds(5, 20);

	protected static Brain<?> makeBrain(Hoglin hoglin, Dynamic<?> dynamic) {
		Brain<Hoglin> brain = new Brain<>(Hoglin.MEMORY_TYPES, Hoglin.SENSOR_TYPES, dynamic);
		initCoreActivity(brain);
		initIdleActivity(brain);
		initFightActivity(brain);
		initRetreatActivity(brain);
		brain.setCoreActivities(ImmutableSet.of(Activity.CORE));
		brain.setDefaultActivity(Activity.IDLE);
		brain.useDefaultActivity();
		return brain;
	}

	private static void initCoreActivity(Brain<Hoglin> brain) {
		brain.addActivity(Activity.CORE, 0, ImmutableList.of(new LookAtTargetSink(45, 90), new MoveToTargetSink(200)));
	}

	private static void initIdleActivity(Brain<Hoglin> brain) {
		brain.addActivity(
			Activity.IDLE,
			10,
			ImmutableList.of(
				new BecomePassiveIfMemoryPresent(MemoryModuleType.NEAREST_REPELLENT, 200),
				new AnimalMakeLove(EntityType.HOGLIN, 0.6F),
				SetWalkTargetAwayFrom.pos(MemoryModuleType.NEAREST_REPELLENT, 1.0F, 8, true),
				new StartAttacking(HoglinAi::findNearestValidAttackTarget),
				new RunIf<PathfinderMob>(Hoglin::isAdult, SetWalkTargetAwayFrom.entity(MemoryModuleType.NEAREST_VISIBLE_ADULT_PIGLIN, 0.4F, 8, false)),
				new RunSometimes<LivingEntity>(new SetEntityLookTarget(8.0F), IntRange.of(30, 60)),
				createIdleMovementBehaviors()
			)
		);
	}

	private static void initFightActivity(Brain<Hoglin> brain) {
		brain.addActivityAndRemoveMemoryWhenStopped(
			Activity.FIGHT,
			10,
			ImmutableList.of(
				new BecomePassiveIfMemoryPresent(MemoryModuleType.NEAREST_REPELLENT, 200),
				new AnimalMakeLove(EntityType.HOGLIN, 0.6F),
				new SetWalkTargetFromAttackTargetIfTargetOutOfReach(1.0F),
				new RunIf<>(Hoglin::isAdult, new MeleeAttack(40)),
				new RunIf<>(AgableMob::isBaby, new MeleeAttack(15)),
				new StopAttackingIfTargetInvalid(),
				new EraseMemoryIf(HoglinAi::isBreeding, MemoryModuleType.ATTACK_TARGET)
			),
			MemoryModuleType.ATTACK_TARGET
		);
	}

	private static void initRetreatActivity(Brain<Hoglin> brain) {
		brain.addActivityAndRemoveMemoryWhenStopped(
			Activity.AVOID,
			10,
			ImmutableList.of(
				SetWalkTargetAwayFrom.entity(MemoryModuleType.AVOID_TARGET, 1.0F, 15, false),
				createIdleMovementBehaviors(),
				new RunSometimes<LivingEntity>(new SetEntityLookTarget(8.0F), IntRange.of(30, 60)),
				new EraseMemoryIf(HoglinAi::wantsToStopFleeing, MemoryModuleType.AVOID_TARGET)
			),
			MemoryModuleType.AVOID_TARGET
		);
	}

	private static RunOne<Hoglin> createIdleMovementBehaviors() {
		return new RunOne<>(
			ImmutableList.of(Pair.of(new RandomStroll(0.4F), 2), Pair.of(new SetWalkTargetFromLookTarget(0.4F, 3), 2), Pair.of(new DoNothing(30, 60), 1))
		);
	}

	protected static void updateActivity(Hoglin hoglin) {
		Brain<Hoglin> brain = hoglin.getBrain();
		Activity activity = (Activity)brain.getActiveNonCoreActivity().orElse(null);
		brain.setActiveActivityToFirstValid(ImmutableList.of(Activity.FIGHT, Activity.AVOID, Activity.IDLE));
		Activity activity2 = (Activity)brain.getActiveNonCoreActivity().orElse(null);
		if (activity != activity2) {
			playActivitySound(hoglin);
		}

		hoglin.setAggressive(brain.hasMemoryValue(MemoryModuleType.ATTACK_TARGET));
	}

	protected static void onHitTarget(Hoglin hoglin, LivingEntity livingEntity) {
		if (!hoglin.isBaby()) {
			if (livingEntity.getType() == EntityType.PIGLIN && piglinsOutnumberHoglins(hoglin)) {
				setAvoidTarget(hoglin, livingEntity);
				broadcastRetreat(hoglin, livingEntity);
			} else {
				broadcastAttackTarget(hoglin, livingEntity);
			}
		}
	}

	private static void broadcastRetreat(Hoglin hoglin, LivingEntity livingEntity) {
		getVisibleAdultHoglins(hoglin).forEach(hoglinx -> retreatFromNearestTarget(hoglinx, livingEntity));
	}

	private static void retreatFromNearestTarget(Hoglin hoglin, LivingEntity livingEntity) {
		Brain<Hoglin> brain = hoglin.getBrain();
		LivingEntity livingEntity2 = BehaviorUtils.getNearestTarget(hoglin, brain.getMemory(MemoryModuleType.AVOID_TARGET), livingEntity);
		livingEntity2 = BehaviorUtils.getNearestTarget(hoglin, brain.getMemory(MemoryModuleType.ATTACK_TARGET), livingEntity2);
		setAvoidTarget(hoglin, livingEntity2);
	}

	private static void setAvoidTarget(Hoglin hoglin, LivingEntity livingEntity) {
		hoglin.getBrain().eraseMemory(MemoryModuleType.ATTACK_TARGET);
		hoglin.getBrain().setMemoryWithExpiry(MemoryModuleType.AVOID_TARGET, livingEntity, (long)RETREAT_DURATION.randomValue(hoglin.level.random));
	}

	private static Optional<? extends LivingEntity> findNearestValidAttackTarget(Hoglin hoglin) {
		return !isPacified(hoglin) && !isBreeding(hoglin) ? hoglin.getBrain().getMemory(MemoryModuleType.NEAREST_VISIBLE_TARGETABLE_PLAYER) : Optional.empty();
	}

	static boolean isPosNearNearestRepellent(Hoglin hoglin, BlockPos blockPos) {
		Optional<BlockPos> optional = hoglin.getBrain().getMemory(MemoryModuleType.NEAREST_REPELLENT);
		return optional.isPresent() && ((BlockPos)optional.get()).closerThan(blockPos, 8.0);
	}

	private static boolean wantsToStopFleeing(Hoglin hoglin) {
		return hoglin.isAdult() && !piglinsOutnumberHoglins(hoglin);
	}

	private static boolean piglinsOutnumberHoglins(Hoglin hoglin) {
		if (hoglin.isBaby()) {
			return false;
		} else {
			int i = hoglin.getBrain().getMemory(MemoryModuleType.VISIBLE_ADULT_PIGLIN_COUNT).orElse(0);
			int j = hoglin.getBrain().getMemory(MemoryModuleType.VISIBLE_ADULT_HOGLIN_COUNT).orElse(0) + 1;
			return i > j;
		}
	}

	protected static void wasHurtBy(Hoglin hoglin, LivingEntity livingEntity) {
		Brain<Hoglin> brain = hoglin.getBrain();
		brain.eraseMemory(MemoryModuleType.PACIFIED);
		brain.eraseMemory(MemoryModuleType.BREED_TARGET);
		if (hoglin.isBaby()) {
			retreatFromNearestTarget(hoglin, livingEntity);
		} else {
			maybeRetaliate(hoglin, livingEntity);
		}
	}

	private static void maybeRetaliate(Hoglin hoglin, LivingEntity livingEntity) {
		if (!hoglin.getBrain().isActive(Activity.AVOID) || livingEntity.getType() != EntityType.PIGLIN) {
			if (EntitySelector.ATTACK_ALLOWED.test(livingEntity)) {
				if (livingEntity.getType() != EntityType.HOGLIN) {
					if (!BehaviorUtils.isOtherTargetMuchFurtherAwayThanCurrentAttackTarget(hoglin, livingEntity, 4.0)) {
						setAttackTarget(hoglin, livingEntity);
						broadcastAttackTarget(hoglin, livingEntity);
					}
				}
			}
		}
	}

	private static void setAttackTarget(Hoglin hoglin, LivingEntity livingEntity) {
		Brain<Hoglin> brain = hoglin.getBrain();
		brain.eraseMemory(MemoryModuleType.CANT_REACH_WALK_TARGET_SINCE);
		brain.eraseMemory(MemoryModuleType.BREED_TARGET);
		brain.setMemoryWithExpiry(MemoryModuleType.ATTACK_TARGET, livingEntity, 200L);
	}

	private static void broadcastAttackTarget(Hoglin hoglin, LivingEntity livingEntity) {
		getVisibleAdultHoglins(hoglin).forEach(hoglinx -> setAttackTargetIfCloserThanCurrent(hoglinx, livingEntity));
	}

	private static void setAttackTargetIfCloserThanCurrent(Hoglin hoglin, LivingEntity livingEntity) {
		Optional<LivingEntity> optional = hoglin.getBrain().getMemory(MemoryModuleType.ATTACK_TARGET);
		LivingEntity livingEntity2 = BehaviorUtils.getNearestTarget(hoglin, optional, livingEntity);
		setAttackTarget(hoglin, livingEntity2);
	}

	private static void playActivitySound(Hoglin hoglin) {
		hoglin.getBrain().getActiveNonCoreActivity().ifPresent(activity -> {
			if (activity == Activity.AVOID || hoglin.isConverting()) {
				hoglin.playRetreatSound();
			} else if (activity == Activity.FIGHT) {
				hoglin.playAngrySound();
			}
		});
	}

	protected static void maybePlayActivitySound(Hoglin hoglin) {
		if ((double)hoglin.level.random.nextFloat() < 0.0125) {
			playActivitySound(hoglin);
		}
	}

	private static List<Hoglin> getVisibleAdultHoglins(Hoglin hoglin) {
		return (List<Hoglin>)hoglin.getBrain().getMemory(MemoryModuleType.NEAREST_VISIBLE_ADULT_HOGLINS).orElse(ImmutableList.of());
	}

	private static boolean isBreeding(Hoglin hoglin) {
		return hoglin.getBrain().hasMemoryValue(MemoryModuleType.BREED_TARGET);
	}

	protected static boolean isPacified(Hoglin hoglin) {
		return hoglin.getBrain().hasMemoryValue(MemoryModuleType.PACIFIED);
	}

	protected static boolean isIdle(Hoglin hoglin) {
		return hoglin.getBrain().isActive(Activity.IDLE);
	}
}
