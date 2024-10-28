package foundation.esoteric.tss.minecraft.plugins.experimental.commands;

import dev.jorel.commandapi.CommandAPICommand;
import dev.jorel.commandapi.executors.CommandArguments;
import net.kyori.adventure.text.Component;
import net.minecraft.server.level.ServerPlayer;
import org.bukkit.entity.Player;
import foundation.esoteric.tss.minecraft.plugins.core.data.Colour;
import foundation.esoteric.tss.minecraft.plugins.core.util.MessageUtil;
import foundation.esoteric.tss.minecraft.plugins.core.util.NMSUtil;

import java.util.Locale;

public class LocaleCommand {
  public LocaleCommand() {
	new CommandAPICommand("locale")
			.executesPlayer((Player player, CommandArguments args) -> {
			  Locale playerLocale = player.locale();
			  String improperPlayerLocale = player.getLocale();

			  ServerPlayer serverPlayer = NMSUtil.getServerPlayer(player);
			  assert serverPlayer != null;

			  String nmsLocale = serverPlayer.adventure$locale.getDisplayLanguage();
			  Locale adventureApiLocale = serverPlayer.adventure$locale;

			  player.sendMessage(Component.text("Your Current Locale:", Colour.SKY_BLUE)
					  .append(
							  Component.text("\nPaper Locale: ", Colour.WHITE).append(
									  Component.text(playerLocale.getDisplayName() + " (" + playerLocale.getDisplayScript() + ", " + playerLocale.getDisplayCountry() + ", " + playerLocale.getDisplayLanguage() + ", " + playerLocale.getDisplayVariant() + ", " + playerLocale.toLanguageTag() + ", " + playerLocale + ")", Colour.YELLOW)
							  )
					  )
					  .append(
							  Component.text("\nImproper Locale: ", Colour.BLOOD_RED)
									  .append(Component.text(improperPlayerLocale, Colour.YELLOW))
					  )
					  .append(
							  Component.text("\nNMS Locale: ", Colour.SKY_BLUE)
									  .append(Component.text(nmsLocale, Colour.YELLOW))
					  )
					  .append(
							  Component.text("\nAdventure API Locale: ", Colour.ORANGE)
									  .append(Component.text(adventureApiLocale.getDisplayName() + " (" + adventureApiLocale.getDisplayScript() + ", " + adventureApiLocale.getDisplayCountry() + ", " + adventureApiLocale.getDisplayLanguage() + ", " + adventureApiLocale.getDisplayVariant() + ", " + adventureApiLocale.toLanguageTag() + ", " + adventureApiLocale + ")", Colour.YELLOW))
					  ).append(
							  Component.text("\nLocale equal to English: ", Colour.ORANGE)
									  .append(MessageUtil.format(Locale.ENGLISH.equals(playerLocale)))
					  )
			  );
			})
			.register();
  }
}
