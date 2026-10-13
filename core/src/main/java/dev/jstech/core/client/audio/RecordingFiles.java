/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.client.audio;

import dev.jstech.core.audio.SoundKey;
import dev.jstech.core.audio.pcm.AudioDecoders;
import dev.jstech.core.audio.pcm.IPcmSource;
import java.io.IOException;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;
import net.minecraft.client.Minecraft;
import net.minecraft.resources.ResourceLocation;

/**
 * A declared sound's own file, opened as samples, for a playing the game's sound engine cannot give as it is: one
 * side of a stereo recording, or a recording through a speaker that cannot play it whole.
 */
final class RecordingFiles {

    private RecordingFiles() {
    }

    /** One of the sound's files, picked at random as the game picks one, decoded as it is read. */
    static IPcmSource open(final SoundKey sound) throws IOException {
        final List<ResourceLocation> files = sound.spec().files();
        final ResourceLocation file = files.get(ThreadLocalRandom.current().nextInt(files.size()));
        final ResourceLocation path = ResourceLocation.fromNamespaceAndPath(file.getNamespace(),
                "sounds/" + file.getPath() + ".ogg");
        return AudioDecoders.open(path.getPath(), Minecraft.getInstance().getResourceManager().open(path));
    }
}
