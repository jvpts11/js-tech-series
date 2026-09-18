/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Computers.
 */
package dev.jstech.computers.machine;

import dev.jstech.computers.ComputingModule;
import dev.jstech.computers.os.FirmwareKind;
import dev.jstech.computers.os.IOsHost;
import dev.jstech.computers.os.OsDef;
import dev.jstech.computers.os.OsRegistry;
import dev.jstech.computers.os.Platform;
import dev.jstech.computers.os.ProgramSpec;
import dev.jstech.computers.item.DiskItem;
import dev.jstech.computers.os.fs.FileType;
import dev.jstech.computers.os.fs.FilesystemContents;
import dev.jstech.computers.os.fs.StoredFile;
import dev.jstech.computers.os.install.SetupJob;
import dev.jstech.computers.os.install.SetupRunner;
import dev.jstech.computers.os.install.SetupTiming;
import dev.jstech.computers.config.ComputersServerConfig;
import dev.jstech.computers.program.install.LiveInstallState;
import dev.jstech.computers.program.install.LiveTurn;
import dev.jstech.computers.program.install.MakeOpts;
import dev.jstech.computers.os.media.MediaFormat;
import dev.jstech.computers.os.media.MediaKind;
import dev.jstech.computers.os.media.MediaReaderBlockEntity;
import dev.jstech.computers.program.ComputerConsoleState;
import dev.jstech.computers.program.Programs;
import dev.jstech.computers.program.cli.ICliComputer;
import dev.jstech.computers.terminal.IComputerTerminalHost;
import dev.jstech.core.tier.HardwareEra;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;
import org.jetbrains.annotations.Nullable;

/**
 * What is installed on a machine, and the installing itself.
 *
 * <p>A program arrives one of three ways: it comes with the system, it is installed from a disc in a linked drive,
 * or a live medium walks the player through installing the system by hand. What a machine has is read the same way
 * whichever way it got there.
 */
public final class InstallService {

    private final IComputerTerminalHost terminal;
    private final ServerLevel level;
    /** The packages the machine installs over the network, which is what says whether it already has one. */
    private final PackageService packages;
    /** The network's own language, since its engine is installed on the Mainframe rather than here. */
    private final IqlService iql;

    public InstallService(final IComputerTerminalHost terminal, final ServerLevel level,
                          final PackageService packages, final IqlService iql) {
        this.terminal = terminal;
        this.level = level;
        this.packages = packages;
        this.iql = iql;
    }

    /**
     * The programs on this machine: the ones its platform ships with, and the ones installed onto it.
     *
     * <p>The platform is what keeps an MC-DOS listing from showing the Frames desktop apps, which come with the
     * Frames platform and with nothing else.
     */
    public List<ICliComputer.ProgramInfo> programs() {
        final List<ICliComputer.ProgramInfo> out = new ArrayList<>();
        final Platform platform = this.platform();
        for (final ProgramSpec spec : Programs.installed()) {
            if (platform == null || spec.platforms().contains(platform)) {
                out.add(new ICliComputer.ProgramInfo(spec.commandName(), spec.id().toString()));
            }
        }
        final ComputerConsoleState console = this.terminal.console();
        if (console != null) {
            for (final String id : console.installed()) {
                final ProgramSpec program = Programs.get(ResourceLocation.tryParse(id));
                if (program != null && !program.preinstalled()) {
                    out.add(new ICliComputer.ProgramInfo(program.commandName(), program.id().toString()));
                }
            }
        }
        return out;
    }

    /** The platform of the system installed on the machine, or null when it cannot be told. */
    @Nullable
    public Platform platform() {
        if (this.terminal instanceof IOsHost host) {
            final OsDef os = OsRegistry.getOs(host.installedOsId());
            return os == null ? null : os.platform();
        }
        return null;
    }

    /** Whether the program with that id is on this machine, installed or as a service the Mainframe keeps. */
    public boolean has(@Nullable final ResourceLocation id) {
        final ProgramSpec spec = id == null ? null : OsRegistry.getProgram(id);
        return spec != null && this.packages.has(spec);
    }

    /**
     * Installs a program from the disc in a linked drive.
     *
     * <p>Installing is something the machine does over time, from that disc. Whether it can, and why not, is decided
     * in one place for every way of asking, so the prompt says exactly what the Setup window on a desktop would.
     */
    public ICliComputer.OpResult install(final String programId) {
        final ResourceLocation location = ResourceLocation.tryParse(
                programId.contains(":") ? programId.toLowerCase(Locale.ROOT)
                        : "jsc:" + programId.toLowerCase(Locale.ROOT));
        final ProgramSpec program = location == null ? null : Programs.get(location);
        if (program == null) {
            return ICliComputer.OpResult.fail("no such program: " + programId);
        }
        if (program.id().equals(Programs.IQL_ENGINE)) {
            // The Engine is a service on the Mainframe, not a console-local app, so install it there.
            return this.iql.control("install");
        }
        final MediaFormat medium = this.installMediumFor(program.id());
        if (medium == null) {
            return ICliComputer.OpResult.fail(program.commandName() + " needs its install disc in a linked drive");
        }
        final IOsHost machine = this.osHost();
        if (machine == null) {
            return ICliComputer.OpResult.fail("this computer cannot store installed programs");
        }
        final Optional<String> refusal = SetupRunner.begin(machine, this.level,
                ((BlockEntity) this.terminal).getBlockPos(), program, medium, false, SetupJob.VIA_INSTALL);
        return refusal.map(ICliComputer.OpResult::fail)
                .orElseGet(() -> ICliComputer.OpResult.ok("Setting up " + program.commandName() + " from "
                        + driveName(medium) + " ..."));
    }

    /** The machine as the thing that installs programs, whichever of the two the prompt is held by. */
    @Nullable
    private IOsHost osHost() {
        if (this.terminal instanceof IOsHost fromHost) {
            return fromHost;
        }
        return (BlockEntity) this.terminal instanceof IOsHost fromBlock ? fromBlock : null;
    }

    /** What the disc a program comes from is called at a prompt. */
    private static String driveName(final MediaFormat medium) {
        return switch (medium) {
            case FLOPPY -> "the floppy";
            case CD -> "the CD";
            case DVD -> "the DVD";
            case USB -> "the USB drive";
        };
    }

    /** The live installation this machine booted into, or null when it booted a system of its own. */
    @Nullable
    public LiveInstallState live() {
        final ComputerConsoleState console = this.terminal.console();
        return console == null ? null : console.liveInstall();
    }

    /**
     * Runs one line of a live installer against the installation in progress.
     *
     * <p>When the sequence finishes, the system it was building lands on the chosen disk, that disk becomes the one
     * the machine boots, and the machine reboots for real: the prompt closes, the POST replays, and the new system
     * comes up. The reboot is asked for rather than done here, because who is at the terminal is the shell's to know.
     *
     * @param reboot what to call once the new system is written and the machine should come up on it
     */
    public LiveTurn liveRun(final String line, final Runnable reboot) {
        final ComputerConsoleState console = this.terminal.console();
        final LiveInstallState state = console == null ? null : console.liveInstall();
        final BlockEntity machine = (BlockEntity) this.terminal;
        if (state == null || !(machine instanceof IOsHost computer)) {
            return LiveTurn.refused("no live medium is booted");
        }
        /*
         * The devices the live system sees: every installed disk, in slot order (sda, sdb, ...), with the size
         * that is really written on it, so the tools print the disk the player put in rather than a made-up one.
         */
        final List<LiveInstallState.Device> devices = new ArrayList<>();
        for (int i = 0; i < computer.diskSlots(); i++) {
            final ItemStack stack = computer.diskInSlot(i);
            if (stack.getItem() instanceof DiskItem disk) {
                final long sizeMb = disk.spec().capacityItems() * disk.spec().era().mbPerItem();
                /* How fast the disk is goes with it, since making a filesystem and unpacking onto it are its work. */
                devices.add(new LiveInstallState.Device("sd" + (char) ('a' + i),
                        (int) Math.min(Integer.MAX_VALUE, sizeMb), disk.spec().tier().speedMultiplier()));
            }
        }
        /*
         * Which firmware this machine has decides whether the disk needs a partition of its own for the
         * bootloader, and which target the bootloader is installed for. It is the machine's generation that
         * says so, exactly as it does for the self-test.
         */
        final boolean uefi = FirmwareKind.forEra(
                computer.installedEra() != null ? computer.installedEra()
                        : HardwareEra.STANDARD)
                == FirmwareKind.UEFI;
        final HardwareEra era = computer.installedEra() != null ? computer.installedEra()
                : HardwareEra.STANDARD;
        final boolean everyStep = state.distro() == LiveInstallState.Distro.ARCH
                ? ComputersServerConfig.archEveryStep() : ComputersServerConfig.gentooEveryStep();
        final LiveTurn result = state.run(line, new LiveInstallState.Env(
                devices, this.packages.reachable(), this.level.getGameTime(), computer.cpuCores(),
                computer.maxCpuMhz(), SetupTiming.eraFactor(era),
                uefi, this.level.getDayTime(), everyStep));
        machine.setChanged();
        if (!result.complete()) {
            return result;
        }
        // The sequence completed: the hand-installed system lands on the chosen disk and boots first.
        final ResourceLocation osId = ResourceLocation.fromNamespaceAndPath("jsc",
                state.distro() == LiveInstallState.Distro.ARCH ? "arch" : "gentoo");
        final int target = state.targetIndex();
        if (!computer.installOs(osId, target)) {
            return LiveTurn.refused(result.text(),
                    "The installation could not be written to the disk (no space or no disk).");
        }
        computer.setBootDiskSlot(target);
        carryOver(computer, state, target);
        /*
         * Ask the host for the console again rather than reusing the reference taken at the top of this
         * method: writing the system may have replaced the disk stack, and the console is bound to the
         * drive it was read from. Clearing the stale binding would leave the finished live session on
         * the newly written disk, so the machine would boot straight back into the installer.
         */
        this.terminal.console().clearLiveInstall();
        machine.setChanged();
        reboot.run();
        return LiveTurn.said(result.text(), "Installation complete. Rebooting into the new system ...");
    }

    /**
     * Carries into the installed system what the player chose while building it.
     *
     * <p>Without this the whole sequence is theatre: the distribution lands on the disk and every decision made
     * getting it there is thrown away, so a machine somebody spent half an hour naming, laying out and fitting
     * with packages comes up nameless and bare. The name, the filesystem table, the build options and the
     * packages are what a person really decided, and each of them goes where that system keeps it.
     */
    private void carryOver(final IOsHost computer, final LiveInstallState state, final int target) {
        final ComputerConsoleState console = computer.console();
        if (console == null) {
            return;
        }
        if (!state.chosenName().isEmpty()) {
            // The name the installer was given is the name the prompt and the network use from here on.
            console.setComputerName(state.chosenName());
        }
        for (final String pkg : state.askedFor()) {
            final ProgramSpec program = Programs.get(ResourceLocation.tryParse(
                    pkg.contains(":") ? pkg : "jsc:" + pkg.toLowerCase(Locale.ROOT)));
            if (program != null) {
                console.install(program.id().toString());
            }
        }
        /*
         * The table goes onto the disk it describes, so the installed system can read back the line its own
         * bootloader was pointed at rather than taking it on trust.
         */
        final String table = state.filesystemTable();
        if (!table.isEmpty()) {
            writeSetting(computer, target, "/etc", "/etc/fstab", table);
        }
        /*
         * The build options go with it for the same reason: a system that builds what it installs goes on
         * building by them, so the jobs somebody gave it while installing are the jobs it compiles with.
         */
        final String options = state.buildOptions();
        if (!options.isEmpty()) {
            writeSetting(computer, target, "/etc/portage", MakeOpts.PATH, options);
        }
    }

    /** Puts one of the system's own settings files on the disk it was written to, where that system keeps it. */
    private static void writeSetting(final IOsHost computer, final int target, final String dir, final String path,
                                     final String content) {
        final int slot = target >= 0 ? target : computer.defaultInstallSlot();
        if (slot < 0) {
            return;
        }
        final ItemStack disk = computer.diskInSlot(slot);
        if (!(disk.getItem() instanceof DiskItem)) {
            return;
        }
        final FilesystemContents was = disk.getOrDefault(ComputingModule.FILESYSTEM.get(), FilesystemContents.EMPTY);
        disk.set(ComputingModule.FILESYSTEM.get(),
                was.withDir("/etc").withDir(dir).with(new StoredFile(path, FileType.CFG, content)));
    }

    /** The format of the disc a program's installer sits on in a linked drive, or null when none does. */
    @Nullable
    private MediaFormat installMediumFor(final ResourceLocation programId) {
        if (!(this.terminal instanceof IOsHost computer)) {
            return null;
        }
        for (final long endpoint : computer.linkedEndpoints()) {
            if (this.level.getBlockEntity(BlockPos.of(endpoint)) instanceof MediaReaderBlockEntity reader
                    && reader.insertedKind() == MediaKind.PROGRAM_INSTALL
                    && programId.equals(reader.insertedPayload())) {
                return reader.insertedFormat();
            }
        }
        return null;
    }

}
