/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core Test Kit.
 */
package dev.jstech.testkit.mixin;

import dev.jstech.testkit.client.ClientTestRunner;
import net.minecraft.client.MouseHandler;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Leaves the cursor free while the client tests run. The game takes the mouse whenever its window has the focus and no
 * screen is open, hiding the cursor and keeping it inside the window; the tests turn the camera, walk and click by
 * themselves, so whoever runs them keeps using their computer while the windows play.
 */
@Mixin(MouseHandler.class)
public abstract class MouseHandlerMixin {

    @Inject(method = "grabMouse", at = @At("HEAD"), cancellable = true)
    private void jstestkit$leaveTheCursorFree(final CallbackInfo info) {
        if (ClientTestRunner.enabled()) {
            info.cancel();
        }
    }
}
