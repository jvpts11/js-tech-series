/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Computers.
 */
package dev.jstech.computers.machine;

import dev.jstech.computers.blockentity.AbstractComputerBlockEntity;
import dev.jstech.computers.blockentity.ClusterManagementComputerBlockEntity;
import dev.jstech.computers.blockentity.CraftingComputerBlockEntity;
import dev.jstech.computers.blockentity.MainframeBlockEntity;
import dev.jstech.computers.blockentity.PersonalComputerBlockEntity;
import dev.jstech.computers.blockentity.ServerRackBlockEntity;
import dev.jstech.computers.os.IOsHost;
import dev.jstech.computers.os.OsDef;
import dev.jstech.computers.program.ComputerConsoleState;
import dev.jstech.computers.program.ServerCliComputer;
import dev.jstech.computers.program.cli.CliTexts;
import dev.jstech.computers.program.cli.ICliComputer;
import dev.jstech.computers.terminal.IComputerTerminalHost;
import dev.jstech.computers.vm.program.IProgramParent;
import dev.jstech.computers.vm.program.ProgramPriority;
import dev.jstech.core.network.NetworkSystem;
import dev.jstech.core.network.ServerNode;
import dev.jstech.core.text.Text;
import dev.jstech.core.text.TextHolder;
import dev.jstech.core.text.TextKey;
import dev.jstech.core.util.ShortId;
import dev.jstech.core.uuid.NetworkUuid;
import dev.jstech.core.uuid.NodeUuid;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.entity.BlockEntity;
import org.jetbrains.annotations.Nullable;

/**
 * The other computers of a machine's network, as what runs on the machine reaches them.
 *
 * <p>A program there can be started, a line run at that computer's prompt, a line sent to one of its programs, and its
 * programs listed. Everything runs on the other computer, out of its own budget and under its own name; whether it
 * takes any of this at all is the other computer's to say.
 */
@TextHolder
public final class RemoteComputerService {

    private final IComputerTerminalHost terminal;
    private final ServerLevel level;

    private static final TextKey NO_SESSION =
            TextKey.of("jsc.service.remote.no_session", "this terminal keeps no session");
    private static final TextKey HOST_NOT_FOUND =
            TextKey.of("jsc.service.remote.host_not_found", "%s: host not found on this network");
    private static final TextKey AMBIGUOUS = TextKey.of("jsc.service.remote.ambiguous",
            "%s matches %s machines (%s) - use the host name or node id");
    private static final TextKey POWERED_OFF =
            TextKey.of("jsc.service.remote.powered_off", "connect to host %s: machine is powered off");
    private static final TextKey CONNECTED =
            TextKey.of("jsc.service.remote.connected", "Connected to %s. Type exit to return.");
    private static final TextKey NOT_CONNECTED = TextKey.of("jsc.service.remote.not_connected",
            "not connected - close the window to leave this terminal");
    private static final TextKey CLOSED = TextKey.of("jsc.service.remote.closed", "Connection closed.");

    /** The commands that open and close a session, whose names its messages begin with. */
    private static final String SSH = "ssh";
    private static final String EXIT = "exit";

    /*
     * What kind of computer a machine is. The list of machines travels on as words (to the prompt's host list and
     * to a Gateway's caller), so there these are handed over in English, the machine's language.
     */
    private static final TextKey TYPE_MAINFRAME = TextKey.of("jsc.service.remote.type.mainframe", "Mainframe");
    private static final TextKey TYPE_COMPUTER = TextKey.of("jsc.service.remote.type.computer", "Computer");
    private static final TextKey TYPE_CRAFTING =
            TextKey.of("jsc.service.remote.type.crafting", "Crafting Computer");
    private static final TextKey TYPE_PERSONAL =
            TextKey.of("jsc.service.remote.type.personal", "Personal Computer");
    private static final TextKey TYPE_CLUSTER =
            TextKey.of("jsc.service.remote.type.cluster", "Cluster Management Computer");

    public RemoteComputerService(final IComputerTerminalHost terminal, final ServerLevel level) {
        this.terminal = terminal;
        this.level = level;
    }

    /**
     * Every machine of this network another computer could reach, by host name: the Mainframe, the personal
     * computers, and the servers in their racks. The machine itself is left out, since you cannot reach the terminal
     * you are already sitting at.
     *
     * <p>Each machine names itself. Nothing is built to ask it, because a name is something a machine knows.
     */
    public Map<String, BlockEntity> machines() {
        final Map<String, BlockEntity> out = new LinkedHashMap<>();
        final NetworkUuid network = this.terminal.networkUuid();
        if (network == null) {
            return out;
        }
        final NetworkSystem system = NetworkSystem.get(this.level);
        final List<BlockEntity> candidates = new ArrayList<>();
        final MainframeBlockEntity mainframe = this.mainframe(network);
        if (mainframe != null) {
            candidates.add(mainframe);
        }
        for (final NetworkSystem.PersonalComputerNode pc : system.personalComputersOf(network)) {
            if (this.level.getBlockEntity(BlockPos.of(pc.pos())) instanceof PersonalComputerBlockEntity be) {
                candidates.add(be);
            }
        }
        for (final ServerNode server : system.serversOf(network)) {
            system.locationOf(server.nodeUuid()).ifPresent(loc -> {
                if (this.level.getBlockEntity(BlockPos.of(loc.rackPos())) instanceof ServerRackBlockEntity rack) {
                    candidates.add(rack);
                }
            });
        }
        final BlockEntity self = (BlockEntity) this.terminal;
        for (final BlockEntity candidate : candidates) {
            if (candidate == self || !(candidate instanceof IComputerTerminalHost host)) {
                continue;
            }
            // A duplicate host name keeps the first machine found, the way a name collision would.
            out.putIfAbsent(host.hostname(), candidate);
        }
        return out;
    }

    /** Every machine another computer could reach, with what each is and whether it is on. */
    public List<ICliComputer.RemoteHost> hosts() {
        final List<ICliComputer.RemoteHost> hosts = new ArrayList<>();
        this.machines().forEach((hostname, machine) -> {
            final OsDef os = machine instanceof IOsHost computer ? computer.installedOs() : null;
            hosts.add(new ICliComputer.RemoteHost(hostname, nameOf(machine), nodeIdOf(machine),
                    os == null ? "" : os.displayName(), typeOf(machine).english(),
                    machine instanceof IComputerTerminalHost host && host.computerRunning()));
        });
        return hosts;
    }

    /**
     * The machines a typed name picks out. A host name, the name its owner gave it and the head of its node id all
     * address a machine; a system name works too, but only while it picks out exactly one, since two Debian servers
     * make "debian" ambiguous, and saying so is more useful than guessing.
     */
    public Map<String, BlockEntity> matching(final String wanted) {
        final String needle = wanted == null ? "" : wanted.trim().toLowerCase(Locale.ROOT);
        final Map<String, BlockEntity> matches = new LinkedHashMap<>();
        if (needle.isEmpty()) {
            return matches;
        }
        this.machines().forEach((hostname, machine) -> {
            final OsDef os = machine instanceof IOsHost computer ? computer.installedOs() : null;
            final boolean hit = hostname.equalsIgnoreCase(needle)
                    || nameOf(machine).equalsIgnoreCase(needle)
                    || nodeIdOf(machine).equalsIgnoreCase(needle)
                    || (os != null && (os.displayName().equalsIgnoreCase(needle)
                            || os.id().getPath().equalsIgnoreCase(needle)));
            if (hit) {
                matches.put(hostname, machine);
            }
        });
        return matches;
    }

    /** The one computer of the network that host name picks out, as its own shell; null when none or several do. */
    @Nullable
    public ServerCliComputer find(final String host) {
        final Map<String, BlockEntity> matches = this.matching(host);
        if (matches.size() != 1) {
            return null;
        }
        return new ServerCliComputer((IComputerTerminalHost) matches.values().iterator().next(), this.level);
    }

    /**
     * Opens a session on another machine: from here on the window's commands run there until it is closed.
     *
     * <p>Being on the same network is what grants access; authentication arrives with the security module.
     */
    public ICliComputer.OpResult connect(final String hostname) {
        final ComputerConsoleState console = this.terminal.console();
        if (console == null) {
            return ICliComputer.OpResult.fail(CliTexts.SAID_BY.with(SSH, NO_SESSION));
        }
        final Map<String, BlockEntity> matches = this.matching(hostname);
        if (matches.isEmpty()) {
            return ICliComputer.OpResult.fail(CliTexts.SAID_BY.with(SSH, HOST_NOT_FOUND.with(hostname)));
        }
        if (matches.size() > 1) {
            return ICliComputer.OpResult.fail(CliTexts.SAID_BY.with(SSH,
                    AMBIGUOUS.with(hostname, matches.size(), String.join(", ", matches.keySet()))));
        }
        final BlockEntity target = matches.values().iterator().next();
        if (!(target instanceof IComputerTerminalHost remote) || !remote.computerRunning()) {
            return ICliComputer.OpResult.fail(CliTexts.SAID_BY.with(SSH, POWERED_OFF.with(hostname)));
        }
        console.setSshTarget(target.getBlockPos().asLong());
        return ICliComputer.OpResult.ok(CONNECTED.with(hostname));
    }

    /** Closes the session and returns to the local machine; fails when there is none open. */
    public ICliComputer.OpResult disconnect() {
        final ComputerConsoleState console = this.terminal.console();
        if (console == null || console.sshTarget() == null) {
            return ICliComputer.OpResult.fail(CliTexts.SAID_BY.with(EXIT, NOT_CONNECTED));
        }
        console.setSshTarget(null);
        return ICliComputer.OpResult.ok(CLOSED);
    }

    /**
     * The name of the machine this session is connected to, or {@code ""} when there is none.
     *
     * <p>The machine at the other end names itself, so nothing is built here to ask it.
     */
    public String session() {
        final ComputerConsoleState console = this.terminal.console();
        if (console == null || console.sshTarget() == null) {
            return "";
        }
        final BlockEntity target = this.level.getBlockEntity(BlockPos.of(console.sshTarget()));
        return target instanceof IComputerTerminalHost remote ? remote.hostname() : "";
    }

    /**
     * Every folder the other running machines of the network share, by host name.
     *
     * <p>Each machine answers for itself, both whether it is on and what it shares, so walking the network builds
     * nothing at all.
     */
    public List<ICliComputer.NetworkShare> networkShares() {
        final List<ICliComputer.NetworkShare> out = new ArrayList<>();
        this.machines().forEach((hostname, machine) -> {
            if (!(machine instanceof IComputerTerminalHost host) || !host.computerRunning()) {
                return;
            }
            for (final ICliComputer.ShareInfo share : MachineConfigService.sharesOf(host)) {
                out.add(new ICliComputer.NetworkShare(hostname, share));
            }
        });
        return out;
    }

    /** Where a path on another machine of the network leads, followed on that machine's own shell. */
    public NetworkPathResolver paths() {
        return new NetworkPathResolver(this.level, this::matching, this::networkShares);
    }

    /**
     * The asking program as the other computer will know it, so that what it starts there is kept for it to read;
     * none for a caller that is not a numbered program on a computer of the network.
     */
    public IProgramParent parentOf(final int callerId) {
        return callerId > 0 && this.terminal instanceof AbstractComputerBlockEntity machine
                && machine.nodeUuid() != null
                ? new IProgramParent.Remote(machine.getBlockPos().asLong(), machine.nodeUuid().value(), callerId)
                : IProgramParent.NONE;
    }

    /** What kind of computer a machine is, in the words the terminal shows. */
    public static Text typeOf(final Object machine) {
        if (machine instanceof MainframeBlockEntity) {
            return TYPE_MAINFRAME.text();
        }
        if (machine instanceof CraftingComputerBlockEntity) {
            return TYPE_CRAFTING.text();
        }
        if (machine instanceof PersonalComputerBlockEntity) {
            return TYPE_PERSONAL.text();
        }
        if (machine instanceof ClusterManagementComputerBlockEntity) {
            return TYPE_CLUSTER.text();
        }
        return TYPE_COMPUTER.text();
    }

    /** The name a machine's owner gave it, or {@code ""} when it has none. */
    private static String nameOf(final BlockEntity machine) {
        return machine instanceof IOsHost computer ? computer.customName() : "";
    }

    /** The short form of a machine's node id, or the dashes a machine with no node shows. */
    private static String nodeIdOf(final BlockEntity machine) {
        final NodeUuid node;
        if (machine instanceof IOsHost computer) {
            node = computer.nodeUuid();
        } else if (machine instanceof MainframeBlockEntity mainframe) {
            node = mainframe.nodeUuid();
        } else {
            node = null;
        }
        return node == null ? "------" : ShortId.of(node.asString());
    }

    /** The Mainframe of the machine's network, or null when it is on none, or none is running. */
    @Nullable
    private MainframeBlockEntity mainframe(@Nullable final NetworkUuid net) {
        if (net == null) {
            return null;
        }
        return NetworkSystem.get(this.level).mainframePositionOf(net)
                .map(pos -> this.level.getBlockEntity(BlockPos.of(pos)) instanceof MainframeBlockEntity mf ? mf : null)
                .orElse(null);
    }

    /** Starts a compiled program from the other computer's disks, on that computer; null when it runs no programs. */
    @Nullable
    public ProgramLauncher.Launch start(final ServerCliComputer remote, final String path, final List<String> arguments,
                                        final IProgramParent parent, final ProgramPriority priority) {
        return remote.machine() instanceof AbstractComputerBlockEntity machine
                ? ProgramLauncher.launch(machine, path, remote::readFile, arguments, parent, priority, 0) : null;
    }

    /** Runs one line at the other computer's prompt and hands back what it printed. */
    public List<String> shell(final ServerCliComputer remote, final String command) {
        return ProgramService.run(remote, command);
    }

    /** Sends a line to a program on the other computer; false when it has no such program or no room for it. */
    public boolean send(final ServerCliComputer remote, final int from, final int to, final String text) {
        final long tick = remote.machine().getLevel() == null ? 0L : remote.machine().getLevel().getGameTime();
        return remote.machine() instanceof AbstractComputerBlockEntity machine
                && machine.programs().send(from, to, text, tick);
    }

    /** The programs the other computer is running. */
    public List<ProgramView> processes(final ServerCliComputer remote) {
        return remote.machine() instanceof AbstractComputerBlockEntity machine ? machine.programs().view() : List.of();
    }
}
