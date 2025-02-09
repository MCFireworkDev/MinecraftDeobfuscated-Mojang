package net.minecraft.util.datafix.schemas;

import com.mojang.datafixers.DSL;
import com.mojang.datafixers.schemas.Schema;
import com.mojang.datafixers.types.templates.TypeTemplate;
import java.util.Map;
import java.util.function.Supplier;
import net.minecraft.util.datafix.fixes.References;

public class V3083 extends NamespacedSchema {
	public V3083(int i, Schema schema) {
		super(i, schema);
	}

	protected static void registerMob(Schema schema, Map<String, Supplier<TypeTemplate>> map, String string) {
		schema.register(
			map,
			string,
			(Supplier<TypeTemplate>)(() -> DSL.optionalFields(
					"ArmorItems",
					DSL.list(References.ITEM_STACK.in(schema)),
					"HandItems",
					DSL.list(References.ITEM_STACK.in(schema)),
					"listener",
					DSL.optionalFields("event", DSL.optionalFields("game_event", References.GAME_EVENT_NAME.in(schema)))
				))
		);
	}

	@Override
	public Map<String, Supplier<TypeTemplate>> registerEntities(Schema schema) {
		Map<String, Supplier<TypeTemplate>> map = super.registerEntities(schema);
		registerMob(schema, map, "minecraft:allay");
		return map;
	}
}
