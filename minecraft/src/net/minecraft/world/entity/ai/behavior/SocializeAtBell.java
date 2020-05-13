package net.minecraft.world.entity.ai.behavior;

import com.google.common.collect.ImmutableMap;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import net.minecraft.core.GlobalPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.Brain;
import net.minecraft.world.entity.ai.memory.MemoryModuleType;
import net.minecraft.world.entity.ai.memory.MemoryStatus;
import net.minecraft.world.entity.ai.memory.WalkTarget;

public class SocializeAtBell extends Behavior<LivingEntity> {
	public SocializeAtBell() {
		super(
			ImmutableMap.of(
				MemoryModuleType.WALK_TARGET,
				MemoryStatus.REGISTERED,
				MemoryModuleType.LOOK_TARGET,
				MemoryStatus.REGISTERED,
				MemoryModuleType.MEETING_POINT,
				MemoryStatus.VALUE_PRESENT,
				MemoryModuleType.VISIBLE_LIVING_ENTITIES,
				MemoryStatus.VALUE_PRESENT,
				MemoryModuleType.INTERACTION_TARGET,
				MemoryStatus.VALUE_ABSENT
			)
		);
	}

	@Override
	protected boolean checkExtraStartConditions(ServerLevel serverLevel, LivingEntity livingEntity) {
		Brain<?> brain = livingEntity.getBrain();
		Optional<GlobalPos> optional = brain.getMemory(MemoryModuleType.MEETING_POINT);
		return serverLevel.getRandom().nextInt(100) == 0
			&& optional.isPresent()
			&& Objects.equals(serverLevel.dimensionType(), ((GlobalPos)optional.get()).dimension())
			&& ((GlobalPos)optional.get()).pos().closerThan(livingEntity.position(), 4.0)
			&& ((List)brain.getMemory(MemoryModuleType.VISIBLE_LIVING_ENTITIES).get())
				.stream()
				.anyMatch(livingEntityx -> EntityType.VILLAGER.equals(livingEntityx.getType()));
	}

	@Override
	protected void start(ServerLevel serverLevel, LivingEntity livingEntity, long l) {
		Brain<?> brain = livingEntity.getBrain();
		brain.getMemory(MemoryModuleType.VISIBLE_LIVING_ENTITIES)
			.ifPresent(
				list -> list.stream()
						.filter(livingEntityxx -> EntityType.VILLAGER.equals(livingEntityxx.getType()))
						.filter(livingEntity2 -> livingEntity2.distanceToSqr(livingEntity) <= 32.0)
						.findFirst()
						.ifPresent(livingEntityxx -> {
							brain.setMemory(MemoryModuleType.INTERACTION_TARGET, livingEntityxx);
							brain.setMemory(MemoryModuleType.LOOK_TARGET, new EntityTracker(livingEntityxx, true));
							brain.setMemory(MemoryModuleType.WALK_TARGET, new WalkTarget(new EntityTracker(livingEntityxx, false), 0.3F, 1));
						})
			);
	}
}
