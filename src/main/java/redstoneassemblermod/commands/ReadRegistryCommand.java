package redstoneassemblermod.commands;

import com.mojang.brigadier.Command;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.context.CommandContext;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.command.CommandRegistryAccess;
import net.minecraft.entity.Entity;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.state.property.Properties;
import net.minecraft.text.Text;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.world.World;
import redstoneassemblermod.RedstoneAssemblerMod;

import java.util.ArrayList;
import java.util.List;

public class ReadRegistryCommand {
    private static final Direction REGISTRY_DIRECTION = Direction.SOUTH;
    private static final Direction STAGGERING_DIRECTION = Direction.DOWN;
    private static final Direction BIT_DIRECTION = Direction.DOWN;
    private static final int STAGGERING_DISTANCE = 1;
    private static final int REGISTRY_OFFSET = 2;
    private static final int REGISTRY_COUNT = 16;
    private static final int BIT_OFFSET = 2;
    private static final int BIT_COUNT = 8;

    public static void command(
        CommandDispatcher<ServerCommandSource> dispatcher,
        CommandRegistryAccess registryAccess,
        CommandManager.RegistrationEnvironment environment
    ) {
        dispatcher.register(
            CommandManager.literal("readregistry")
                .requires(ServerCommandSource::isExecutedByPlayer)
                .executes(ReadRegistryCommand::execute)
        );
    }

    private static int execute(CommandContext<ServerCommandSource> context) {
        Entity player = context.getSource().getEntity();
        if (player == null) return -1;
        final List<Short> registryContent = new ArrayList<>();
        registryContent.add((short) 0);

        for (int i = 1; i < REGISTRY_COUNT; ++i) {
            Short value = readRegister(
                context.getSource().getWorld(),
                player.getBlockPos()
                    .add(REGISTRY_DIRECTION.getVector().multiply(REGISTRY_OFFSET).multiply(i - 1))
                    .add(STAGGERING_DIRECTION.getVector().multiply(STAGGERING_DISTANCE).multiply(i % 2 == 0 ? 1 : 0))
            );

            if (value == null) {
                context.getSource().sendError(Text.literal("Error while reading blocks"));
            }

            registryContent.add(value);
        }

        StringBuilder str = new StringBuilder("Registry bank:\n");
        for (int i = 0; i < registryContent.size(); ++i) {
            str.append("r").append(i).append(": ");
            for (int j = 0; j < 8; ++j) {
                str.append(((registryContent.get(i) << j) & 0x80) != 0 ? '1' : '0');
            }
            str.append(" ").append(registryContent.get(i).byteValue()).append("\n");
        }

        context.getSource().sendFeedback(() -> Text.literal(str.toString()),  false);
        return Command.SINGLE_SUCCESS;
    }

    private static Short readRegister(World world, BlockPos loc) {
        short value = 0;
        for (int i = BIT_COUNT - 1; i >= 0; --i) {
            BlockPos blockPos = loc.add(BIT_DIRECTION.getVector().multiply(BIT_OFFSET).multiply(i));
            BlockState block = world.getBlockState(blockPos);
            if (!block.getBlock().equals(Blocks.REPEATER)) return null;
            if (block.get(Properties.POWERED)) {
                value |= (short) (1 << (BIT_COUNT - i - 1));
            }
        }
        return value;
    }
}
