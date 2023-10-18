package net.minecraft.data.tags;

import java.util.concurrent.CompletableFuture;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.registries.Registries;
import net.minecraft.data.PackOutput;
import net.minecraft.tags.EntityTypeTags;
import net.minecraft.world.entity.EntityType;

public class EntityTypeTagsProvider extends IntrinsicHolderTagsProvider<EntityType<?>> {
	public EntityTypeTagsProvider(PackOutput packOutput, CompletableFuture<HolderLookup.Provider> completableFuture) {
		super(packOutput, Registries.ENTITY_TYPE, completableFuture, entityType -> entityType.builtInRegistryHolder().key());
	}

	@Override
	protected void addTags(HolderLookup.Provider provider) {
		this.tag(EntityTypeTags.SKELETONS).add(EntityType.SKELETON, EntityType.STRAY, EntityType.WITHER_SKELETON, EntityType.SKELETON_HORSE);
		this.tag(EntityTypeTags.ZOMBIES)
			.add(
				EntityType.ZOMBIE_HORSE,
				EntityType.ZOMBIE,
				EntityType.ZOMBIE_VILLAGER,
				EntityType.ZOMBIFIED_PIGLIN,
				EntityType.ZOGLIN,
				EntityType.DROWNED,
				EntityType.HUSK
			);
		this.tag(EntityTypeTags.RAIDERS)
			.add(EntityType.EVOKER, EntityType.PILLAGER, EntityType.RAVAGER, EntityType.VINDICATOR, EntityType.ILLUSIONER, EntityType.WITCH);
		this.tag(EntityTypeTags.UNDEAD).addTag(EntityTypeTags.SKELETONS).addTag(EntityTypeTags.ZOMBIES).add(EntityType.WITHER).add(EntityType.PHANTOM);
		this.tag(EntityTypeTags.BEEHIVE_INHABITORS).add(EntityType.BEE);
		this.tag(EntityTypeTags.ARROWS).add(EntityType.ARROW, EntityType.SPECTRAL_ARROW);
		this.tag(EntityTypeTags.IMPACT_PROJECTILES)
			.addTag(EntityTypeTags.ARROWS)
			.add(EntityType.FIREWORK_ROCKET)
			.add(
				EntityType.SNOWBALL,
				EntityType.FIREBALL,
				EntityType.SMALL_FIREBALL,
				EntityType.EGG,
				EntityType.TRIDENT,
				EntityType.DRAGON_FIREBALL,
				EntityType.WITHER_SKULL
			);
		this.tag(EntityTypeTags.POWDER_SNOW_WALKABLE_MOBS).add(EntityType.RABBIT, EntityType.ENDERMITE, EntityType.SILVERFISH, EntityType.FOX);
		this.tag(EntityTypeTags.AXOLOTL_HUNT_TARGETS)
			.add(EntityType.TROPICAL_FISH, EntityType.PUFFERFISH, EntityType.SALMON, EntityType.COD, EntityType.SQUID, EntityType.GLOW_SQUID, EntityType.TADPOLE);
		this.tag(EntityTypeTags.AXOLOTL_ALWAYS_HOSTILES).add(EntityType.DROWNED, EntityType.GUARDIAN, EntityType.ELDER_GUARDIAN);
		this.tag(EntityTypeTags.FREEZE_IMMUNE_ENTITY_TYPES).add(EntityType.STRAY, EntityType.POLAR_BEAR, EntityType.SNOW_GOLEM, EntityType.WITHER);
		this.tag(EntityTypeTags.FREEZE_HURTS_EXTRA_TYPES).add(EntityType.STRIDER, EntityType.BLAZE, EntityType.MAGMA_CUBE);
		this.tag(EntityTypeTags.CAN_BREATHE_UNDER_WATER)
			.addTag(EntityTypeTags.UNDEAD)
			.add(
				EntityType.AXOLOTL,
				EntityType.FROG,
				EntityType.GUARDIAN,
				EntityType.ELDER_GUARDIAN,
				EntityType.TURTLE,
				EntityType.GLOW_SQUID,
				EntityType.COD,
				EntityType.PUFFERFISH,
				EntityType.SALMON,
				EntityType.SQUID,
				EntityType.TROPICAL_FISH,
				EntityType.TADPOLE,
				EntityType.ARMOR_STAND
			);
		this.tag(EntityTypeTags.FROG_FOOD).add(EntityType.SLIME, EntityType.MAGMA_CUBE);
		this.tag(EntityTypeTags.FALL_DAMAGE_IMMUNE)
			.add(
				EntityType.IRON_GOLEM,
				EntityType.SNOW_GOLEM,
				EntityType.SHULKER,
				EntityType.ALLAY,
				EntityType.BAT,
				EntityType.BEE,
				EntityType.BLAZE,
				EntityType.CAT,
				EntityType.CHICKEN,
				EntityType.GHAST,
				EntityType.PHANTOM,
				EntityType.MAGMA_CUBE,
				EntityType.OCELOT,
				EntityType.PARROT,
				EntityType.WITHER
			);
		this.tag(EntityTypeTags.DISMOUNTS_UNDERWATER)
			.add(
				EntityType.CAMEL,
				EntityType.CHICKEN,
				EntityType.DONKEY,
				EntityType.HORSE,
				EntityType.LLAMA,
				EntityType.MULE,
				EntityType.PIG,
				EntityType.RAVAGER,
				EntityType.SPIDER,
				EntityType.STRIDER,
				EntityType.TRADER_LLAMA,
				EntityType.ZOMBIE_HORSE
			);
		this.tag(EntityTypeTags.NON_CONTROLLING_RIDER).add(EntityType.SLIME, EntityType.MAGMA_CUBE);
	}
}
