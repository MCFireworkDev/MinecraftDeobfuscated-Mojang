package net.minecraft.network.chat;

import com.mojang.brigadier.ParseResults;
import com.mojang.brigadier.arguments.ArgumentType;
import com.mojang.brigadier.context.CommandContextBuilder;
import com.mojang.brigadier.context.ParsedArgument;
import com.mojang.brigadier.context.ParsedCommandNode;
import com.mojang.brigadier.tree.ArgumentCommandNode;
import com.mojang.brigadier.tree.CommandNode;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.commands.arguments.PreviewedArgument;

public record PreviewableCommand<S>(List<PreviewableCommand.Argument<S>> arguments) {
	public static <S> PreviewableCommand<S> of(ParseResults<S> parseResults) {
		CommandContextBuilder<S> commandContextBuilder = parseResults.getContext();
		CommandContextBuilder<S> commandContextBuilder2 = commandContextBuilder;

		List<PreviewableCommand.Argument<S>> list;
		CommandContextBuilder<S> commandContextBuilder3;
		for(list = collectArguments(commandContextBuilder);
			(commandContextBuilder3 = commandContextBuilder2.getChild()) != null;
			commandContextBuilder2 = commandContextBuilder3
		) {
			boolean bl = commandContextBuilder3.getRootNode() != commandContextBuilder.getRootNode();
			if (!bl) {
				break;
			}

			list.addAll(collectArguments(commandContextBuilder3));
		}

		return new PreviewableCommand<>(list);
	}

	private static <S> List<PreviewableCommand.Argument<S>> collectArguments(CommandContextBuilder<S> commandContextBuilder) {
		List<PreviewableCommand.Argument<S>> list = new ArrayList();

		for(ParsedCommandNode<S> parsedCommandNode : commandContextBuilder.getNodes()) {
			CommandNode parsedArgument = parsedCommandNode.getNode();
			if (parsedArgument instanceof ArgumentCommandNode argumentCommandNode) {
				ArgumentType var7 = argumentCommandNode.getType();
				if (var7 instanceof PreviewedArgument previewedArgument) {
					ParsedArgument<S, ?> parsedArgumentx = (ParsedArgument)commandContextBuilder.getArguments().get(argumentCommandNode.getName());
					if (parsedArgumentx != null) {
						list.add(new PreviewableCommand.Argument<S>(argumentCommandNode, parsedArgumentx, previewedArgument));
					}
				}
			}
		}

		return list;
	}

	public boolean isPreviewed(CommandNode<?> commandNode) {
		for(PreviewableCommand.Argument<S> argument : this.arguments) {
			if (argument.node() == commandNode) {
				return true;
			}
		}

		return false;
	}

	public static record Argument<S>(ArgumentCommandNode<S, ?> node, ParsedArgument<S, ?> parsedValue, PreviewedArgument<?> previewType) {
		public String name() {
			return this.node.getName();
		}
	}
}
