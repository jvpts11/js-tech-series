/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Computers.
 */
package dev.jstech.computers.client.os;

import dev.jstech.computers.gui.help.HelpCommand;
import dev.jstech.computers.operation.payload.HelpPayload;
import dev.jstech.computers.operation.payload.RequestHelpPayload;
import dev.jstech.computers.operation.payload.WireLine;
import dev.jstech.core.text.GameText;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import net.minecraft.core.BlockPos;
import net.neoforged.neoforge.network.PacketDistributor;

/**
 * What a machine answers when a help program asks what it can do: its commands, filed under what each is for, and the
 * manual page of one of them.
 *
 * <p>Every help program asks the same way, a window on a desktop and a viewer that has taken a terminal alike, and
 * each hears the answers for the machine it is showing.
 */
public final class HelpAnswers {

    /** Whoever is waiting for answers, in the order they started waiting. */
    private static final List<IListener> LISTENING = new CopyOnWriteArrayList<>();

    private HelpAnswers() {
    }

    /** Someone told of a machine's answers. */
    @FunctionalInterface
    public interface IListener {

        /**
         * A machine answered.
         *
         * @param host     the machine
         * @param commands what it can do, in the order its help lists them
         * @param page     the command whose page came with them, or empty when none was asked for
         * @param lines    that page, in the player's language
         */
        void answered(BlockPos host, List<HelpCommand> commands, String page, List<String> lines);
    }

    /** Starts telling {@code listener} of every answer. */
    public static void listen(final IListener listener) {
        if (!LISTENING.contains(listener)) {
            LISTENING.add(listener);
        }
    }

    /** Stops telling it. */
    public static void stop(final IListener listener) {
        LISTENING.remove(listener);
    }

    /** Asks a machine for its commands and, when a name is given, that command's page. */
    public static void ask(final BlockPos host, final String name) {
        PacketDistributor.sendToServer(new RequestHelpPayload(host, name == null ? "" : name));
    }

    /** Hands a machine's answer, put in the player's language, to everyone listening. */
    public static void accept(final HelpPayload payload) {
        final List<HelpCommand> commands = new ArrayList<>(payload.entries().size());
        for (final HelpPayload.Entry entry : payload.entries()) {
            commands.add(new HelpCommand(GameText.resolve(entry.group()), entry.name(),
                    GameText.resolve(entry.summary())));
        }
        final List<String> lines = new ArrayList<>(payload.lines().size());
        for (final WireLine line : payload.lines()) {
            lines.add(line.toLine().text(GameText.LOADED));
        }
        for (final IListener listener : LISTENING) {
            listener.answered(payload.hostPos(), List.copyOf(commands), payload.page(), List.copyOf(lines));
        }
    }
}
