package redstoneassemblermod.commands;

import com.mojang.brigadier.Command;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.context.CommandContext;
import net.minecraft.command.CommandRegistryAccess;
import net.minecraft.entity.Entity;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.text.Text;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import redstoneassemblermod.RedstoneAssemblerMod;
import redstoneassemblermod.utils.AssemblerCommandUtils;

import java.util.ArrayList;
import java.util.List;

public class ReadMemoryCommand {
    private static final Direction BIT_DIRECTION = Direction.DOWN;
    private static final int BIT_OFFSET = 2;
    private static final int BIT_COUNT = 8;

    private static final Direction REGISTRY_DIRECTION = Direction.EAST;
    private static final int REGISTRY_OFFSET = 2;
    private static final int REGISTRY_COUNT = 16;

    private static final Direction REGISTRY_LINE_DIRECTION = Direction.SOUTH;
    private static final int REGISTRY_LINE_OFFSET = 2;
    private static final int REGISTRY_LINE_COUNT = 2;

    private static final Direction STAGGERING_DIRECTION = Direction.UP;
    private static final int STAGGERING_OFFSET = 1;

    private static final Direction REGISTRY_BANK_DIRECTION = Direction.EAST;
    private static final int REGISTRY_BANK_OFFSET = 6;
    private static final int REGISTRY_BANK_COUNT = 2;

    private static final Direction REGISTRY_BANK_LINE_DIRECTION = Direction.SOUTH;
    private static final int REGISTRY_BANK_LINE_OFFSET = 16;
    private static final int REGISTRY_BANK_LINE_COUNT = 4;

    public static void command(
        CommandDispatcher<ServerCommandSource> dispatcher,
        CommandRegistryAccess registryAccess,
        CommandManager.RegistrationEnvironment environment
    ) {
        dispatcher.register(
            CommandManager.literal("readmemory")
                .requires(ServerCommandSource::isExecutedByPlayer)
                .executes(ReadMemoryCommand::execute)
        );
    }

    public static int execute(CommandContext<ServerCommandSource> context) {
        List<Byte> registries = new ArrayList<>();

        Entity player = context.getSource().getEntity();
        if (player == null) return -1;

        for (int registryBankLine = 0; registryBankLine < REGISTRY_BANK_LINE_COUNT; ++registryBankLine) {
            for (int registryLine = 0; registryLine < REGISTRY_LINE_COUNT; ++registryLine) {
                for (int registryBank = 0; registryBank < REGISTRY_BANK_COUNT; ++registryBank) {
                    for (int registry = 0; registry < REGISTRY_COUNT; ++registry) {
                        BlockPos pos = player.getBlockPos().add(
                                REGISTRY_BANK_LINE_DIRECTION.getVector()
                                    .multiply(REGISTRY_BANK_LINE_OFFSET)
                                    .multiply(registryBankLine)
                            )
                            .add(
                                REGISTRY_BANK_DIRECTION.getVector()
                                    .multiply(REGISTRY_BANK_OFFSET)
                                    .add(REGISTRY_DIRECTION.getVector().multiply(REGISTRY_OFFSET).multiply(REGISTRY_COUNT - 1))
                                    .multiply(registryBank)
                            )
                            .add(
                                REGISTRY_LINE_DIRECTION.getVector()
                                    .multiply(REGISTRY_LINE_OFFSET)
                                    .multiply(registryLine)
                        )
                            .add(
                                REGISTRY_DIRECTION.getVector()
                                    .multiply(REGISTRY_OFFSET)
                                    .multiply(registry)
                            )
                            .add(
                                STAGGERING_DIRECTION.getVector()
                                    .multiply(STAGGERING_OFFSET)
                                    .multiply((registry + registryBank) % 2)
                            );
                        RedstoneAssemblerMod.LOGGER.info("Reading block at {}", pos);
                        registries.add(
                            AssemblerCommandUtils.readRegistry(
                                context.getSource().getWorld(),
                                pos,
                                BIT_COUNT,
                                BIT_DIRECTION,
                                BIT_OFFSET
                            )
                        );
                    }
                }
            }
        }

        StringBuilder result = new StringBuilder("Memory:\n");
        for (int i = 0; i < registries.size(); ++i) {
            if (registries.get(i) != null && registries.get(i) == 0) continue;
            result.append("Address ")
                .append(i)
                .append(": ");
            if (registries.get(i) == null) result.append("Read error");
            else {
                result.append(AssemblerCommandUtils.byteToString(registries.get(i)))
                    .append(" ")
                    .append(registries.get(i));
            }
            result.append("\n");
        }

        context.getSource().sendFeedback(() -> Text.literal(result.toString()), false);
        return Command.SINGLE_SUCCESS;
    }
}
