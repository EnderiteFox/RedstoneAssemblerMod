package redstoneassemblermod.commands;

import com.mojang.brigadier.Command;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.context.CommandContext;
import fr.enderitefox.redstoneassembler.api.emulators.AssemblyLanguageEmulator;
import fr.enderitefox.redstoneassembler.core.redstone_assembly.emulator.RedstoneAssemblyEmulator;
import fr.enderitefox.redstoneassembler.core.redstone_assembly.emulator.RedstoneAssemblyResult;
import net.minecraft.command.CommandRegistryAccess;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.text.Text;
import redstoneassemblermod.utils.AssemblerCommandUtils;

import java.util.List;

public class EmulateProgramCommand {
    public static void command(
        CommandDispatcher<ServerCommandSource> dispatcher,
        CommandRegistryAccess registryAccess,
        CommandManager.RegistrationEnvironment environment
    ) {
        dispatcher.register(
            CommandManager.literal("emulateprogram")
                .requires(ServerCommandSource::isExecutedByPlayer)
                .executes(EmulateProgramCommand::execute)
        );
    }

    private static int execute(CommandContext<ServerCommandSource> context) {
        if (!(context.getSource().getEntity() instanceof PlayerEntity player)) return -1;

        List<Short> assembledProgram = AssemblerCommandUtils.assembleProgramInBook(context, player);
        if (assembledProgram == null) return Command.SINGLE_SUCCESS;

        AssemblyLanguageEmulator<RedstoneAssemblyResult> emulator = new RedstoneAssemblyEmulator();
        RedstoneAssemblyResult result;

        try {
            result = emulator.emulateProgram(assembledProgram);
        }
        catch (InternalError e) {
            context.getSource().sendError(Text.literal("Error while running program:"));
            context.getSource().sendError(Text.literal(e.getMessage()));
            return Command.SINGLE_SUCCESS;
        }

        context.getSource().sendFeedback(
            () -> Text.literal(result.toString()),
            false
        );
        return Command.SINGLE_SUCCESS;
    }
}
