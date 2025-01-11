package redstoneassemblermod.commands;

import com.mojang.brigadier.Command;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.context.CommandContext;
import net.minecraft.command.CommandRegistryAccess;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.text.Text;
import redstoneassemblermod.utils.AssemblerCommandUtils;

import java.util.List;

public class MachineCodeCommand {
    @SuppressWarnings("unused")
    public static void command(
        CommandDispatcher<ServerCommandSource> dispatcher,
        CommandRegistryAccess registryAccess,
        CommandManager.RegistrationEnvironment environment
    ) {
        dispatcher.register(
            CommandManager.literal("machinecode")
                .requires(ServerCommandSource::isExecutedByPlayer)
                .executes(MachineCodeCommand::execute)
        );
    }

    private static int execute(CommandContext<ServerCommandSource> context) {
        if (!(context.getSource().getEntity() instanceof PlayerEntity player)) return -1;

        List<Short> assembledProgram = AssemblerCommandUtils.assembleProgramInBook(context, player, false);
        if (assembledProgram == null) return Command.SINGLE_SUCCESS;

        context.getSource().sendFeedback(
            () -> Text.literal(AssemblerCommandUtils.assembledProgramToString(assembledProgram)),
            false
        );

        return Command.SINGLE_SUCCESS;
    }
}
