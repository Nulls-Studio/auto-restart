package io.github.noone_075.auto_restart;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.command.PluginCommand;

import net.kyori.adventure.text.Component;

import java.io.IOException;
import java.nio.file.Path;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;

public class AutoRestartPlugin extends JavaPlugin {
    private static final String CONFIG_FILENAME = "files.txt";

    private boolean autoRestartEnabled = true;
    private long pausedUntil = 0;
    private final AtomicBoolean restartScheduled = new AtomicBoolean(false);

    private Path serverRoot;
    private Path configFilePath;

    private Config config;
    private WatchFiles watcher;

    @Override
    public void onEnable() {
        serverRoot = getServer().getWorldContainer().toPath().toAbsolutePath().normalize();
        configFilePath = getDataFolder().toPath().resolve(CONFIG_FILENAME);

        if (!getDataFolder().exists() && !getDataFolder().mkdirs()) {
            getLogger().warning("Could not create plugin data folder: " + getDataFolder().getAbsolutePath());
        }

        try {
            Config.createDefaultConfig(configFilePath);
        } catch (IOException e) {
            getLogger().severe("Could not create default " + CONFIG_FILENAME + ": " + e.getMessage());
        }

        config = new Config(configFilePath, serverRoot);
        watcher = new WatchFiles(this,
                this::onWatchedPathChanged,
                () -> autoRestartEnabled,
                () -> pausedUntil);
        AutoRestartCommand command = new AutoRestartCommand(this);

        PluginCommand pluginCommand = getCommand("auto-restart");
        if (pluginCommand != null) {
            pluginCommand.setExecutor(command);
            pluginCommand.setTabCompleter(command);
        }

        reloadConfigAndWatcher();
        autoRestartEnabled = true;
        pausedUntil = 0;
        getLogger().info("AutoRestart is enabled and watching configured files.");
    }

    @Override
    public void onDisable() {
        watcher.stopWatching();
        getLogger().info("AutoRestart disabled.");
    }

    public void reloadConfigAndWatcher() {
        config.load(getLogger());
        Map<Path, WatchType> rules = config.getWatchRules();
        watcher.setWatchConfig(rules);
        watcher.startWatching();
    }

    public void setAutoRestartEnabled(boolean enabled) {
        autoRestartEnabled = enabled;
    }

    public void setPausedUntil(long timestamp) {
        pausedUntil = timestamp;
    }

    public String getStatusMessage() {
        StringBuilder status = new StringBuilder();
        status.append(autoRestartEnabled ? "enabled" : "disabled");
        if (pausedUntil > System.currentTimeMillis()) {
            status.append(" (paused until ").append(java.time.Instant.ofEpochMilli(pausedUntil)).append(")");
        }
        status.append(". Configured watchers: ").append(config.getWatchRules().size());
        return status.toString();
    }

    private void onWatchedPathChanged(Path changedPath) {
        if (!restartScheduled.compareAndSet(false, true)) {
            return;
        }

        Bukkit.getScheduler().runTask(this, () -> performRestart(changedPath));
    }

    private void performRestart(Path changedPath) {
        String relativeChanged = serverRoot.relativize(changedPath).toString().replace('\\', '/');
        String kickMessageForPerm = "Server is restarting because a change was applied to " + relativeChanged + ".";
        String defaultKickMessage = "Server is restarting.";

        Component kickMessageForPermComponent = Component.text(kickMessageForPerm);
        Component defaultKickMessageComponent = Component.text(defaultKickMessage);

        for (Player player : Bukkit.getOnlinePlayers()) {
            if (player.hasPermission("auto-restart")) {
                player.kick(kickMessageForPermComponent);
            } else {
                player.kick(defaultKickMessageComponent);
            }
        }

        getLogger().info("Restarting server due to watched change: " + changedPath);
        Bukkit.getServer().shutdown();
    }
}
