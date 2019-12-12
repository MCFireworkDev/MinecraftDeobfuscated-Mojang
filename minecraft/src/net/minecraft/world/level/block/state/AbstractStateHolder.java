package net.minecraft.world.level.block.state;

import com.google.common.collect.ArrayTable;
import com.google.common.collect.HashBasedTable;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Maps;
import com.google.common.collect.Table;
import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.Map;
import java.util.Map.Entry;
import java.util.function.Function;
import java.util.stream.Collectors;
import javax.annotation.Nullable;
import net.minecraft.world.level.block.state.properties.Property;

public abstract class AbstractStateHolder<O, S> implements StateHolder<S> {
	private static final Function<Entry<Property<?>, Comparable<?>>, String> PROPERTY_ENTRY_TO_STRING_FUNCTION = new Function<Entry<Property<?>, Comparable<?>>, String>(
		
	) {
		public String apply(@Nullable Entry<Property<?>, Comparable<?>> entry) {
			if (entry == null) {
				return "<NULL>";
			} else {
				Property<?> property = (Property)entry.getKey();
				return property.getName() + "=" + this.getName(property, (Comparable<?>)entry.getValue());
			}
		}

		private <T extends Comparable<T>> String getName(Property<T> property, Comparable<?> comparable) {
			return property.getName((T)comparable);
		}
	};
	protected final O owner;
	private final ImmutableMap<Property<?>, Comparable<?>> values;
	private Table<Property<?>, Comparable<?>, S> neighbours;

	protected AbstractStateHolder(O object, ImmutableMap<Property<?>, Comparable<?>> immutableMap) {
		this.owner = object;
		this.values = immutableMap;
	}

	public <T extends Comparable<T>> S cycle(Property<T> property) {
		return this.setValue(property, findNextInCollection(property.getPossibleValues(), this.getValue(property)));
	}

	protected static <T> T findNextInCollection(Collection<T> collection, T object) {
		Iterator<T> iterator = collection.iterator();

		while(iterator.hasNext()) {
			if (iterator.next().equals(object)) {
				if (iterator.hasNext()) {
					return (T)iterator.next();
				}

				return (T)collection.iterator().next();
			}
		}

		return (T)iterator.next();
	}

	public String toString() {
		StringBuilder stringBuilder = new StringBuilder();
		stringBuilder.append(this.owner);
		if (!this.getValues().isEmpty()) {
			stringBuilder.append('[');
			stringBuilder.append((String)this.getValues().entrySet().stream().map(PROPERTY_ENTRY_TO_STRING_FUNCTION).collect(Collectors.joining(",")));
			stringBuilder.append(']');
		}

		return stringBuilder.toString();
	}

	public Collection<Property<?>> getProperties() {
		return Collections.unmodifiableCollection(this.values.keySet());
	}

	public <T extends Comparable<T>> boolean hasProperty(Property<T> property) {
		return this.values.containsKey(property);
	}

	@Override
	public <T extends Comparable<T>> T getValue(Property<T> property) {
		Comparable<?> comparable = (Comparable)this.values.get(property);
		if (comparable == null) {
			throw new IllegalArgumentException("Cannot get property " + property + " as it does not exist in " + this.owner);
		} else {
			return (T)property.getValueClass().cast(comparable);
		}
	}

	@Override
	public <T extends Comparable<T>, V extends T> S setValue(Property<T> property, V comparable) {
		Comparable<?> comparable2 = (Comparable)this.values.get(property);
		if (comparable2 == null) {
			throw new IllegalArgumentException("Cannot set property " + property + " as it does not exist in " + this.owner);
		} else if (comparable2 == comparable) {
			return (S)this;
		} else {
			S object = this.neighbours.get(property, comparable);
			if (object == null) {
				throw new IllegalArgumentException("Cannot set property " + property + " to " + comparable + " on " + this.owner + ", it is not an allowed value");
			} else {
				return object;
			}
		}
	}

	public void populateNeighbours(Map<Map<Property<?>, Comparable<?>>, S> map) {
		if (this.neighbours != null) {
			throw new IllegalStateException();
		} else {
			Table<Property<?>, Comparable<?>, S> table = HashBasedTable.create();

			for(Entry<Property<?>, Comparable<?>> entry : this.values.entrySet()) {
				Property<?> property = (Property)entry.getKey();

				for(Comparable<?> comparable : property.getPossibleValues()) {
					if (comparable != entry.getValue()) {
						table.put(property, comparable, (S)map.get(this.makeNeighbourValues(property, comparable)));
					}
				}
			}

			this.neighbours = (Table<Property<?>, Comparable<?>, S>)(table.isEmpty() ? table : ArrayTable.create(table));
		}
	}

	private Map<Property<?>, Comparable<?>> makeNeighbourValues(Property<?> property, Comparable<?> comparable) {
		Map<Property<?>, Comparable<?>> map = Maps.newHashMap(this.values);
		map.put(property, comparable);
		return map;
	}

	@Override
	public ImmutableMap<Property<?>, Comparable<?>> getValues() {
		return this.values;
	}
}
