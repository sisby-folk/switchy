package folk.sisby.switchy.compat;

import folk.sisby.switchy.Switchy;
import folk.sisby.switchy.SwitchyPreset;
import io.github.ashisbored.playerpronouns.PlayerPronouns;
import io.github.ashisbored.playerpronouns.data.PronounList;
import io.github.ashisbored.playerpronouns.data.Pronouns;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.LiteralText;
import net.minecraft.text.Text;
import org.jetbrains.annotations.Nullable;

import java.util.Map;

public class PlayerPronounsCompat extends PresetCompat {
	public static final String KEY = "playerPronouns";

	public static final String KEY_PRONOUNS_RAW = "pronounsRaw";

	// Overwritten on save when null
	@Nullable public Pronouns pronouns;

	@Override
	public void updateFromPlayer(PlayerEntity player) {
		this.pronouns = PlayerPronouns.getPronouns((ServerPlayerEntity) player);
	}

	@Override
	public void applyToPlayer(PlayerEntity player) {
		if (!PlayerPronouns.setPronouns((ServerPlayerEntity) player, this.pronouns)) {
			Switchy.LOGGER.error("Failed to apply Player Pronouns pronouns");
		}
	}

	@Override
	public NbtCompound toNbt(SwitchyPreset preset) {
		NbtCompound outNbt = new NbtCompound();
		if (this.pronouns != null) outNbt.putString(KEY_PRONOUNS_RAW, this.pronouns.raw());
		return outNbt;
	}

	@Override
	public void fillFromNbt(NbtCompound nbt) {
		String pronounsRaw = nbt.contains(KEY_PRONOUNS_RAW) ? nbt.getString(KEY_PRONOUNS_RAW): null;
		if (pronounsRaw != null) {
			// Ripped logic from the command
			Map<String, Text> pronounTexts = PronounList.get().getCalculatedPronounStrings();
			if (pronounTexts.containsKey(pronounsRaw)) {
				pronouns = new Pronouns(pronounsRaw, pronounTexts.get(pronounsRaw));
			} else {
				pronouns = new Pronouns(pronounsRaw, new LiteralText(pronounsRaw));
			}
		}
	}

	@Override
	public String getKey() {
		return KEY;
	}

	@Override
	public PresetCompat getEmptyModule() {
		return new PlayerPronounsCompat();
	}

	public static void register() {
		Switchy.COMPAT_MODULES.add(new PlayerPronounsCompat());
	}
}
