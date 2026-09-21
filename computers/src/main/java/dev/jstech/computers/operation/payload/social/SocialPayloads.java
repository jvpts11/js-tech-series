/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Computers.
 */
package dev.jstech.computers.operation.payload.social;

import dev.jstech.computers.blockentity.ServerServices;
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
 * <p>Both services run on a server mounted in a rack, and both are reached through whichever computer the
 * player happens to be sitting at. The service belongs to that machine: somebody mounted it, installed the
 * software and switched it on, and pulling the server takes the conversations and the source with it. What
 * the network gives is only the way to reach it from anywhere on the network.
 */
public final class SocialPayloads {

    /** The longest line of a comparison that travels, which is what the payload's own cap allows. */
    private static final int MAX_DIFF_LINE = 240;

    /** The services these two windows talk to, by the path each is installed under. */
    private static final String MESSENGER_SERVICE = "messenger_service";
    private static final String KNOT_SERVICE = "knothub";

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
        final ServerServices.Host host = serviceFor(player, level,
                payload.hostPos(), payload.monitorPos(), MESSENGER_SERVICE);
        if (host == null) {
            /*
             * No server on this network is running it. The window is told so rather than left waiting, and
             * it puts its own compose box beyond use: a message typed into a service that does not exist
             * has nowhere at all to go, and letting it look sent would be the worst of the answers.
             */
            PacketDistributor.sendToPlayer(player, new MessengerStatePayload(
                    new MessengerStatePayload.Service(false, "", 0L, 0),
                    List.of(), List.of(MessengerLog.LOBBY), MessengerLog.LOBBY, List.of()));
            return;
        }
        final MessengerLog log = host.rack().messengerAt(host.slot());
        if (log == null) {
            return;
        }
        final String who = player.getGameProfile().getName();
        final String room = payload.room().isBlank() ? MessengerLog.LOBBY : payload.room();
        final long now = level.getGameTime();
        // Anybody who has gone quiet is let go of first, so the weight below is of who is really there.
        log.forgetIdle(now);
        if (payload.action() == MessengerActionPayload.LEAVE) {
            log.disconnect(who);
            return; // nothing to draw for a window that is closing
        }
        log.connect(who, room, now);
        final boolean spoke = switch (payload.action()) {
            case MessengerActionPayload.SAY -> log.say(room, who, payload.text(), now, false) != null;
            case MessengerActionPayload.NUDGE -> log.say(room, who, "", now, true) != null;
            default -> false;
        };
        /*
         * Somebody speaking is told to everybody, because a conversation nobody else sees arrive is not a
         * conversation. Somebody merely looking is answered alone: every window asks every couple of
         * seconds, and telling all of them each time would be a packet for every pair of people watching.
         */
        if (spoke) {
            host.changed(); // what was said rides on the Server item, so it is written down at once
            tellEveryone(level, host, log);
        } else {
            PacketDistributor.sendToPlayer(player, stateOf(host, room, log));
        }
    }

    /**
     * Sends the state to every player on the server who has this network's messenger open.
     *
     * <p>Each of them is sent the room they are actually looking at, not the room that was just spoken in.
     * Sending everybody the same room moved their window into a conversation they had not opened, so one
     * person saying something in the lobby pulled everybody else out of whatever they were reading.
     *
     * <p>Somebody who has left the game is dropped here rather than being left counted for ever. The
     * window says goodbye when it closes, but a client that crashed or a player who logged out never got
     * to, and the service's memory cost would climb with people who are not there any more.
     */
    private static void tellEveryone(final ServerLevel level, final ServerServices.Host host,
                                     final MessengerLog log) {
        for (final String who : log.connected()) {
            final ServerPlayer other = level.getServer().getPlayerList().getPlayerByName(who);
            if (other == null) {
                log.disconnect(who);
            } else {
                PacketDistributor.sendToPlayer(other, stateOf(host, log.roomOf(who), log));
            }
        }
    }

    private static MessengerStatePayload stateOf(final ServerServices.Host host,
                                                 final String room, final MessengerLog log) {
        final List<MessengerStatePayload.Line> lines = new ArrayList<>();
        final List<String> connected = log.connected();
        for (final MessengerLog.Message message : log.room(room, MessengerStatePayload.MAX_LINES)) {
            lines.add(new MessengerStatePayload.Line(message.from(), message.text(), message.nudge(),
                    connected.contains(message.from())));
        }
        final List<String> rooms = log.rooms();
        return new MessengerStatePayload(
                new MessengerStatePayload.Service(true,
                        cut(host.name(), MessengerStatePayload.Service.MAX_HOST),
                        log.historyBytes(), log.ramMb()),
                names(connected), names(rooms), cut(room, MessengerStatePayload.MAX_NAME_LETTERS), lines);
    }

    /** As many names as the packet lists, each as long as it carries one. */
    private static List<String> names(final List<String> values) {
        final List<String> out = new ArrayList<>(Math.min(values.size(), MessengerStatePayload.MAX_NAMES));
        for (final String value : values) {
            if (out.size() >= MessengerStatePayload.MAX_NAMES) {
                break;
            }
            out.add(cut(value, MessengerStatePayload.MAX_NAME_LETTERS));
        }
        return out;
    }

    private static void handleMessengerState(final MessengerStatePayload payload, final Player player) {
        MessengerApp.accept(payload);
    }

    /* The repository */

    private static void handleKnot(final KnotActionPayload payload, final ServerPlayer player,
                                   final ServerLevel level) {
        final ServerServices.Host host = serviceFor(player, level,
                payload.hostPos(), payload.monitorPos(), KNOT_SERVICE);
        final KnotRepository repository = host == null ? null : host.rack().knotAt(host.slot());
        if (repository == null) {
            PacketDistributor.sendToPlayer(player, new KnotStatePayload(
                    new KnotStatePayload.Service(false, "", 0L), List.of(), List.of(), 0, List.of()));
            return;
        }
        final String note = switch (payload.action()) {
            case KnotActionPayload.PUSH -> push(payload, player, level, host, repository);
            case KnotActionPayload.PULL -> pull(payload, level, repository);
            default -> "";
        };
        PacketDistributor.sendToPlayer(player,
                knotStateOf(host, repository, payload.revision(), note));
    }

    /**
     * Reads the file off the machine the player is at and saves it as a revision.
     *
     * <p>Answers what to tell them, because every one of these can fail for a reason they can act on and a
     * button that does nothing at all is the worst of the possible answers.
     */
    private static String push(final KnotActionPayload payload, final ServerPlayer player,
                               final ServerLevel level, final ServerServices.Host host,
                               final KnotRepository repository) {
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
        if (repository.commit(payload.file(), player.getGameProfile().getName(),
                payload.message(), content, level.getGameTime()) == null) {
            return "Nothing changed since the last revision";
        }
        host.changed();
        return "Pushed " + payload.file();
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

    private static KnotStatePayload knotStateOf(final ServerServices.Host host,
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
                new KnotStatePayload.Service(true, cut(host.name(), KnotStatePayload.Service.MAX_HOST),
                        repository.bytes(), cut(note, KnotStatePayload.Service.MAX_NOTE)),
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
     * The server on the player's network that is running {@code programPath}, or null when none is.
     *
     * <p>The service runs somewhere else than the computer in front of the player, which is what makes it
     * worth having: two players at two different machines reach the same one. Null is a complete answer,
     * and the window shows it as the service not being there.
     */
    @Nullable
    private static ServerServices.Host serviceFor(final ServerPlayer player, final ServerLevel level,
                                                  final BlockPos hostPos, final BlockPos monitorPos,
                                                  final String programPath) {
        final IComputerTerminalHost terminal = niHost(player, level, hostPos, monitorPos);
        return terminal == null ? null
                : ServerServices.find(level, terminal.networkUuid(), programPath);
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
        return cut(text, MAX_DIFF_LINE);
    }

    /**
     * Cuts a string to what its field carries.
     *
     * <p>Every one of these fields refuses what it is handed by throwing rather than by shortening it, so a
     * machine named at length, or a note built out of a long file name, would take the whole window down
     * instead of simply reading short. Cut where the answer is built, once, for each field that can grow.
     */
    private static String cut(final String text, final int most) {
        return text.length() > most ? text.substring(0, most) : text;
    }
}
