package dev.aio.fabric;

import com.google.gson.JsonElement;
import com.google.gson.JsonParser;
import com.mojang.serialization.JsonOps;
import net.minecraft.client.Minecraft;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.RegistryOps;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.crafting.RecipeHolder;

import java.io.Reader;
import java.net.URI;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.nio.file.FileSystem;
import java.nio.file.FileSystemAlreadyExistsException;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

/**
 * Standard-Rezepte von Minecraft für Server, die keine Rezepte schicken. Sie stammen aus dem
 * Vanilla-Datenpaket, das im Spiel-Jar liegt ({@code data/minecraft/recipe/*.json}), und werden
 * mit den Registries der aktuellen Welt gelesen, damit Tags (z. B. alle Holzbretter) passen.
 */
final class VanillaRecipes {
    private static final String DIR = "data/minecraft/recipe";
    private static volatile List<RecipeHolder<?>> cache;

    private VanillaRecipes() {}

    static void clear() { cache = null; }

    static List<RecipeHolder<?>> get(Minecraft client) {
        List<RecipeHolder<?>> c = cache;
        if (c == null && client.level != null) {
            c = load(client);
            cache = c;
        }
        return c == null ? List.of() : c;
    }

    private static List<RecipeHolder<?>> load(Minecraft client) {
        List<RecipeHolder<?>> out = new ArrayList<>();
        try {
            URL marker = Minecraft.class.getClassLoader().getResource(DIR + "/stick.json");
            if (marker == null) {
                AioFabricClient.LOGGER.warn("Vanilla-Rezepte nicht gefunden");
                return out;
            }
            RegistryOps<JsonElement> ops = RegistryOps.create(JsonOps.INSTANCE, client.level.registryAccess());
            URI uri = marker.toURI();
            Path dir;
            FileSystem fs = null;
            if ("jar".equals(uri.getScheme())) {
                try {
                    fs = FileSystems.newFileSystem(uri, Map.of());
                } catch (FileSystemAlreadyExistsException e) {
                    fs = FileSystems.getFileSystem(uri);
                }
                dir = fs.getPath("/" + DIR);
            } else {
                dir = Path.of(uri).getParent();
            }
            try (Stream<Path> files = Files.walk(dir)) {
                for (Path file : (Iterable<Path>) files.filter(p -> p.toString().endsWith(".json"))::iterator) {
                    String rel = dir.relativize(file).toString().replace('\\', '/');
                    String name = rel.substring(0, rel.length() - 5);
                    try (Reader reader = Files.newBufferedReader(file, StandardCharsets.UTF_8)) {
                        JsonElement json = JsonParser.parseReader(reader);
                        Recipe.CODEC.parse(ops, json).result().ifPresent(recipe -> out.add(new RecipeHolder<>(
                                ResourceKey.create(Registries.RECIPE, Identifier.withDefaultNamespace(name)), recipe)));
                    } catch (Exception e) {
                        // einzelne Sonderrezepte überspringen
                    }
                }
            }
            AioFabricClient.LOGGER.info("{} Vanilla-Rezepte geladen", out.size());
        } catch (Exception e) {
            AioFabricClient.LOGGER.error("Vanilla-Rezepte konnten nicht geladen werden", e);
        }
        return out;
    }
}
