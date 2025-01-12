package redstoneassemblermod;

import net.fabricmc.api.ModInitializer;

import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import redstoneassemblermod.commands.*;

public class RedstoneAssemblerMod implements ModInitializer {
	public static final String MOD_ID = "redstoneassemblermod";

	// This logger is used to write text to the console and the log file.
	// It is considered best practice to use your mod id as the logger's name.
	// That way, it's clear which mod wrote info, warnings, and errors.
	public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

	@Override
	public void onInitialize() {
		// This code runs as soon as Minecraft is in a mod-load-ready state.
		// However, some things (like resources) may still be uninitialized.
		// Proceed with mild caution.

		LOGGER.info("Hello Fabric world!");

		CommandRegistrationCallback.EVENT.register(MachineCodeCommand::command);
		CommandRegistrationCallback.EVENT.register(FillProgramRomCommand::command);
		CommandRegistrationCallback.EVENT.register(EmulateProgramCommand::command);
		CommandRegistrationCallback.EVENT.register(ReadRegistryCommand::command);
		CommandRegistrationCallback.EVENT.register(ReadMemoryCommand::command);
		CommandRegistrationCallback.EVENT.register(BaseCommand::command);
	}
}
