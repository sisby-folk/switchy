package folk.sisby.switchy;

import com.unascribed.drogtor.DrogtorPlayer;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.nbt.NbtList;
import net.minecraft.nbt.NbtString;
import net.minecraft.util.Formatting;
import org.jetbrains.annotations.Nullable;
import org.samo_lego.fabrictailor.casts.TailoredPlayer;
import com.mojang.authlib.properties.Property;

import java.util.Objects;
import java.util.Optional;

public class SwitchyPreset {

	public String presetName;

	// When these are null, they'll be overwritten by whatever the player currently has when swapped in.
	@Nullable public String nickname;
	@Nullable public Formatting namecolor;
	@Nullable public String bio;
	@Nullable public String pronouns;
	@Nullable public String skinValue;
	@Nullable public String skinSignature;

	public SwitchyPreset(String name) {
		this.presetName = name;
	}

	public NbtList toNbt() {
		NbtList outList = new NbtList();
		outList.add(NbtString.of(Optional.ofNullable(presetName).orElse("\u0000")));
		outList.add(NbtString.of(Optional.ofNullable(nickname).orElse("\u0000")));
		outList.add(NbtString.of(namecolor == null ? "\u0000" : namecolor.getName()));
		outList.add(NbtString.of(Optional.ofNullable(bio).orElse("\u0000")));
		outList.add(NbtString.of(Optional.ofNullable(pronouns).orElse("\u0000")));
		outList.add(NbtString.of(Optional.ofNullable(skinValue).orElse("\u0000")));
		outList.add(NbtString.of(Optional.ofNullable(skinSignature).orElse("\u0000")));
		return outList;
	}
	public static SwitchyPreset fromNbt(NbtList nbtList) {
		SwitchyPreset outPreset = new SwitchyPreset(nbtList.getString(0));
		outPreset.nickname = Objects.equals(nbtList.getString(1), "\u0000") ? null : nbtList.getString(1);
		outPreset.namecolor = Objects.equals(nbtList.getString(2), "\u0000") ? null: Formatting.byName(nbtList.getString(2));
		outPreset.bio = Objects.equals(nbtList.getString(3), "\u0000") ? null: nbtList.getString(3);
		outPreset.pronouns = Objects.equals(nbtList.getString(4), "\u0000") ? null: nbtList.getString(4);
		outPreset.skinValue = Objects.equals(nbtList.getString(5), "\u0000") ? null: nbtList.getString(5);
		outPreset.skinSignature = Objects.equals(nbtList.getString(6), "\u0000") ? null: nbtList.getString(6);
		return outPreset;
	}

	public void updateFromPlayer(PlayerEntity player) {
		// Drogtor
		DrogtorPlayer drogtorPlayer = (DrogtorPlayer) player;
		this.nickname = drogtorPlayer.drogtor$getNickname();
		this.namecolor = drogtorPlayer.drogtor$getNameColor();
		this.bio = drogtorPlayer.drogtor$getBio();

		// Player Pronouns

		// Fabric Tailor
		TailoredPlayer tailoredPlayer = (TailoredPlayer) player;
		this.skinValue = tailoredPlayer.getSkinValue();
		this.skinSignature = tailoredPlayer.getSkinSignature();
	}

	public void applyToPlayer(PlayerEntity player) {
		// Drogtor
		DrogtorPlayer drogtorPlayer = (DrogtorPlayer) player;
		drogtorPlayer.drogtor$setNickname(this.nickname);
		drogtorPlayer.drogtor$setNameColor(this.namecolor);
		drogtorPlayer.drogtor$setBio(this.bio);

		// Player Pronouns

		// Fabric Tailor
		TailoredPlayer tailoredPlayer = (TailoredPlayer) player;
		if (this.skinValue != null && this.skinSignature != null) {
			tailoredPlayer.setSkin(this.skinValue, this.skinSignature, true);
		}
	}

	@Override
	public String toString() {
		return presetName;
	}
}
