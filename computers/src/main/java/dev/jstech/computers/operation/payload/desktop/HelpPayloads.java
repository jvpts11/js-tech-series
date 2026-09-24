/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Computers.
 */
package dev.jstech.computers.operation.payload.desktop;

import dev.jstech.computers.advancement.JscEvents;
import dev.jstech.computers.client.os.HelpViewerApp;
import dev.jstech.computers.operation.payload.ClientPayloadHandlers;
import dev.jstech.computers.operation.payload.ComputerAccess;
import dev.jstech.computers.operation.payload.HelpPayload;
import dev.jstech.computers.operation.payload.RequestHelpPayload;
import dev.jstech.computers.operation.payload.WireLine;
import dev.jstech.computers.program.ServerCliComputer;
import dev.jstech.computers.program.cli.CliCommands;
import dev.jstech.computers.program.cli.CliLine;
import dev.jstech.computers.program.cli.CommandGroup;
import dev.jstech.computers.program.cli.ICliCommand;
import dev.jstech.computers.program.cli.man.ManPage;
import dev.jstech.computers.terminal.IComputerTerminalHost;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.PacketDistributor;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;

/**
 * The payloads of the Help window: it asks what this computer can do, and the machine answers with the list
 * and one page of it.
 *
 * <p>Both come from the same place the prompt gets them, so the window and the terminal are two readings of
 * one manual: a command the machine cannot run has no row and no page, and a command that gains an option
 * gains it in the window without anybody writing it twice.
 */
public final class HelpPayloads {

    private HelpPayloads() {
    }

    public static void register(final PayloadRegistrar registrar) {
        ComputerAccess.accept(registrar, RequestHelpPayload.TYPE, RequestHelpPayload.STREAM_CODEC,
                ComputerAccess.machine(RequestHelpPayload::hostPos), HelpPayloads::handle);
        registrar.playToClient(HelpPayload.TYPE, HelpPayload.STREAM_CODEC,
                ClientPayloadHandlers.onMainThread((payload, player) -> HelpViewerApp.accept(payload)));
    }

    /**
     * The list, filed under what each command says it is for, the groups in the order a person meets them, and the
     * page asked for, or the first there is.
     */
    private static void handle(final RequestHelpPayload payload, final ServerPlayer player,
                               final ServerLevel level) {
        if (!(level.getBlockEntity(payload.hostPos()) instanceof IComputerTerminalHost host)) {
            return;
        }
        final ServerCliComputer computer = new ServerCliComputer(host, level, player);
        final Map<CommandGroup, List<HelpPayload.Entry>> byGroup = new EnumMap<>(CommandGroup.class);
        ICliCommand opening = null;
        for (final ICliCommand command : CliCommands.commandsFor(computer.shellFamily())) {
            if (!command.available(computer)) {
                continue;
            }
            byGroup.computeIfAbsent(command.group(), group -> new ArrayList<>())
                    .add(new HelpPayload.Entry(command.group().title().text(), command.name(), command.summary()));
            if (opening == null || command.name().equalsIgnoreCase(payload.name())) {
                opening = command;
            }
        }
        final List<HelpPayload.Entry> entries = new ArrayList<>();
        byGroup.values().forEach(entries::addAll);
        final List<WireLine> lines = new ArrayList<>();
        if (opening != null) {
            for (final CliLine line : ManPage.lines(opening, true)) {
                lines.add(WireLine.of(line));
            }
            JscEvents.award(player, JscEvents.MAN_PAGE);
        }
        PacketDistributor.sendToPlayer(player, new HelpPayload(payload.hostPos(), entries,
                opening == null ? "" : opening.name(), lines));
    }
}
