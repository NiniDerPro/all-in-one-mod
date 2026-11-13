package dev.aio.forge.shim;

import net.minecraftforge.fml.ModList;
import net.minecraftforge.fml.loading.FMLPaths;

import java.nio.file.Path;
import java.util.Optional;

/** What the adapter needs to know from the mod loader. */
public final class Loader {
    private Loader() {}

    public static Path configDir() { return FMLPaths.CONFIGDIR.get(); }

    public static boolean isModLoaded(String id) { return ModList.isLoaded(id); }

    /** Display name of a mod, by mod id. */
    public static Optional<String> modName(String id) {
        return ModList.getModContainerById(id).map(c -> c.getModInfo().getDisplayName());
    }

    public static Optional<String> modVersion(String id) {
        return ModList.getModContainerById(id).map(c -> c.getModInfo().getVersion().toString());
    }
}
