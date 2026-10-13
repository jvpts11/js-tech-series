/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Computers.
 */
package dev.jstech.computers.audio;

import dev.jstech.computers.blockentity.AbstractComputerBlockEntity;
import dev.jstech.computers.blockentity.ServerServices;
import dev.jstech.computers.os.fs.FsPaths;
import dev.jstech.computers.os.fs.RecordingFile;
import dev.jstech.computers.program.Programs;
import dev.jstech.computers.program.SoundfoundryListeners;
import dev.jstech.computers.terminal.IComputerTerminalHost;
import dev.jstech.core.network.DataLink;
import dev.jstech.core.network.NetworkSystem;
import java.util.List;
import java.util.Optional;
import net.minecraft.server.level.ServerLevel;
import org.jetbrains.annotations.Nullable;

/**
 * The Soundfoundry Servers a computer's Soundfoundry reaches over its network, and what each keeps: the network's
 * library, which is the songs in the server machine's own music folder. A song a computer sent it is kept in a
 * folder named after that computer, which is how the library says who it is from; any other song there is the
 * server's own.
 *
 * <p>A server streams to a computer only while it has the memory to hold the stream open, so a computer asks to
 * listen before it plays, and says so again every little while as long as it does.
 */
public final class SoundfoundryServers {

    private SoundfoundryServers() {
    }

    /** Every Soundfoundry Server running on the computer's network, in the order the network lists them. */
    public static List<ServerServices.Host> of(final ServerLevel level, final AbstractComputerBlockEntity computer) {
        return ServerServices.all(level, computer.networkUuid(), Programs.SOUNDFOUNDRY_SERVER);
    }

    /** The server the computer's Soundfoundry streams from: the one the player picked, else the first found. */
    @Nullable
    public static ServerServices.Host chosen(final ServerLevel level, final AbstractComputerBlockEntity computer) {
        final List<ServerServices.Host> all = of(level, computer);
        if (all.isEmpty()) {
            return null;
        }
        final String picked = computer.console().soundfoundry().server();
        for (final ServerServices.Host host : all) {
            if (host.id().equals(picked)) {
                return host;
            }
        }
        return all.getFirst();
    }

    /** The server on the computer's network known by that id, or null when it is not running there. */
    @Nullable
    public static ServerServices.Host byId(final ServerLevel level, final AbstractComputerBlockEntity computer,
                                           final String id) {
        for (final ServerServices.Host host : of(level, computer)) {
            if (host.id().equals(id)) {
                return host;
            }
        }
        return null;
    }

    /** The folder the server keeps the network's library in: its own music folder. */
    public static String libraryFolder(final ServerServices.Host host) {
        return MusicImports.musicFolderOf(host.machine());
    }

    /** Every song of the server's library, by its path on the server. */
    public static List<String> library(final ServerLevel level, final ServerServices.Host host) {
        return SongFiles.under(level, host.machine(), libraryFolder(host));
    }

    /** The song of the server's library at that path, or null when the library holds none there. */
    @Nullable
    public static RecordingFile song(final ServerLevel level, final ServerServices.Host host, final String path) {
        return FsPaths.isUnder(libraryFolder(host), path) ? SongFiles.read(level, host.machine(), path) : null;
    }

    /** Where a computer's songs go on the server: a folder named after that computer, in the library. */
    public static String folderFor(final ServerServices.Host host, final String hostname) {
        return FsPaths.join(libraryFolder(host), hostname);
    }

    /** What a computer goes by on the network, which is what the folder of the songs it sends is called. */
    public static String hostnameOf(final AbstractComputerBlockEntity computer) {
        return computer instanceof IComputerTerminalHost terminal ? terminal.hostname()
                : computer.console().computerName();
    }

    /** What the library says a song is from: the computer that sent it, or the server itself. */
    public static String fromOf(final ServerServices.Host host, final String path) {
        final String folder = libraryFolder(host);
        if (!FsPaths.isUnder(folder, path)) {
            return host.hostname();
        }
        final String inside = folder.isEmpty() ? path : path.substring(folder.length() + 1);
        final int slash = inside.indexOf('/');
        return slash < 0 ? host.hostname() : inside.substring(0, slash);
    }

    /** The slowest cable between the computer and the server, which is as fast as a song goes between them. */
    public static Optional<DataLink> linkTo(final ServerLevel level, final AbstractComputerBlockEntity computer,
                                            final ServerServices.Host host) {
        final NetworkSystem system = NetworkSystem.get(level);
        return system.connectivity().slowestBetween(computer.networkCables(level),
                system.connectivity().bridgedBy(host.rack().getBlockPos().asLong()));
    }

    /**
     * The computer asks to listen to the server, or says it still does.
     *
     * @return whether the server streams to it: not when it is new and the server has no memory left for its stream
     */
    public static boolean listen(final ServerLevel level, final ServerServices.Host host,
                                 final AbstractComputerBlockEntity computer) {
        final SoundfoundryListeners listeners = host.rack().listenersAt(host.slot());
        if (listeners == null) {
            return false;
        }
        final long at = computer.getBlockPos().asLong();
        final long now = level.getGameTime();
        if (!listeners.listening(at, now)
                && !host.machine().ramLedger().fits(SoundfoundryListeners.RAM_PER_LISTENER_MB)) {
            return false;
        }
        listeners.listen(at, now);
        return true;
    }

    /** How many computers are listening to the server. */
    public static int listeners(final ServerLevel level, final ServerServices.Host host) {
        final SoundfoundryListeners listeners = host.rack().listenersAt(host.slot());
        return listeners == null ? 0 : listeners.count(level.getGameTime());
    }

    /** The computer stopped listening to the server. */
    public static void leave(final ServerServices.Host host, final AbstractComputerBlockEntity computer) {
        final SoundfoundryListeners listeners = host.rack().listenersAt(host.slot());
        if (listeners != null) {
            listeners.leave(computer.getBlockPos().asLong());
        }
    }
}
