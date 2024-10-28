package foundation.esoteric.tss.minecraft.plugins.experimental;

import foundation.esoteric.tss.minecraft.plugins.experimental.commands.*;
import foundation.esoteric.tss.minecraft.plugins.core.TSSCorePlugin;
import org.bukkit.Bukkit;
import org.bukkit.plugin.java.JavaPlugin;

public final class TSSSlimeLabsPlugin extends JavaPlugin {

  private final TSSCorePlugin corePlugin = (TSSCorePlugin) Bukkit.getPluginManager().getPlugin("TSSCore");

  public TSSCorePlugin getCore() {
	return corePlugin;
  }

  @Override
  public void onEnable() {
	new StyledTextTestCommand(this);
	new LaunchCommand(this);
	new LocaleCommand();
	new ClearPDCCommand();
	new GetPDCValuesCommand(this);
	new LogContainerContentsCommand();
  }
}
