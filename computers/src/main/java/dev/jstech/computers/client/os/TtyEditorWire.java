/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Computers.
 */
package dev.jstech.computers.client.os;

import dev.jstech.computers.operation.payload.RequestFileContentPayload;
import dev.jstech.computers.operation.payload.SaveFilePayload;
import java.util.function.Supplier;
import net.minecraft.core.BlockPos;
import net.neoforged.neoforge.network.PacketDistributor;

/**
 * What a terminal editor asks of the machine it is running on, carried there and back.
 *
 * <p>A machine has two terminals, the prompt that is the whole glass and the window on a desktop, and an
 * editor running in either asks for the same two things in the same way: a file written, and a file read.
 * Only what happens when the editor is done differs, since each terminal has its own prompt to bring back.
 */
public final class TtyEditorWire implements TtyEditor.IHost {

    private final Supplier<BlockPos> machine;
    private final Runnable done;

    /**
     * @param machine where the machine is, asked for each time because a screen learns it after it is built
     * @param done    what giving the terminal back means to the terminal this editor took
     */
    public TtyEditorWire(final Supplier<BlockPos> machine, final Runnable done) {
        this.machine = machine;
        this.done = done;
    }

    @Override
    public void save(final String path, final String text) {
        PacketDistributor.sendToServer(new SaveFilePayload(this.machine.get(), path, text));
        FilesApps.diskChanged();
    }

    @Override
    public void read(final String path, final TtyEditor.IFound then) {
        CodeFileReplies.expectContent(new CodeFileReplies.IReader() {
            @Override
            public void onContent(final String found, final String content, final boolean exists) {
                then.found(content, exists);
            }
        }, path);
        PacketDistributor.sendToServer(new RequestFileContentPayload(this.machine.get(), path));
    }

    @Override
    public void quit() {
        this.done.run();
    }
}
