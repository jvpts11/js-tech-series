/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.client.model.kit;

import dev.jstech.core.model.network.ModelToolPayload;
import net.minecraft.client.Minecraft;

/** What the model commands do on the player's game: open a model's preview, or read the models again. */
public final class KitTools {

    private KitTools() {
    }

    /** Carries out a model command; on the render thread. */
    public static void receive(final ModelToolPayload payload) {
        if (payload.preview()) {
            Minecraft.getInstance().setScreen(new ModelPreviewScreen(payload.model()));
        } else {
            KitModels.reloadNow();
        }
    }
}
