package dev.aio.forge.compat;

import org.objectweb.asm.tree.ClassNode;
import org.spongepowered.asm.mixin.extensibility.IMixinConfigPlugin;
import org.spongepowered.asm.mixin.extensibility.IMixinInfo;

import java.util.List;
import java.util.Set;

/**
 * Observes the application of the AIO mixins. The mixin config is not "required": if
 * a mixin cannot be applied (e.g. because another mod changed the class), the game starts
 * anyway, and AIO only switches off the affected module.
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
        // Only reached if the application ran without errors. Whether every single injection found a place
        // cannot be checked here: MixinExtras injections (WrapOperation,
        // ModifyReturnValue, …) are only applied after this call.
        HookStatus.finished.add(simple(mixinClassName));
    }

    private static String simple(String name) {
        return name.substring(name.lastIndexOf('.') + 1);
    }
}
