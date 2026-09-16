/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Computers.
 */
package dev.jstech.computers.program;

import dev.jstech.computers.blockentity.AbstractComputerBlockEntity;
import dev.jstech.computers.blockentity.CraftingComputerBlockEntity;
import dev.jstech.computers.blockentity.MainframeBlockEntity;
import dev.jstech.computers.blockentity.PersonalComputerBlockEntity;
import dev.jstech.computers.machine.DriveTable;
import dev.jstech.computers.machine.MachinePrograms;
import dev.jstech.computers.machine.NetworkPathResolver;
import dev.jstech.computers.operation.MoveLabels;
import dev.jstech.computers.os.IOsHost;
import dev.jstech.computers.os.KernelDef;
import dev.jstech.computers.os.OsDef;
import dev.jstech.computers.os.OsRegistry;
import dev.jstech.computers.os.fs.DiskFilesystem;
import dev.jstech.computers.program.cli.DosPath;
import dev.jstech.computers.program.cli.ICliComputer;
import dev.jstech.computers.program.iql.IqlOperation;
import dev.jstech.computers.program.iql.IqlParseResult;
import dev.jstech.computers.program.iql.IqlParser;
import dev.jstech.computers.program.iql.IqlVerb;
import dev.jstech.computers.storage.StorageKey;
import dev.jstech.computers.terminal.IComputerTerminalHost;
import dev.jstech.core.network.NetworkSystem;
import dev.jstech.core.util.ShortId;
import dev.jstech.core.uuid.NetworkUuid;
import dev.jstech.core.uuid.NodeUuid;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Backs the Command Prompt's {@link ICliComputer} facade with a real computer and its network. Every command the shell
 * runs ultimately calls one of these methods on the server; effecting verbs route through the same Mainframe operation
 * dispatch the graphical terminal uses, so the CLI is a true alternative interface, not a parallel code path.
 */
public final class ServerCliComputer implements ICliComputer {

    private final IComputerTerminalHost host;
    private final BlockEntity hostBlock;
    private final ServerLevel level;

    public ServerCliComputer(final IComputerTerminalHost host, final ServerLevel level) {
        this.host = host;
        this.hostBlock = (BlockEntity) host;
        this.level = level;
    }

    @Override
    public String name() {
        // The Mainframe has no custom name; its kind is shown by type() instead.
        if (hostBlock instanceof IOsHost computer) {
            return computer.customName();
        }
        return "";
    }

    @Override
    public String type() {
        if (hostBlock instanceof MainframeBlockEntity) {
            return "Mainframe";
        }
        if (hostBlock instanceof CraftingComputerBlockEntity) {
            return "Crafting Computer";
        }
        if (hostBlock instanceof PersonalComputerBlockEntity) {
            return "Personal Computer";
        }
        if (hostBlock instanceof dev.jstech.computers.blockentity.ClusterManagementComputerBlockEntity) {
            return "Cluster Management Computer";
        }
        return "Computer";
    }

    @Override
    public Object hostBlock() {
        return hostBlock;
    }

    @Override
    public String nodeId() {
        final NodeUuid node;
        if (hostBlock instanceof IOsHost computer) {
            node = computer.nodeUuid();
        } else if (hostBlock instanceof MainframeBlockEntity mainframe) {
            node = mainframe.nodeUuid();
        } else {
            node = null;
        }
        return node == null ? "------" : ShortId.of(node.asString());
    }

    @Override
    public boolean running() {
        return host.computerRunning();
    }

    @Override
    public long cpuCapacity() {
        return host.orchestrationCapacity();
    }

    @Override
    public long ramBuffer() {
        return host.computerRamBuffer();
    }

    @Override
    public boolean onNetwork() {
        return networkReads().online();
    }

    @Override
    public String networkId() {
        return networkReads().id();
    }

    @Override
    public boolean isMainframe() {
        return networkReads().isMainframe();
    }

    // Remote shells

    /**
     * Every machine on this network a remote shell can reach, keyed by host name: the Mainframe, the
     * personal computers, and the servers in their racks. The local machine is left out, since you cannot
     * ssh into the terminal you are already sitting at.
     */
    /** The reachable machines by host name, for callers outside the CLI (Remote Control's list). */
    public Map<String, BlockEntity> remoteMachines() {
        return reachableMachines();
    }

    private Map<String, BlockEntity> reachableMachines() {
        final Map<String, BlockEntity> out = new java.util.LinkedHashMap<>();
        final NetworkUuid network = host.networkUuid();
        if (network == null) {
            return out;
        }
        final NetworkSystem system = NetworkSystem.get(level);
        final java.util.List<BlockEntity> candidates = new ArrayList<>();
        final MainframeBlockEntity mainframe = mainframe(network);
        if (mainframe != null) {
            candidates.add(mainframe);
        }
        for (final NetworkSystem.PersonalComputerNode pc : system.personalComputersOf(network)) {
            if (level.getBlockEntity(net.minecraft.core.BlockPos.of(pc.pos()))
                    instanceof PersonalComputerBlockEntity be) {
                candidates.add(be);
            }
        }
        for (final dev.jstech.core.network.ServerNode server : system.serversOf(network)) {
            system.locationOf(server.nodeUuid()).ifPresent(loc -> {
                if (level.getBlockEntity(net.minecraft.core.BlockPos.of(loc.rackPos()))
                        instanceof dev.jstech.computers.blockentity
                                .ServerRackBlockEntity rack) {
                    candidates.add(rack);
                }
            });
        }
        for (final BlockEntity candidate : candidates) {
            if (candidate == hostBlock || !(candidate instanceof IComputerTerminalHost terminalHost)) {
                continue;
            }
            final String hostname = new ServerCliComputer(terminalHost, level).hostname();
            // A duplicate host name keeps the first machine found, the way a name collision would.
            out.putIfAbsent(hostname, candidate);
        }
        return out;
    }

    @Override
    public List<RemoteHost> reachableHosts() {
        final List<RemoteHost> hosts = new ArrayList<>();
        reachableMachines().forEach((hostname, machine) -> {
            final ServerCliComputer remote = new ServerCliComputer((IComputerTerminalHost) machine, level);
            final dev.jstech.computers.os.OsDef os = remote.installedOsDef();
            hosts.add(new RemoteHost(hostname, remote.name(), remote.nodeId(),
                    os == null ? "" : os.displayName(), remote.type(), remote.running()));
        });
        return hosts;
    }

    /**
     * Resolves what the player typed to one machine. A host name, the machine's own name and the
     * head of its node id all address it; an OS name works too, but only while it picks out exactly
     * one machine, since two Debian servers make "debian" ambiguous, and saying so is more useful than
     * guessing.
     */
    private Map<String, BlockEntity> matchMachines(final String wanted) {
        final String needle = wanted == null ? "" : wanted.trim().toLowerCase(java.util.Locale.ROOT);
        final Map<String, BlockEntity> matches = new java.util.LinkedHashMap<>();
        if (needle.isEmpty()) {
            return matches;
        }
        reachableMachines().forEach((hostname, machine) -> {
            final ServerCliComputer remote = new ServerCliComputer((IComputerTerminalHost) machine, level);
            final dev.jstech.computers.os.OsDef os = remote.installedOsDef();
            final boolean hit = hostname.equalsIgnoreCase(needle)
                    || remote.name().equalsIgnoreCase(needle)
                    || remote.nodeId().equalsIgnoreCase(needle)
                    || (os != null && (os.displayName().equalsIgnoreCase(needle)
                            || os.id().getPath().equalsIgnoreCase(needle)));
            if (hit) {
                matches.put(hostname, machine);
            }
        });
        return matches;
    }

    @Override
    public OpResult sshConnect(final String hostname) {
        final ComputerConsoleState console = host.console();
        if (console == null) {
            return OpResult.fail("ssh: this terminal keeps no session");
        }
        final Map<String, BlockEntity> matches = matchMachines(hostname);
        if (matches.isEmpty()) {
            return OpResult.fail("ssh: " + hostname + ": host not found on this network");
        }
        if (matches.size() > 1) {
            return OpResult.fail("ssh: " + hostname + " matches " + matches.size() + " machines ("
                    + String.join(", ", matches.keySet()) + ") - use the host name or node id");
        }
        final BlockEntity target = matches.values().iterator().next();
        final ServerCliComputer remote = new ServerCliComputer((IComputerTerminalHost) target, level);
        if (!remote.running()) {
            return OpResult.fail("ssh: connect to host " + hostname + ": machine is powered off");
        }
        console.setSshTarget(target.getBlockPos().asLong());
        return OpResult.ok("Connected to " + hostname + ". Type exit to return.");
    }

    @Override
    public OpResult sshDisconnect() {
        final ComputerConsoleState console = host.console();
        if (console == null || console.sshTarget() == null) {
            return OpResult.fail("exit: not connected - close the window to leave this terminal");
        }
        console.setSshTarget(null);
        return OpResult.ok("Connection closed.");
    }

    @Override
    public String sshSession() {
        final ComputerConsoleState console = host.console();
        return console == null || console.sshTarget() == null ? "" : hostname();
    }

    @Override
    public NetSummary network() {
        return networkReads().summary();
    }

    @Override
    public List<StoredItem> query(final dev.jstech.computers.program.iql.IIqlCondition where,
                                  final String server, final int limit) {
        return networkReads().query(where, server, limit);
    }

    @Override
    public List<StoredItem> queryObject(final String object,
                                        final dev.jstech.computers.program.iql.IIqlCondition where,
                                        final String server, final int limit) {
        return networkReads().queryObject(object, where, server, limit);
    }

    @Override
    public List<ServerUse> servers() {
        return networkReads().servers();
    }

    @Override
    public ServerUse networkUse() {
        return networkReads().use();
    }

    @Override
    public List<Holding> find(final String item) {
        return networkReads().find(item);
    }

    @Override
    public OpResult select(final String item, final long quantity) {
        return select(item, quantity, MoveLabels.SHELL);
    }

    @Override
    public OpResult select(final String item, final long quantity, final String origin) {
        return operations().select(item, quantity, origin);
    }

    @Override
    public List<OperationStat> operationStats() {
        return mainframeStats().work();
    }

    @Override
    public int peakOperationsToday() {
        return mainframeStats().peakToday();
    }

    @Override
    public OpResult repriorityOperation(final String id, final String priority) {
        return operations().reprioritise(id, priority);
    }

    public OpResult cancelOperation(final String id) {
        return operations().cancel(id);
    }

    @Override
    public OpResult insert(final String item, final long quantity) {
        return insert(item, quantity, dev.jstech.core.operation.OperationPriority.DEFAULT, MoveLabels.SHELL);
    }

    @Override
    public OpResult insert(final String item, final long quantity, final String origin) {
        return insert(item, quantity, dev.jstech.core.operation.OperationPriority.DEFAULT, origin);
    }

    private OpResult insert(final String item, final long quantity,
                            final dev.jstech.core.operation.OperationPriority priority,
                            final String origin) {
        return operations().insert(item, quantity, priority, origin);
    }

    @Override
    public OpResult craft(final String item, final long quantity) {
        return craft(item, quantity, dev.jstech.core.operation.OperationPriority.DEFAULT, MoveLabels.SHELL);
    }

    @Override
    public OpResult craft(final String item, final long quantity, final String origin) {
        return craft(item, quantity, dev.jstech.core.operation.OperationPriority.DEFAULT, origin);
    }

    private OpResult craft(final String item, final long quantity,
                           final dev.jstech.core.operation.OperationPriority priority,
                           final String origin) {
        return operations().craft(item, quantity, priority, origin);
    }

    @Override
    public OpResult lock(final String item, final long quantity) {
        return operations().lock(item, quantity);
    }

    @Override
    public OpResult unlock(final String item) {
        return operations().unlock(item);
    }

    @Override
    public List<StoredItem> locks() {
        return operations().locks();
    }

    @Override
    public List<ActiveOp> activeOps() {
        return operations().list();
    }

    @Override
    public OpResult maintenance(final String action) {
        return operations().maintenance(action);
    }

    @Override
    public List<String> peripherals() {
        if (hostBlock instanceof dev.jstech.core.peripheral.IPeripheralOwnerSupport owner) {
            final List<String> rows = new ArrayList<>();
            for (final long endpoint : owner.peripheralEndpoints()) {
                final BlockPos pos = BlockPos.of(endpoint);
                rows.add(level.getBlockState(pos).getBlock().getName().getString()
                        + " @ " + pos.getX() + "," + pos.getY() + "," + pos.getZ());
            }
            return rows;
        }
        return List.of();
    }

    @Override
    public List<ProgramInfo> programs() {
        final List<ProgramInfo> out = new ArrayList<>();
        /*
         * Pre-installed programs the host's platform supports, so an MC-DOS listing does not show the Frames
         * desktop apps (which are pre-installed only on the Frames platform).
         */
        final dev.jstech.computers.os.Platform platform = hostPlatform();
        for (final dev.jstech.computers.os.ProgramSpec spec : Programs.installed()) {
            if (platform == null || spec.platforms().contains(platform)) {
                out.add(new ProgramInfo(spec.commandName(), spec.id().toString()));
            }
        }
        final ComputerConsoleState console = host.console();
        if (console != null) {
            for (final String id : console.installed()) {
                final var program = Programs.get(ResourceLocation.tryParse(id));
                if (program != null && !program.preinstalled()) {
                    out.add(new ProgramInfo(program.commandName(), program.id().toString()));
                }
            }
        }
        return out;
    }

    /** The platform of the OS installed on the host computer, or {@code null} when it cannot be resolved. */
    private dev.jstech.computers.os.Platform hostPlatform() {
        if (host instanceof dev.jstech.computers.os.IOsHost oc) {
            final dev.jstech.computers.os.OsDef os =
                    dev.jstech.computers.os.OsRegistry.getOs(oc.installedOsId());
            return os == null ? null : os.platform();
        }
        return null;
    }

    @Override
    public OpResult install(final String programId) {
        final ResourceLocation location = ResourceLocation.tryParse(
                programId.contains(":") ? programId.toLowerCase(java.util.Locale.ROOT)
                        : "jsc:" + programId.toLowerCase(java.util.Locale.ROOT));
        final dev.jstech.computers.os.ProgramSpec program =
                location == null ? null : Programs.get(location);
        if (program == null) {
            return OpResult.fail("no such program: " + programId);
        }
        if (program.id().equals(Programs.IQL_ENGINE)) {
            // The Engine is a service on the Mainframe, not a console-local app, so install it there.
            return engineControl("install");
        }
        /*
         * Installing is something the machine does over time, from the disc in a linked drive. Whether
         * it can, and why not, is decided in one place for every way of asking, so the prompt says
         * exactly what the Setup window on a desktop would.
         */
        final dev.jstech.computers.os.media.MediaFormat medium = installMediumFormatFor(program.id());
        if (medium == null) {
            return OpResult.fail(program.commandName() + " needs its install disc in a linked drive");
        }
        final dev.jstech.computers.os.IOsHost machine = osHost();
        if (machine == null) {
            return OpResult.fail("this computer cannot store installed programs");
        }
        final java.util.Optional<String> refusal = dev.jstech.computers.os.install.SetupRunner.begin(
                machine, level, hostBlock.getBlockPos(), program, medium, false,
                dev.jstech.computers.os.install.SetupJob.VIA_INSTALL);
        return refusal.map(OpResult::fail)
                .orElseGet(() -> OpResult.ok("Setting up " + program.commandName() + " from "
                        + mediumDriveName(medium) + " ..."));
    }

    /** The machine as the thing that installs programs, whichever of the two handles this prompt holds. */
    @org.jetbrains.annotations.Nullable
    private dev.jstech.computers.os.IOsHost osHost() {
        if (host instanceof dev.jstech.computers.os.IOsHost fromHost) {
            return fromHost;
        }
        return hostBlock instanceof dev.jstech.computers.os.IOsHost fromBlock ? fromBlock : null;
    }

    /** What the disc a program comes from is called at a prompt. */
    private static String mediumDriveName(final dev.jstech.computers.os.media.MediaFormat medium) {
        return switch (medium) {
            case FLOPPY -> "the floppy";
            case CD -> "the CD";
            case DVD -> "the DVD";
            case USB -> "the USB drive";
        };
    }

    /** The format of the disc a program's installer sits on in a linked drive, or null when none does. */
    @org.jetbrains.annotations.Nullable
    private dev.jstech.computers.os.media.MediaFormat installMediumFormatFor(final ResourceLocation programId) {
        if (!(host instanceof dev.jstech.computers.os.IOsHost computer)) {
            return null;
        }
        for (final long endpoint : computer.linkedEndpoints()) {
            if (level.getBlockEntity(net.minecraft.core.BlockPos.of(endpoint))
                    instanceof dev.jstech.computers.os.media.MediaReaderBlockEntity reader
                    && reader.insertedKind() == dev.jstech.computers.os.media.MediaKind.PROGRAM_INSTALL
                    && programId.equals(reader.insertedPayload())) {
                return reader.insertedFormat();
            }
        }
        return null;
    }

    /**
     * Refuses a program the machine is too old to run, or {@code null} when the era is fine. Software
     * cannot predate its hardware generation: a desktop of the 2010s does not install on a machine of
     * the 1990s, however much disk it has free. Both install paths (the package manager and the install
     * medium) go through this, so neither is a way around the rule.
     */
    @org.jetbrains.annotations.Nullable
    private OpResult eraGate(final dev.jstech.computers.os.ProgramSpec spec) {
        if (spec.minEra() == dev.jstech.core.tier.HardwareEra.VINTAGE) {
            return null; // no requirement
        }
        /*
         * displayEra, not installedEra: a Vintage or Legacy chassis IS that generation whatever board
         * sits in it, and that chassis is the only way a machine of an older era exists right now.
         */
        final dev.jstech.core.tier.HardwareEra era =
                hostBlock instanceof IOsHost computer ? computer.displayEra() : null;
        if (era != null && dev.jstech.computers.os.OsGating.canInstall(spec.minEra(), era)) {
            return null;
        }
        final String needed = spec.minEra().name();
        return OpResult.fail(spec.commandName() + " needs "
                + (needed.charAt(0) + needed.substring(1).toLowerCase(java.util.Locale.ROOT))
                + " hardware or later");
    }

    @Override
    public OpResult engineControl(final String action) {
        return iql().control(action);
    }

    @Override
    public java.util.List<ServiceStatus> services() {
        final MainframeBlockEntity mainframe = mainframe(host.networkUuid());
        if (mainframe == null) {
            return java.util.List.of();
        }
        // The Mirror's own state still lives here, with the package commands it belongs to.
        return java.util.List.of(new ServiceStatus("IQL Engine", iql().state()),
                new ServiceStatus("Mirror", mirrorState(mainframe)));
    }

    @Override
    public boolean iqlEngineInstalled() {
        return iql().installed();
    }

    @Override
    public OpResult execute(final IqlOperation op) {
        return switch (op.verb()) {
            case SELECT -> iql().select(op);
            case INSERT -> iql().insert(op);
            case CRAFT -> craft(op.item(), op.quantity(), op.priority(), MoveLabels.IQL);
            case DELETE -> iql().destroy(op, "DELETE");
            case DROP -> iql().destroy(op, "DROP");
            case MOVE -> iql().move(op);
            case LOCK -> lock(op.item(), op.quantity());
            case UNLOCK -> unlock(op.item());
            case ANALYZE -> maintenance("analyze");
            case VACUUM -> maintenance("vacuum");
            case REINDEX -> maintenance("reindex");
            case QUERY, COUNT -> OpResult.fail("a read does not run as an operation");
        };
    }

    // helpers

    private MainframeBlockEntity mainframe(final NetworkUuid net) {
        if (net == null) {
            return null;
        }
        return NetworkSystem.get(level).mainframePositionOf(net)
                .map(pos -> level.getBlockEntity(BlockPos.of(pos)) instanceof MainframeBlockEntity mf ? mf : null)
                .orElse(null);
    }

    /** The storage key an item name stands for, as the shell reads names, or null for one it does not know. */
    public static StorageKey itemKey(final String name) {
        return resolveKey(name);
    }

    private static StorageKey resolveKey(final String name) {
        final Item item = resolveItem(name);
        return item == null ? null : StorageKey.of(item);
    }

    private static Item resolveItem(final String name) {
        if (name == null || name.isBlank()) {
            return null;
        }
        final String id = name.contains(":") ? name : "minecraft:" + name;
        final ResourceLocation location = ResourceLocation.tryParse(id.toLowerCase(java.util.Locale.ROOT));
        if (location == null) {
            return null;
        }
        return BuiltInRegistries.ITEM.getOptional(location).orElse(null);
    }

    // filesystem

    /** A path resolved against the current location: its drive letter, that drive, and the path on it. */
    private record Resolved(char drive, DriveTable.Drive ctx, String path) {}

    /** The drive with that letter, or {@code null} when the letter is not mapped. */
    private DriveTable.Drive diskFor(final char drive) {
        return DriveTable.of(hostBlock, level).find(drive);
    }

    /** Resolves a DOS path argument against the current location, mapping it to the target drive's context. */
    private Resolved resolve(final String input) {
        final DosPath.Location loc = DosPath.resolve(currentLocation(), input);
        return new Resolved(loc.drive(), diskFor(loc.drive()), loc.storagePath());
    }

    /** The error for an unmapped drive. */
    private FsResult driveError(final char drive) {
        return DriveTable.missing(drive);
    }

    /** The error for a mapped but empty drive (a media reader with no medium inserted). */
    private static FsResult notReady(final char drive) {
        return DriveTable.notReady(drive);
    }


    /** The shell family of the OS installed on {@code host} (DOS when it has no OS or is not a computer). */
    public static dev.jstech.computers.os.ShellFamily shellFamilyOf(final Object host) {
        if (host instanceof IOsHost computer) {
            final OsDef os = computer.installedOs();
            final KernelDef kernel = os == null ? null : OsRegistry.getKernel(os.kernelId());
            if (kernel != null) {
                return kernel.shellFamily();
            }
        }
        return dev.jstech.computers.os.ShellFamily.DOS;
    }

    @Override
    public dev.jstech.computers.os.ShellFamily shellFamily() {
        return shellFamilyOf(hostBlock);
    }

    // Set by the reboot verb during a command run; the payload handler reads it once the shell returns.
    private boolean firmwareReboot;
    private boolean reboot;

    @Override
    public void requestFirmwareReboot() {
        this.firmwareReboot = true;
    }

    @Override
    public boolean firmwareRebootRequested() {
        return firmwareReboot;
    }

    @Override
    public void requestReboot() {
        this.reboot = true;
    }

    @Override
    public boolean rebootRequested() {
        return reboot;
    }

    // packages: the Linux package managers over the network's Mirror service

    private OsDef installedOsDef() {
        return hostBlock instanceof IOsHost c ? c.installedOs() : null;
    }

    @Override
    public dev.jstech.computers.os.PackageManagerKind packageManager() {
        final OsDef os = installedOsDef();
        return os == null ? dev.jstech.computers.os.PackageManagerKind.NONE : os.packageManager();
    }

    /** The network's Mainframe when its Mirror service is serving, else null. */
    private MainframeBlockEntity mirrorMainframe() {
        final MainframeBlockEntity mf = mainframe(host.networkUuid());
        return mf != null && mf.isMirrorActive() ? mf : null;
    }

    @Override
    public boolean mirrorReachable() {
        return mirrorMainframe() != null;
    }

    /** Moves finished source builds into the installed set (lazy: runs whenever packages are touched). */
    private void settleBuilds() {
        final dev.jstech.computers.program.ComputerConsoleState console = host.console();
        if (console != null && !console.settleBuilds(level.getGameTime()).isEmpty()) {
            hostBlock.setChanged();
        }
    }

    @Override
    public java.util.List<String> drainBuildNotices() {
        settleBuilds();
        final java.util.List<String> out = new ArrayList<>();
        // What the machine itself has to say goes first: it happened as the world loaded, before any build finished.
        if (hostBlock instanceof dev.jstech.computers.blockentity.AbstractComputerBlockEntity computer) {
            for (final String notice : computer.programs().drainNotices()) {
                out.add(">>> " + notice);
            }
        }
        final dev.jstech.computers.program.ComputerConsoleState console = host.console();
        final java.util.List<String> finished = console == null ? java.util.List.of() : console.drainFinishedBuilds();
        if (!finished.isEmpty()) {
            hostBlock.setChanged();
        }
        for (final String id : finished) {
            final dev.jstech.computers.os.ProgramSpec spec =
                    OsRegistry.getProgram(net.minecraft.resources.ResourceLocation.tryParse(id));
            out.add(">>> " + (spec != null ? spec.commandName() : id) + ": build finished, package installed");
        }
        return out;
    }

    /** Whether the named program is present on this computer (console install, or a Mainframe service flag). */
    private boolean hasPackage(final dev.jstech.computers.os.ProgramSpec spec) {
        if (hostBlock instanceof MainframeBlockEntity mf) {
            switch (spec.id().getPath()) {
                case "iqlengine" -> {
                    return mf.isIqlEngineInstalled();
                }
                case "automation_engine" -> {
                    return mf.isAutomationEngineInstalled();
                }
                case "mirror" -> {
                    return mf.isMirrorInstalled();
                }
                default -> {
                }
            }
        }
        final dev.jstech.computers.program.ComputerConsoleState console = host.console();
        return console != null && console.isInstalled(spec.id().toString());
    }

    /** Every program the mirror can serve a Linux computer: the specs that list the Linux platform. */
    /**
     * The packages a manager on THIS computer can offer: everything installable that runs on the
     * platform it is running. Reading the platform (rather than assuming Linux) is what lets the
     * Frames manager see the Frames-only software the mirror serves.
     */
    private java.util.List<dev.jstech.computers.os.ProgramSpec> mirrorPackages() {
        final dev.jstech.computers.os.OsDef os = installedOsDef();
        final dev.jstech.computers.os.Platform platform =
                os == null ? dev.jstech.computers.os.Platform.LINUX : os.platform();
        final java.util.List<dev.jstech.computers.os.ProgramSpec> out = new ArrayList<>();
        for (final dev.jstech.computers.os.ProgramSpec spec : OsRegistry.programs()) {
            if (spec.installable() && spec.platforms().contains(platform)) {
                out.add(spec);
            }
        }
        return out;
    }

    @Override
    public java.util.List<PackageInfo> packagesAvailable() {
        settleBuilds();
        if (mirrorMainframe() == null) {
            return java.util.List.of();
        }
        final dev.jstech.computers.program.ComputerConsoleState console = host.console();
        final java.util.List<PackageInfo> out = new ArrayList<>();
        for (final dev.jstech.computers.os.ProgramSpec spec : mirrorPackages()) {
            final boolean building = console != null && console.pendingBuilds().containsKey(spec.id().toString());
            out.add(new PackageInfo(spec.commandName(), spec.displayName()
                    + (spec.kind() == dev.jstech.computers.os.ProgramKind.SERVICE ? " (service)" : ""),
                    hasPackage(spec), building));
        }
        // Then whatever players on this network have published, marked as theirs.
        final MainframeBlockEntity mirror = mirrorMainframe();
        if (mirror != null) {
            for (final var shelved : mirror.shelvedPackages().entrySet()) {
                final dev.jstech.computers.sigma.pack.Packed packed =
                        dev.jstech.computers.sigma.pack.Packed.read(shelved.getValue());
                if (packed == null) {
                    continue;
                }
                final String about = packed.manifest().about();
                out.add(new PackageInfo(shelved.getKey(),
                        (about.isBlank() ? packed.manifest().label() : about)
                                + " - " + packed.manifest().house(),
                        false, false, true));
            }
        }
        return out;
    }

    @Override
    public OpResult publishPackage(final String path) {
        final MainframeBlockEntity mirror = mirrorMainframe();
        if (mirror == null) {
            return OpResult.fail("could not resolve mirror:// - connect this computer to a network whose"
                    + " Mainframe runs the Mirror service");
        }
        final FsResult read = readFile(path);
        if (!read.ok()) {
            return OpResult.fail(read.message());
        }
        final dev.jstech.computers.sigma.pack.Packed packed =
                dev.jstech.computers.sigma.pack.Packed.read(read.message());
        if (packed == null) {
            return OpResult.fail(path + ": this is not a package (build one with 'sgpack build')");
        }
        final java.util.List<String> wrong = packed.problems();
        if (!wrong.isEmpty()) {
            return OpResult.fail(path + ": " + wrong.getFirst());
        }
        final String name = packed.manifest().name();
        final boolean replacing = mirror.shelvedPackage(name) != null;
        if (!mirror.shelve(name, read.message())) {
            return OpResult.fail("the Mirror is full (" + MainframeBlockEntity.SHELF_MAX + " packages)");
        }
        return OpResult.ok((replacing ? "replaced " : "published ") + packed.manifest().label()
                + " on the Mirror");
    }

    @Override
    public OpResult unpublishPackage(final String name) {
        final MainframeBlockEntity mirror = mirrorMainframe();
        if (mirror == null) {
            return OpResult.fail("could not resolve mirror://");
        }
        if (!mirror.unshelve(name == null ? "" : name.trim())) {
            return OpResult.fail("the Mirror is not serving " + name);
        }
        return OpResult.ok("took " + name + " off the Mirror");
    }

    /**
     * The folder a player's package is unpacked into.
     *
     * <p>One folder each, named after the package, so two of them cannot quietly overwrite each other's
     * files and removing one takes exactly its own files with it.
     */
    private static final String COMMUNITY_DIR = "PROGRAMS";

    /**
     * Installs a package a player published, if that is what this name is.
     *
     * <p>Returns null when the name belongs to something else, so the usual path carries on.
     */
    @org.jetbrains.annotations.Nullable
    private OpResult installCommunity(final String wanted) {
        final MainframeBlockEntity mirror = mirrorMainframe();
        final String held = mirror == null ? null : mirror.shelvedPackage(wanted);
        if (held == null) {
            return null;
        }
        final dev.jstech.computers.sigma.pack.Packed packed =
                dev.jstech.computers.sigma.pack.Packed.read(held);
        if (packed == null || !packed.problems().isEmpty()) {
            return OpResult.fail(wanted + ": the Mirror's copy of this package is not readable");
        }
        if (!dev.jstech.computers.program.cli.SigmaCommands.installed(this,
                dev.jstech.computers.program.cli.SigmaCommands.RUNTIME)) {
            return OpResult.fail(wanted + " is a Σ# program; install sigma first");
        }
        final dev.jstech.computers.program.ComputerConsoleState console = host.console();
        if (console == null) {
            return OpResult.fail("no system disk to install onto");
        }
        // Its own folder, made before anything is written into it.
        final String folder = COMMUNITY_DIR + "/" + wanted;
        makeDir(COMMUNITY_DIR);
        if (!makeDir(folder).ok() && listDisk(folder).entries().isEmpty()) {
            return OpResult.fail(wanted + ": this system has no folders to install into");
        }
        for (final var file : packed.files().entrySet()) {
            final FsResult written = writeFile(folder + "/" + file.getKey(), file.getValue());
            if (!written.ok()) {
                return OpResult.fail(wanted + ": " + written.message());
            }
        }
        console.addCommunity(new dev.jstech.computers.program.ComputerConsoleState.Community(
                wanted, packed.manifest().version(), packed.manifest().house(),
                packed.manifest().icon(), folder + "/" + packed.manifest().entry()));
        hostBlock.setChanged();
        return OpResult.ok("installed " + packed.manifest().label() + " into " + folder);
    }

    @Override
    public OpResult packageInstall(final String name) {
        settleBuilds();
        final dev.jstech.computers.os.PackageManagerKind manager = packageManager();
        if (manager == dev.jstech.computers.os.PackageManagerKind.NONE) {
            return OpResult.fail("this system installs programs from install media, not a package manager");
        }
        final OpResult community =
                installCommunity(name == null ? "" : name.trim().toLowerCase(java.util.Locale.ROOT));
        if (community != null) {
            return community;
        }
        if (mirrorMainframe() == null) {
            return OpResult.fail("could not resolve mirror:// - connect this computer to a network whose Mainframe"
                    + " runs the Mirror service");
        }
        dev.jstech.computers.os.ProgramSpec spec = null;
        final String wanted = name == null ? "" : name.trim().toLowerCase(java.util.Locale.ROOT);
        for (final dev.jstech.computers.os.ProgramSpec candidate : mirrorPackages()) {
            if (candidate.commandName().equalsIgnoreCase(wanted) || candidate.id().getPath().equalsIgnoreCase(wanted)) {
                spec = candidate;
                break;
            }
        }
        if (spec == null) {
            return OpResult.fail("unable to locate package " + wanted);
        }
        final OpResult tooOld = eraGate(spec);
        if (tooOld != null) {
            return tooOld;
        }
        if (spec.hostScope() == dev.jstech.computers.os.HostScope.MAINFRAME
                && !(hostBlock instanceof MainframeBlockEntity)) {
            return OpResult.fail(spec.commandName() + " only installs on the Mainframe");
        }
        if (spec.hostScope() == dev.jstech.computers.os.HostScope.SERVER
                && !(hostBlock instanceof dev.jstech.computers.blockentity
                        .ServerRackBlockEntity)) {
            return OpResult.fail(spec.commandName() + " only installs on a server in a rack");
        }
        if (spec.hostScope() == dev.jstech.computers.os.HostScope.CLUSTER_MANAGEMENT_COMPUTER
                && !(hostBlock instanceof dev.jstech.computers.blockentity
                        .ClusterManagementComputerBlockEntity)) {
            return OpResult.fail(spec.commandName() + " only installs on a Cluster Management Computer");
        }
        if (hasPackage(spec)) {
            return OpResult.ok(spec.commandName() + " is already the newest version");
        }
        // Re-running emerge on a package still compiling reports the build instead of restarting it from zero.
        final Long readyAt = host.console() == null ? null : host.console().pendingBuilds().get(spec.id().toString());
        if (readyAt != null) {
            final long left = Math.max(0L, readyAt - level.getGameTime());
            return OpResult.ok(">>> " + spec.commandName() + " is already compiling (about " + (left / 20) + "s left)");
        }
        // A Mainframe service switches its flag on directly (a prebuilt daemon, so no source build either).
        if (hostBlock instanceof MainframeBlockEntity mf
                && spec.kind() == dev.jstech.computers.os.ProgramKind.SERVICE) {
            final boolean done = switch (spec.id().getPath()) {
                case "iqlengine" -> mf.installIqlEngine();
                case "automation_engine" -> mf.installAutomationEngine();
                case "mirror" -> mf.installMirror();
                default -> host.console() != null && host.console().install(spec.id().toString());
            };
            hostBlock.setChanged();
            return done ? OpResult.ok("Setting up " + spec.commandName() + " ... done")
                    : OpResult.fail(spec.commandName() + " could not be set up");
        }
        if (hostBlock instanceof IOsHost oc
                && !OsRegistry.canInstallProgram(oc.installedOsId(), spec.id(), oc.maxCpuMhz(), oc.totalVramMb(),
                        oc.systemDiskFreeMb())) {
            return OpResult.fail(spec.commandName() + ": unmet requirements (hardware or free disk space)");
        }
        final dev.jstech.computers.program.ComputerConsoleState console = host.console();
        if (console == null) {
            return OpResult.fail("this computer cannot store installed programs");
        }
        if (manager.compilesFromSource()) {
            final long ticks = buildTicks(spec);
            console.startBuild(spec.id().toString(), level.getGameTime() + ticks, ticks);
            hostBlock.setChanged();
            return OpResult.ok(">>> Emerging " + spec.commandName() + " ... compiling (about " + (ticks / 20) + "s)");
        }
        /*
         * A package from the Mirror is fetched over the network and set up over time, the way the same
         * program from a disc is; the manager's own gates above have already said it may.
         */
        final dev.jstech.computers.os.IOsHost machine = osHost();
        if (machine == null) {
            return OpResult.fail("this computer cannot store installed programs");
        }
        final dev.jstech.computers.os.ProgramSpec fetched = spec;
        final java.util.Optional<String> refusal = dev.jstech.computers.os.install.SetupRunner.begin(
                machine, level, hostBlock.getBlockPos(), fetched, null, false, manager.command());
        return refusal.map(OpResult::fail).orElseGet(() -> OpResult.ok(fetchLines(manager, fetched)));
    }

    /**
     * What a package manager prints before the download starts, in its own words.
     *
     * <p>Each of them has a voice a player who has used the real one knows on sight, and the lines are
     * that voice: what was resolved, what will be installed, how big it is, and where it comes from.
     * They are one message, line by line, and the bar the machine draws afterwards follows them.
     */
    private String fetchLines(final dev.jstech.computers.os.PackageManagerKind manager,
                              final dev.jstech.computers.os.ProgramSpec spec) {
        final String pkg = spec.commandName();
        final String ver = dev.jstech.computers.os.ProgramVersions.of(spec.id());
        final int mb = spec.minDiskMb();
        return switch (manager) {
            case APT -> String.join("\n",
                    "Reading package lists... Done",
                    "Building dependency tree... Done",
                    "The following NEW packages will be installed:",
                    "  " + pkg,
                    "Need to get " + mb + " MB of archives.",
                    "Get:1 mirror://" + mirrorHostname() + " stable/main " + pkg + " " + ver + " [" + mb + " MB]");
            case DNF -> String.join("\n",
                    "Last metadata expiration check: 0:00:01 ago.",
                    "Dependencies resolved.",
                    "Installing:  " + pkg + "  x86_64  " + ver + "  mirror  " + mb + " MB",
                    "Downloading Packages:");
            case PACMAN -> String.join("\n",
                    "resolving dependencies...",
                    "looking for conflicting packages...",
                    "Packages (1) " + pkg + "-" + ver,
                    "Total Download Size: " + mb + ".00 MiB",
                    ":: Retrieving packages...");
            default -> "Fetching " + pkg + " " + ver + " from mirror://" + mirrorHostname() + " [" + mb + " MB]";
        };
    }

    /** The same, for a removal: what the manager says before it takes the package off. */
    private static String removeLines(final dev.jstech.computers.os.PackageManagerKind manager,
                                      final dev.jstech.computers.os.ProgramSpec spec) {
        final String pkg = spec.commandName();
        final String ver = dev.jstech.computers.os.ProgramVersions.of(spec.id());
        return switch (manager) {
            case APT -> String.join("\n",
                    "Reading package lists... Done",
                    "Building dependency tree... Done",
                    "The following packages will be REMOVED:",
                    "  " + pkg,
                    "After this operation, " + spec.minDiskMb() + " MB disk space will be freed.",
                    "Removing " + pkg + " (" + ver + ") ...");
            case DNF -> String.join("\n",
                    "Dependencies resolved.",
                    "Removing:  " + pkg + "  x86_64  " + ver,
                    "Running transaction");
            case PACMAN -> String.join("\n",
                    "checking dependencies...",
                    "Packages (1) " + pkg + "-" + ver,
                    ":: Removing " + pkg + " ...");
            default -> "Removing " + pkg + " ...";
        };
    }

    /** The name the Mirror's Mainframe goes by in a package line, or the plain word when it has none. */
    private String mirrorHostname() {
        final MainframeBlockEntity mirror = mirrorMainframe();
        final String name = mirror == null ? "" : mirror.console() == null ? "" : mirror.console().computerName();
        return name == null || name.isBlank() ? "mainframe" : name;
    }

    /** The build every package the Mirror serves is currently at: the mod's own version. */
    public static String modVersion() {
        return net.neoforged.fml.ModList.get()
                .getModContainerById(dev.jstech.computers.JsComputers.MODID)
                .map(container -> container.getModInfo().getVersion().toString())
                .orElse("0");
    }

    @Override
    public OpResult packageUpdate() {
        settleBuilds();
        final dev.jstech.computers.os.PackageManagerKind manager = packageManager();
        if (manager == dev.jstech.computers.os.PackageManagerKind.NONE) {
            return OpResult.fail("this system installs programs from install media, not a package manager");
        }
        final ComputerConsoleState console = host.console();
        if (console == null) {
            return OpResult.fail("this computer cannot store installed programs");
        }
        if (mirrorMainframe() == null) {
            return OpResult.fail("could not resolve mirror:// - connect this computer to a network whose Mainframe"
                    + " runs the Mirror service");
        }
        /*
         * Each package has a version of its own, and one installed at an older one is what an update
         * brings up. The program itself always runs the code this build ships, so an update reconciles
         * the record rather than moving files.
         */
        final java.util.List<String> outdated = new java.util.ArrayList<>();
        for (final String id : console.installed()) {
            if (!dev.jstech.computers.os.ProgramVersions.of(id).equals(console.installedVersion(id))) {
                outdated.add(id);
            }
        }
        if (outdated.isEmpty()) {
            return OpResult.ok("All packages are up to date.");
        }
        final StringBuilder lines = new StringBuilder();
        for (final String id : outdated) {
            final String version = dev.jstech.computers.os.ProgramVersions.of(id);
            console.setInstalledVersion(id, version);
            final String path = id.contains(":") ? id.substring(id.indexOf(':') + 1) : id;
            lines.append("Setting up ").append(path).append(" (").append(version).append(") ...\n");
        }
        hostBlock.setChanged();
        return OpResult.ok(lines + "Updated " + outdated.size() + " package" + (outdated.size() == 1 ? "" : "s") + ".");
    }

    /**
     * How long a source build takes: proportional to the package's footprint and inversely to the CPU clock, so
     * faster hardware compiles faster (balancing estimate, clamped to a few seconds ... half an hour).
     */
    private long buildTicks(final dev.jstech.computers.os.ProgramSpec spec) {
        final int cpu = Math.max(100, hostBlock instanceof IOsHost c ? c.maxCpuMhz() : 100);
        final long seconds = Math.max(5L, Math.min(1800L, Math.max(16L, spec.minDiskMb()) * 1000L / cpu));
        return seconds * 20L;
    }

    @Override
    public OpResult packageRemove(final String name) {
        settleBuilds();
        final String wanted = name == null ? "" : name.trim().toLowerCase(java.util.Locale.ROOT);
        final dev.jstech.computers.program.ComputerConsoleState theirs = host.console();
        if (theirs != null && theirs.communityProgram(wanted) != null) {
            // Its own files and nothing else: what was written when it was installed.
            for (final ICliComputer.FsEntry file
                    : listDisk(COMMUNITY_DIR + "/" + wanted).entries()) {
                // The listing's name is the whole last segment, extension and all.
                deleteFile(COMMUNITY_DIR + "/" + wanted + "/" + file.name());
            }
            theirs.removeCommunity(wanted);
            hostBlock.setChanged();
            return OpResult.ok("removed " + wanted);
        }
        dev.jstech.computers.os.ProgramSpec spec = null;
        for (final dev.jstech.computers.os.ProgramSpec candidate : OsRegistry.programs()) {
            if (candidate.installable()
                    && (candidate.commandName().equalsIgnoreCase(wanted)
                            || candidate.id().getPath().equalsIgnoreCase(wanted))) {
                spec = candidate;
                break;
            }
        }
        if (spec == null) {
            return OpResult.fail("unable to locate package " + wanted);
        }
        final dev.jstech.computers.program.ComputerConsoleState console = host.console();
        // A build still compiling is simply cancelled.
        if (console != null && console.cancelBuild(spec.id().toString())) {
            hostBlock.setChanged();
            return OpResult.ok(">>> " + spec.commandName() + ": build cancelled");
        }
        /*
         * Removing is the same job as installing, run backwards and quicker; a Mainframe service also
         * turns its agent off when the job ends, so nothing is left running headless.
         */
        final dev.jstech.computers.os.IOsHost machine = osHost();
        if (machine == null) {
            return OpResult.fail("this computer cannot store installed programs");
        }
        final dev.jstech.computers.os.ProgramSpec removing = spec;
        final dev.jstech.computers.os.PackageManagerKind manager = packageManager();
        final String via = manager == dev.jstech.computers.os.PackageManagerKind.NONE ? "uninstall" : manager.command();
        final java.util.Optional<String> refusal = dev.jstech.computers.os.install.SetupRunner.begin(
                machine, level, hostBlock.getBlockPos(), removing, null, true, via);
        return refusal.map(OpResult::fail).orElseGet(() -> OpResult.ok(removeLines(manager, removing)));
    }

    @Override
    public OpResult formatDrive(final char letterRaw) {
        return files().formatDrive(letterRaw);
    }

    @Override
    public boolean hasProgram(final net.minecraft.resources.ResourceLocation id) {
        final dev.jstech.computers.os.ProgramSpec spec =
                id == null ? null : OsRegistry.getProgram(id);
        return spec != null && hasPackage(spec);
    }

    @Override
    public SystemInfo systemInfo() {
        if (!(hostBlock instanceof IOsHost computer) || computer.installedOs() == null) {
            return null;
        }
        final dev.jstech.computers.os.OsDef os = computer.installedOs();
        final net.minecraft.resources.ResourceLocation desktopId = computer.installedDesktopId();
        final dev.jstech.computers.os.DesktopEnvironmentDef chrome =
                desktopId == null ? null : OsRegistry.getDesktop(desktopId);
        final ItemStack systemDisk = computer.systemDisk();
        final long totalMb = systemDisk.getItem()
                instanceof dev.jstech.computers.item.DiskItem disk
                ? disk.spec().capacityItems() * StorageKey.MB_EQ_PER_ITEM : 0L;
        final long freeMb = computer.systemDiskFreeMb();
        final dev.jstech.computers.program.ComputerConsoleState console = host.console();
        return new SystemInfo(
                os.id().getPath(),
                os.displayName(),
                shellFamily() == dev.jstech.computers.os.ShellFamily.POSIX
                        ? "Linux 6.8-jsc x86_64" : "JSC " + os.id().getPath(),
                hostname(),
                os.shellId(),
                chrome != null ? chrome.displayName() : "none (tty1)",
                computer.maxCpuMhz() + " MHz",
                (int) Math.min(Integer.MAX_VALUE, computer.ramBuffer()),
                Math.max(0L, totalMb - freeMb),
                totalMb,
                console == null ? 0 : console.installed().size(),
                level.getGameTime());
    }

    @Override
    public java.util.Map<String, Long> buildsRemaining() {
        settleBuilds();
        final dev.jstech.computers.program.ComputerConsoleState console = host.console();
        if (console == null) {
            return java.util.Map.of();
        }
        final java.util.Map<String, Long> out = new java.util.LinkedHashMap<>();
        final long now = level.getGameTime();
        console.pendingBuilds().forEach((id, readyAt) -> out.put(id, Math.max(0L, readyAt - now)));
        return out;
    }

    @Override
    public OpResult mirrorControl(final String action) {
        final MainframeBlockEntity mainframe = mainframe(host.networkUuid());
        if (mainframe == null) {
            return OpResult.fail("the network has no running Mainframe to host the Mirror");
        }
        return switch (action == null ? "" : action.toLowerCase(java.util.Locale.ROOT)) {
            case "install" -> mainframe.installMirror()
                    ? OpResult.ok("Mirror installed on the Mainframe and serving packages")
                    : OpResult.fail("the Mirror is already installed");
            case "status", "" -> OpResult.ok("Mirror: " + mirrorState(mainframe));
            default -> OpResult.fail("usage: mirror install|status");
        };
    }

    private static String mirrorState(final MainframeBlockEntity mainframe) {
        if (!mainframe.isMirrorInstalled()) {
            return "not installed";
        }
        return mainframe.isMirrorActive() ? "serving" : "installed (Mainframe off)";
    }

    @Override
    public String hostname() {
        // The host resolves its own name so the shell, the provenance rows and the remote host list agree.
        return host.hostname();
    }

    @Override
    public dev.jstech.computers.program.install.LiveInstallState liveInstall() {
        final dev.jstech.computers.program.ComputerConsoleState console = host.console();
        return console == null ? null : console.liveInstall();
    }

    @Override
    public OpResult liveRun(final String line) {
        final dev.jstech.computers.program.ComputerConsoleState console = host.console();
        final dev.jstech.computers.program.install.LiveInstallState state =
                console == null ? null : console.liveInstall();
        if (state == null || !(hostBlock instanceof IOsHost computer)) {
            return OpResult.fail("no live medium is booted");
        }
        // The devices the live system sees: every installed disk, in slot order (sda, sdb, ...).
        final java.util.List<String> devices = new ArrayList<>();
        for (int i = 0; i < computer.diskSlots(); i++) {
            if (computer.diskInSlot(i).getItem() instanceof dev.jstech.computers.item.DiskItem) {
                devices.add("sd" + (char) ('a' + i));
            }
        }
        final long kernelTicks = Math.max(5L, Math.min(1800L, 64_000L / Math.max(100, computer.maxCpuMhz()))) * 20L;
        final dev.jstech.computers.program.install.LiveInstallState.Result result =
                state.run(line, new dev.jstech.computers.program.install.LiveInstallState.Env(
                        devices, mirrorReachable(), level.getGameTime(), kernelTicks));
        hostBlock.setChanged();
        final String text = String.join("\n", result.lines());
        if (!result.complete()) {
            return result.ok() ? OpResult.ok(text) : OpResult.fail(text);
        }
        // The sequence completed: the hand-installed system lands on the chosen disk and boots first.
        final ResourceLocation osId = ResourceLocation.fromNamespaceAndPath("jsc",
                state.distro() == dev.jstech.computers.program.install.LiveInstallState.Distro.ARCH
                        ? "arch" : "gentoo");
        final int target = state.targetIndex();
        if (!computer.installOs(osId, target)) {
            return OpResult.fail(text + "\nThe installation could not be written to the disk (no space or no disk).");
        }
        computer.setBootDiskSlot(target);
        /*
         * Ask the host for the console again rather than reusing the reference taken at the top of this
         * method: writing the system may have replaced the disk stack, and the console is bound to the
         * drive it was read from. Clearing the stale binding would leave the finished live session on
         * the newly written disk, so the machine would boot straight back into the installer.
         */
        host.console().clearLiveInstall();
        hostBlock.setChanged();
        // The live medium's reboot is a real one: the shell closes, the POST replays, the new system boots.
        requestReboot();
        return OpResult.ok(text + "\nInstallation complete. Rebooting into the new system ...");
    }

    @Override
    public String prompt() {
        final dev.jstech.computers.program.install.LiveInstallState live = liveInstall();
        if (live != null) {
            return live.prompt();
        }
        if (shellFamily() != dev.jstech.computers.os.ShellFamily.POSIX) {
            return currentLocation().dosPath() + ">";
        }
        final String cwd = dev.jstech.computers.program.cli.PosixPath.renderForPrompt(currentLocation());
        final OsDef os = hostBlock instanceof IOsHost c ? c.installedOs() : null;
        final boolean zsh = os != null && os.shellId().equals("zsh");
        return zsh ? "player@" + hostname() + " " + cwd + " %" : "player@" + hostname() + ":" + cwd + "$";
    }

    @Override
    public java.util.List<MountInfo> mounts() {
        return files().mounts();
    }

    /** The terminal window's shell session this prompt speaks for, or 0 for the machine's own prompt. */
    private int session;

    /**
     * Says which terminal window's shell this is.
     *
     * <p>A window that has moved with {@code cd} is where it went, whatever the other windows and the
     * full-screen prompt are doing; one that has not is wherever the machine's prompt is.
     */
    public void useSession(final int session) {
        this.session = session;
    }

    @Override
    public dev.jstech.computers.program.cli.DosPath.Location currentLocation() {
        final dev.jstech.computers.program.ComputerConsoleState console = host.console();
        if (console == null) {
            return dev.jstech.computers.program.cli.DosPath.Location.root('C');
        }
        final dev.jstech.computers.program.ComputerConsoleState.ShellSpot spot =
                session == 0 ? null : console.sessionLocation(session);
        if (spot != null) {
            final java.util.List<String> parts = spot.dir().isEmpty()
                    ? java.util.List.of() : java.util.List.of(spot.dir().split("/"));
            return new dev.jstech.computers.program.cli.DosPath.Location(spot.drive(), parts);
        }
        /*
         * A fresh POSIX session starts in the home directory (a DOS one at the drive root); once the player
         * has changed directory the stored location wins, so "cd /" really lands on the root.
         */
        if (!console.hasTerminalLocation() && console.terminalDrive() == 'C'
                && shellFamily() == dev.jstech.computers.os.ShellFamily.POSIX) {
            return dev.jstech.computers.program.cli.PosixPath.home();
        }
        final String dir = console.terminalDir();
        final java.util.List<String> segments = dir.isEmpty()
                ? java.util.List.of() : java.util.List.of(dir.split("/"));
        return new dev.jstech.computers.program.cli.DosPath.Location(console.terminalDrive(), segments);
    }

    @Override
    public void setCurrentLocation(final dev.jstech.computers.program.cli.DosPath.Location location) {
        final dev.jstech.computers.program.ComputerConsoleState console = host.console();
        if (console == null) {
            return;
        }
        if (session != 0) {
            console.setSessionLocation(session, location.drive(), location.storagePath());
        } else {
            console.setTerminalLocation(location.drive(), location.storagePath());
        }
    }

    @Override
    public FsResult listDisk(final String dir) {
        return files().listDisk(dir);
    }

    @Override
    public FsResult readFile(final String path) {
        return files().readFile(path);
    }

    @Override
    public FsResult deleteFile(final String path) {
        return files().deleteFile(path);
    }

    @Override
    public FsResult runScript(final String path) {
        // Check the extension first so the error names the right problem.
        final String ext = extensionOf(path);
        if (!"iql".equalsIgnoreCase(ext)) {
            return FsResult.fail(path + ": only .iql files can be run (got ." + (ext.isEmpty() ? "<none>" : ext) + ")");
        }
        final Resolved r = resolve(path);
        if (r.ctx() == null) {
            return driveError(r.drive());
        }
        if (r.ctx().disk().isEmpty()) {
            return notReady(r.drive());
        }
        final DriveTable.Drive ctx = r.ctx();
        final String real = r.path();
        final java.util.Optional<String> content = DiskFilesystem.read(ctx.disk(), real);
        if (content.isEmpty()) {
            return FsResult.fail(path + ": file not found");
        }
        // Parse and dispatch through the exact same path the 'operation' command uses.
        final IqlParseResult parsed = IqlParser.tryParse(content.get().trim());
        if (!parsed.ok()) {
            return FsResult.fail(path + ": syntax error: " + parsed.error());
        }
        final IqlOperation op = parsed.operation();
        /*
         * QUERY/COUNT are read operations that produce rows, not timed operations; they cannot be
         * dispatched via execute(). The caller should use 'operation' for those.
         */
        if (op.verb() == IqlVerb.QUERY || op.verb() == IqlVerb.COUNT) {
            return FsResult.fail(path + ": QUERY/COUNT are not supported by 'run', use 'operation' instead");
        }
        final OpResult result = execute(op);
        return FsResult.iqlResult(result);
    }

    @Override
    public FsResult writeFile(final String path, final String content) {
        return files().writeFile(path, content);
    }

    @Override
    public FsResult appendFile(final String path, final String content) {
        return files().appendFile(path, content);
    }

    @Override
    public FsResult changeDir(final String input) {
        return files().changeDir(input, this::setCurrentLocation);
    }

    @Override
    public FsResult changeDrive(final char drive) {
        return files().changeDrive(drive, letter -> {
            final dev.jstech.computers.program.ComputerConsoleState console = host.console();
            if (console != null) {
                console.setTerminalDrive(letter);
            }
        });
    }

    @Override
    public FsResult makeDir(final String path) {
        return files().makeDir(path);
    }

    @Override
    public FsResult removeDir(final String path) {
        return files().removeDir(path);
    }

    @Override
    public FsResult copyPath(final String src, final String dest) {
        return files().copyPath(src, dest);
    }

    @Override
    public FsResult movePath(final String src, final String destDir) {
        return files().movePath(src, destDir);
    }

    @Override
    public FsResult renamePath(final String src, final String newName) {
        return files().renamePath(src, newName);
    }

    @Override
    public java.util.List<String> configSummary() {
        final dev.jstech.computers.program.ComputerConsoleState console = host.console();
        if (console == null) {
            return java.util.List.of();
        }
        final java.util.List<String> lines = new java.util.ArrayList<>();
        final String name = console.computerName();
        lines.add(String.format(java.util.Locale.ROOT, "  %-12s%s", "name", name.isEmpty() ? "(unnamed)" : name));
        lines.add(String.format(java.util.Locale.ROOT, "  %-12s%d permille", "netshare", systemDiskPermille()));
        lines.addAll(console.settings().summaryLines());
        lines.add("  'config share <folder> [read|write]' opens a folder to the network as \\\\"
                + hostname() + "\\<name>; 'config unshare <name>' closes it");
        return lines;
    }

    @Override
    public OpResult setConfig(final String key, final String value) {
        final dev.jstech.computers.program.ComputerConsoleState console = host.console();
        if (console == null) {
            return OpResult.fail("this computer has no settings store");
        }
        final String k = key == null ? "" : key.toLowerCase(java.util.Locale.ROOT).trim();
        switch (k) {
            case "name" -> {
                console.setComputerName(value == null ? "" : value.trim());
                hostBlock.setChanged();
                return OpResult.ok("name set");
            }
            case "wallpaper" -> {
                console.setWallpaper(value == null ? "" : value.trim());
                hostBlock.setChanged();
                return OpResult.ok("wallpaper set");
            }
            case "theme" -> {
                // A theme preset bundles an accent and a wallpaper, so picking one restyles the desktop.
                final String preset = value == null ? "" : value.trim().toLowerCase(java.util.Locale.ROOT);
                console.settings().setThemePreset(preset.equals("system") ? "" : preset);
                switch (preset) {
                    case "ocean" -> {
                        console.settings().setAccent(0xFF12A26F);
                        console.setWallpaper("winxp");
                    }
                    case "slate" -> {
                        console.settings().setAccent(0xFF7B52C9);
                        console.setWallpaper("win11");
                    }
                    default -> {
                        console.settings().setAccent(0);
                        console.setWallpaper("");
                    }
                }
                hostBlock.setChanged();
                return OpResult.ok("theme set");
            }
            case "netshare" -> {
                final Integer permille = tryInt(value);
                if (permille == null) {
                    return OpResult.fail("netshare needs a number from 0 to 1000");
                }
                if (!setSystemDiskPermille(permille)) {
                    return OpResult.fail("no system disk to share");
                }
                hostBlock.setChanged();
                return OpResult.ok("netshare set");
            }
            case "share" -> {
                return shareFolder(console, value);
            }
            case "unshare" -> {
                final String wanted = value == null ? "" : value.trim();
                if (!console.settings().unshare(wanted)) {
                    return OpResult.fail("nothing is shared as " + wanted);
                }
                hostBlock.setChanged();
                return OpResult.ok("no longer shared: " + wanted);
            }
            default -> {
                if (console.settings().applySetting(k, value)) {
                    hostBlock.setChanged();
                    return OpResult.ok(k + " set");
                }
                return OpResult.fail("unknown setting: " + k);
            }
        }
    }

    /**
     * Shares a folder of this machine with the others on its network: {@code config share C:\pub}
     * for reading, {@code config share C:\pub write} for writing too. The folder has to exist.
     */
    private OpResult shareFolder(final dev.jstech.computers.program.ComputerConsoleState console,
                                 final String value) {
        String path = value == null ? "" : value.trim();
        boolean writable = false;
        final int space = path.lastIndexOf(' ');
        if (space > 0) {
            final String mode = path.substring(space + 1).toLowerCase(java.util.Locale.ROOT);
            if (mode.equals("write") || mode.equals("read")) {
                writable = mode.equals("write");
                path = path.substring(0, space).trim();
            }
        }
        if (path.isEmpty()) {
            return OpResult.fail("usage: config share <folder> [read|write]");
        }
        final Resolved r = resolve(path);
        if (r.ctx() == null) {
            return OpResult.fail(driveError(r.drive()).message());
        }
        if (r.ctx().disk().isEmpty()) {
            return OpResult.fail(notReady(r.drive()).message());
        }
        if (!r.path().isEmpty() && !dirExists(r.ctx(), r.path())) {
            return OpResult.fail(path + ": no such folder");
        }
        final String dos = r.drive() + ":\\" + r.path().replace('/', '\\');
        if (!console.settings().share(dos, writable)) {
            return OpResult.fail("this computer already shares "
                    + dev.jstech.computers.program.ComputerSettings.MAX_SHARES + " folders");
        }
        hostBlock.setChanged();
        final String name = dev.jstech.computers.program.ComputerSettings.shareNameOf(dos);
        return OpResult.ok("shared " + dos + " as \\\\" + hostname() + "\\" + name
                + (writable ? " (read and write)" : " (read only)"));
    }

    @Override
    public List<ShareInfo> shares() {
        final dev.jstech.computers.program.ComputerConsoleState console = host.console();
        if (console == null) {
            return List.of();
        }
        final List<ShareInfo> out = new ArrayList<>();
        for (final dev.jstech.computers.program.ComputerSettings.Share share : console.settings().shares()) {
            out.add(new ShareInfo(share.name(), share.path(), share.writable()));
        }
        return out;
    }

    @Override
    public List<NetworkShare> networkShares() {
        final List<NetworkShare> out = new ArrayList<>();
        reachableMachines().forEach((hostname, machine) -> {
            final ServerCliComputer remote = new ServerCliComputer((IComputerTerminalHost) machine, level);
            if (!remote.running()) {
                return;
            }
            for (final ShareInfo share : remote.shares()) {
                out.add(new NetworkShare(hostname, share));
            }
        });
        return out;
    }

    /** Where a path on another machine of the network leads, followed on that machine's own shell. */
    private NetworkPathResolver networkPaths() {
        return new NetworkPathResolver(level, this::matchMachines, this::networkShares);
    }

    /** The machines of this network that name picks out, by host name, for whoever follows a network path. */
    public Map<String, BlockEntity> machinesNamed(final String name) {
        return matchMachines(name);
    }

    /** The network's own language, as this shell speaks it. */
    private dev.jstech.computers.machine.IqlService iql() {
        return new dev.jstech.computers.machine.IqlService(host, level, this, operations(), networkReads());
    }

    /** What the Mainframe of this machine's network keeps about the work it has done. */
    private dev.jstech.computers.machine.MainframeStatsService mainframeStats() {
        return new dev.jstech.computers.machine.MainframeStatsService(host, level);
    }

    /** The work this machine asks of its network, as this shell asks for it. */
    private dev.jstech.computers.machine.OperationsService operations() {
        return new dev.jstech.computers.machine.OperationsService(host, level, this);
    }

    /** The data network this machine is on, as this shell reads it. */
    private dev.jstech.computers.machine.NetworkReadService networkReads() {
        return new dev.jstech.computers.machine.NetworkReadService(host, level, this, operations());
    }

    /** This machine's drives as this shell reaches them, from where this shell's window stands. */
    private dev.jstech.computers.machine.FileService files() {
        return new dev.jstech.computers.machine.FileService(hostBlock, level, networkPaths(), this::currentLocation);
    }

    /** The machine on this network that {@code name} picks out, as its own shell; null when none or several. */
    @org.jetbrains.annotations.Nullable
    public ServerCliComputer remoteShell(final String name) {
        final Map<String, BlockEntity> matches = matchMachines(name);
        if (matches.size() != 1) {
            return null;
        }
        return new ServerCliComputer((IComputerTerminalHost) matches.values().iterator().next(), level);
    }

    /** The block this shell runs on. */
    public BlockEntity machine() {
        return hostBlock;
    }

    /** The Mainframe of the network this machine is on, or null off any network. */
    @org.jetbrains.annotations.Nullable
    public MainframeBlockEntity mainframe() {
        return mainframe(host.networkUuid());
    }

    /** Whether this machine takes programs and commands from the other computers on its network. */
    public boolean remoteAllowed() {
        final dev.jstech.computers.program.ComputerConsoleState console = host.console();
        return console != null && console.settings().remoteAllowed();
    }

    /** The system disk's public-share permille (0 when there is no system disk). */
    private int systemDiskPermille() {
        final DriveTable.Drive ctx = diskFor('C');
        return ctx == null || ctx.disk().isEmpty() ? 0
                : dev.jstech.computers.item.DiskItem.publicPermille(ctx.disk());
    }

    /** Writes a clamped public-share permille onto the system disk; false when there is none. */
    private boolean setSystemDiskPermille(final int permille) {
        final DriveTable.Drive ctx = diskFor('C');
        if (ctx == null || ctx.disk().isEmpty()) {
            return false;
        }
        dev.jstech.computers.item.DiskItem.setPublicPermille(ctx.disk(), permille);
        return true;
    }

    private static Integer tryInt(final String v) {
        try {
            return Integer.parseInt(v == null ? "" : v.trim());
        } catch (final NumberFormatException e) {
            return null;
        }
    }

    /** True if {@code storagePath} is the drive root or an existing (explicit or implicit) directory. */
    private boolean dirExists(final DriveTable.Drive ctx, final String storagePath) {
        return DriveTable.dirExists(osHost(), ctx, storagePath);
    }

    /** Returns the lowercase extension of a file path (after the last dot), or {@code ""} if none. */
    private static String extensionOf(final String path) {
        final int dot = path.lastIndexOf('.');
        return dot >= 0 && dot < path.length() - 1
                ? path.substring(dot + 1).toLowerCase(java.util.Locale.ROOT)
                : "";
    }

    // Script processes: the Σ# programs this machine is running.

    /**
     * The machine's programs when one of them has the terminal, or null when the prompt is free.
     *
     * <p>A machine has one prompt, so it has at most one program in front of it; whoever is at the
     * keyboard is typing at that program until it returns.
     */
    @org.jetbrains.annotations.Nullable
    public MachinePrograms foreground() {
        if (hostBlock instanceof AbstractComputerBlockEntity computer && computer.programs().held() != 0) {
            return computer.programs();
        }
        return null;
    }

    @Override
    public OpResult startSigma(final String path, final int heapMb) {
        return this.startSigma(path, heapMb, List.of());
    }

    @Override
    public OpResult startSigma(final String path, final int heapMb, final List<String> arguments) {
        if (!(hostBlock instanceof AbstractComputerBlockEntity computer)) {
            return OpResult.fail("sigma: this machine cannot run programs");
        }
        final var launch = dev.jstech.computers.machine.ProgramLauncher.launch(computer, path, this::readFile,
                arguments, dev.jstech.computers.vm.program.IProgramParent.NONE,
                dev.jstech.computers.vm.program.ProgramPriority.MEDIUM, heapMb);
        if (!launch.ok()) {
            return OpResult.fail(switch (launch.refusal()) {
                case NO_RUNNER -> path + ": nothing installed runs a program of this kind"
                        + " (compile a source file first)";
                case NO_MEMORY -> "sigma: " + launch.roomMb() + " MB will not fit in " + launch.freeMb()
                        + " MB of free memory";
                case UNREADABLE, NOT_STARTED -> launch.message();
            });
        }
        final var one = computer.programs().byId(launch.id());
        if (one != null && !one.process().isService()) {
            /*
             * A program that runs at a terminal takes the one that started it, the way it does on any
             * machine: the prompt is its, and comes back when it returns.
             */
            computer.programs().hold(launch.id());
            return OpResult.ok("");
        }
        return OpResult.ok(launch.message());
    }

    @Override
    public OpResult stopSigma(final int id) {
        if (!(hostBlock instanceof AbstractComputerBlockEntity computer)) {
            return OpResult.fail("sigma: this machine cannot run programs");
        }
        if (!computer.programs().stop(id)) {
            return OpResult.fail("sigma: nothing is running as " + id);
        }
        computer.setChanged();
        return OpResult.ok("stopped " + id);
    }

    @Override
    public List<SigmaProcess> sigmaProcesses() {
        if (!(hostBlock instanceof AbstractComputerBlockEntity computer)) {
            return List.of();
        }
        final List<SigmaProcess> running = new java.util.ArrayList<>();
        for (final var one : computer.programs().view()) {
            running.add(new SigmaProcess(one.id(), one.name(), one.state(), one.heldBytes(), one.heapBytes(),
                    one.file()));
        }
        return running;
    }
}
