package foundation.esoteric.tss.minecraft.plugins.experimental.commands;

import dev.jorel.commandapi.CommandAPICommand;
import dev.jorel.commandapi.CommandPermission;
import dev.jorel.commandapi.executors.CommandArguments;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.TextComponent;
import net.kyori.adventure.text.event.ClickEvent;
import net.kyori.adventure.text.event.HoverEvent;
import net.kyori.adventure.text.format.TextDecoration;
import foundation.esoteric.tss.minecraft.plugins.core.data.Colour;
import foundation.esoteric.tss.minecraft.plugins.experimental.TSSSlimeLabsPlugin;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

public class StyledTextTestCommand {

  public StyledTextTestCommand(TSSSlimeLabsPlugin plugin) {
	new CommandAPICommand("test-styled-text")
			.withShortDescription("A test for styled text.")
			.withFullDescription("Sends a styled message in chat, and 2 seconds later kicks you to test whether the styles work in the kick menu. (they don't)")
			.withUsage("/test-styled-text")
			.withAliases("tst", "styled-text", "styled-text-test", "style-text")
			.withPermission(CommandPermission.OP)
			.executesPlayer((Player player, CommandArguments args) -> {
			  String discordInviteURL = "https://www.discord.gg/" + plugin.getCore().getConfig().getString("discord-server-invite-code");

			  TextComponent styledText = Component.text(discordInviteURL, Colour.BLURPLE, TextDecoration.UNDERLINED);

			  styledText = styledText.clickEvent(ClickEvent.clickEvent(ClickEvent.Action.OPEN_URL, discordInviteURL));
			  styledText = styledText.hoverEvent(HoverEvent.hoverEvent(HoverEvent.Action.SHOW_TEXT, styledText));
			  player.sendMessage(styledText);

			  TextComponent finalClickableText = styledText;

			  Bukkit.getScheduler().runTaskLater(plugin, () -> {
				player.kick(finalClickableText);
			  }, 40);
			})
			.register();
  }
}
