package io.github.noone_075.auto_restart;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class Config {
    private static final Pattern LINE_PATTERN = Pattern.compile("^(file|dir|rdir)\\s+(.+)$", Pattern.CASE_INSENSITIVE);
    private final Path configFile;
    private final Path serverRoot;
    private final Map<Path, WatchType> watchRules = new LinkedHashMap<>();

    public Config(Path configFile, Path serverRoot) {
        this.configFile = configFile;
        this.serverRoot = serverRoot;
    }

    public Map<Path, WatchType> getWatchRules() {
        return watchRules;
    }

    public void load(Logger logger) {
        watchRules.clear();

        if (!Files.exists(configFile)) {
            logger.warning(configFile.getFileName() + " does not exist. Create it in " + configFile.toAbsolutePath());
            return;
        }

        try {
            List<String> lines = Files.readAllLines(configFile, StandardCharsets.UTF_8);
            for (String rawLine : lines) {
                String line = rawLine.trim();
                if (line.isEmpty() || line.startsWith("#")) {
                    continue;
                }

                Matcher matcher = LINE_PATTERN.matcher(line);
                if (!matcher.matches()) {
                    logger.warning("Invalid watcher line: " + line);
                    continue;
                }

                WatchType watchType = WatchType.valueOf(matcher.group(1).toUpperCase());
                String pathToken = matcher.group(2).trim().replace('\\', '/');
                if (pathToken.isEmpty()) {
                    logger.warning("Invalid watcher path: " + line);
                    continue;
                }

                String normalizedPathToken = pathToken.replaceAll("/+$", "");
                Path target = serverRoot.resolve(normalizedPathToken.startsWith("/") ? normalizedPathToken.substring(1) : normalizedPathToken).normalize();

                if (watchType == WatchType.FILE && target.getParent() == null) {
                    logger.warning("Cannot watch file without parent directory: " + target);
                    continue;
                }

                watchRules.put(target, watchType);
                logger.info("Configured watch: " + watchType + " -> " + target);
            }
        } catch (IOException e) {
            logger.severe("Failed to read " + configFile.getFileName() + ": " + e.getMessage());
        }
    }

    public static void createDefaultConfig(Path file) throws IOException {
        if (Files.exists(file)) {
            return;
        }

        Files.writeString(file,
                "# Example watch rules:\n" +
                "# file /server.properties\n" +
                "# dir /plugins/\n" +
                "# rdir /config/\n" +
                "file /server.properties\n" +
                "dir /plugins/\n" +
                "rdir /config/\n",
                StandardCharsets.UTF_8);
    }
}
