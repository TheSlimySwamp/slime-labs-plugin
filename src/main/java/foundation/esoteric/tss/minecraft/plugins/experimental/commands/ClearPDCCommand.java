package foundation.esoteric.tss.minecraft.plugins.experimental.commands;

import dev.jorel.commandapi.CommandAPICommand;
import dev.jorel.commandapi.executors.CommandArguments;
import org.bukkit.Chunk;
import org.bukkit.NamespacedKey;
import org.bukkit.entity.Player;
import org.bukkit.persistence.PersistentDataContainer;

public class ClearPDCCommand {
  public ClearPDCCommand() {
	new CommandAPICommand("clear-persistent-data-container")
			.executesPlayer((Player player, CommandArguments args) -> {
			  for (Chunk chunk : player.getWorld().getLoadedChunks()) {
				PersistentDataContainer container = chunk.getPersistentDataContainer();

				for (NamespacedKey key : container.getKeys()) {
				  container.remove(key);
				}
			  }
			})
			.register();
  }
}
