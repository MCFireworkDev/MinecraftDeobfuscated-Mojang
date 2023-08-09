package net.minecraft.advancements.critereon;

import com.google.common.collect.ImmutableList;
import com.google.gson.JsonElement;
import com.google.gson.JsonParseException;
import com.mojang.serialization.Codec;
import com.mojang.serialization.JsonOps;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import com.mojang.serialization.codecs.RecordCodecBuilder.Instance;
import java.util.List;
import java.util.Optional;
import javax.annotation.Nullable;
import net.minecraft.Util;
import net.minecraft.core.registries.Registries;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.ExtraCodecs;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageType;
import net.minecraft.world.phys.Vec3;

public record DamageSourcePredicate(List<TagPredicate<DamageType>> tags, Optional<EntityPredicate> directEntity, Optional<EntityPredicate> sourceEntity) {
	public static final Codec<DamageSourcePredicate> CODEC = RecordCodecBuilder.create(
		instance -> instance.group(
					ExtraCodecs.strictOptionalField(TagPredicate.codec(Registries.DAMAGE_TYPE).listOf(), "tags", List.of()).forGetter(DamageSourcePredicate::tags),
					ExtraCodecs.strictOptionalField(EntityPredicate.CODEC, "direct_entity").forGetter(DamageSourcePredicate::directEntity),
					ExtraCodecs.strictOptionalField(EntityPredicate.CODEC, "source_entity").forGetter(DamageSourcePredicate::sourceEntity)
				)
				.apply(instance, DamageSourcePredicate::new)
	);

	static Optional<DamageSourcePredicate> of(List<TagPredicate<DamageType>> list, Optional<EntityPredicate> optional, Optional<EntityPredicate> optional2) {
		return list.isEmpty() && optional.isEmpty() && optional2.isEmpty() ? Optional.empty() : Optional.of(new DamageSourcePredicate(list, optional, optional2));
	}

	public boolean matches(ServerPlayer serverPlayer, DamageSource damageSource) {
		return this.matches(serverPlayer.serverLevel(), serverPlayer.position(), damageSource);
	}

	public boolean matches(ServerLevel serverLevel, Vec3 vec3, DamageSource damageSource) {
		for(TagPredicate<DamageType> tagPredicate : this.tags) {
			if (!tagPredicate.matches(damageSource.typeHolder())) {
				return false;
			}
		}

		if (this.directEntity.isPresent() && !((EntityPredicate)this.directEntity.get()).matches(serverLevel, vec3, damageSource.getDirectEntity())) {
			return false;
		} else {
			return !this.sourceEntity.isPresent() || ((EntityPredicate)this.sourceEntity.get()).matches(serverLevel, vec3, damageSource.getEntity());
		}
	}

	public static Optional<DamageSourcePredicate> fromJson(@Nullable JsonElement jsonElement) {
		return jsonElement != null && !jsonElement.isJsonNull()
			? Optional.of((DamageSourcePredicate)Util.getOrThrow(CODEC.parse(JsonOps.INSTANCE, jsonElement), JsonParseException::new))
			: Optional.empty();
	}

	public JsonElement serializeToJson() {
		return Util.getOrThrow(CODEC.encodeStart(JsonOps.INSTANCE, this), IllegalStateException::new);
	}

	public static class Builder {
		private final ImmutableList.Builder<TagPredicate<DamageType>> tags = ImmutableList.builder();
		private Optional<EntityPredicate> directEntity = Optional.empty();
		private Optional<EntityPredicate> sourceEntity = Optional.empty();

		public static DamageSourcePredicate.Builder damageType() {
			return new DamageSourcePredicate.Builder();
		}

		public DamageSourcePredicate.Builder tag(TagPredicate<DamageType> tagPredicate) {
			this.tags.add(tagPredicate);
			return this;
		}

		public DamageSourcePredicate.Builder direct(EntityPredicate.Builder builder) {
			this.directEntity = builder.build();
			return this;
		}

		public DamageSourcePredicate.Builder source(EntityPredicate.Builder builder) {
			this.sourceEntity = builder.build();
			return this;
		}

		public Optional<DamageSourcePredicate> build() {
			return DamageSourcePredicate.of(this.tags.build(), this.directEntity, this.sourceEntity);
		}
	}
}
