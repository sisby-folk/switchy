package folk.sisby.switchy;

import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import folk.sisby.switchy.api.SwitchyEvents;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;
import org.quiltmc.qsl.networking.api.ServerPlayNetworking;

import java.util.List;
import java.util.function.BiConsumer;
import java.util.function.Predicate;

import static folk.sisby.switchy.util.Feedback.helpText;

/**
 * Switchy addon initializer for server-side Switchy Client.
 *
 * @author Sisby folk
 * @since 2.0.0
 */
public class SwitchyClientServer implements SwitchyEvents.Init, SwitchyEvents.CommandInit {
	@Override
	public void onInitialize() {
		SwitchyClientServerNetworking.InitializeReceivers();
		SwitchyClientServerNetworking.InitializeRelays();
	}

	@Override
	public void registerCommands(LiteralArgumentBuilder<ServerCommandSource> switchyArgument, BiConsumer<Text, Predicate<ServerPlayerEntity>> helpTextRegistry) {
		List.of(
				helpText("commands.switchy_client.export.help", "commands.switchy_client.export.command"),
				helpText("commands.switchy_client.import.help", "commands.switchy_client.import.command", "commands.switchy.help.placeholder.file")
		).forEach(t -> helpTextRegistry.accept(t, p -> ServerPlayNetworking.canSend(p, SwitchyClientServerNetworking.S2C_PRESETS)));
	}
}
