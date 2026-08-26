package io.github.noone_075.auto_restart;

import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class AutoRestartCommand implements CommandExecutor, TabCompleter {

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
            sender.sendMessage("Usage: /auto-restart <start|stop|pause|reload|status>");
            sender.sendMessage("Current state: " + plugin.getStatusMessage());
            return true;
        }

        String subcommand = args[0].toLowerCase();
        if (subcommand.equals("start")) {
            plugin.setAutoRestartEnabled(true);
            plugin.setPausedUntil(0);
            sender.sendMessage("Auto-restart watching has been started.");
            return true;
        } else if (subcommand.equals("stop")) {
            plugin.setAutoRestartEnabled(false);
            plugin.setPausedUntil(0);
            sender.sendMessage("Auto-restart watching has been stopped.");
            return true;
        } else if (subcommand.equals("pause")) {
            if (args.length < 2) {
                sender.sendMessage("Usage: /auto-restart pause <time> (example: 10s, 2m, 3h)");
                return true;
            }

            long millis = parseDurationMillis(args[1]);
            if (millis <= 0) {
                sender.sendMessage("Invalid duration. Use values like 10s, 2m, 3h.");
                return true;
            }

            plugin.setPausedUntil(System.currentTimeMillis() + millis);
            plugin.setAutoRestartEnabled(true);
            sender.sendMessage("Auto-restart paused for " + formatDurationFromMillis(millis) + ".");
            return true;
        } else if (subcommand.equals("reload")) {
            plugin.reloadConfigAndWatcher();
            sender.sendMessage("Auto-restart configuration reloaded and watching restarted.");
            return true;
        } else if (subcommand.equals("status")) {
            sender.sendMessage(plugin.getStatusMessage());
            return true;
        } else {
            sender.sendMessage("Usage: /auto-restart <start|stop|pause|reload|status>");
            return true;
        }
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (!sender.hasPermission("auto-restart")) {
            return Collections.emptyList();
        }

        if (args.length == 1) {
            List<String> subcommands = new ArrayList<>();
            subcommands.add("start");
            subcommands.add("stop");
            subcommands.add("pause");
            subcommands.add("reload");
            subcommands.add("status");

            String input = args[0].toLowerCase();
            List<String> matches = new ArrayList<>();

            for (String sub : subcommands) {
                if (sub.startsWith(input)) {
                    matches.add(sub);
                }
            }

            return matches;
        }

        if (args.length == 2 && args[0].equalsIgnoreCase("pause")) {
            String input = args[1];

            // User typed only digits -> suggest units
            if (input.matches("\\d+")) {
                List<String> suggestions = new ArrayList<>();
                suggestions.add(input + "s");
                suggestions.add(input + "m");
                suggestions.add(input + "h");
                return suggestions;
            }

            // User already started typing the unit
            Matcher matcher = Pattern.compile("^(\\d+)([smhSMH]?)$").matcher(input);
            if (matcher.matches()) {
                String number = matcher.group(1);
                String unit = matcher.group(2).toLowerCase();

                List<String> suggestions = new ArrayList<>();

                if ("s".startsWith(unit)) {
                    suggestions.add(number + "s");
                }
                if ("m".startsWith(unit)) {
                    suggestions.add(number + "m");
                }
                if ("h".startsWith(unit)) {
                    suggestions.add(number + "h");
                }

                return suggestions;
            }
        }

        return Collections.emptyList();
    }

    private long parseDurationMillis(String token) {
        Matcher matcher = TIME_PATTERN.matcher(token);
        if (!matcher.matches()) {
            return 0;
        }

        long value = Long.parseLong(matcher.group(1));
        char unit = Character.toLowerCase(matcher.group(2).charAt(0));

        switch (unit) {
            case 's':
                return value * 1000;
            case 'm':
                return value * 60 * 1000;
            case 'h':
                return value * 60 * 60 * 1000;
            default:
                return 0;
        }
    }

    private String formatDurationFromMillis(long millis) {
        long seconds = millis / 1000;

        if (seconds % 3600 == 0) {
            return (seconds / 3600) + "h";
        }
        if (seconds % 60 == 0) {
            return (seconds / 60) + "m";
        }
        return seconds + "s";
    }
}
