package net.minecraft.core;

import com.mojang.serialization.Lifecycle;
import java.util.Optional;
import java.util.function.Function;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.RandomSource;

public class DefaultedRegistry<T> extends MappedRegistry<T> {
	private final ResourceLocation defaultKey;
	private Holder<T> defaultValue;

	public DefaultedRegistry(
		String string, ResourceKey<? extends Registry<T>> resourceKey, Lifecycle lifecycle, @Nullable Function<T, Holder.Reference<T>> function
	) {
		super(resourceKey, lifecycle, function);
		this.defaultKey = new ResourceLocation(string);
	}

	@Override
	public Holder<T> registerMapping(int i, ResourceKey<T> resourceKey, T object, Lifecycle lifecycle) {
		Holder<T> holder = super.registerMapping(i, resourceKey, object, lifecycle);
		if (this.defaultKey.equals(resourceKey.location())) {
			this.defaultValue = holder;
		}

		return holder;
	}

	@Override
	public int getId(@Nullable T object) {
		int i = super.getId(object);
		return i == -1 ? super.getId(this.defaultValue.value()) : i;
	}

	@Nonnull
	@Override
	public ResourceLocation getKey(T object) {
		ResourceLocation resourceLocation = super.getKey(object);
		return resourceLocation == null ? this.defaultKey : resourceLocation;
	}

	@Nonnull
	@Override
	public T get(@Nullable ResourceLocation resourceLocation) {
		T object = super.get(resourceLocation);
		return (T)(object == null ? this.defaultValue.value() : object);
	}

	@Override
	public Optional<T> getOptional(@Nullable ResourceLocation resourceLocation) {
		return Optional.ofNullable(super.get(resourceLocation));
	}

	@Nonnull
	@Override
	public T byId(int i) {
		T object = super.byId(i);
		return (T)(object == null ? this.defaultValue.value() : object);
	}

	@Override
	public Optional<Holder<T>> getRandom(RandomSource randomSource) {
		return super.getRandom(randomSource).or(() -> Optional.of(this.defaultValue));
	}

	public ResourceLocation getDefaultKey() {
		return this.defaultKey;
	}
}
