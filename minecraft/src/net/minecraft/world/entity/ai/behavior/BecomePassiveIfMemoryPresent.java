package net.minecraft.world.entity.ai.behavior;

import com.mojang.datafixers.util.Function3;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.behavior.declarative.BehaviorBuilder;
import net.minecraft.world.entity.ai.behavior.declarative.MemoryAccessor;
import net.minecraft.world.entity.ai.behavior.declarative.Trigger;
import net.minecraft.world.entity.ai.memory.MemoryModuleType;

public class BecomePassiveIfMemoryPresent {
	public static BehaviorControl<LivingEntity> create(MemoryModuleType<?> memoryModuleType, int i) {
		return BehaviorBuilder.create(
			instance -> instance.group(
						instance.registered(MemoryModuleType.ATTACK_TARGET), instance.absent(MemoryModuleType.PACIFIED), instance.present(memoryModuleType)
					)
					.apply(
						instance,
						instance.point(
							() -> "[BecomePassive if " + memoryModuleType + " present]",
							(Function3<MemoryAccessor, MemoryAccessor, MemoryAccessor, Trigger<LivingEntity>>)(memoryAccessor, memoryAccessor2, memoryAccessor3) -> (serverLevel, livingEntity, l) -> {
									memoryAccessor2.setWithExpiry(true, (long)i);
									memoryAccessor.erase();
									return true;
								}
						)
					)
		);
	}
}
