package dev.aio.forge;

import com.google.gson.JsonElement;
import com.google.gson.JsonParser;
import net.minecraft.client.Minecraft;
import net.minecraft.resources.Identifier;
import net.minecraft.server.packs.resources.Resource;

import java.io.Reader;
import java.net.URI;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.nio.file.FileSystem;
import java.nio.file.FileSystemAlreadyExistsException;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;
import java.util.stream.Stream;

/**
 * JSON files from data packs for the item browser. In singleplayer from the own server (i.e. with
 * data packs and mods), otherwise from the vanilla data pack in the game jar (servers do not send this data).
 */
final class DataFiles {
    private DataFiles() {}

    static Map<String, JsonElement> read(Minecraft client, String dir) {
        var server = client.getSingleplayerServer();
        if (server != null) {
            try {
                return fromServer(server.getResourceManager().listResources(dir, id -> id.getPath().endsWith(".json")), dir);
            } catch (Exception e) {
                AioForgeClient.LOGGER.warn("Daten '{}' vom Server nicht lesbar, nutze Vanilla: {}", dir, e.toString());
            }
        }
        return fromJar(dir);
    }

    private static Map<String, JsonElement> fromServer(Map<Identifier, Resource> resources, String dir) {
        Map<String, JsonElement> out = new HashMap<>();
        for (Map.Entry<Identifier, Resource> e : resources.entrySet()) {
            String path = e.getKey().getPath();
            String id = e.getKey().getNamespace() + ":" + path.substring(dir.length() + 1, path.length() - 5);
            try (Reader reader = e.getValue().openAsReader()) {
                out.put(id, JsonParser.parseReader(reader));
            } catch (Exception ex) {
                // skip broken file
            }
        }
        return out;
    }

    /** Vanilla data from the game jar ({@code data/minecraft/<dir>/**.json}). */
    private static Map<String, JsonElement> fromJar(String dir) {
        Map<String, JsonElement> out = new HashMap<>();
        String base = "data/minecraft/" + dir;
        try {
            // An entry that certainly exists shows where the data pack is
            URL marker = Minecraft.class.getClassLoader().getResource("data/minecraft/recipe/stick.json");
            if (marker == null) return out;
            URI uri = marker.toURI();
            FileSystem fs = null;
            Path root;
            if ("jar".equals(uri.getScheme())) {
                try {
                    fs = FileSystems.newFileSystem(uri, Map.of());
                    root = fs.getPath("/" + base);
                } catch (FileSystemAlreadyExistsException e) {
                    root = FileSystems.getFileSystem(uri).getPath("/" + base);
                }
            } else {
                root = Path.of(uri).getParent().getParent().resolve(dir);
            }
            try {
                if (Files.isDirectory(root)) {
                    try (Stream<Path> files = Files.walk(root)) {
                        for (Path file : (Iterable<Path>) files.filter(p -> p.toString().endsWith(".json"))::iterator) {
                            String rel = root.relativize(file).toString().replace('\\', '/');
                            try (Reader reader = Files.newBufferedReader(file, StandardCharsets.UTF_8)) {
                                out.put("minecraft:" + rel.substring(0, rel.length() - 5), JsonParser.parseReader(reader));
                            } catch (Exception ex) {
                                // skip
                            }
                        }
                    }
                }
            } finally {
                if (fs != null) fs.close();
            }
        } catch (Exception e) {
            AioForgeClient.LOGGER.warn("Vanilla-Daten '{}' nicht lesbar: {}", dir, e.toString());
        }
        return out;
    }
}
