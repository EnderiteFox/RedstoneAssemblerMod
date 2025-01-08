package redstoneassemblermod.utils;

import com.mojang.brigadier.context.CommandContext;
import fr.enderitefox.redstoneassembler.api.Assembler;
import fr.enderitefox.redstoneassembler.api.OperationTable;
import fr.enderitefox.redstoneassembler.api.preprocessors.CompoundPreprocessor;
import fr.enderitefox.redstoneassembler.core.preprocessors.AssemblyPreprocessor;
import fr.enderitefox.redstoneassembler.core.redstone_assembly.RedstoneAssembler;
import fr.enderitefox.redstoneassembler.core.redstone_assembly.RedstoneOperationTable;
import fr.enderitefox.redstoneassembler.core.redstone_assembly.preprocessors.RedstoneAliasesPreprocessor;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.component.type.WritableBookContentComponent;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.text.Text;
import net.minecraft.util.Hand;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class AssemblerCommandUtils {
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
}
