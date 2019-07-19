package net.minecraft.core;

import com.google.common.collect.BiMap;
import com.google.common.collect.HashBiMap;
import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.Optional;
import java.util.Random;
import java.util.Set;
import javax.annotation.Nullable;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.CrudeIncrementalIntIdentityHashBiMap;
import org.apache.commons.lang3.Validate;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class MappedRegistry<T> extends WritableRegistry<T> {
	protected static final Logger LOGGER = LogManager.getLogger();
	protected final CrudeIncrementalIntIdentityHashBiMap<T> map = new CrudeIncrementalIntIdentityHashBiMap<>(256);
	protected final BiMap<ResourceLocation, T> storage = HashBiMap.create();
	protected Object[] randomCache;
	private int nextId;

	@Override
	public <V extends T> V registerMapping(int i, ResourceLocation resourceLocation, V object) {
		this.map.addMapping((T)object, i);
		Validate.notNull(resourceLocation);
		Validate.notNull((T)object);
		this.randomCache = null;
		if (this.storage.containsKey(resourceLocation)) {
			LOGGER.debug("Adding duplicate key '{}' to registry", resourceLocation);
		}

		this.storage.put(resourceLocation, (T)object);
		if (this.nextId <= i) {
			this.nextId = i + 1;
		}

		return object;
	}

	@Override
	public <V extends T> V register(ResourceLocation resourceLocation, V object) {
		return this.registerMapping(this.nextId, resourceLocation, object);
	}

	@Nullable
	@Override
	public ResourceLocation getKey(T object) {
		return (ResourceLocation)this.storage.inverse().get(object);
	}

	@Override
	public int getId(@Nullable T object) {
		return this.map.getId(object);
	}

	@Nullable
	@Override
	public T byId(int i) {
		return this.map.byId(i);
	}

	public Iterator<T> iterator() {
		return this.map.iterator();
	}

	@Nullable
	@Override
	public T get(@Nullable ResourceLocation resourceLocation) {
		return (T)this.storage.get(resourceLocation);
	}

	@Override
	public Optional<T> getOptional(@Nullable ResourceLocation resourceLocation) {
		return Optional.ofNullable(this.storage.get(resourceLocation));
	}

	@Override
	public Set<ResourceLocation> keySet() {
		return Collections.unmodifiableSet(this.storage.keySet());
	}

	@Override
	public boolean isEmpty() {
		return this.storage.isEmpty();
	}

	@Nullable
	@Override
	public T getRandom(Random random) {
		if (this.randomCache == null) {
			Collection<?> collection = this.storage.values();
			if (collection.isEmpty()) {
				return null;
			}

			this.randomCache = collection.toArray(new Object[collection.size()]);
		}

		return (T)this.randomCache[random.nextInt(this.randomCache.length)];
	}

	@Environment(EnvType.CLIENT)
	@Override
	public boolean containsKey(ResourceLocation resourceLocation) {
		return this.storage.containsKey(resourceLocation);
	}
}
