package redstoneassemblermod.commands;

import com.mojang.brigadier.Command;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.context.CommandContext;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.command.CommandRegistryAccess;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.state.property.Properties;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.Vec3d;
import net.minecraft.util.math.Vec3i;
import net.minecraft.world.World;
import redstoneassemblermod.utils.AssemblerCommandUtils;

import java.util.List;

public class FillProgramRomCommand {
    private static final int PROGRAM_MAX_SIZE = 1024;
    private static final Short NOP = 0x0000;

    private static final int BLOCK_SPACING = 2;
    private static final int BYTE_SPACING = 2;

    private static final Direction REPEATER_DIRECTION = Direction.SOUTH;
    private static final BlockState ZERO_BLOCK = Blocks.PURPLE_CONCRETE.getDefaultState();

    private static final Direction COLUMN_DIRECTION = Direction.NORTH;
    private static final int COLUMN_SPACING = 7;

    private static final Direction ALTERNATING_DIRECTION = Direction.WEST;
    private static final int ALTERNATING_SPACING = 1;

    private static final Direction ROW_DIRECTION = Direction.WEST;
    private static final int ROW_SPACING = 2;

    private static final int BANK_ROW_COUNT = 16;
    private static final int BANK_COLUMN_COUNT = 16;
    private static final int BANK_COUNT = 2;
    private static final int BANK_SPACING = 3;

    private static final int INTERSPACING_COUNT = 2;
    private static final Direction INTERSPACING_DIRECTION = Direction.NORTH;
    private static final int INTERSPACING_SPACING = 2;

    @SuppressWarnings("unused")
    public static void command(
        CommandDispatcher<ServerCommandSource> dispatcher,
        CommandRegistryAccess registryAccess,
        CommandManager.RegistrationEnvironment environment
    ) {
        dispatcher.register(
            CommandManager.literal("fillrom")
                .requires(ServerCommandSource::isExecutedByPlayer)
                .requires(source -> source.hasPermissionLevel(1))
                .executes(FillProgramRomCommand::execute)
        );
    }

    private static int execute(CommandContext<ServerCommandSource> context) {
        if (!(context.getSource().getEntity() instanceof PlayerEntity player)) return -1;

        List<Short> assembledProgram = AssemblerCommandUtils.assembleProgramInBook(context, player);
        if (assembledProgram == null) return Command.SINGLE_SUCCESS;
        while (assembledProgram.size() < PROGRAM_MAX_SIZE) assembledProgram.add(NOP);

        writeAllInstructions(
            assembledProgram,
            context.getSource().getWorld(),
            player.getBlockPos().add(0, -1, 0)
        );
        return Command.SINGLE_SUCCESS;
    }

    private static void writeAllInstructions(List<Short> instructions, World world, BlockPos origin) {
        int currentInstruction = 0;
        for (int interspacing = 0; interspacing < INTERSPACING_COUNT; ++interspacing) {
            for (int bank = 0; bank < BANK_COUNT; ++bank) {
                for (int row = 0; row < BANK_ROW_COUNT; ++row) {
                    for (int column = 0; column < BANK_COLUMN_COUNT; ++column) {
                        Vec3d offset = COLUMN_DIRECTION.getDoubleVector().multiply(column * COLUMN_SPACING);
                        offset = offset.add(ALTERNATING_DIRECTION.getDoubleVector().multiply(((column + bank) % 2) * ALTERNATING_SPACING));
                        offset = offset.add(ROW_DIRECTION.getDoubleVector().multiply(row * ROW_SPACING));
                        offset = offset.add(ROW_DIRECTION.getDoubleVector().multiply(BANK_ROW_COUNT * ROW_SPACING * bank));
                        offset = offset.add(ROW_DIRECTION.getDoubleVector().multiply(BANK_SPACING * bank));
                        offset = offset.add(INTERSPACING_DIRECTION.getDoubleVector().multiply(interspacing * INTERSPACING_SPACING));
                        writeInstruction(
                            world,
                            instructions.get(currentInstruction),
                            origin.add(new Vec3i((int) offset.x, (int) offset.y, (int) offset.z)),
                            interspacing % 2 != 0
                        );
                        ++currentInstruction;
                    }
                }
            }
        }
    }

    private static void writeInstruction(World world, Short instruction, BlockPos pos, boolean invertRepeater) {
        for (int i = 0; i < 16; ++i) {
            BlockPos placePos = pos.add(0, -i * BLOCK_SPACING, 0);
            if (i >= 8) placePos = placePos.add(0, -BYTE_SPACING, 0);
            if (((instruction << i) & 0x8000) == 0) world.setBlockState(placePos, ZERO_BLOCK);
            else {
                world.setBlockState(
                    placePos,
                    Blocks.REPEATER.getDefaultState()
                        .with(
                            Properties.HORIZONTAL_FACING,
                            invertRepeater ? REPEATER_DIRECTION.getOpposite() : REPEATER_DIRECTION
                        ),
                    0x2 | 0x20
                );
            }
        }
    }
}
