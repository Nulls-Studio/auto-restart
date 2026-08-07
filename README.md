# AutoRestart

A lightweight Minecraft plugin that automatically restarts your server whenever selected files or directories are modified.

AutoRestart monitors the files and directories you specify and automatically restarts the server when changes are detected. This makes it ideal for automated deployments, configuration updates, or development environments.

---

## Features

- Automatically restart the server when watched files change.
- Monitor individual files.
- Monitor directories.
- Monitor directories recursively.
- Lightweight and easy to configure.
- In-game management commands.
- Reload the configuration without restarting the server.
- Enabled by default.

---

## Installation

1. Download the latest release [here](https://github.com/noone-075/auto-restart/releases/latest). 
2. Place the plugin JAR into your server's `plugins` folder.
3. Start your server.
4. The plugin will generate its configuration files.
5. Edit `plugins/AutoRestart/files.txt` to configure what should be monitored.

---

## Configuration

The watch list is located at:

`plugins/AutoRestart/files.txt`

Each line represents a watch rule.

### Supported Rules

| Rule | Description |
|------|-------------|
| `file <path>` | Watch a single file. |
| `dir <path>` | Watch a directory (non-recursive). |
| `rdir <path>` | Watch a directory recursively, including all subdirectories. |

### Example

```text
# Example watch rules

file /server.properties
dir /plugins/
rdir /config/
```

Whenever one of these files / directory get changed, AutoRestart will automatically restart the server unless monitoring is paused or disabled.

---

## Commands

`/auto-restart <start|stop|pause|reload|status> [duration]`

### Available Sub-Commands

| Command | Description |
|---------|-------------|
| `start` | Enable automatic restarting. |
| `stop` | Disable automatic restarting. |
| `pause <duration>` | Pause monitoring for a specific amount of time. |
| `reload` | Reload the configuration and watch list. |
| `status` | Display the current plugin status. |

### Pause Duration

The pause command accepts the following format:

| Example | Meaning |
|---------|---------|
| `30s` | 30 seconds |
| `5m` | 5 minutes |
| `1h` | 1 hour |

Examples:

```text
/auto-restart pause 30s
/auto-restart pause 5m
/auto-restart pause 1h
```

---

## How It Works

AutoRestart continuously watches the configured files and directories.

When a change is detected, the plugin automatically initiates a normal Minecraft server restart.

If you edit `files.txt`, simply run:

```text
/auto-restart reload
```

to reload the watch list without restarting the server.

---

## Default Behavior

- Automatic restarting is enabled by default.
- The plugin starts monitoring immediately after the server starts.

---

## Compatibility

Compatible with Paper, Spigot, and other Bukkit-based server software.

---

## Author

**noone-075**

---

## License

This project is licensed under the **AutoRestart Free Distribution License (AFDL) v1.0**.

You are free to use, modify, and redistribute this plugin, provided that it remains free to obtain and is not sold or placed behind a paywall. See the `LICENSE` file for the full license text.
