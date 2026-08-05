package io.github.noone_075.auto_restart;

import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;

import java.time.Duration;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class AutoRestartCommand implements CommandExecutor {
    private static final Pattern TIME_PATTERN = Pattern.compile("^(\\d+)([smhSMH])$");
    private final AutoRestartPlugin plugin;

    public AutoRestartCommand(AutoRestartPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!sender.hasPermission("auto-restart")) {
            sender.sendMessage("You do not have permission to use this command.");
            return true;
        }

        if (args.length == 0) {
            sender.sendMessage("Usage: /auto-restart <start|stop|pause|reload>");
            sender.sendMessage("Current state: " + plugin.getStatusMessage());
            return true;
        }

        switch (args[0].toLowerCase()) {
            case "start":
                plugin.setAutoRestartEnabled(true);
                plugin.setPausedUntil(0);
                sender.sendMessage("Auto-restart watching has been started.");
                return true;
            case "stop":
                plugin.setAutoRestartEnabled(false);
                plugin.setPausedUntil(0);
                sender.sendMessage("Auto-restart watching has been stopped.");
                return true;
            case "pause":
                if (args.length < 2) {
                    sender.sendMessage("Usage: /auto-restart pause <time> (example: 10s, 2m, 3h)");
                    return true;
                }
                Duration duration = parseDuration(args[1]);
                if (duration == null || duration.isZero() || duration.isNegative()) {
                    sender.sendMessage("Invalid duration. Use values like 10s, 2m, 3h.");
                    return true;
                }
                plugin.setPausedUntil(System.currentTimeMillis() + duration.toMillis());
                plugin.setAutoRestartEnabled(true);
                sender.sendMessage("Auto-restart paused for " + formatDuration(duration) + ".");
                return true;
            case "reload":
                plugin.reloadConfigAndWatcher();
                sender.sendMessage("Auto-restart configuration reloaded and watching restarted.");
                return true;
            case "status":
                sender.sendMessage(plugin.getStatusMessage());
                return true;
            default:
                sender.sendMessage("Usage: /auto-restart <start|stop|pause|reload>");
                return true;
        }
    }

    private Duration parseDuration(String token) {
        Matcher matcher = TIME_PATTERN.matcher(token);
        if (!matcher.matches()) {
            return null;
        }

        long value = Long.parseLong(matcher.group(1));
        char unit = Character.toLowerCase(matcher.group(2).charAt(0));
        return switch (unit) {
            case 's' -> Duration.ofSeconds(value);
            case 'm' -> Duration.ofMinutes(value);
            case 'h' -> Duration.ofHours(value);
            default -> null;
        };
    }

    private String formatDuration(Duration duration) {
        long seconds = duration.getSeconds();
        if (seconds % 3600 == 0) {
            return (seconds / 3600) + "h";
        }
        if (seconds % 60 == 0) {
            return (seconds / 60) + "m";
        }
        return seconds + "s";
    }
}
