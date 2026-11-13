package dev.aio.fabric.mixin;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import dev.aio.fabric.ScreenInput;
import net.minecraft.client.MouseHandler;
import net.minecraft.client.gui.screens.Screen;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

/** Mouse dragging on screens (chat edges, shift-dragging items): lets the adapter see and swallow it. */
@Mixin(MouseHandler.class)
public abstract class ScreenDragMixin {
    @WrapOperation(method = "*", require = 0,
            at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/screens/Screen;mouseDragged(DDIDD)Z"))
    private boolean aio$screenDrag(Screen screen, double x, double y, int button, double dx, double dy, Operation<Boolean> original) {
        if (!ScreenInput.allowDrag(screen, x, y, button, dx, dy)) return true;
        return original.call(screen, x, y, button, dx, dy);
    }
}
