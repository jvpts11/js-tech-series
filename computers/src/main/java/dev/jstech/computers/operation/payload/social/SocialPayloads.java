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
import dev.jstech.computers.client.os.MessengerKeptDialog;
import dev.jstech.computers.client.portable.PortableApps;
import dev.jstech.computers.operation.payload.ClientPayloadHandlers;
import dev.jstech.computers.operation.payload.ComputerAccess;
import dev.jstech.computers.operation.payload.KnotActionPayload;
import dev.jstech.computers.operation.payload.KnotStatePayload;
import dev.jstech.computers.operation.payload.MessengerActionPayload;
import dev.jstech.computers.operation.payload.MessengerKeptPayload;
import dev.jstech.computers.operation.payload.MessengerStatePayload;
import dev.jstech.computers.operation.payload.files.FileAccess;
import dev.jstech.computers.os.FilesystemKind;
import dev.jstech.computers.os.IOsHost;
import dev.jstech.computers.os.fs.DiskFilesystem;
import dev.jstech.computers.os.fs.FileType;
import dev.jstech.computers.program.KnotRepository;
import dev.jstech.computers.program.MessengerLog;
import dev.jstech.computers.program.Programs;
import dev.jstech.computers.terminal.IComputerTerminalHost;
import dev.jstech.core.text.Text;
import dev.jstech.core.text.TextBounds;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
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

    private SocialPayloads() {
    }

    /** Registers the payloads this class handles. */
    public static void register(final PayloadRegistrar registrar) {
        ComputerAccess.accept(registrar, MessengerActionPayload.TYPE, MessengerActionPayload.STREAM_CODEC,
                ComputerAccess.machine(MessengerActionPayload::hostPos), SocialPayloads::handleMessenger);
        registrar.playToClient(MessengerStatePayload.TYPE, MessengerStatePayload.STREAM_CODEC,
                ClientPayloadHandlers.onMainThread(SocialPayloads::handleMessengerState));
        registrar.playToClient(MessengerKeptPayload.TYPE, MessengerKeptPayload.STREAM_CODEC,
                ClientPayloadHandlers.onMainThread(SocialPayloads::handleMessengerKept));
        ComputerAccess.accept(registrar, KnotActionPayload.TYPE, KnotActionPayload.STREAM_CODEC,
                ComputerAccess.machine(KnotActionPayload::hostPos), SocialPayloads::handleKnot);
        registrar.playToClient(KnotStatePayload.TYPE, KnotStatePayload.STREAM_CODEC,
                ClientPayloadHandlers.onMainThread(SocialPayloads::handleKnotState));
    }

    /* The messenger */

    private static void handleMessenger(final MessengerActionPayload payload, final ServerPlayer player,
                                        final ServerLevel level) {
        final ServerServices.Host host = serviceFor(player, level, payload.hostPos(), payload.monitorPos(),
                Programs.MESSENGER_SERVICE);
        // The window is at the service's own server when the machine it is on is that very unit of that rack.
        final boolean atServer = host != null && host.rack().getBlockPos().equals(payload.hostPos())
                && host.rack().shownRow() == host.slot();
        if (payload.action() >= MessengerActionPayload.KEPT) {
            if (atServer) {
                keep(player, level, host, payload.action(), payload.room());
            }
            return;
        }
        talk(player, level, host, payload.action(), payload.room(), payload.text(), atServer);
    }

    /*
     * The kept conversations, at the service's own server: listed, one deleted or all of them. Whoever is at that
     * server may, there being no accounts yet to keep it to an administrator; the deletion reaches every Messenger
     * through the same path as anything said.
     */
    private static void keep(final ServerPlayer player, final ServerLevel level, final ServerServices.Host host,
                             final int action, final String room) {
        final KeptConversations kept = KeptConversations.at(level, host);
        if (kept == null) {
            return;
        }
        if (action == MessengerActionPayload.DELETE) {
            kept.delete(room);
        } else if (action == MessengerActionPayload.DELETE_ALL) {
            kept.deleteAll();
        }
        final List<MessengerKeptPayload.Kept> rows = new ArrayList<>();
        for (final MessengerLog.Conversation conversation : kept.list()) {
            if (rows.size() == MessengerKeptPayload.MOST) {
                break;
            }
            rows.add(new MessengerKeptPayload.Kept(
                    TextBounds.clip(conversation.room(), MessengerKeptPayload.MOST_LETTERS),
                    TextBounds.clip(String.join(", ", conversation.names()), MessengerKeptPayload.MOST_LETTERS),
                    conversation.lines(), conversation.bytes(), kept.clockAt(conversation.lastAt())));
        }
        PacketDistributor.sendToPlayer(player, new MessengerKeptPayload(rows, kept.status().bytes()));
    }

    /**
     * What {@code player} did in the Messenger that {@code host} runs, or that none does: looked at {@code room},
     * said {@code text} in it, nudged it or left; one of {@link MessengerActionPayload}'s actions. A computer's window
     * and a portable's app speak to the service alike.
     */
    public static void talk(final ServerPlayer player, final ServerLevel level,
                            @Nullable final ServerServices.Host host, final int action, final String asked,
                            final String text) {
        talk(player, level, host, action, asked, text, false);
    }

    private static void talk(final ServerPlayer player, final ServerLevel level,
                             @Nullable final ServerServices.Host host, final int action, final String asked,
                             final String text, final boolean atServer) {
        if (host == null) {
            /*
             * No server on this network is running it. The window is told so rather than left waiting, and
             * it puts its own compose box beyond use: a message typed into a service that does not exist
             * has nowhere at all to go, and letting it look sent would be the worst of the answers.
             */
            PacketDistributor.sendToPlayer(player, new MessengerStatePayload(
                    new MessengerStatePayload.Service(false, "", 0L, 0, false),
                    List.of(), List.of(MessengerLog.LOBBY), MessengerLog.LOBBY, List.of()));
            return;
        }
        final MessengerLog log = host.rack().messengerAt(host.slot());
        if (log == null) {
            return;
        }
        final String who = player.getGameProfile().getName();
        // A private room asked for by somebody who is not one of its two is the lobby for them.
        final String named = asked.isBlank() ? MessengerLog.LOBBY : asked;
        final String room = MessengerLog.mayEnter(named, who) ? named : MessengerLog.LOBBY;
        final long now = level.getGameTime();
        // Anybody who has gone quiet is let go of first, so the weight below is of who is really there.
        log.forgetIdle(now);
        if (action == MessengerActionPayload.LEAVE) {
            log.disconnect(who);
            return; // nothing to draw for a window that is closing
        }
        log.connect(who, room, now, atServer);
        final boolean spoke = switch (action) {
            case MessengerActionPayload.SAY -> log.say(room, who, text, now, false) != null;
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
            PacketDistributor.sendToPlayer(player, stateOf(host, room, log, who));
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
    static void tellEveryone(final ServerLevel level, final ServerServices.Host host, final MessengerLog log) {
        for (final String who : log.connected()) {
            final ServerPlayer other = level.getServer().getPlayerList().getPlayerByName(who);
            if (other == null) {
                log.disconnect(who);
            } else {
                PacketDistributor.sendToPlayer(other, stateOf(host, log.roomOf(who), log, who));
            }
        }
    }

    private static MessengerStatePayload stateOf(final ServerServices.Host host, final String room,
                                                 final MessengerLog log, final String viewer) {
        final List<MessengerStatePayload.Line> lines = new ArrayList<>();
        final List<String> connected = log.connected();
        for (final MessengerLog.Message message : log.room(room, MessengerStatePayload.MAX_LINES)) {
            lines.add(new MessengerStatePayload.Line(message.from(), message.text(), message.nudge(),
                    connected.contains(message.from())));
        }
        final List<String> rooms = log.roomsFor(viewer);
        return new MessengerStatePayload(
                new MessengerStatePayload.Service(true,
                        TextBounds.clip(host.name(), MessengerStatePayload.Service.MAX_HOST),
                        log.historyBytes(), log.ramMb(), log.atServer(viewer)),
                names(connected), names(rooms), TextBounds.clip(room, MessengerStatePayload.MAX_NAME_LETTERS), lines);
    }

    /** As many names as the packet lists, each as long as it carries one. */
    private static List<String> names(final List<String> values) {
        final List<String> out = new ArrayList<>(Math.min(values.size(), MessengerStatePayload.MAX_NAMES));
        for (final String value : values) {
            if (out.size() >= MessengerStatePayload.MAX_NAMES) {
                break;
            }
            out.add(TextBounds.clip(value, MessengerStatePayload.MAX_NAME_LETTERS));
        }
        return out;
    }

    private static void handleMessengerState(final MessengerStatePayload payload, final Player player) {
        // A computer's window or a portable's app has it open: whichever it is shows it.
        MessengerApp.accept(payload);
        PortableApps.take(payload);
    }

    private static void handleMessengerKept(final MessengerKeptPayload payload, final Player player) {
        MessengerKeptDialog.accept(payload);
    }

    /* The repository */

    private static void handleKnot(final KnotActionPayload payload, final ServerPlayer player,
                                   final ServerLevel level) {
        final ServerServices.Host host = serviceFor(player, level,
                payload.hostPos(), payload.monitorPos(), Programs.KNOT_HUB);
        final KnotRepository repository = host == null ? null : host.rack().knotAt(host.slot());
        if (repository == null) {
            PacketDistributor.sendToPlayer(player, new KnotStatePayload(
                    new KnotStatePayload.Service(false, "", 0L), List.of(), List.of(), 0, List.of()));
            return;
        }
        final Text note = switch (payload.action()) {
            case KnotActionPayload.PUSH -> push(payload, player, level, host, repository);
            case KnotActionPayload.PULL -> pull(payload, level, repository);
            default -> Text.EMPTY;
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
    private static Text push(final KnotActionPayload payload, final ServerPlayer player,
                             final ServerLevel level, final ServerServices.Host host,
                             final KnotRepository repository) {
        if (!(level.getBlockEntity(payload.hostPos()) instanceof IOsHost computer)) {
            return KnotStatePayload.NO_COMPUTER.text();
        }
        final ItemStack disk = computer.systemDisk();
        if (disk.isEmpty()) {
            return KnotStatePayload.NO_SYSTEM_DISK.text();
        }
        final String content = DiskFilesystem.read(disk, payload.file()).orElse(null);
        if (content == null) {
            return KnotStatePayload.NO_SUCH_FILE.with(payload.file());
        }
        if (repository.commit(payload.file(), player.getGameProfile().getName(),
                payload.message(), content, level.getGameTime()) == null) {
            return KnotStatePayload.NOTHING_CHANGED.text();
        }
        host.changed();
        return KnotStatePayload.PUSHED.with(payload.file());
    }

    /** Writes a revision back onto the machine's disk, where the editor can open it. */
    private static Text pull(final KnotActionPayload payload, final ServerLevel level,
                             final KnotRepository repository) {
        final KnotRepository.Revision revision = repository.revision(payload.revision());
        final String file = repository.fileOf(payload.revision());
        if (revision == null || file == null) {
            return KnotStatePayload.NO_SUCH_REVISION.text();
        }
        if (!(level.getBlockEntity(payload.hostPos()) instanceof IOsHost computer)) {
            return KnotStatePayload.NO_COMPUTER.text();
        }
        final ItemStack disk = computer.systemDisk();
        final FilesystemKind kind = FileAccess.filesystemKindOf(computer);
        if (disk.isEmpty() || kind == FilesystemKind.NONE) {
            return KnotStatePayload.NO_SYSTEM_DISK.text();
        }
        final DiskFilesystem.WriteResult result = DiskFilesystem.write(disk, file, typeOf(file),
                revision.content(), computer.systemDiskFreeWeight(), kind, level.getGameTime());
        if (result != DiskFilesystem.WriteResult.OK) {
            return result == DiskFilesystem.WriteResult.DISK_FULL
                    ? KnotStatePayload.NO_ROOM.text() : KnotStatePayload.COULD_NOT_WRITE.with(file);
        }
        computer.setChanged();
        return KnotStatePayload.WROTE.with(revision.number(), file);
    }

    private static KnotStatePayload knotStateOf(final ServerServices.Host host,
                                                final KnotRepository repository, final int shown,
                                                final Text note) {
        final List<KnotStatePayload.Revision> revisions = new ArrayList<>();
        for (final KnotRepository.Revision revision
                : repository.recent(KnotStatePayload.MAX_REVISIONS)) {
            final String file = repository.fileOf(revision.number());
            revisions.add(new KnotStatePayload.Revision(revision.number(), revision.author(),
                    revision.message(), file == null ? "" : file));
        }
        return new KnotStatePayload(
                new KnotStatePayload.Service(true, TextBounds.clip(host.name(), KnotStatePayload.Service.MAX_HOST),
                        repository.bytes(), note),
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
     * The server on the player's network that is running {@code program}, or null when none is.
     *
     * <p>The service runs somewhere else than the computer in front of the player, which is what makes it
     * worth having: two players at two different machines reach the same one. Null is a complete answer,
     * and the window shows it as the service not being there.
     */
    @Nullable
    private static ServerServices.Host serviceFor(final ServerPlayer player, final ServerLevel level,
                                                  final BlockPos hostPos, final BlockPos monitorPos,
                                                  final ResourceLocation program) {
        final IComputerTerminalHost terminal = niHost(player, level, hostPos, monitorPos);
        return terminal == null ? null
                : ServerServices.find(level, terminal.networkUuid(), program);
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
        return TextBounds.clip(text, MAX_DIFF_LINE);
    }
}
