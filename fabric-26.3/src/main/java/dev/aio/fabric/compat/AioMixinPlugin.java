package dev.aio.fabric.compat;

import org.objectweb.asm.tree.ClassNode;
import org.spongepowered.asm.mixin.extensibility.IMixinConfigPlugin;
import org.spongepowered.asm.mixin.extensibility.IMixinInfo;

import java.util.List;
import java.util.Set;

/**
 * Beobachtet den Einbau der AIO-Mixins. Die Mixin-Konfiguration ist nicht "required": Scheitert
 * der Einbau eines Mixins (z. B. weil eine andere Mod die Klasse verändert hat), startet das Spiel
 * trotzdem, und AIO schaltet nur das betroffene Modul ab.
 */
public final class AioMixinPlugin implements IMixinConfigPlugin {
    @Override public void onLoad(String mixinPackage) {}

    @Override public String getRefMapperConfig() { return null; }

    @Override public boolean shouldApplyMixin(String targetClassName, String mixinClassName) { return true; }

    @Override public void acceptTargets(Set<String> myTargets, Set<String> otherTargets) {}

    @Override public List<String> getMixins() { return null; }

    @Override public void preApply(String targetClassName, ClassNode targetClass, String mixinClassName, IMixinInfo mixinInfo) {
        HookStatus.started.add(simple(mixinClassName));
    }

    @Override public void postApply(String targetClassName, ClassNode targetClass, String mixinClassName, IMixinInfo mixinInfo) {
        // Erreicht nur, wenn der Einbau ohne Fehler durchlief. Ob jede einzelne Injektion eine Stelle
        // gefunden hat, lässt sich hier nicht prüfen: MixinExtras-Injektionen (WrapOperation,
        // ModifyReturnValue, …) werden erst nach diesem Aufruf eingebaut.
        HookStatus.finished.add(simple(mixinClassName));
    }

    private static String simple(String name) {
        return name.substring(name.lastIndexOf('.') + 1);
    }
}
