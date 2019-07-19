package net.minecraft.util.datafix.fixes;

import com.mojang.datafixers.DSL;
import com.mojang.datafixers.DataFix;
import com.mojang.datafixers.DataFixUtils;
import com.mojang.datafixers.Dynamic;
import com.mojang.datafixers.TypeRewriteRule;
import com.mojang.datafixers.Typed;
import com.mojang.datafixers.schemas.Schema;
import com.mojang.datafixers.types.Type;
import com.mojang.datafixers.util.Pair;
import java.util.Optional;
import java.util.stream.Stream;

public class MobSpawnerEntityIdentifiersFix extends DataFix {
	public MobSpawnerEntityIdentifiersFix(Schema schema, boolean bl) {
		super(schema, bl);
	}

	private Dynamic<?> fix(Dynamic<?> dynamic) {
		if (!"MobSpawner".equals(dynamic.get("id").asString(""))) {
			return dynamic;
		} else {
			Optional<String> optional = dynamic.get("EntityId").asString();
			if (optional.isPresent()) {
				Dynamic<?> dynamic2 = DataFixUtils.orElse(dynamic.get("SpawnData").get(), dynamic.emptyMap());
				dynamic2 = dynamic2.set("id", dynamic2.createString(((String)optional.get()).isEmpty() ? "Pig" : (String)optional.get()));
				dynamic = dynamic.set("SpawnData", dynamic2);
				dynamic = dynamic.remove("EntityId");
			}

			Optional<? extends Stream<? extends Dynamic<?>>> optional2 = dynamic.get("SpawnPotentials").asStreamOpt();
			if (optional2.isPresent()) {
				dynamic = dynamic.set(
					"SpawnPotentials",
					dynamic.createList(
						((Stream)optional2.get())
							.map(
								dynamicx -> {
									Optional<String> optionalxx = dynamicx.get("Type").asString();
									if (optionalxx.isPresent()) {
										Dynamic<?> dynamic2 = DataFixUtils.orElse(dynamicx.get("Properties").get(), dynamicx.emptyMap())
											.set("id", dynamicx.createString((String)optionalxx.get()));
										return dynamicx.set("Entity", dynamic2).remove("Type").remove("Properties");
									} else {
										return dynamicx;
									}
								}
							)
					)
				);
			}

			return dynamic;
		}
	}

	@Override
	public TypeRewriteRule makeRule() {
		Type<?> type = this.getOutputSchema().getType(References.UNTAGGED_SPAWNER);
		return this.fixTypeEverywhereTyped("MobSpawnerEntityIdentifiersFix", this.getInputSchema().getType(References.UNTAGGED_SPAWNER), type, typed -> {
			Dynamic<?> dynamic = typed.get(DSL.remainderFinder());
			dynamic = dynamic.set("id", dynamic.createString("MobSpawner"));
			Pair<?, ? extends Optional<? extends Typed<?>>> pair = type.readTyped(this.fix(dynamic));
			return !((Optional)pair.getSecond()).isPresent() ? typed : (Typed)((Optional)pair.getSecond()).get();
		});
	}
}
