package foundation.esoteric.tss.minecraft.plugins.experimental.commands;

import dev.jorel.commandapi.CommandAPICommand;
import dev.jorel.commandapi.executors.CommandArguments;
import net.kyori.adventure.text.Component;
import foundation.esoteric.tss.minecraft.plugins.core.data.Colour;
import foundation.esoteric.tss.minecraft.plugins.core.data.player.Message;
import foundation.esoteric.tss.minecraft.plugins.core.util.MessageUtil;
import foundation.esoteric.tss.minecraft.plugins.experimental.TSSSlimeLabsPlugin;
import org.bukkit.Chunk;
import org.bukkit.NamespacedKey;
import org.bukkit.entity.Player;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;

public class GetPDCValuesCommand {

  public GetPDCValuesCommand(TSSSlimeLabsPlugin plugin) {
	new CommandAPICommand("get-persistent-data-container-values")
			.executesPlayer((Player player, CommandArguments args) -> {
			  plugin.getCore().getMessageManager().sendMessage(player, Message.CHUNK_PERSISTENT_DATA_CONTAINER);

			  Chunk playerChunk = player.getChunk();
			  PersistentDataContainer container = playerChunk.getPersistentDataContainer();

			  for (NamespacedKey key : container.getKeys()) {
				for (PersistentDataType type : new PersistentDataType[] {
						PersistentDataType.STRING,
						PersistentDataType.LONG,
						PersistentDataType.BOOLEAN,
						PersistentDataType.BYTE,
						PersistentDataType.DOUBLE,
						PersistentDataType.BYTE_ARRAY,
						PersistentDataType.FLOAT,
						PersistentDataType.INTEGER,
						PersistentDataType.INTEGER_ARRAY,
						PersistentDataType.LONG_ARRAY,
						PersistentDataType.SHORT,
						PersistentDataType.TAG_CONTAINER,
						PersistentDataType.TAG_CONTAINER_ARRAY,
				}) {
				  try {
					player.sendMessage(
							Component.text(
									key.getKey(),
									Colour.YELLOW
							).append(
									MessageUtil.getColon()
							).append(
									Component.space()
							).append(
									MessageUtil.format(container.get(key, type))
							)
					);

				  } catch (IllegalArgumentException ignoredException) {

				  }
				}
			  }
			})
			.register();
  }
}
