package foundation.esoteric.tss.minecraft.plugins.experimental.commands;

import dev.jorel.commandapi.CommandAPICommand;
import dev.jorel.commandapi.CommandPermission;
import dev.jorel.commandapi.arguments.BooleanArgument;
import dev.jorel.commandapi.arguments.DoubleArgument;
import dev.jorel.commandapi.arguments.EntitySelectorArgument;
import dev.jorel.commandapi.executors.CommandArguments;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.TextComponent;
import net.kyori.adventure.text.event.ClickEvent;
import net.kyori.adventure.text.event.HoverEvent;
import net.minecraft.ChatFormatting;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.phys.Vec3;
import foundation.esoteric.tss.minecraft.plugins.core.data.Colour;
import foundation.esoteric.tss.minecraft.plugins.core.data.player.Message;
import foundation.esoteric.tss.minecraft.plugins.core.managers.MessageManager;
import foundation.esoteric.tss.minecraft.plugins.core.util.DebugUtil;
import foundation.esoteric.tss.minecraft.plugins.core.util.MessageUtil;
import foundation.esoteric.tss.minecraft.plugins.core.util.NMSUtil;
import foundation.esoteric.tss.minecraft.plugins.experimental.TSSSlimeLabsPlugin;
import org.bukkit.Bukkit;
import org.bukkit.Color;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scheduler.BukkitTask;
import org.bukkit.scoreboard.*;
import org.bukkit.util.Vector;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

public class LaunchCommand {

  final int[] ticks = {0};
  private final ScoreboardManager boardManager = Bukkit.getScoreboardManager();
  private final MessageManager messageManager;
  private final ArrayList<BukkitTask> spawnParticlesTasks = new ArrayList<>();
  private final AtomicReference<ArrayList<Boolean>> onGroundStats = new AtomicReference<>(new ArrayList<>());

  private final List<VelocityEstimation> yVelocityEstimations = Arrays.asList(
		  (double initialVelocity, double acceleration, double drag, double ticksPassed) -> (initialVelocity - acceleration) * Math.pow(1 - drag, ticksPassed) - acceleration * (1 - Math.pow(1 - drag, ticksPassed)) / drag,
		  (double initialVelocity, double acceleration, double drag, double ticksPassed) -> initialVelocity * Math.pow(1 - drag, ticksPassed) - acceleration * (1 - Math.pow(1 - drag, ticksPassed)) / drag * (1 - drag),
		  (double initialVelocity, double acceleration, double drag, double ticksPassed) -> initialVelocity * Math.pow(1 - drag, ticksPassed + 1) - acceleration * (1 - Math.pow(1 - drag, ticksPassed + 1)) / drag * (1 - drag),
		  (double initialVelocity, double acceleration, double drag, double ticksPassed) -> Math.pow(50.0D / 49.0D, 1 - ticksPassed) + 98.0D / 25.0D * (Math.pow(49.0D / 50.0D, ticksPassed) - 1)
  );

  private final List<VelocityEstimationBasedOnPreviousVelocity> yVelocityEstimationsBasedOnPreviousVelocities = List.of(
		  (double previousVelocity) -> (previousVelocity - 0.08D) * 0.9800000190734863
  );

  private final ArrayList<ArrayList<Double>> yVelocityEstimationErrors = new ArrayList<>();
  private final ArrayList<ArrayList<Double>> yVelocityEstimationsBasedOnPreviousVelocitiesErrors = new ArrayList<>();

  private final List<VelocityEstimation> xzVelocityEstimations = List.of(
		  (double initialVelocity, double acceleration, double drag, double ticksPassed) -> initialVelocity * Math.pow(0.98D, ticksPassed)
  );

  private final ArrayList<ArrayList<Double>> xVelocityEstimationErrors = new ArrayList<>();
  private final ArrayList<ArrayList<Double>> zVelocityEstimationErrors = new ArrayList<>();

  private BukkitTask analysisTask;

  public LaunchCommand(@NotNull TSSSlimeLabsPlugin plugin) {
	messageManager = plugin.getCore().getMessageManager();

	new CommandAPICommand("launch")
			.withShortDescription("Fling an entity in the sky!")
			.withFullDescription("Flings an entity in the sky with a specified velocity. Analyses the flight path and returns some information about it once the analysis is done. Analysis finishes either on landing, when clicking the popup, or when the /finish-launch command is run. (see argument 3)")
			.withUsage("/launch (entity) (velocity) (finish on landing)")
			.withAliases("fling")
			.withPermission(CommandPermission.OP)
			.withOptionalArguments(new EntitySelectorArgument.OneEntity("entity"), new DoubleArgument("velocity"), new BooleanArgument("finish on landing"))
			.executesPlayer((Player player, CommandArguments args) -> {
			  if (analysisTask != null) {
				messageManager.sendMessage(player, Message.ACTIVE_LAUNCH_ANALYSIS_TASK);
				return;
			  }

			  TextComponent launchInfoTitle = messageManager.getPlayerMessage(Message.LAUNCH, player);
			  player.sendMessage(launchInfoTitle);

			  Entity entity = (Entity) args.get("entity");
			  if (entity == null) {
				entity = player;
			  }

			  TextComponent colon = MessageUtil.getColon();
			  messageManager.sendMessage(player, Message.ENTITY, entity.getName());

			  Boolean finishOnLanding = (Boolean) args.get("finish on landing");

			  if (finishOnLanding == null || !finishOnLanding) {
				TextComponent finishLaunchAnalysis = messageManager.getPlayerMessage(Message.VIEW_LAUNCH_STATISTICS, player);

				finishLaunchAnalysis = finishLaunchAnalysis.clickEvent(ClickEvent.clickEvent(ClickEvent.Action.RUN_COMMAND, "/finish-launch"));
				finishLaunchAnalysis = finishLaunchAnalysis.hoverEvent(HoverEvent.hoverEvent(HoverEvent.Action.SHOW_TEXT, messageManager.getPlayerMessage(Message.STOP_LAUNCH_ANALYSIS, player)));

				player.sendMessage(finishLaunchAnalysis);
			  }

			  Scoreboard board = boardManager.getNewScoreboard();
			  Objective objective = board.registerNewObjective("launch_info", Criteria.DUMMY, launchInfoTitle);
			  objective.setDisplaySlot(DisplaySlot.SIDEBAR);
			  objective.displayName(launchInfoTitle);

			  TextComponent pipe = MessageUtil.getPipe();

			  Team time = board.registerNewTeam("Time");
			  time.addEntry(ChatFormatting.YELLOW.toString());
			  time.prefix(messageManager.getPlayerMessage(Message.TIME, player));

			  Team isOnGround = board.registerNewTeam("On ground");
			  isOnGround.addEntry(ChatFormatting.GRAY.toString());
			  isOnGround.prefix(messageManager.getPlayerMessage(Message.ON_GROUND, player));

			  Team vx = board.registerNewTeam("vx");
			  vx.addEntry(ChatFormatting.RED.toString() + ChatFormatting.RED);
			  vx.prefix(pipe.append(Component.text("vx", Colour.RED)).append(colon));

			  Team vz = board.registerNewTeam("vz");
			  vz.addEntry(ChatFormatting.AQUA.toString() + ChatFormatting.AQUA);
			  vz.prefix(pipe.append(Component.text("vz", Colour.SKY_BLUE)).append(colon));

			  Team vy = board.registerNewTeam("vy");
			  vy.addEntry(ChatFormatting.GREEN.toString() + ChatFormatting.GREEN);
			  vy.prefix(pipe.append(Component.text("vy", Colour.SLIME)).append(colon));

			  objective.getScore(ChatFormatting.YELLOW.toString()).setScore(7);
			  objective.getScore(ChatFormatting.GRAY.toString()).setScore(6);
			  objective.getScore("").setScore(5);
			  objective.getScore(ChatFormatting.RED.toString() + ChatFormatting.RED).setScore(4);
			  objective.getScore(ChatFormatting.AQUA.toString() + ChatFormatting.AQUA).setScore(3);
			  objective.getScore(" ").setScore(2);
			  objective.getScore(ChatFormatting.GREEN.toString() + ChatFormatting.GREEN).setScore(1);

			  Double velocity = (Double) args.get("velocity");
			  if (velocity == null) {
				velocity = 1.0D;
			  }

			  Vector initialVelocity = entity.getLocation().getDirection().multiply(velocity);
			  entity.setVelocity(initialVelocity);

			  double initialXVelocity = initialVelocity.getX();
			  double initialYVelocity = initialVelocity.getY();
			  double initialZVelocity = initialVelocity.getZ();

			  player.setScoreboard(board);

			  ServerPlayer serverPlayer = NMSUtil.getServerPlayer(player);
			  assert serverPlayer != null;

			  ticks[0] = 0;

			  Entity finalEntity = entity;
			  net.minecraft.world.entity.Entity nmsEntity = (net.minecraft.world.entity.Entity) NMSUtil.invokeGetHandle(entity);
			  assert nmsEntity != null;

			  onGroundStats.set(new ArrayList<>());

			  for (int i = 0; i < yVelocityEstimations.size(); i++) {
				yVelocityEstimationErrors.add(new ArrayList<>());
			  }

			  for (int i = 0; i < xzVelocityEstimations.size(); i++) {
				xVelocityEstimationErrors.add(new ArrayList<>());
				zVelocityEstimationErrors.add(new ArrayList<>());
			  }

			  analysisTask = new BukkitRunnable() {

				private Location previousLocation = finalEntity.getLocation();

				private Vector previousVelocity = new Vector();
				private Vec3 previousNmsVelocity = new Vec3(0, 0, 0);
				private Vector previousRealVelocity = new Vector();

				@Override
				public void run() {
				  ticks[0] += 1;

				  if (finishOnLanding != null && finishOnLanding && ((finalEntity.isOnGround() && nmsEntity.onGround()) || (finalEntity.isDead() && !nmsEntity.isAlive()))) {
					endLaunchAnalysis(player);
					return;
				  }

				  Vector velocity = finalEntity.getVelocity();
				  Location currentLocation = finalEntity.getLocation();

				  Vec3 nmsVelocity = nmsEntity.getDeltaMovement();

				  double realXVelocity = currentLocation.getX() - previousLocation.getX();
				  double realYVelocity = currentLocation.getY() - previousLocation.getY();
				  double realZVelocity = currentLocation.getZ() - previousLocation.getZ();

				  Vector realVelocity = new Vector(
						  realXVelocity,
						  realYVelocity,
						  realZVelocity
				  );

				  previousLocation = currentLocation;

				  for (int i = 0; i < yVelocityEstimations.size(); i++) {
					VelocityEstimation estimation = yVelocityEstimations.get(i);
					double estimatedVelocity = estimation.estimateVelocity(initialYVelocity, 0.08D, 0.02D, ticks[0]);
					double error = estimatedVelocity - realYVelocity;

					DebugUtil.log("Y Error: " + error);

					yVelocityEstimationErrors.get(i).add(error);
				  }

				  for (int i = 0; i < xzVelocityEstimations.size(); i++) {
					VelocityEstimation estimation = xzVelocityEstimations.get(i);
					double estimatedXVelocity = estimation.estimateVelocity(initialXVelocity, 0.08D, 0.02D, ticks[0]);
					double xVelocityEstimationError = estimatedXVelocity - realXVelocity;

					double estimatedZVelocity = estimation.estimateVelocity(initialZVelocity, 0.08D, 0.02D, ticks[0]);
					double zVelocityEstimationError = estimatedZVelocity - realZVelocity;

					DebugUtil.log("X Error: " + xVelocityEstimationError);
					DebugUtil.log("Z Error: " + zVelocityEstimationError);

					xVelocityEstimationErrors.get(i).add(xVelocityEstimationError);
					zVelocityEstimationErrors.get(i).add(zVelocityEstimationError);
				  }

				  DebugUtil.log("Spigot velocity: " + velocity, "NMS velocity: " + nmsVelocity, "Real velocity: " + realVelocity);

				  time.suffix(
						  Component.text(
										  MessageUtil.formatNumber(ticks[0] / 20.0D) + "s", Colour.YELLOW
								  )
								  .append(Component.text(" (", Colour.LIGHT_GREY))
								  .append(Component.text(ticks[0] + "t", Colour.SKY_BLUE))
								  .append(Component.text(")", Colour.LIGHT_GREY))
				  );

				  isOnGround.suffix(
						  MessageUtil.format(finalEntity.isOnGround())
								  .append(Component.space())
								  .append(pipe)
								  .append(MessageUtil.format(nmsEntity.onGround))
				  );

				  onGroundStats.get().add(nmsEntity.onGround);

				  TextComponent comma = MessageUtil.getComma();

				  vx.suffix(
						  Component.text(MessageUtil.formatNumber(velocity.getX()), Colour.LIGHT_GREY)
								  .append(comma)
								  .append(Component.text(MessageUtil.formatNumber(realXVelocity)))
								  .append(comma)
								  .append(Component.text(MessageUtil.formatNumber(nmsVelocity.x)))
				  );

				  vz.suffix(
						  Component.text(MessageUtil.formatNumber(velocity.getZ()), Colour.LIGHT_GREY)
								  .append(comma)
								  .append(Component.text(MessageUtil.formatNumber(realZVelocity)))
								  .append(comma)
								  .append(Component.text(MessageUtil.formatNumber(nmsVelocity.z)))
				  );

				  vy.suffix(
						  Component.text(MessageUtil.formatNumber(velocity.getY()), Colour.LIGHT_GREY)
								  .append(comma)
								  .append(Component.text(MessageUtil.formatNumber(realYVelocity)))
								  .append(comma)
								  .append(Component.text(MessageUtil.formatNumber(nmsVelocity.y)))
				  );

				  previousVelocity = velocity;
				  previousNmsVelocity = nmsVelocity;
				  previousRealVelocity = realVelocity;


				  spawnParticlesTasks.add(new BukkitRunnable() {

					private final Location particleLocation = finalEntity.getLocation();

					@Override
					public void run() {
					  particleLocation.getWorld().spawnParticle(Particle.DUST_COLOR_TRANSITION, particleLocation, 1, new Particle.DustTransition(
									  Color.RED,
									  Color.RED,
									  0.85F
							  )
					  );

					  if (finishOnLanding != null && finishOnLanding && ((finalEntity.isOnGround() && nmsEntity.onGround()) || (finalEntity.isDead() && !nmsEntity.isAlive()))) {
						endLaunchAnalysis(player);
					  }
					}
				  }.runTaskTimer(plugin, 0, 1));
				}
			  }.runTaskTimer(plugin, 2, 1);
			})
			.register();

	new CommandAPICommand("finish-launch")
			.executesPlayer((Player player, CommandArguments args) -> endLaunchAnalysis(player))
			.register();
  }

  private void endLaunchAnalysis(Player player) {
	if (analysisTask == null) {
	  messageManager.sendMessage(player, Message.NO_ANALYSIS_TASK);
	  return;
	}

	analysisTask.cancel();
	analysisTask = null;
	player.setScoreboard(boardManager.getNewScoreboard());

	for (BukkitTask spawnParticlesTask : spawnParticlesTasks) {
	  spawnParticlesTask.cancel();
	}

	spawnParticlesTasks.clear();

	ArrayList<Boolean> onGroundStatsList = onGroundStats.get();

	messageManager.sendMessage(player, Message.LAUNCH_STATISTICS);
	player.sendMessage(MessageUtil.createCompactArray(onGroundStatsList.toArray()));

	DebugUtil.log(yVelocityEstimationErrors);
  }

  private interface VelocityEstimation {
	double estimateVelocity(double initialVelocity, double acceleration, double drag, double ticksPassed);
  }

  private interface VelocityEstimationBasedOnPreviousVelocity {
	double estimateVelocity(double previousVelocity);
  }
}
