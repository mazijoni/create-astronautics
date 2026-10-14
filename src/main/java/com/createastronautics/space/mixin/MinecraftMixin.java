package com.createastronautics.space.mixin;

import com.createastronautics.client.SeamlessTransition;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

// Same approach as Create-Cosmonautics' MinecraftMixin (dev.devce.rocketnautics.mixin) -
// https://github.com/CosmonauticsTeam/Create-Cosmonautics - GPLv3-licensed; see LICENSE.
@Mixin(Minecraft.class)
public class MinecraftMixin {
    @Inject(method = "setScreen", at = @At("HEAD"), cancellable = true)
    private void createastronautics$skipLoadingScreen(Screen screen, CallbackInfo ci) {
        if (SeamlessTransition.shouldSuppress(screen)) {
            ci.cancel();
        }
    }
}
