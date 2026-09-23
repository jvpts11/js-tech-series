/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Computers.
 */
package dev.jstech.computers.machine;

import dev.jstech.computers.program.ServerCliComputer;
import dev.jstech.computers.program.cli.ICliComputer;
import dev.jstech.computers.program.cli.NetPath;
import dev.jstech.computers.terminal.IComputerTerminalHost;
import dev.jstech.core.text.Text;
import dev.jstech.core.text.TextHolder;
import dev.jstech.core.text.TextKey;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.function.Supplier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.entity.BlockEntity;

/**
 * Where a path on another machine of the network leads: {@code \\host\share\rest}.
 *
 * <p>The other machine answers for its own disks, so a path below a share is followed on that machine, exactly as it
 * would be at that machine's own prompt. A host that is not on the network, one that is switched off, and a share
 * nobody opened under that name each say so; a share opened for reading only refuses a write the way its owner's
 * prompt would.
 */
@TextHolder
public final class NetworkPathResolver {

    /** Where a network path leads: the other machine's shell, the share, and the path on that machine. */
    public record Reached(ServerCliComputer remote, ICliComputer.ShareInfo share, String path,
                          ICliComputer.FsResult error) {

        static Reached failed(final Text message) {
            return new Reached(null, null, "", ICliComputer.FsResult.fail(message));
        }

        /** Whether the path led anywhere; when it did not, {@link #error()} says why. */
        public boolean ok() {
            return this.error == null;
        }
    }

    private final ServerLevel level;
    /** The machines of the network a name picks out, by host name, as the asking machine finds them. */
    private final Function<String, Map<String, BlockEntity>> machinesNamed;
    /** Every folder the other running machines of the network share. */
    private final Supplier<List<ICliComputer.NetworkShare>> networkShares;

    private static final TextKey SHARE_UNNAMED =
            TextKey.of("jsc.service.share.unnamed", "%s: a share has to be named (\\\\host\\share)");
    private static final TextKey HOST_NOT_FOUND =
            TextKey.of("jsc.service.share.host_not_found", "\\\\%s: host not found on this network");
    private static final TextKey HOST_AMBIGUOUS = TextKey.of("jsc.service.share.host_ambiguous",
            "\\\\%s matches %s machines (%s) - use the host name or node id");
    private static final TextKey HOST_OFF = TextKey.of("jsc.service.share.host_off", "\\\\%s: machine is powered off");
    private static final TextKey NO_SUCH_SHARE =
            TextKey.of("jsc.service.share.no_such_share", "\\\\%1$s\\%2$s: no such share on %1$s");

    public NetworkPathResolver(final ServerLevel level,
                               final Function<String, Map<String, BlockEntity>> machinesNamed,
                               final Supplier<List<ICliComputer.NetworkShare>> networkShares) {
        this.level = level;
        this.machinesNamed = machinesNamed;
        this.networkShares = networkShares;
    }

    /**
     * Follows a network path to the machine and the share it names.
     *
     * <p>What comes back is the other machine's shell, so a path below the share resolves there exactly as it would at
     * that machine's own prompt.
     */
    public Reached reach(final NetPath net) {
        if (net.isNetwork() || net.isHost()) {
            return Reached.failed(SHARE_UNNAMED.with(net.display()));
        }
        final Map<String, BlockEntity> matches = this.machinesNamed.apply(net.host());
        if (matches.isEmpty()) {
            return Reached.failed(HOST_NOT_FOUND.with(net.host()));
        }
        if (matches.size() > 1) {
            return Reached.failed(HOST_AMBIGUOUS.with(net.host(), String.valueOf(matches.size()),
                    String.join(", ", matches.keySet())));
        }
        final BlockEntity target = matches.values().iterator().next();
        final ServerCliComputer remote = new ServerCliComputer((IComputerTerminalHost) target, this.level);
        if (!remote.running()) {
            return Reached.failed(HOST_OFF.with(net.host()));
        }
        for (final ICliComputer.ShareInfo share : remote.shares()) {
            if (share.name().equalsIgnoreCase(net.share())) {
                return new Reached(remote, share, net.remotePath(share.path()), null);
            }
        }
        return Reached.failed(NO_SUCH_SHARE.with(net.host(), net.share()));
    }

    /** Lists what a network path holds: the hosts sharing something, a host's shares, or a shared folder. */
    public ICliComputer.FsResult list(final NetPath net) {
        final List<ICliComputer.FsEntry> entries = new ArrayList<>();
        if (net.isNetwork()) {
            final Set<String> hosts = new LinkedHashSet<>();
            for (final ICliComputer.NetworkShare share : this.networkShares.get()) {
                hosts.add(share.hostname());
            }
            for (final String hostname : hosts) {
                entries.add(new ICliComputer.FsEntry(hostname, "", 0L, true, true, 0L));
            }
            return ICliComputer.FsResult.listing(entries);
        }
        if (net.isHost()) {
            boolean found = false;
            for (final ICliComputer.NetworkShare share : this.networkShares.get()) {
                if (net.onHost(share.hostname())) {
                    found = true;
                    entries.add(new ICliComputer.FsEntry(share.share().name(), "", 0L,
                            !share.share().writable(), true, 0L));
                }
            }
            if (!found && this.machinesNamed.apply(net.host()).isEmpty()) {
                return ICliComputer.FsResult.fail(HOST_NOT_FOUND.with(net.host()));
            }
            return ICliComputer.FsResult.listing(entries);
        }
        final Reached reached = this.reach(net);
        if (!reached.ok()) {
            return reached.error();
        }
        final ICliComputer.FsResult listing = reached.remote().listDisk(reached.path());
        if (!listing.ok() || reached.share().writable() || listing.entries() == null) {
            return listing;
        }
        // Everything under a read-only share reads as read-only, whatever the other machine says of it.
        final List<ICliComputer.FsEntry> kept = new ArrayList<>();
        for (final ICliComputer.FsEntry entry : listing.entries()) {
            kept.add(new ICliComputer.FsEntry(entry.name(), entry.ext(), entry.weightMbEq(), true, entry.isDir(),
                    entry.modified()));
        }
        return ICliComputer.FsResult.listing(kept);
    }

    /** Whether a network path names a folder that exists, for a copy that lands "into" it. */
    public boolean dirExists(final NetPath net) {
        if (net.isNetwork() || net.isHost()) {
            return false;
        }
        if (net.rest().isEmpty()) {
            return this.reach(net).ok();
        }
        // A listing of a path that is not there comes back empty rather than failed, so ask the parent.
        final Reached above = this.reach(net.parent());
        if (!above.ok()) {
            return false;
        }
        final ICliComputer.FsResult listing = above.remote().listDisk(above.path());
        return listing.ok() && listing.entries() != null && listing.entries().stream()
                .anyMatch(entry -> entry.isDir() && entry.name().equalsIgnoreCase(net.name()));
    }
}
