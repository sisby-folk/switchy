package folk.sisby.switchy;

import com.mojang.brigadier.arguments.StringArgumentType;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.nbt.NbtList;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Style;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import org.quiltmc.loader.api.ModContainer;
import org.quiltmc.qsl.base.api.entrypoint.ModInitializer;
import org.quiltmc.qsl.command.api.CommandRegistrationCallback;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Objects;

import static net.minecraft.server.command.CommandManager.argument;
import static net.minecraft.server.command.CommandManager.literal;

public class Switchy implements ModInitializer {
	public static final Logger LOGGER = LoggerFactory.getLogger("Switchy");

	@Override
	public void onInitialize(ModContainer mod) {

		CommandRegistrationCallback.EVENT.register((dispatcher, buildContext, environment) -> dispatcher.register(
				literal("switchy")
						.then(literal("help")
								.executes((context) -> {
									ServerPlayerEntity player = context.getSource().getPlayer();

									inform(player, "Commands: new, set, delete, list");
									inform(player, "/switchy new {name} - create a new preset");
									inform(player, "/switchy set {name} - saves current preset and swaps to another");
									inform(player, "/switchy delete {name} - delete a preset");
									inform(player, "/switchy list - list all created presets");
									return 1;
								}))
						.then(literal("list")
								.executes((context) -> {
									ServerPlayerEntity player = context.getSource().getPlayer();

									SwitchyPresets presets = ((SwitchyPlayer) player).switchy$getPresets();
									player.sendMessage(Text.literal("Presets: ").append(Text.literal(presets.toString())), false);
									player.sendMessage(Text.literal("Current Preset: ").append(Text.literal(Objects.toString(presets.getCurrentPreset(), "<None>"))), false);
									return 1;
								}))
						.then(literal("new")
								.then(argument("preset", StringArgumentType.word())
										.executes((context) -> {
											ServerPlayerEntity player = context.getSource().getPlayer();
											String presetName = context.getArgument("preset", String.class);
											SwitchyPresets presets = validateSwitchyPlayer(player).switchy$getPresets();

											if (presets.addPreset(new SwitchyPreset(presetName))) {
												informWithPreset(player, "Successfully added empty preset ", presetName);
											} else {
												informWithPreset(player, "That preset already exists! - try /switchy set ", presetName);
											}
											return 1;
										})))
						.then(literal("set")
								.then(argument("preset", StringArgumentType.word())
										.executes((context) -> {
											ServerPlayerEntity player = context.getSource().getPlayer();
											String presetName = context.getArgument("preset", String.class);
											SwitchyPresets presets = validateSwitchyPlayer(player).switchy$getPresets();

											String oldPresetName = Objects.toString(presets.getCurrentPreset(), "<None>");
											if (presets.setCurrentPreset(presetName)) {
												informSwitch(player, oldPresetName, presetName);
											} else {
												inform(player, "That preset doesn't exist! /switchy list");
											}
											return 1;
										})))
						.then(literal("delete")
								.then(argument("preset", StringArgumentType.word())
										.executes((context) -> {
											ServerPlayerEntity player = context.getSource().getPlayer();
											String presetName = context.getArgument("preset", String.class);
											SwitchyPresets presets = validateSwitchyPlayer(player).switchy$getPresets();

											if (presets.deletePreset(presetName)) {
												informWithPreset(player, "Preset deleted: ", presetName);
											} else {
												inform(player, "That preset doesn't exist! /switchy list");
											}
											return 1;
										})))
		));

		LOGGER.info("Switchy Initialized!");
	}

	private SwitchyPlayer validateSwitchyPlayer(ServerPlayerEntity player) {
		if (((SwitchyPlayer) player).switchy$getPresets() == null) {
			((SwitchyPlayer) player).switchy$setPresets(SwitchyPresets.fromNbt(player, new NbtList()));
		}
		return (SwitchyPlayer) player;
	}

	private void informSwitch(ServerPlayerEntity player, String oldPreset, String newPreset) {
		player.sendMessage(Text.literal("You've switched from ")
						.setStyle(Style.EMPTY.withColor(Formatting.YELLOW))
						.append(Text.literal(oldPreset))
						.append(Text.literal(" to "))
						.append(Text.literal(newPreset))
				, false);
	}

	private void informWithPreset(ServerPlayerEntity player, String literal, String preset) {
		player.sendMessage(Text.literal(literal)
						.setStyle(Style.EMPTY.withColor(Formatting.YELLOW))
						.append(Text.literal(preset))
				, false);
	}

	private void inform(ServerPlayerEntity player, String literal) {
		player.sendMessage(Text.literal(literal)
						.setStyle(Style.EMPTY.withColor(Formatting.YELLOW))
				, false);
	}

}
