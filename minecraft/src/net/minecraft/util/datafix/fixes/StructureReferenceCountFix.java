package net.minecraft.util.datafix.fixes;

import com.mojang.datafixers.DSL;
import com.mojang.datafixers.DataFix;
import com.mojang.datafixers.Dynamic;
import com.mojang.datafixers.TypeRewriteRule;
import com.mojang.datafixers.Typed;
import com.mojang.datafixers.schemas.Schema;
import com.mojang.datafixers.types.Type;

public class StructureReferenceCountFix extends DataFix {
	public StructureReferenceCountFix(Schema schema, boolean bl) {
		super(schema, bl);
	}

	@Override
	protected TypeRewriteRule makeRule() {
		Type<?> type = this.getInputSchema().getType(References.STRUCTURE_FEATURE);
		return this.fixTypeEverywhereTyped(
			"Structure Reference Fix", type, typed -> typed.update(DSL.remainderFinder(), StructureReferenceCountFix::setCountToAtLeastOne)
		);
	}

	private static <T> Dynamic<T> setCountToAtLeastOne(Dynamic<T> dynamic) {
		return dynamic.update("references", dynamicx -> dynamicx.createInt(dynamicx.asNumber().map(Number::intValue).filter(integer -> integer > 0).orElse(1)));
	}
}
