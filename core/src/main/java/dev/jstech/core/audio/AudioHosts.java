/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.audio;

import dev.jstech.core.JsCore;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.capabilities.BlockCapability;

/**
 * How one mod finds a block of another that plays sound through its own hardware: a speaker asks the computer beside
 * it, a machine asks the controller wired to it, with no mod knowing another's classes.
 */
public final class AudioHosts {

    /** A block's audio host, whichever side it is asked from. */
    public static final BlockCapability<IAudioHost, Void> CAPABILITY =
            BlockCapability.createVoid(ResourceLocation.fromNamespaceAndPath(JsCore.MODID, "audio_host"),
                    IAudioHost.class);

    private AudioHosts() {
    }
}
