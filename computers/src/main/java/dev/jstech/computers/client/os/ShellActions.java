/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Computers.
 */
package dev.jstech.computers.client.os;

import dev.jstech.computers.operation.payload.DesktopShellOutputPayload;
import dev.jstech.computers.operation.payload.DesktopShellRunPayload;
import dev.jstech.computers.operation.payload.TerminalKeyboard;
import dev.jstech.computers.operation.payload.WireLine;
import dev.jstech.computers.program.cli.CliLine;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;
import java.util.function.Consumer;
import net.minecraft.core.BlockPos;
import net.neoforged.neoforge.network.PacketDistributor;
import org.jetbrains.annotations.Nullable;

/**
 * The commands a menu shell runs for its actions, on a shell session of its own that nobody sees: COPY, DEL, TYPE and
 * the rest, typed out the way a person would, one at a time, each answered with what the command printed. A command
 * that stops to ask something is handed back as a question, for the shell to put to the player.
 */
final class ShellActions implements ShellViews.IListener {

    private final BlockPos host;
    private final int session = ShellViews.newSession();
    private final Deque<Pending> queue = new ArrayDeque<>();
    private final List<CliLine> said = new ArrayList<>();
    @Nullable
    private Pending running;
    /** Who asks the player when a command stops to ask: given the question, it answers with what was typed. */
    private final Consumer<Question> asker;

    /** How wide the session's answers are laid out. */
    private static final int COLUMNS = 76;

    ShellActions(final BlockPos host, final Consumer<Question> asker) {
        this.host = host;
        this.asker = asker;
        ShellViews.register(this);
    }

    @Override
    public int session() {
        return this.session;
    }

    /** Runs {@code line}, after whatever is already running; {@code done} is handed what it printed. */
    void run(final String line, final Consumer<List<CliLine>> done) {
        this.queue.add(new Pending(line, done));
        if (this.running == null) {
            next();
        }
    }

    /** Whether a command is running. */
    boolean busy() {
        return this.running != null;
    }

    /** Stops listening, for a shell that is gone. */
    void release() {
        ShellViews.forget(this);
        this.queue.clear();
        this.running = null;
    }

    @Override
    public void accept(final DesktopShellOutputPayload payload) {
        if (this.running == null || payload.session() != this.session) {
            return;
        }
        for (final WireLine line : payload.lines()) {
            this.said.add(line.toLine());
        }
        if (payload.informational()) {
            return;
        }
        final TerminalKeyboard keyboard = payload.keyboard();
        if (keyboard.asking()) {
            this.asker.accept(new Question(keyboard.standing().text().stripTrailing(),
                    answer -> PacketDistributor.sendToServer(new DesktopShellRunPayload(this.host, answer,
                            this.session, COLUMNS))));
            return;
        }
        if (payload.busy() || keyboard.busy()) {
            return;
        }
        final Pending done = this.running;
        final List<CliLine> lines = List.copyOf(this.said);
        this.running = null;
        this.said.clear();
        done.then().accept(lines);
        next();
    }

    private void next() {
        final Pending pending = this.queue.poll();
        if (pending == null) {
            return;
        }
        this.running = pending;
        PacketDistributor.sendToServer(new DesktopShellRunPayload(this.host, pending.line(), this.session, COLUMNS));
    }

    /** A command waiting its turn, and who is told what it printed. */
    private record Pending(String line, Consumer<List<CliLine>> then) {
    }

    /** What a command asked, and where the answer goes. */
    record Question(String asked, Consumer<String> answer) {
    }
}
