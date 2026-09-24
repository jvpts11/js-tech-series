/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Computers.
 */
package dev.jstech.computers.machine;

import dev.jstech.computers.JsComputers;
import dev.jstech.computers.advancement.JscEvents;
import dev.jstech.computers.blockentity.AbstractComputerBlockEntity;
import dev.jstech.computers.blockentity.IMainframeService;
import dev.jstech.computers.blockentity.MainframeBlockEntity;
import dev.jstech.computers.os.IOsHost;
import dev.jstech.computers.os.OsDef;
import dev.jstech.computers.os.OsRegistry;
import dev.jstech.computers.os.PackageManagerKind;
import dev.jstech.computers.os.Platform;
import dev.jstech.computers.os.ProgramKind;
import dev.jstech.computers.os.ProgramSpec;
import dev.jstech.computers.os.ProgramVersions;
import dev.jstech.computers.os.install.SetupRunner;
import dev.jstech.computers.program.ComputerConsoleState;
import dev.jstech.computers.program.cli.CliTexts;
import dev.jstech.computers.program.cli.ICliComputer;
import dev.jstech.computers.program.cli.ICliPackages;
import dev.jstech.computers.program.cli.SigmaCommands;
import dev.jstech.computers.program.install.MirrorPackage;
import dev.jstech.computers.program.install.voice.PackageManagerVoices;
import dev.jstech.computers.program.tty.TtyScriptProcess;
import dev.jstech.computers.terminal.IComputerTerminalHost;
import dev.jstech.core.text.Text;
import dev.jstech.core.text.TextHolder;
import dev.jstech.core.text.TextKey;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.neoforged.fml.ModList;
import org.jetbrains.annotations.Nullable;

/**
 * The packages a machine installs over its network's Mirror, in the words of its own package manager.
 *
 * <p>The Mirror itself, which is the network's and not the machine's, is {@link MirrorService}; the packages players
 * publish on it are installed by {@link CommunityPackages}. A machine that cannot reach a serving Mirror is told so
 * in the words its own package manager would use, because that is what a player reads at the prompt.
 */
@TextHolder
public final class PackageService {

    private final IComputerTerminalHost terminal;
    private final ServerLevel level;
    private final MirrorService mirror;
    private final CommunityPackages community;
    private final InstallGates gates;

    /** What every way of installing says on a machine that has nowhere to keep what it installs. */
    static final TextKey NO_STORE =
            TextKey.of("jsc.service.package.no_store", "this computer cannot store installed programs");

    private static final TextKey NO_MANAGER = TextKey.of("jsc.service.package.no_manager",
            "this system installs programs from install media, not a package manager");
    private static final TextKey UP_TO_DATE =
            TextKey.of("jsc.service.package.up_to_date", "All packages are up to date.");
    private static final TextKey SETTING_UP_VERSION =
            TextKey.of("jsc.service.package.setting_up_version", "Setting up %s (%s) ...");
    private static final TextKey UPDATED_ONE = TextKey.of("jsc.service.package.updated_one", "Updated %s package.");
    private static final TextKey UPDATED_MANY =
            TextKey.of("jsc.service.package.updated_many", "Updated %s packages.");
    private static final TextKey ALREADY_NEWEST =
            TextKey.of("jsc.service.package.already_newest", "%s is already the newest version");
    /* pkg's two lines for a package already there, kept apart so the second can be coloured as the outcome. */
    private static final TextKey INTEGRITY_CHECKED = TextKey.of("jsc.service.package.integrity_checked",
            "Checking integrity... done (0 conflicting)");
    private static final TextKey NEWEST_INSTALLED = TextKey.of("jsc.service.package.newest_installed",
            "The most recent versions of packages are already installed");
    private static final TextKey SET_UP = TextKey.of("jsc.service.package.set_up", "Setting up %s ... done");
    private static final TextKey NOT_SET_UP = TextKey.of("jsc.service.package.not_set_up", "%s could not be set up");
    private static final TextKey UNMET =
            TextKey.of("jsc.service.package.unmet", "unmet requirements (hardware or free disk space)");

    public PackageService(final IComputerTerminalHost terminal, final ServerLevel level, final FileService files,
                          final IqlService iql) {
        this.terminal = terminal;
        this.level = level;
        this.mirror = new MirrorService(terminal, level, files, iql);
        this.community = new CommunityPackages(terminal, files, this.mirror, this::hasRuntime);
        this.gates = new InstallGates(terminal);
    }

    /** The network's Mirror, as this machine reaches it. */
    public MirrorService mirror() {
        return this.mirror;
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
        if (machine instanceof MainframeBlockEntity mf && mf.service(spec.id()) != null) {
            return mf.service(spec.id()).installed();
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
            /*
             * The hardware is the one thing that is already true of a machine still being built by hand, so the era
             * rule every other way of installing keeps is kept here too.
             */
            if (spec.installable() && spec.kind() != ProgramKind.SERVICE && spec.platforms().contains(Platform.LINUX)
                    && named(typed, spec, hasATree) && this.gates.machine(spec) == null
                    && this.gates.era(spec) == null) {
                return SourceChains.packageOf(spec);
            }
        }
        return null;
    }

    /**
     * What the Mirror has for this machine: first what it can serve, each saying whether the machine already has
     * it, then whatever players on this network have published, marked as theirs.
     */
    public List<ICliComputer.PackageInfo> available() {
        if (!this.mirror.reachable()) {
            return List.of();
        }
        final List<ICliComputer.PackageInfo> out = new ArrayList<>();
        for (final ProgramSpec spec : this.offered()) {
            out.add(new ICliComputer.PackageInfo(spec.commandName(), spec.displayName()
                    + (spec.kind() == ProgramKind.SERVICE ? " (service)" : ""), this.has(spec)));
        }
        out.addAll(this.mirror.shelved());
        return out;
    }

    /** What stands between this machine and a program it is asked to install. */
    InstallGates gates() {
        return this.gates;
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
            return ICliPackages.Installing.said(ICliComputer.OpResult.fail(ICliPackages.UNABLE_TO_LOCATE.with(wanted)));
        }
        final ICliComputer.OpResult stopped = this.whyNotHere(spec);
        if (stopped != null) {
            return ICliPackages.Installing.said(stopped);
        }
        final BlockEntity machine = (BlockEntity) this.terminal;
        final ComputerConsoleState console = this.terminal.console();
        if (this.manager().compilesFromSource() && machine instanceof IOsHost builder && console != null
                && !(machine instanceof MainframeBlockEntity && spec.kind() == ProgramKind.SERVICE)
                && InstallGates.fits(builder, spec, true)) {
            return ICliPackages.Installing.running(new TtyScriptProcess(SourceBuild.of(spec, builder, ask,
                    () -> builtHere(machine, console, spec))));
        }
        return ICliPackages.Installing.said(this.installAtOnce(spec));
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
            return ICliComputer.OpResult.fail(NO_MANAGER);
        }
        final ComputerConsoleState console = this.terminal.console();
        if (console == null) {
            return ICliComputer.OpResult.fail(NO_STORE);
        }
        if (!this.mirror.reachable()) {
            return ICliComputer.OpResult.fail(MirrorService.NO_MIRROR_SERVICE);
        }
        /*
         * Each package has a version of its own, and one installed at an older one is what an update
         * brings up. The program itself always runs the code this build ships, so an update reconciles
         * the record rather than moving files.
         */
        final List<String> outdated = console.outdatedPackages();
        if (outdated.isEmpty()) {
            return ICliComputer.OpResult.ok(UP_TO_DATE);
        }
        final List<Text> lines = new ArrayList<>(outdated.size() + 1);
        for (final String id : outdated) {
            final String version = ProgramVersions.of(id);
            console.setInstalledVersion(id, version);
            final String path = id.contains(":") ? id.substring(id.indexOf(':') + 1) : id;
            lines.add(SETTING_UP_VERSION.with(path, version));
        }
        lines.add((outdated.size() == 1 ? UPDATED_ONE : UPDATED_MANY).with(outdated.size()));
        ((BlockEntity) this.terminal).setChanged();
        return ICliComputer.OpResult.ok(ICliPackages.lines(lines));
    }

    /** Takes a package off this machine, whether it is a player's or one the Mirror serves. */
    public ICliComputer.OpResult remove(final String name) {
        final String wanted = name == null ? "" : name.trim().toLowerCase(Locale.ROOT);
        final ICliComputer.OpResult theirs = this.community.remove(wanted);
        if (theirs != null) {
            return theirs;
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
            return ICliComputer.OpResult.fail(ICliPackages.UNABLE_TO_LOCATE.with(wanted));
        }
        /*
         * Removing is the same job as installing, run backwards and quicker; a Mainframe service also
         * turns its agent off when the job ends, so nothing is left running headless.
         */
        final BlockEntity machine = (BlockEntity) this.terminal;
        if (!(machine instanceof IOsHost host)) {
            return ICliComputer.OpResult.fail(NO_STORE);
        }
        final ProgramSpec removing = spec;
        final PackageManagerKind manager = this.manager();
        final Optional<String> refusal = SetupRunner.begin(host, this.level, machine.getBlockPos(), removing, null,
                true, manager);
        return refusal.map(ICliComputer.OpResult::fail).orElseGet(() -> ICliComputer.OpResult.ok(
                PackageManagerVoices.remove(manager, removing.commandName(), ProgramVersions.of(removing.id()),
                        removing.minDiskMb())));
    }

    /**
     * What a program built on this machine from source means to it once the build ends: installed, at the version
     * it was built at like every other way of installing it (without that it was listed as outdated the moment it
     * finished building), and recorded as built here, which is what lets it ask a little less of the machine.
     */
    static void builtHere(final BlockEntity machine, final ComputerConsoleState console, final ProgramSpec spec) {
        final String id = spec.id().toString();
        console.install(id);
        console.setInstalledVersion(id, ProgramVersions.of(id));
        console.markBuiltFromSource(id);
        machine.setChanged();
        reportInstalled(machine, spec);
    }

    /** What is answered before the Mirror is looked at: a system with no manager, and a player's own package. */
    @Nullable
    private ICliComputer.OpResult beforeTheMirror(final String wanted) {
        if (this.manager() == PackageManagerKind.NONE) {
            return ICliComputer.OpResult.fail(NO_MANAGER);
        }
        final ICliComputer.OpResult theirs = this.community.install(wanted);
        if (theirs != null) {
            return theirs;
        }
        if (!this.mirror.reachable()) {
            return ICliComputer.OpResult.fail(MirrorService.NO_MIRROR_SERVICE);
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

    /** Why that program is not installed on this machine, or null when nothing stands in its way. */
    @Nullable
    private ICliComputer.OpResult whyNotHere(final ProgramSpec spec) {
        final ICliComputer.OpResult tooOld = this.gates.era(spec);
        if (tooOld != null) {
            return tooOld;
        }
        final ICliComputer.OpResult elsewhere = this.gates.machine(spec);
        if (elsewhere != null) {
            return elsewhere;
        }
        if (this.has(spec)) {
            // Said the way the manager asked says it: pkg checks and finds nothing to do, the others report a version.
            return ICliComputer.OpResult.ok(this.manager() == PackageManagerKind.PKG
                    ? ICliPackages.lines(List.of(INTEGRITY_CHECKED.text(), NEWEST_INSTALLED.text()))
                    : ALREADY_NEWEST.with(spec.commandName()));
        }
        return null;
    }

    /** Whether the Σ# runtime is on this machine, which is what a player's package needs to run. */
    private boolean hasRuntime() {
        final ProgramSpec runtime = OsRegistry.getProgram(ResourceLocation.tryParse(SigmaCommands.RUNTIME));
        return runtime != null && this.has(runtime);
    }

    /* A package the Mirror handed over, installed without the timed setup that reports its own. */
    private static void reportInstalled(final BlockEntity machine, final ProgramSpec spec) {
        JscEvents.awardOperator(machine, JscEvents.PROGRAM_INSTALLED, spec.id().getPath());
        JscEvents.awardOperator(machine, JscEvents.MIRROR_INSTALL, spec.id().getPath());
    }

    /** Installs a program the way a manager that ships built packages does, answering at once. */
    private ICliComputer.OpResult installAtOnce(final ProgramSpec spec) {
        final PackageManagerKind manager = this.manager();
        final BlockEntity machine = (BlockEntity) this.terminal;
        final ComputerConsoleState console = this.terminal.console();
        // A Mainframe service switches its flag on directly (a prebuilt daemon, so no source build either).
        if (machine instanceof MainframeBlockEntity mf && spec.kind() == ProgramKind.SERVICE) {
            final IMainframeService service = mf.service(spec.id());
            final boolean switchedOn = service != null && service.install();
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
            if (done) {
                reportInstalled(machine, spec);
            }
            return done ? ICliComputer.OpResult.ok(SET_UP.with(spec.commandName()))
                    : ICliComputer.OpResult.fail(NOT_SET_UP.with(spec.commandName()));
        }
        if (machine instanceof IOsHost oc && !InstallGates.fits(oc, spec, false)) {
            return ICliComputer.OpResult.fail(CliTexts.SAID_BY.with(spec.commandName(), UNMET));
        }
        if (console == null) {
            return ICliComputer.OpResult.fail(NO_STORE);
        }
        /*
         * A package from the Mirror is fetched over the network and set up over time, the way the same
         * program from a disc is; the manager's own gates above have already said it may.
         */
        if (!(machine instanceof IOsHost host)) {
            return ICliComputer.OpResult.fail(NO_STORE);
        }
        final Optional<String> refusal = SetupRunner.begin(host, this.level, machine.getBlockPos(), spec, null,
                false, manager);
        return refusal.map(ICliComputer.OpResult::fail).orElseGet(() -> ICliComputer.OpResult.ok(
                PackageManagerVoices.fetch(manager, spec.commandName(), ProgramVersions.of(spec.id()),
                        spec.minDiskMb(), this.mirror.hostname())));
    }
}
