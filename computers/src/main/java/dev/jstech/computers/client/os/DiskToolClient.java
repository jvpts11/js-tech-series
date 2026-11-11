/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Computers.
 */
package dev.jstech.computers.client.os;

import dev.jstech.computers.operation.payload.RequestFileContentPayload;
import dev.jstech.computers.os.disk.DiskCommand;
import dev.jstech.computers.os.disk.DiskReport;
import net.minecraft.Util;
import net.minecraft.core.BlockPos;
import net.neoforged.neoforge.network.PacketDistributor;
import org.jetbrains.annotations.Nullable;

/**
 * What a disk tool's window knows of the machine's disks (computers/docs/design/storage-devices.md): the report it last
 * read, asked for as a screen asks for a file and asked again every few seconds, and why the last change it asked for
 * was refused. Disk Management, the KDE Partition Manager and GNOME Disks each keep one.
 */
final class DiskToolClient implements CodeFileReplies.IReader {

    private final BlockPos host;
    private DiskReport report = DiskReport.NONE;
    @Nullable
    private DiskReport.Refusal refusal;
    private long askedAt;
    private boolean answered;

    /* How often the window asks again, in milliseconds. */
    private static final long ASK_EVERY_MS = 2_000L;

    DiskToolClient(final BlockPos host) {
        this.host = host;
        this.ask(DiskCommand.look());
    }

    @Override
    public void onContent(final String path, final String content, final boolean exists) {
        if (!DiskCommand.names(path)) {
            return;
        }
        this.report = DiskReport.read(content);
        this.answered = true;
        // A change refused says so until the next change; the reading it is asked for again keeps no refusal.
        if (!path.equals(DiskCommand.look()) || this.report.refusal() != null) {
            this.refusal = this.report.refusal();
        }
    }

    /** The disks as the machine last said. */
    DiskReport report() {
        return this.report;
    }

    /** Whether the machine has answered at all yet. */
    boolean answered() {
        return this.answered;
    }

    /** Why the last change asked for was refused, or null. */
    @Nullable
    DiskReport.Refusal refusal() {
        return this.refusal;
    }

    /** Forgets the last refusal, as a dialog closed over it does. */
    void clearRefusal() {
        this.refusal = null;
    }

    /** Asks again when it has been a while, as the window is drawn. */
    void keepUp() {
        if (Util.getMillis() - this.askedAt >= ASK_EVERY_MS) {
            this.ask(DiskCommand.look());
        }
    }

    /** Asks the machine for a change, its disks coming back after it. */
    void change(final String path) {
        this.refusal = null;
        this.ask(path);
    }

    /** Stops listening, as the window closes. */
    void close() {
        CodeFileReplies.forget(this);
    }

    private void ask(final String path) {
        this.askedAt = Util.getMillis();
        CodeFileReplies.expectContent(this, path);
        PacketDistributor.sendToServer(new RequestFileContentPayload(this.host, path));
    }
}
