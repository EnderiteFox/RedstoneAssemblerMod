package redstoneassemblermod.utils;

import com.mojang.brigadier.context.CommandContext;
import fr.enderitefox.redstoneassembler.api.Assembler;
import fr.enderitefox.redstoneassembler.api.OperationTable;
import fr.enderitefox.redstoneassembler.api.preprocessors.CompoundPreprocessor;
import fr.enderitefox.redstoneassembler.core.preprocessors.AssemblyPreprocessor;
import fr.enderitefox.redstoneassembler.core.redstone_assembly.RedstoneAssembler;
import fr.enderitefox.redstoneassembler.core.redstone_assembly.RedstoneOperationTable;
import fr.enderitefox.redstoneassembler.core.redstone_assembly.preprocessors.RedstoneAliasesPreprocessor;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.component.type.WritableBookContentComponent;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.state.property.Properties;
import net.minecraft.text.Text;
import net.minecraft.util.Hand;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.world.World;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class AssemblerCommandUtils {
    public static int MAX_PROGRAM_SIZE = 1024;
    public static short NOP = 0x0000;

    public static ItemStack getBookItem(CommandContext<ServerCommandSource> context, PlayerEntity player) {
        ItemStack itemStack = player.getStackInHand(Hand.MAIN_HAND);
        if (itemStack.isEmpty()) {
            context.getSource().sendError(Text.literal("The program book is not in your hand!"));
            return null;
        }
        if (!itemStack.getItem().equals(Items.WRITABLE_BOOK)) {
            context.getSource().sendError(Text.literal("This ItemStack is not a writable book!"));
            return null;
        }
        return itemStack;
    }

    public static List<String> readProgramInBook(CommandContext<ServerCommandSource> context, PlayerEntity player) {
        ItemStack itemStack = getBookItem(context, player);
        if (itemStack == null) return null;

        WritableBookContentComponent component = itemStack.getComponents().get(DataComponentTypes.WRITABLE_BOOK_CONTENT);
        if (component == null) {
            context.getSource().sendError(Text.literal("Can't find writable book component!"));
            return null;
        }

        List<String> program = new ArrayList<>();
        for (String page : component.stream(false).toList()) {
            program.addAll(Arrays.asList(page.split("\n")));
        }
        return program;
    }

    public static List<Short> assembleProgramInBook(CommandContext<ServerCommandSource> context, PlayerEntity player) {
        return assembleProgramInBook(context, player, true);
    }

    public static List<Short> assembleProgramInBook(CommandContext<ServerCommandSource> context, PlayerEntity player, boolean fillWithNops) {
        List<String> program = readProgramInBook(context, player);
        if (program == null) return null;

        OperationTable operationTable = new RedstoneOperationTable();
        CompoundPreprocessor preprocessor = new AssemblyPreprocessor(operationTable);
        preprocessor.registerPreprocessor(new RedstoneAliasesPreprocessor(), 0);
        try {
            program = preprocessor.preprocessProgram(program);
        }
        catch (IllegalArgumentException e) {
            context.getSource().sendError(Text.literal("Error while preprocessing program\n" + e.getMessage()));
            return null;
        }

        Assembler assembler = new RedstoneAssembler(operationTable);
        List<Short> assembledProgram;

        try {
            assembledProgram = assembler.readProgram(program);
        }
        catch (InternalError e) {
            context.getSource().sendError(Text.literal("Internal error\n" + e.getMessage()));
            return null;
        }
        catch (IllegalArgumentException e) {
            context.getSource().sendError(Text.literal("Error while reading program\n" + e.getMessage()));
            return null;
        }

        if (fillWithNops) while (assembledProgram.size() < MAX_PROGRAM_SIZE) assembledProgram.add(NOP);

        return assembledProgram;
    }

    public static String assembledProgramToString(List<Short> assembledProgram) {
        StringBuilder strInst = new StringBuilder();
        for (Short instruction : assembledProgram) {
            for (int i = 0; i < 16; ++i) {
                strInst.append((((instruction << i) & 0x8000) != 0) ? "1" : "0");
            }
            strInst.append("\n");
        }
        return strInst.toString();
    }

    public static Byte readRegistry(World world, BlockPos loc, final int BIT_COUNT, final Direction BIT_DIRECTION, final int BIT_OFFSET) {
        byte value = 0;
        for (int i = BIT_COUNT - 1; i >= 0; --i) {
            BlockPos blockPos = loc.add(BIT_DIRECTION.getVector().multiply(BIT_OFFSET).multiply(i));
            BlockState block = world.getBlockState(blockPos);
            if (!block.getBlock().equals(Blocks.REPEATER)) return null;
            if (block.get(Properties.POWERED)) {
                value |= (byte) (1 << (BIT_COUNT - i - 1));
            }
        }
        return value;
    }

    public static String byteToString(byte number) {
        StringBuilder str = new StringBuilder();
        for (int i = 0; i < 8; ++i) {
            str.append(((number << i) & 0x80) != 0 ? '1' : '0');
        }
        return str.toString();
    }
}
