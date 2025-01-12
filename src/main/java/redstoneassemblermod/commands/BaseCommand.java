package redstoneassemblermod.commands;

import com.mojang.brigadier.Command;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import net.minecraft.command.CommandRegistryAccess;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.text.Text;
import redstoneassemblermod.utils.AssemblerCommandUtils;

public class BaseCommand {
    private static final String NUMBER_ARGUMENT = "number";

    public static void command(
        CommandDispatcher<ServerCommandSource> dispatcher,
        CommandRegistryAccess registryAccess,
        CommandManager.RegistrationEnvironment environment
    ) {
        dispatcher.register(
            CommandManager.literal("base")
                .then(
                    CommandManager.literal("binary")
                        .then(
                            CommandManager.argument(NUMBER_ARGUMENT, IntegerArgumentType.integer())
                                .executes(BaseCommand::executeBinary)
                        )
                )
                .then(
                    CommandManager.literal("decimal")
                        .then(
                            CommandManager.argument(NUMBER_ARGUMENT, StringArgumentType.word())
                                .executes(BaseCommand::executeDecimal)
                        )
                )
        );
    }

    private static int executeBinary(CommandContext<ServerCommandSource> context) {
        byte number = (byte) IntegerArgumentType.getInteger(context, NUMBER_ARGUMENT);
        context.getSource().sendFeedback(
            () -> Text.literal(number + " in binary: " + AssemblerCommandUtils.byteToString(number)),
            false
        );
        return Command.SINGLE_SUCCESS;
    }

    private static int executeDecimal(CommandContext<ServerCommandSource> context) {
        String argument = StringArgumentType.getString(context, NUMBER_ARGUMENT);
        int number;
        try {
            number = Integer.parseInt(argument, 2);
        }
        catch (NumberFormatException e) {
            context.getSource().sendError(Text.literal(argument + " is not a binary number"));
            return -1;
        }
        context.getSource().sendFeedback(
            () -> Text.literal(
                argument + " in:\nSigned decimal: " + ((byte) number) + "\nUnsigned decimal: " + number
            ),
            false
        );
        return Command.SINGLE_SUCCESS;
    }
}

