package dev.aio.fabric.compat;

import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.AnnotationNode;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.MethodInsnNode;
import org.objectweb.asm.tree.MethodNode;
import org.spongepowered.asm.mixin.extensibility.IMixinConfigPlugin;
import org.spongepowered.asm.mixin.extensibility.IMixinInfo;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

/**
 * Beobachtet den Einbau der AIO-Mixins. Die Mixin-Konfiguration ist nicht "required" und
 * verlangt keine Mindestanzahl Treffer: Passt eine Stelle nicht (weil eine andere Mod sie
 * verändert hat), startet das Spiel trotzdem. Hier wird festgehalten, welche Eingriffe wirklich
 * greifen, damit AIO nur das betroffene Modul abschaltet.
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
        String mixin = simple(mixinClassName);
        HookStatus.finished.add(mixin);
        try {
            for (String handler : handlers(mixinInfo)) {
                HookStatus.handlers.put(mixin + "#" + handler, isCalled(targetClass, handler));
            }
        } catch (RuntimeException e) {
            // Prüfung ist nur Zusatz: im Zweifel als funktionierend werten
        }
    }

    private static String simple(String name) {
        return name.substring(name.lastIndexOf('.') + 1);
    }

    /** Namen aller Injektor-Methoden (aio$...) des Mixins. */
    private static List<String> handlers(IMixinInfo info) {
        List<String> out = new ArrayList<>();
        ClassNode node = info.getClassNode(0);
        if (node == null || node.methods == null) return out;
        for (MethodNode m : node.methods) {
            if (m.name.startsWith("aio$") && isInjector(m.visibleAnnotations) || m.name.startsWith("aio$") && isInjector(m.invisibleAnnotations)) {
                out.add(m.name);
            }
        }
        return out;
    }

    private static boolean isInjector(List<AnnotationNode> annotations) {
        if (annotations == null) return false;
        for (AnnotationNode a : annotations) {
            if (a.desc.startsWith("Lorg/spongepowered/asm/mixin/injection/") || a.desc.startsWith("Lcom/llamalad7/mixinextras/injector/")) {
                return true;
            }
        }
        return false;
    }

    /**
     * Greift der Eingriff? Mixin kopiert die Methode (umbenannt, der Name bleibt enthalten) in die
     * Zielklasse und ruft sie an der Einbaustelle auf. Wird sie von keiner anderen Methode
     * aufgerufen, wurde keine passende Stelle gefunden.
     */
    private static boolean isCalled(ClassNode target, String handler) {
        for (MethodNode m : target.methods) {
            if (m.name.contains(handler) || m.instructions == null) continue;
            for (AbstractInsnNode insn = m.instructions.getFirst(); insn != null; insn = insn.getNext()) {
                if (insn instanceof MethodInsnNode call && call.name.contains(handler)) return true;
                if (insn instanceof org.objectweb.asm.tree.InvokeDynamicInsnNode indy && indy.bsmArgs != null) {
                    for (Object arg : indy.bsmArgs) {
                        if (arg instanceof org.objectweb.asm.Handle h && h.getName().contains(handler)) return true;
                    }
                }
            }
        }
        return false;
    }
}
