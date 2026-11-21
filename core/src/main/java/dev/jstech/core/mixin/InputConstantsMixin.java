/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.mixin;

import com.mojang.blaze3d.platform.InputConstants;
import dev.jstech.core.client.mcp.AgentKeys;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Tells the game a key an agent holds is down, when it asks the window whether it is: a modifier a key mapping waits
 * for, Control held over a screen, a key a mod reads itself ({@link AgentKeys}).
 */
@Mixin(InputConstants.class)
public abstract class InputConstantsMixin {

    @Inject(method = "isKeyDown", at = @At("HEAD"), cancellable = true)
    private static void jscore$agentHolds(final long window, final int key,
                                          final CallbackInfoReturnable<Boolean> info) {
        if (AgentKeys.holds(key)) {
            info.setReturnValue(true);
        }
    }
}
