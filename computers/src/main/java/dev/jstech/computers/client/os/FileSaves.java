/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Computers.
 */
package dev.jstech.computers.client.os;

import dev.jstech.computers.operation.payload.SaveFilePayload;
import dev.jstech.core.text.Text;
import net.minecraft.core.BlockPos;
import net.neoforged.neoforge.network.PacketDistributor;

/**
 * Files a program on the glass sends to be saved, each checked against what a file holds before it goes.
 *
 * <p>A message longer than its cap is not cut on the way: writing it throws, and the player is dropped from the
 * game. So every save that sends text the player or a compiler made goes through here, and a file that is too
 * long stays on the glass with a word to the player instead.
 */
public final class FileSaves {

    private FileSaves() {
    }

    /** Sends the file to be saved; false, with nothing sent, when it is longer than a file holds. */
    public static boolean send(final BlockPos host, final String path, final String content) {
        if (content.length() > SaveFilePayload.MAX_CONTENT) {
            return false;
        }
        PacketDistributor.sendToServer(new SaveFilePayload(host, path, content));
        return true;
    }

    /** What the player is told about a file too long to save. */
    public static Text tooLong(final String content) {
        return EditorTexts.TOO_LONG.with(content.length(), SaveFilePayload.MAX_CONTENT);
    }
}
