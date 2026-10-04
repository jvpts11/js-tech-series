/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.client.motion;

import dev.jstech.core.JsCore;
import dev.jstech.core.motion.DeclaredMotion;
import dev.jstech.core.motion.MotionProfile;
import dev.jstech.core.motion.MotionProfiles;
import java.io.IOException;
import java.io.InputStream;
import java.util.Optional;
import net.minecraft.server.packs.resources.Resource;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.server.packs.resources.ResourceManagerReloadListener;

/**
 * Reads every declared motion profile back from the resource packs, each time the packs are loaded.
 *
 * <p>The file the top-most pack holds for a profile wins, as it does for a texture, and replaces the declared profile
 * whole: a kind it leaves out does not move. A profile no pack speaks for keeps the declared one, and one whose file
 * cannot be read is said so in the log and keeps it too.
 */
public final class MotionReloadListener implements ResourceManagerReloadListener {

    @Override
    public void onResourceManagerReload(final ResourceManager manager) {
        for (final DeclaredMotion motion : MotionProfiles.all()) {
            final Optional<Resource> file = manager.getResource(motion.file());
            if (file.isEmpty()) {
                motion.reset();
                continue;
            }
            try (InputStream in = file.get().open()) {
                motion.load(MotionProfile.read(in.readAllBytes()));
            } catch (final IOException | RuntimeException unreadable) {
                JsCore.LOGGER.warn("The motion profile {} could not be read, so it keeps its own: {}", motion.file(),
                        unreadable.getMessage());
                motion.reset();
            }
        }
    }
}
