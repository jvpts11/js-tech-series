/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Computers.
 */
package dev.jstech.computers.operation.payload.desktop;

import dev.jstech.computers.client.os.HelpViewerApp;
import dev.jstech.computers.operation.payload.ClientPayloadHandlers;
import dev.jstech.computers.operation.payload.ComputerAccess;
import dev.jstech.computers.operation.payload.HelpPayload;
import dev.jstech.computers.operation.payload.RequestHelpPayload;
import dev.jstech.computers.program.ServerCliComputer;
import dev.jstech.computers.program.cli.CliCommands;
import dev.jstech.computers.program.cli.ICliCommand;
import dev.jstech.computers.program.cli.man.ManPage;
import dev.jstech.computers.terminal.IComputerTerminalHost;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
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

    private static void handle(final RequestHelpPayload payload, final ServerPlayer player,
                               final ServerLevel level) {
        if (!(level.getBlockEntity(payload.hostPos()) instanceof IComputerTerminalHost host)) {
            return;
        }
        final ServerCliComputer computer = new ServerCliComputer(host, level, player);
        final List<HelpPayload.Entry> entries = new ArrayList<>();
        ICliCommand opening = null;
        for (final ICliCommand command : CliCommands.commandsFor(computer.shellFamily())) {
            if (!command.available(computer)) {
                continue;
            }
            entries.add(new HelpPayload.Entry(groupOf(command), command.name(), command.summary()));
            if (opening == null || command.name().equalsIgnoreCase(payload.name())) {
                opening = command;
            }
        }
        final List<String> lines = opening == null ? List.of() : ManPage.lines(opening, true);
        PacketDistributor.sendToPlayer(player, new HelpPayload(payload.hostPos(), entries,
                opening == null ? "" : opening.name(), lines));
    }

    /**
     * What a command is filed under, which is what a person looking for one would think to open.
     *
     * <p>By subject and not by the mod's own packages: somebody looking for a way to copy a file is not
     * thinking about which class it lives in.
     */
    private static String groupOf(final ICliCommand command) {
        final String name = command.name().toLowerCase(Locale.ROOT);
        if (List.of("interac", "iql", "net", "ssh", "gateway", "cluster").contains(name)) {
            return "The Network";
        }
        if (List.of("ls", "dir", "cat", "type", "cp", "copy", "mv", "move", "rm", "del", "mkdir", "rmdir",
                "cd", "pwd", "touch", "write", "grep", "find", "sort", "head", "tail", "wc", "less", "more",
                "du", "df", "tree", "ren", "mkfs", "format").contains(name)) {
            return "Files";
        }
        if (List.of("apt", "dnf", "pacman", "emerge", "pkg", "pckmgr", "installpkg", "programs", "install",
                "uninstall", "store", "mirror", "services").contains(name)) {
            return "Software";
        }
        if (List.of("vim", "emacs", "nano", "sgsc", "scc", "sigma", "sgpack").contains(name)) {
            return "Writing programs";
        }
        if (List.of("man", "whatis", "apropos", "help", "listcmd").contains(name)) {
            return "Finding your way";
        }
        return "The Machine";
    }
}
