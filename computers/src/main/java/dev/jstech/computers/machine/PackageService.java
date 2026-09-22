/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Computers.
 */
package dev.jstech.computers.machine;

import dev.jstech.computers.JsComputers;
import dev.jstech.computers.blockentity.AbstractComputerBlockEntity;
import dev.jstech.computers.blockentity.ClusterManagementComputerBlockEntity;
import dev.jstech.computers.blockentity.MainframeBlockEntity;
import dev.jstech.computers.blockentity.ServerRackBlockEntity;
import dev.jstech.computers.os.HostScope;
import dev.jstech.computers.os.IOsHost;
import dev.jstech.computers.os.OsGating;
import dev.jstech.computers.os.ProgramVersions;
import dev.jstech.computers.os.install.SetupRunner;
import dev.jstech.computers.program.cli.SigmaCommands;
import dev.jstech.core.tier.HardwareEra;
import java.util.Optional;
import net.minecraft.resources.ResourceLocation;
import dev.jstech.computers.os.OsDef;
import dev.jstech.computers.os.OsRegistry;
import dev.jstech.computers.os.PackageManagerKind;
import dev.jstech.computers.os.Platform;
import dev.jstech.computers.os.ProgramKind;
import dev.jstech.computers.os.ProgramSpec;
import dev.jstech.computers.program.ComputerConsoleState;
import dev.jstech.computers.program.cli.ICliComputer;
import dev.jstech.computers.program.cli.ICliPackages;
import dev.jstech.computers.program.install.MirrorPackage;
import dev.jstech.computers.program.tty.TtyScriptProcess;
import dev.jstech.computers.sigma.pack.Packed;
import dev.jstech.computers.terminal.IComputerTerminalHost;
import dev.jstech.core.network.NetworkSystem;
import dev.jstech.core.uuid.NetworkUuid;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.neoforged.fml.ModList;
import org.jetbrains.annotations.Nullable;

/**
 * The packages a machine installs over its network's Mirror, and the Mirror itself.
 *
 * <p>The Mirror is the network's, not the machine's: it is installed on the Mainframe and every computer of the
 * network installs from that one. A machine that cannot reach a serving Mirror is told so in the words its own
 * package manager would use, because that is what a player reads at the prompt.
 */
public final class PackageService {

    private final IComputerTerminalHost terminal;
    private final ServerLevel level;
    /** The machine's drives, for the files a published package is unpacked into and taken back from. */
    private final FileService files;
    /** The network's own language, for the row its engine fills in the list of services. */
    private final IqlService iql;

    public PackageService(final IComputerTerminalHost terminal, final ServerLevel level, final FileService files,
                          final IqlService iql) {
        this.terminal = terminal;
        this.level = level;
        this.files = files;
        this.iql = iql;
    }

    /** Installs the Mirror on the network's Mainframe, or says how it stands. */
    public ICliComputer.OpResult control(final String action) {
        final MainframeBlockEntity mainframe = this.mainframe();
        if (mainframe == null) {
            return ICliComputer.OpResult.fail("the network has no running Mainframe to host the Mirror");
        }
        return switch (action == null ? "" : action.toLowerCase(Locale.ROOT)) {
            case "install" -> mainframe.installMirror()
                    ? ICliComputer.OpResult.ok("Mirror installed on the Mainframe and serving packages")
                    : ICliComputer.OpResult.fail("the Mirror is already installed");
            case "status", "" -> ICliComputer.OpResult.ok("Mirror: " + this.state());
            default -> ICliComputer.OpResult.fail("usage: mirror install|status");
        };
    }

    /** How the Mirror stands on the network's Mainframe, in the words every view shows. */
    public String state() {
        final MainframeBlockEntity mainframe = this.mainframe();
        if (mainframe == null || !mainframe.isMirrorInstalled()) {
            return "not installed";
        }
        return mainframe.isMirrorActive() ? "serving" : "installed (Mainframe off)";
    }

    /** Whether a Mirror is serving this machine right now. */
    public boolean reachable() {
        return this.mirrorMainframe() != null;
    }

    /** The network's Mainframe while its Mirror is serving, else null. */
    @Nullable
    public MainframeBlockEntity mirrorMainframe() {
        final MainframeBlockEntity mainframe = this.mainframe();
        return mainframe != null && mainframe.isMirrorActive() ? mainframe : null;
    }

    /** The name the Mirror's Mainframe goes by in a package line, or the plain word when it has none. */
    public String mirrorHostname() {
        final MainframeBlockEntity mirror = this.mirrorMainframe();
        final String name = mirror == null ? "" : mirror.console() == null ? "" : mirror.console().computerName();
        return name == null || name.isBlank() ? "mainframe" : name;
    }

    /** The system installed on the machine, or null when it has none, or is not a machine that takes one. */
    @Nullable
    public OsDef installedOs() {
        final BlockEntity machine = (BlockEntity) this.terminal;
        return machine instanceof IOsHost host ? host.installedOs() : null;
    }

    /** The installed system's package manager; {@code NONE} on the platforms that install from media instead. */
    public PackageManagerKind manager() {
        final OsDef os = this.installedOs();
        return os == null ? PackageManagerKind.NONE : os.packageManager();
    }

    /** Whether the named program is on this machine: installed at its console, or a service flag on a Mainframe. */
    public boolean has(final ProgramSpec spec) {
        final BlockEntity machine = (BlockEntity) this.terminal;
        if (machine instanceof MainframeBlockEntity mf) {
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
        final ComputerConsoleState console = this.terminal.console();
        return console != null && console.isInstalled(spec.id().toString());
    }

    /**
     * The packages a manager on THIS machine can offer: everything installable that runs on the platform it is
     * running. Reading the platform (rather than assuming Linux) is what lets the Frames manager see the
     * Frames-only software the Mirror serves.
     */
    public List<ProgramSpec> offered() {
        final OsDef os = this.installedOs();
        final Platform platform = os == null ? Platform.LINUX : os.platform();
        final List<ProgramSpec> out = new ArrayList<>();
        for (final ProgramSpec spec : OsRegistry.programs()) {
            if (spec.installable() && spec.platforms().contains(platform)) {
                out.add(spec);
            }
        }
        return out;
    }

    /**
     * What the Mirror has under that name for a Linux system that is still being built by hand, or null when it
     * has nothing by it.
     *
     * <p>The machine has no system of its own yet to say what it runs, or has a different one on another disk,
     * so this goes by what a Linux system can run and not by what is installed. A service is left out: those
     * are switched on by the machine that runs them once it is up, not laid onto a disk beforehand.
     *
     * @param hasATree whether the system being built files its packages by category, and so knows them by those
     *                 names as well
     */
    @Nullable
    public MirrorPackage whileInstalling(final String typed, final boolean hasATree) {
        for (final ProgramSpec spec : OsRegistry.programs()) {
            if (spec.installable() && spec.kind() != ProgramKind.SERVICE && spec.platforms().contains(Platform.LINUX)
                    && named(typed, spec, hasATree) && this.wrongMachine(spec) == null) {
                return SourceChains.packageOf(spec);
            }
        }
        return null;
    }

    /**
     * What the Mirror has for this machine: first what it can serve, each saying whether the machine already has
     * it, then whatever players on this network have published, marked as theirs. A published package the machine
     * cannot read is left out rather than shown broken.
     */
    public List<ICliComputer.PackageInfo> available() {
        if (this.mirrorMainframe() == null) {
            return List.of();
        }
        final List<ICliComputer.PackageInfo> out = new ArrayList<>();
        for (final ProgramSpec spec : this.offered()) {
            out.add(new ICliComputer.PackageInfo(spec.commandName(), spec.displayName()
                    + (spec.kind() == ProgramKind.SERVICE ? " (service)" : ""), this.has(spec)));
        }
        final MainframeBlockEntity mirror = this.mirrorMainframe();
        if (mirror != null) {
            for (final var shelved : mirror.shelvedPackages().entrySet()) {
                final Packed packed = Packed.read(shelved.getValue());
                if (packed == null) {
                    continue;
                }
                final String about = packed.manifest().about();
                out.add(new ICliComputer.PackageInfo(shelved.getKey(),
                        (about.isBlank() ? packed.manifest().label() : about) + " - " + packed.manifest().house(),
                        false, true));
            }
        }
        return out;
    }

    /**
     * The folder a player's package is unpacked into.
     *
     * <p>One folder each, named after the package, so two of them cannot quietly overwrite each other's files and
     * removing one takes exactly its own files with it.
     */
    private static final String COMMUNITY_DIR = "PROGRAMS";

    /**
     * Refuses a program the machine is too old to run, or {@code null} when the era is fine. Software cannot predate
     * its hardware generation: a desktop of the 2010s does not install on a machine of the 1990s, however much disk
     * it has free.
     *
     * <p>This is the package manager's gate. The install medium does not come through here: it is refused by
     * {@code SetupGate}, which tests the same rule among the others it checks and names the machine's era in its
     * message. Two doors, one rule.
     */
    @Nullable
    public ICliComputer.OpResult eraGate(final ProgramSpec spec) {
        if (spec.minEra() == HardwareEra.VINTAGE) {
            return null; // no requirement
        }
        /*
         * displayEra, not installedEra: a Vintage or Legacy chassis IS that generation whatever board
         * sits in it, and that chassis is the only way a machine of an older era exists right now.
         */
        final BlockEntity machine = (BlockEntity) this.terminal;
        final HardwareEra era = machine instanceof IOsHost computer ? computer.displayEra() : null;
        if (era != null && OsGating.canInstall(spec.minEra(), era)) {
            return null;
        }
        final String needed = spec.minEra().name();
        return ICliComputer.OpResult.fail(spec.commandName() + " needs "
                + (needed.charAt(0) + needed.substring(1).toLowerCase(Locale.ROOT))
                + " hardware or later");
    }

    /**
     * What the prompt prints ahead of the next command, each line handed over once: what the machine itself has to
     * say, which happened as the world loaded.
     */
    public List<String> notices() {
        final List<String> out = new ArrayList<>();
        if ((BlockEntity) this.terminal instanceof AbstractComputerBlockEntity computer) {
            for (final String notice : computer.programs().drainNotices()) {
                out.add(">>> " + notice);
            }
        }
        return out;
    }

    /** Puts a built package on the network's Mirror, for anyone on the network to install. */
    public ICliComputer.OpResult publish(final String path) {
        final MainframeBlockEntity mirror = this.mirrorMainframe();
        if (mirror == null) {
            return ICliComputer.OpResult.fail("could not resolve mirror:// - connect this computer to a network whose"
                    + " Mainframe runs the Mirror service");
        }
        final ICliComputer.FsResult read = this.files.readFile(path);
        if (!read.ok()) {
            return ICliComputer.OpResult.fail(read.message());
        }
        final Packed packed = Packed.read(read.message());
        if (packed == null) {
            return ICliComputer.OpResult.fail(path + ": this is not a package (build one with 'sgpack build')");
        }
        final List<String> wrong = packed.problems();
        if (!wrong.isEmpty()) {
            return ICliComputer.OpResult.fail(path + ": " + wrong.getFirst());
        }
        final String name = packed.manifest().name();
        final boolean replacing = mirror.shelvedPackage(name) != null;
        if (!mirror.shelve(name, read.message())) {
            return ICliComputer.OpResult.fail("the Mirror is full ("
                    + MainframeBlockEntity.SHELF_MAX + " packages)");
        }
        return ICliComputer.OpResult.ok((replacing ? "replaced " : "published ") + packed.manifest().label()
                + " on the Mirror");
    }

    /** Takes one back off the Mirror. */
    public ICliComputer.OpResult unpublish(final String name) {
        final MainframeBlockEntity mirror = this.mirrorMainframe();
        if (mirror == null) {
            return ICliComputer.OpResult.fail("could not resolve mirror://");
        }
        if (!mirror.unshelve(name == null ? "" : name.trim())) {
            return ICliComputer.OpResult.fail("the Mirror is not serving " + name);
        }
        return ICliComputer.OpResult.ok("took " + name + " off the Mirror");
    }

    /**
     * Installs a package a player published, if that is what this name is.
     *
     * <p>Returns null when the name belongs to something else, so the usual path carries on.
     */
    @Nullable
    private ICliComputer.OpResult installCommunity(final String wanted) {
        final MainframeBlockEntity mirror = this.mirrorMainframe();
        final String held = mirror == null ? null : mirror.shelvedPackage(wanted);
        if (held == null) {
            return null;
        }
        final Packed packed = Packed.read(held);
        if (packed == null || !packed.problems().isEmpty()) {
            return ICliComputer.OpResult.fail(wanted + ": the Mirror's copy of this package is not readable");
        }
        if (!this.hasRuntime()) {
            return ICliComputer.OpResult.fail(wanted + " is a Σ# program; install sigma first");
        }
        final ComputerConsoleState console = this.terminal.console();
        if (console == null) {
            return ICliComputer.OpResult.fail("no system disk to install onto");
        }
        // Its own folder, made before anything is written into it.
        final String folder = COMMUNITY_DIR + "/" + wanted;
        this.files.makeDir(COMMUNITY_DIR);
        // A folder already there is as good as one made now: it is where the files go either way.
        if (!this.files.makeDir(folder).ok() && !this.files.folderExists(folder)) {
            return ICliComputer.OpResult.fail(wanted + ": this system has no folders to install into");
        }
        for (final var file : packed.files().entrySet()) {
            final ICliComputer.FsResult written = this.files.writeFile(folder + "/" + file.getKey(), file.getValue());
            if (!written.ok()) {
                return ICliComputer.OpResult.fail(wanted + ": " + written.message());
            }
        }
        console.addCommunity(new ComputerConsoleState.Community(
                wanted, packed.manifest().version(), packed.manifest().house(),
                packed.manifest().icon(), folder + "/" + packed.manifest().entry()));
        ((BlockEntity) this.terminal).setChanged();
        return ICliComputer.OpResult.ok("installed " + packed.manifest().label() + " into " + folder);
    }

    /** Whether the Σ# runtime is on this machine, which is what a player's package needs to run. */
    private boolean hasRuntime() {
        final ProgramSpec runtime = OsRegistry.getProgram(ResourceLocation.tryParse(SigmaCommands.RUNTIME));
        return runtime != null && this.has(runtime);
    }

    /**
     * Installs the named package from the network's Mirror, in the words the machine's package manager uses.
     *
     * <p>A manager that builds what it installs leaves the build running in front of the terminal, and the
     * program is on the machine when that build ends. Every other answer is given at once.
     *
     * @param ask whether the manager was told to list what it would do and ask before doing it
     */
    public ICliPackages.Installing install(final String name, final boolean ask) {
        final String wanted = name == null ? "" : name.trim().toLowerCase(Locale.ROOT);
        final ICliComputer.OpResult early = this.beforeTheMirror(wanted);
        if (early != null) {
            return ICliPackages.Installing.said(early);
        }
        final ProgramSpec spec = this.offeredAs(wanted);
        if (spec == null) {
            return ICliPackages.Installing.said(ICliComputer.OpResult.fail("unable to locate package " + wanted));
        }
        final ICliComputer.OpResult stopped = this.whyNotHere(spec);
        if (stopped != null) {
            return ICliPackages.Installing.said(stopped);
        }
        final BlockEntity machine = (BlockEntity) this.terminal;
        final ComputerConsoleState console = this.terminal.console();
        if (this.manager().compilesFromSource() && machine instanceof IOsHost builder && console != null
                && !(machine instanceof MainframeBlockEntity && spec.kind() == ProgramKind.SERVICE)
                && this.fits(builder, spec)) {
            final String id = spec.id().toString();
            return ICliPackages.Installing.running(new TtyScriptProcess(SourceBuild.of(spec, builder, ask, () -> {
                console.install(id);
                /*
                 * And at the version it was built at, like every other way of installing it. Without this a
                 * package built from source was listed as outdated the moment it finished building.
                 */
                console.setInstalledVersion(id, ProgramVersions.of(id));
                machine.setChanged();
            })));
        }
        return ICliPackages.Installing.said(this.installAtOnce(spec));
    }

    /** What is answered before the Mirror is looked at: a system with no manager, and a player's own package. */
    @Nullable
    private ICliComputer.OpResult beforeTheMirror(final String wanted) {
        if (this.manager() == PackageManagerKind.NONE) {
            return ICliComputer.OpResult.fail(
                    "this system installs programs from install media, not a package manager");
        }
        final ICliComputer.OpResult community = this.installCommunity(wanted);
        if (community != null) {
            return community;
        }
        if (this.mirrorMainframe() == null) {
            return ICliComputer.OpResult.fail("could not resolve mirror:// - connect this computer to a network whose"
                    + " Mainframe runs the Mirror service");
        }
        return null;
    }

    /**
     * The program the Mirror offers this machine under that name, or null when it offers none.
     *
     * <p>A system with a package tree also knows its programs by their place in it, so a desktop can be asked
     * for there the way it really is, by its category and its name.
     */
    @Nullable
    private ProgramSpec offeredAs(final String wanted) {
        final boolean hasATree = this.manager().compilesFromSource();
        for (final ProgramSpec candidate : this.offered()) {
            if (named(wanted, candidate, hasATree)) {
                return candidate;
            }
        }
        return null;
    }

    /** Whether what was typed is that program: its command, its id, or its place in a package tree. */
    private static boolean named(final String typed, final ProgramSpec spec, final boolean hasATree) {
        return spec.commandName().equalsIgnoreCase(typed) || spec.id().getPath().equalsIgnoreCase(typed)
                || (hasATree && SourceChains.names(typed, spec));
    }

    /** Why a program made for another kind of machine does not install on this one, or null when it does. */
    @Nullable
    private ICliComputer.OpResult wrongMachine(final ProgramSpec spec) {
        final BlockEntity machine = (BlockEntity) this.terminal;
        if (spec.hostScope() == HostScope.MAINFRAME && !(machine instanceof MainframeBlockEntity)) {
            return ICliComputer.OpResult.fail(spec.commandName() + " only installs on the Mainframe");
        }
        if (spec.hostScope() == HostScope.SERVER && !(machine instanceof ServerRackBlockEntity)) {
            return ICliComputer.OpResult.fail(spec.commandName() + " only installs on a server in a rack");
        }
        if (spec.hostScope() == HostScope.CLUSTER_MANAGEMENT_COMPUTER
                && !(machine instanceof ClusterManagementComputerBlockEntity)) {
            return ICliComputer.OpResult.fail(spec.commandName()
                    + " only installs on a Cluster Management Computer");
        }
        return null;
    }

    /** Why that program is not installed on this machine, or null when nothing stands in its way. */
    @Nullable
    private ICliComputer.OpResult whyNotHere(final ProgramSpec spec) {
        final ICliComputer.OpResult tooOld = this.eraGate(spec);
        if (tooOld != null) {
            return tooOld;
        }
        final ICliComputer.OpResult elsewhere = this.wrongMachine(spec);
        if (elsewhere != null) {
            return elsewhere;
        }
        if (this.has(spec)) {
            // Said the way the manager asked says it: pkg checks and finds nothing to do, the others report a version.
            return ICliComputer.OpResult.ok(this.manager() == PackageManagerKind.PKG
                    ? "Checking integrity... done (0 conflicting)\n"
                            + "The most recent versions of packages are already installed"
                    : spec.commandName() + " is already the newest version");
        }
        return null;
    }

    /** Whether that machine meets what the program asks of its hardware and has the room for it. */
    private boolean fits(final IOsHost host, final ProgramSpec spec) {
        return OsRegistry.canInstallProgram(host.installedOsId(), spec.id(), host.maxCpuMhz(), host.totalVramMb(),
                host.systemDiskFreeMb());
    }

    /** Installs a program the way a manager that ships built packages does, answering at once. */
    private ICliComputer.OpResult installAtOnce(final ProgramSpec spec) {
        final PackageManagerKind manager = this.manager();
        final BlockEntity machine = (BlockEntity) this.terminal;
        final ComputerConsoleState console = this.terminal.console();
        // A Mainframe service switches its flag on directly (a prebuilt daemon, so no source build either).
        if (machine instanceof MainframeBlockEntity mf && spec.kind() == ProgramKind.SERVICE) {
            final boolean switchedOn = switch (spec.id().getPath()) {
                case "iqlengine" -> mf.installIqlEngine();
                case "automation_engine" -> mf.installAutomationEngine();
                case "mirror" -> mf.installMirror();
                default -> false;
            };
            /*
             * And listed in the console as well as switched on, which is what the disc's setup does. Only
             * the flag was set here, so a service installed from a package manager was serving while the
             * machine still reported nothing installed, and the memory it held was never counted.
             */
            final String id = spec.id().toString();
            final boolean listed = console != null && console.install(id);
            if (listed) {
                console.setInstalledVersion(id, ProgramVersions.of(id));
            }
            final boolean done = switchedOn || listed;
            machine.setChanged();
            return done ? ICliComputer.OpResult.ok("Setting up " + spec.commandName() + " ... done")
                    : ICliComputer.OpResult.fail(spec.commandName() + " could not be set up");
        }
        if (machine instanceof IOsHost oc && !this.fits(oc, spec)) {
            return ICliComputer.OpResult.fail(spec.commandName()
                    + ": unmet requirements (hardware or free disk space)");
        }
        if (console == null) {
            return ICliComputer.OpResult.fail("this computer cannot store installed programs");
        }
        /*
         * A package from the Mirror is fetched over the network and set up over time, the way the same
         * program from a disc is; the manager's own gates above have already said it may.
         */
        if (!(machine instanceof IOsHost host)) {
            return ICliComputer.OpResult.fail("this computer cannot store installed programs");
        }
        final ProgramSpec fetched = spec;
        final Optional<String> refusal = SetupRunner.begin(host, this.level, machine.getBlockPos(), fetched, null,
                false, manager.command());
        return refusal.map(ICliComputer.OpResult::fail)
                .orElseGet(() -> ICliComputer.OpResult.ok(this.fetchLines(manager, fetched)));
    }

    /**
     * What a package manager prints before the download starts, in its own words.
     *
     * <p>Each of them has a voice a player who has used the real one knows on sight, and the lines are that voice:
     * what was resolved, what will be installed, how big it is, and where it comes from. They are one message, line
     * by line, and the bar the machine draws afterwards follows them.
     */
    private String fetchLines(final PackageManagerKind manager, final ProgramSpec spec) {
        final String pkg = spec.commandName();
        final String ver = ProgramVersions.of(spec.id());
        final int mb = spec.minDiskMb();
        return switch (manager) {
            case APT -> String.join("\n",
                    "Reading package lists... Done",
                    "Building dependency tree... Done",
                    "The following NEW packages will be installed:",
                    "  " + pkg,
                    "Need to get " + mb + " MB of archives.",
                    "Get:1 mirror://" + this.mirrorHostname() + " stable/main " + pkg + " " + ver
                            + " [" + mb + " MB]");
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
            case PKG -> String.join("\n",
                    "The following 1 package(s) will be affected (of 0 checked):",
                    "",
                    "New packages to be INSTALLED:",
                    "        " + pkg + ": " + ver,
                    "",
                    "Number of packages to be installed: 1",
                    "",
                    mb + " MiB to be downloaded.",
                    "[1/1] Fetching " + pkg + "-" + ver + ".pkg from mirror://" + this.mirrorHostname());
            default -> "Fetching " + pkg + " " + ver + " from mirror://" + this.mirrorHostname()
                    + " [" + mb + " MB]";
        };
    }

    /** The same, for a removal: what the manager says before it takes the package off. */
    private static String removeLines(final PackageManagerKind manager, final ProgramSpec spec) {
        final String pkg = spec.commandName();
        final String ver = ProgramVersions.of(spec.id());
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
            case PKG -> String.join("\n",
                    "Checking integrity... done (0 conflicting)",
                    "Deinstallation has been requested for the following 1 packages:",
                    "",
                    "Installed packages to be REMOVED:",
                    "        " + pkg + ": " + ver,
                    "",
                    "The operation will free " + spec.minDiskMb() + " MiB.",
                    "[1/1] Deinstalling " + pkg + "-" + ver + "...");
            default -> "Removing " + pkg + " ...";
        };
    }

    /** The build every package the Mirror serves is currently at: the mod's own version. */
    public static String modVersion() {
        return ModList.get()
                .getModContainerById(JsComputers.MODID)
                .map(container -> container.getModInfo().getVersion().toString())
                .orElse("0");
    }

    /** Brings every installed package up to the build this version ships. */
    public ICliComputer.OpResult update() {
        final PackageManagerKind manager = this.manager();
        if (manager == PackageManagerKind.NONE) {
            return ICliComputer.OpResult.fail(
                    "this system installs programs from install media, not a package manager");
        }
        final ComputerConsoleState console = this.terminal.console();
        if (console == null) {
            return ICliComputer.OpResult.fail("this computer cannot store installed programs");
        }
        if (this.mirrorMainframe() == null) {
            return ICliComputer.OpResult.fail("could not resolve mirror:// - connect this computer to a network whose"
                    + " Mainframe runs the Mirror service");
        }
        /*
         * Each package has a version of its own, and one installed at an older one is what an update
         * brings up. The program itself always runs the code this build ships, so an update reconciles
         * the record rather than moving files.
         */
        final List<String> outdated = console.outdatedPackages();
        if (outdated.isEmpty()) {
            return ICliComputer.OpResult.ok("All packages are up to date.");
        }
        final StringBuilder lines = new StringBuilder();
        for (final String id : outdated) {
            final String version = ProgramVersions.of(id);
            console.setInstalledVersion(id, version);
            final String path = id.contains(":") ? id.substring(id.indexOf(':') + 1) : id;
            lines.append("Setting up ").append(path).append(" (").append(version).append(") ...\n");
        }
        ((BlockEntity) this.terminal).setChanged();
        return ICliComputer.OpResult.ok(lines + "Updated " + outdated.size() + " package"
                + (outdated.size() == 1 ? "" : "s") + ".");
    }

    /** Takes a package off this machine, whether it is a player's or one the Mirror serves. */
    public ICliComputer.OpResult remove(final String name) {
        final String wanted = name == null ? "" : name.trim().toLowerCase(Locale.ROOT);
        final BlockEntity machine = (BlockEntity) this.terminal;
        final ComputerConsoleState theirs = this.terminal.console();
        if (theirs != null && theirs.communityProgram(wanted) != null) {
            // Its own files and nothing else: what was written when it was installed.
            for (final ICliComputer.FsEntry file : this.files.listDisk(COMMUNITY_DIR + "/" + wanted).entries()) {
                // The listing's name is the whole last segment, extension and all.
                this.files.deleteFile(COMMUNITY_DIR + "/" + wanted + "/" + file.name());
            }
            // And the folder it was installed into, which was its own: left behind, it stood in a reinstall's way.
            this.files.removeDir(COMMUNITY_DIR + "/" + wanted);
            theirs.removeCommunity(wanted);
            machine.setChanged();
            return ICliComputer.OpResult.ok("removed " + wanted);
        }
        ProgramSpec spec = null;
        for (final ProgramSpec candidate : OsRegistry.programs()) {
            if (candidate.installable()
                    && (candidate.commandName().equalsIgnoreCase(wanted)
                            || candidate.id().getPath().equalsIgnoreCase(wanted))) {
                spec = candidate;
                break;
            }
        }
        if (spec == null) {
            return ICliComputer.OpResult.fail("unable to locate package " + wanted);
        }
        /*
         * Removing is the same job as installing, run backwards and quicker; a Mainframe service also
         * turns its agent off when the job ends, so nothing is left running headless.
         */
        if (!(machine instanceof IOsHost host)) {
            return ICliComputer.OpResult.fail("this computer cannot store installed programs");
        }
        final ProgramSpec removing = spec;
        final PackageManagerKind manager = this.manager();
        final String via = manager == PackageManagerKind.NONE ? "uninstall" : manager.command();
        final Optional<String> refusal = SetupRunner.begin(host, this.level, machine.getBlockPos(), removing, null,
                true, via);
        return refusal.map(ICliComputer.OpResult::fail)
                .orElseGet(() -> ICliComputer.OpResult.ok(removeLines(manager, removing)));
    }

    /**
     * The services the network offers and how each stands, each row answered by whoever keeps that service: the
     * engine's by the language, the Mirror's here. A machine on no network with a Mainframe has none to list.
     */
    public List<ICliComputer.ServiceStatus> services() {
        if (this.mainframe() == null) {
            return List.of();
        }
        return List.of(new ICliComputer.ServiceStatus("IQL Engine", this.iql.state()),
                new ICliComputer.ServiceStatus("Mirror", this.state()));
    }

    /** The Mainframe of the machine's network, or null when it is on none, or none is running. */
    @Nullable
    private MainframeBlockEntity mainframe() {
        final NetworkUuid net = this.terminal.networkUuid();
        if (net == null) {
            return null;
        }
        return NetworkSystem.get(this.level).mainframePositionOf(net)
                .map(pos -> this.level.getBlockEntity(BlockPos.of(pos)) instanceof MainframeBlockEntity mf ? mf : null)
                .orElse(null);
    }
}
