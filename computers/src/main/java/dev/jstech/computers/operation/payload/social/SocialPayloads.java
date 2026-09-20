/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Computers.
 */
package dev.jstech.computers.operation.payload.social;

import dev.jstech.computers.blockentity.MainframeBlockEntity;
import dev.jstech.computers.client.os.KnotApp;
import dev.jstech.computers.client.os.MessengerApp;
import dev.jstech.computers.operation.payload.ClientPayloadHandlers;
import dev.jstech.computers.operation.payload.ComputerAccess;
import dev.jstech.computers.operation.payload.KnotActionPayload;
import dev.jstech.computers.operation.payload.KnotStatePayload;
import dev.jstech.computers.operation.payload.MessengerActionPayload;
import dev.jstech.computers.operation.payload.MessengerStatePayload;
import dev.jstech.computers.operation.payload.files.FileAccess;
import dev.jstech.computers.os.FilesystemKind;
import dev.jstech.computers.os.IOsHost;
import dev.jstech.computers.os.fs.DiskFilesystem;
import dev.jstech.computers.os.fs.FileType;
import dev.jstech.computers.program.KnotRepository;
import dev.jstech.computers.program.MessengerLog;
import dev.jstech.computers.terminal.IComputerTerminalHost;
import dev.jstech.core.network.NetworkSystem;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.network.PacketDistributor;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import static dev.jstech.computers.operation.payload.terminal.TerminalHosts.niHost;

/**
 * The payloads behind the two programs where the other end is another player: the messenger and the
 * source repository.
 *
 * <p>Both services live on the network's Mainframe, the way the IQL Engine and the Mirror do, and both
 * are reached through whichever computer the player happens to be sitting at. That is what makes them
 * the network's rather than one machine's.
 */
public final class SocialPayloads {

    /** The longest line of a comparison that travels, which is what the payload's own cap allows. */
    private static final int MAX_DIFF_LINE = 240;

    private SocialPayloads() {
    }

    /** Registers the payloads this class handles. */
    public static void register(final PayloadRegistrar registrar) {
        ComputerAccess.accept(registrar, MessengerActionPayload.TYPE, MessengerActionPayload.STREAM_CODEC,
                ComputerAccess.machine(MessengerActionPayload::hostPos), SocialPayloads::handleMessenger);
        registrar.playToClient(MessengerStatePayload.TYPE, MessengerStatePayload.STREAM_CODEC,
                ClientPayloadHandlers.onMainThread(SocialPayloads::handleMessengerState));
        ComputerAccess.accept(registrar, KnotActionPayload.TYPE, KnotActionPayload.STREAM_CODEC,
                ComputerAccess.machine(KnotActionPayload::hostPos), SocialPayloads::handleKnot);
        registrar.playToClient(KnotStatePayload.TYPE, KnotStatePayload.STREAM_CODEC,
                ClientPayloadHandlers.onMainThread(SocialPayloads::handleKnotState));
    }

    /* The messenger */

    private static void handleMessenger(final MessengerActionPayload payload, final ServerPlayer player,
                                        final ServerLevel level) {
        final MainframeBlockEntity mainframe = orchestratorFor(player, level,
                payload.hostPos(), payload.monitorPos());
        if (mainframe == null) {
            PacketDistributor.sendToPlayer(player, new MessengerStatePayload(
                    new MessengerStatePayload.Service(false, "", 0L, 0),
                    List.of(), List.of(MessengerLog.LOBBY), MessengerLog.LOBBY, List.of()));
            return;
        }
        final MessengerLog log = mainframe.messengerLog();
        final String who = player.getGameProfile().getName();
        final String room = payload.room().isBlank() ? MessengerLog.LOBBY : payload.room();
        final long now = level.getGameTime();
        // Anybody who has gone quiet is let go of first, so the weight below is of who is really there.
        log.forgetIdle(now);
        if (payload.action() == MessengerActionPayload.LEAVE) {
            log.disconnect(who);
            return; // nothing to draw for a window that is closing
        }
        log.connect(who, now);
        final boolean spoke = switch (payload.action()) {
            case MessengerActionPayload.SAY -> mainframe.messengerSay(room, who, payload.text(), now, false);
            case MessengerActionPayload.NUDGE -> mainframe.messengerSay(room, who, "", now, true);
            default -> false;
        };
        /*
         * Somebody speaking is told to everybody, because a conversation nobody else sees arrive is not a
         * conversation. Somebody merely looking is answered alone: every window asks every couple of
         * seconds, and telling all of them each time would be a packet for every pair of people watching.
         */
        if (spoke) {
            tellEveryone(level, mainframe, room);
        } else {
            PacketDistributor.sendToPlayer(player, stateOf(mainframe, room, log));
        }
    }

    /**
     * Sends the state to every player on the server who has this network's messenger open.
     *
     * <p>Somebody who has left the game is dropped here rather than being left counted for ever. The
     * window says goodbye when it closes, but a client that crashed or a player who logged out never got
     * to, and the service's memory cost would climb with people who are not there any more.
     */
    private static void tellEveryone(final ServerLevel level, final MainframeBlockEntity mainframe,
                                     final String room) {
        final MessengerLog log = mainframe.messengerLog();
        for (final String who : log.connected()) {
            final ServerPlayer other = level.getServer().getPlayerList().getPlayerByName(who);
            if (other == null) {
                log.disconnect(who);
            } else {
                PacketDistributor.sendToPlayer(other, stateOf(mainframe, room, log));
            }
        }
    }

    private static MessengerStatePayload stateOf(final MainframeBlockEntity mainframe,
                                                 final String room, final MessengerLog log) {
        final List<MessengerStatePayload.Line> lines = new ArrayList<>();
        final List<String> connected = log.connected();
        for (final MessengerLog.Message message : log.room(room, MessengerStatePayload.MAX_LINES)) {
            lines.add(new MessengerStatePayload.Line(message.from(), message.text(), message.nudge(),
                    connected.contains(message.from())));
        }
        final List<String> rooms = log.rooms();
        return new MessengerStatePayload(
                new MessengerStatePayload.Service(mainframe.isMessengerActive(), nameOf(mainframe),
                        log.historyBytes(), log.ramMb()),
                clip(connected, MessengerStatePayload.MAX_NAMES),
                clip(rooms, MessengerStatePayload.MAX_NAMES), room, lines);
    }

    /** What a machine calls itself, or nothing when it has not been named. */
    private static String nameOf(final MainframeBlockEntity mainframe) {
        return mainframe.console() == null ? "" : mainframe.console().computerName();
    }

    private static void handleMessengerState(final MessengerStatePayload payload, final Player player) {
        MessengerApp.accept(payload);
    }

    /* The repository */

    private static void handleKnot(final KnotActionPayload payload, final ServerPlayer player,
                                   final ServerLevel level) {
        final MainframeBlockEntity mainframe = orchestratorFor(player, level,
                payload.hostPos(), payload.monitorPos());
        if (mainframe == null || !mainframe.isKnotActive()) {
            PacketDistributor.sendToPlayer(player, new KnotStatePayload(
                    new KnotStatePayload.Service(false, "", 0L), List.of(), List.of(), 0, List.of()));
            return;
        }
        final KnotRepository repository = mainframe.knotRepository();
        final String note = switch (payload.action()) {
            case KnotActionPayload.PUSH -> push(payload, player, level, mainframe);
            case KnotActionPayload.PULL -> pull(payload, level, repository);
            default -> "";
        };
        PacketDistributor.sendToPlayer(player,
                knotStateOf(mainframe, repository, payload.revision(), note));
    }

    /**
     * Reads the file off the machine the player is at and saves it as a revision.
     *
     * <p>Answers what to tell them, because every one of these can fail for a reason they can act on and a
     * button that does nothing at all is the worst of the possible answers.
     */
    private static String push(final KnotActionPayload payload, final ServerPlayer player,
                               final ServerLevel level, final MainframeBlockEntity mainframe) {
        if (!(level.getBlockEntity(payload.hostPos()) instanceof IOsHost computer)) {
            return "No computer";
        }
        final ItemStack disk = computer.systemDisk();
        if (disk.isEmpty()) {
            return "No system disk";
        }
        final String content = DiskFilesystem.read(disk, payload.file()).orElse(null);
        if (content == null) {
            return "No " + payload.file() + " on this machine";
        }
        return mainframe.knotCommit(payload.file(), player.getGameProfile().getName(),
                payload.message(), content, level.getGameTime()) == null
                ? "Nothing changed since the last revision"
                : "Pushed " + payload.file();
    }

    /** Writes a revision back onto the machine's disk, where the editor can open it. */
    private static String pull(final KnotActionPayload payload, final ServerLevel level,
                               final KnotRepository repository) {
        final KnotRepository.Revision revision = repository.revision(payload.revision());
        final String file = repository.fileOf(payload.revision());
        if (revision == null || file == null) {
            return "No such revision";
        }
        if (!(level.getBlockEntity(payload.hostPos()) instanceof IOsHost computer)) {
            return "No computer";
        }
        final ItemStack disk = computer.systemDisk();
        final FilesystemKind kind = FileAccess.filesystemKindOf(computer);
        if (disk.isEmpty() || kind == FilesystemKind.NONE) {
            return "No system disk";
        }
        final DiskFilesystem.WriteResult result = DiskFilesystem.write(disk, file, typeOf(file),
                revision.content(), computer.systemDiskFreeWeight(), kind, level.getGameTime());
        if (result != DiskFilesystem.WriteResult.OK) {
            return result == DiskFilesystem.WriteResult.DISK_FULL
                    ? "Not enough free space" : "Could not write " + file;
        }
        computer.setChanged();
        return "Wrote r" + revision.number() + " to " + file;
    }

    private static KnotStatePayload knotStateOf(final MainframeBlockEntity mainframe,
                                                final KnotRepository repository, final int shown,
                                                final String note) {
        final List<KnotStatePayload.Revision> revisions = new ArrayList<>();
        for (final KnotRepository.Revision revision
                : repository.recent(KnotStatePayload.MAX_REVISIONS)) {
            final String file = repository.fileOf(revision.number());
            revisions.add(new KnotStatePayload.Revision(revision.number(), revision.author(),
                    revision.message(), file == null ? "" : file));
        }
        return new KnotStatePayload(
                new KnotStatePayload.Service(mainframe.isKnotActive(), nameOf(mainframe),
                        repository.bytes(), clipLine(note)),
                clip(repository.files(), KnotStatePayload.MAX_FILES),
                revisions, shown, diffOf(repository, shown));
    }

    /** What changed between a revision and the one before it of the same file. */
    private static List<KnotStatePayload.DiffLine> diffOf(final KnotRepository repository,
                                                          final int number) {
        final String file = repository.fileOf(number);
        if (file == null) {
            return List.of();
        }
        final List<KnotRepository.Revision> revisions = repository.revisionsOf(file);
        String before = "";
        String after = "";
        for (int i = 0; i < revisions.size(); i++) {
            if (revisions.get(i).number() == number) {
                after = revisions.get(i).content();
                before = i > 0 ? revisions.get(i - 1).content() : "";
                break;
            }
        }
        final List<KnotStatePayload.DiffLine> out = new ArrayList<>();
        for (final KnotRepository.DiffLine line : KnotRepository.diff(before, after)) {
            if (out.size() >= KnotStatePayload.MAX_DIFF) {
                break;
            }
            out.add(new KnotStatePayload.DiffLine(switch (line.kind()) {
                case ADDED -> KnotStatePayload.DiffLine.ADDED;
                case REMOVED -> KnotStatePayload.DiffLine.REMOVED;
                case CONTEXT -> KnotStatePayload.DiffLine.CONTEXT;
            }, clipLine(line.text())));
        }
        return out;
    }

    private static void handleKnotState(final KnotStatePayload payload, final Player player) {
        KnotApp.accept(payload);
    }

    /* Shared */

    /**
     * The Mainframe that orchestrates the network the player's machine is on, or null when there is none.
     *
     * <p>Both services live there rather than on the computer in front of the player, which is what makes
     * them the network's: two players at two different machines are talking to the same service.
     */
    @Nullable
    private static MainframeBlockEntity orchestratorFor(final ServerPlayer player, final ServerLevel level,
                                                        final BlockPos hostPos, final BlockPos monitorPos) {
        final IComputerTerminalHost host = niHost(player, level, hostPos, monitorPos);
        if (host == null) {
            return null;
        }
        if (host instanceof MainframeBlockEntity self) {
            return self;
        }
        if (host.networkUuid() == null) {
            return null;
        }
        return NetworkSystem.get(level).mainframePositionOf(host.networkUuid())
                .map(pos -> level.getBlockEntity(BlockPos.of(pos))
                        instanceof MainframeBlockEntity mainframe ? mainframe : null)
                .orElse(null);
    }

    private static FileType typeOf(final String path) {
        final int dot = path.lastIndexOf('.');
        return FileType.of(dot >= 0 && dot < path.length() - 1
                ? path.substring(dot + 1).toLowerCase(Locale.ROOT) : "");
    }

    private static List<String> clip(final List<String> values, final int limit) {
        return values.size() > limit ? List.copyOf(values.subList(0, limit)) : List.copyOf(values);
    }

    /**
     * Cuts a line of a comparison to what the packet can carry.
     *
     * <p>A source file may have a line longer than the payload's own cap, and that cap throws when it is
     * handed more than it takes: a single long line would otherwise stop the whole window working rather
     * than being shown short.
     */
    private static String clipLine(final String text) {
        return text.length() > MAX_DIFF_LINE ? text.substring(0, MAX_DIFF_LINE) : text;
    }
}
