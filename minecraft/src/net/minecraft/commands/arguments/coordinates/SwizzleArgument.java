package net.minecraft.commands.arguments.coordinates;

import com.mojang.brigadier.StringReader;
import com.mojang.brigadier.arguments.ArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.exceptions.SimpleCommandExceptionType;
import java.util.Arrays;
import java.util.Collection;
import java.util.EnumSet;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.TranslatableComponent;

public class SwizzleArgument implements ArgumentType<EnumSet<Direction.Axis>> {
	private static final Collection<String> EXAMPLES = Arrays.asList("xyz", "x");
	private static final SimpleCommandExceptionType ERROR_INVALID = new SimpleCommandExceptionType(new TranslatableComponent("arguments.swizzle.invalid"));

	public static SwizzleArgument swizzle() {
		return new SwizzleArgument();
	}

	public static EnumSet<Direction.Axis> getSwizzle(CommandContext<CommandSourceStack> commandContext, String string) {
		return commandContext.getArgument(string, EnumSet.class);
	}

	public EnumSet<Direction.Axis> parse(StringReader stringReader) throws CommandSyntaxException {
		EnumSet<Direction.Axis> enumSet = EnumSet.noneOf(Direction.Axis.class);

		while(stringReader.canRead() && stringReader.peek() != ' ') {
			char c = stringReader.read();
			Direction.Axis axis;
			switch(c) {
				case 'x':
					axis = Direction.Axis.X;
					break;
				case 'y':
					axis = Direction.Axis.Y;
					break;
				case 'z':
					axis = Direction.Axis.Z;
					break;
				default:
					throw ERROR_INVALID.create();
			}

			if (enumSet.contains(axis)) {
				throw ERROR_INVALID.create();
			}

			enumSet.add(axis);
		}

		return enumSet;
	}

	@Override
	public Collection<String> getExamples() {
		return EXAMPLES;
	}
}
