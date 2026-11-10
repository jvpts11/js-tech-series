/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Computers.
 */
package dev.jstech.computers.machine;

import dev.jstech.computers.os.IOsHost;
import dev.jstech.computers.os.OsDef;
import dev.jstech.computers.os.OsRegistry;
import dev.jstech.computers.os.devices.DeviceMap;
import dev.jstech.computers.os.devices.DeviceMaps;
import dev.jstech.computers.os.media.IMediaDrive;
import dev.jstech.computers.os.media.MachineMedia;
import dev.jstech.computers.os.media.MediaFormat;
import dev.jstech.computers.os.player.PlayerDesktop;
import dev.jstech.computers.os.player.PlayerSystem;
import dev.jstech.computers.os.player.PlayerSystems;
import dev.jstech.computers.os.player.SystemRun;
import dev.jstech.computers.program.cli.ICliComputer;
import dev.jstech.computers.sigma.SystemFacts;
import dev.jstech.computers.vm.program.IProgramParent;
import dev.jstech.computers.vm.program.ProgramPriority;
import dev.jstech.core.text.GameText;
import dev.jstech.core.text.Text;
import dev.jstech.core.text.TextHolder;
import dev.jstech.core.text.TextKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.entity.BlockEntity;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * What a machine does for the system a player wrote that runs on it (computers/docs/design/player-systems.md):
 * the picture of its coming up, its panic page, its text screen, its services, its desktop, the program the input
 * goes to, and what it reads of the machine: its disk, its memory and its devices.
 *
 * <p>The drawing, the files and the windows are the machine's work, which costs the system nothing beyond its call.
 */
@TextHolder
public final class PlayerSystemService {

    private final IOsHost machine;
    private final MachineServices services;

    /** The words a status answers in, as a service manager's are. */
    public static final String RUNNING = "running";
    public static final String STOPPED = "stopped";
    public static final String UNKNOWN = "unknown";

    /* The class of device a desktop is drawn on, as a system's drivers name it. */
    private static final String GRAPHICS = "graphics";
    private static final TextKey NO_GRAPHICS = TextKey.of("jsc.service.system.no_graphics",
            "the system drives no graphics screen, so it has nowhere to draw a desktop");

    PlayerSystemService(final IOsHost machine, final MachineServices services) {
        this.machine = machine;
        this.services = services;
    }

    /**
     * Whether the system that machine runs drives devices of that class: each of the mod's does, and a player's on a
     * kernel of its own only those it chose, a device with no driver not being there for it at all.
     */
    public static boolean drives(final IOsHost machine, final String deviceClass) {
        final OsDef os = machine.installedOs();
        final PlayerSystem system = os == null ? null : PlayerSystems.withFiles(os.id());
        return system == null || system.facts().kernel().drives(deviceClass);
    }

    /** Whether the system this machine runs drives devices of that class. */
    public boolean drives(final String deviceClass) {
        return drives(this.machine, deviceClass);
    }

    /** What the system has said while it runs here, or null when the machine runs none of a player's. */
    @Nullable
    public SystemRun run() {
        return PlayerSystemBoot.isPlayers(this.machine) ? this.machine.systemRun() : null;
    }

    /** What the system declared, or null when the machine runs none of a player's. */
    @Nullable
    public SystemFacts facts() {
        final OsDef os = this.machine.installedOs();
        final PlayerSystem system = os == null ? null : PlayerSystems.withFiles(os.id());
        return system == null ? null : system.facts();
    }

    /** Brings the system down on its own page with that message. */
    public void halt(final String message) {
        PlayerSystemBoot.panic(this.machine, this.machine.installedOs(), message, List.of());
    }

    /**
     * Starts a service by name: the program registered under it, or the listing of that name in the system's folder,
     * which its build wrote out for each service its sources hold. Answers whether it is up.
     */
    public boolean startService(final String name) {
        final SystemRun run = this.run();
        final ProgramService programs = this.services.programs();
        if (run == null || programs == null) {
            return false;
        }
        final Integer up = run.started().get(name);
        if (up != null && programs.running(up, "")) {
            return true;
        }
        final String path = fromRoot(run.registered().getOrDefault(name,
                this.folder() + name.toLowerCase(Locale.ROOT) + ".asm"));
        final ProgramLauncher.Launch launch = programs.start(path, List.of(),
                new IProgramParent.Local(MachinePrograms.SYSTEM_PID), ProgramPriority.MEDIUM);
        if (!launch.ok()) {
            return false;
        }
        run.started().put(name, launch.id());
        this.machine.setChanged();
        return true;
    }

    /** Stops a service by name; answers whether it was up. */
    public boolean stopService(final String name) {
        final SystemRun run = this.run();
        final ProgramService programs = this.services.programs();
        final Integer up = run == null ? null : run.started().remove(name);
        return up != null && programs != null && programs.kill(up, "");
    }

    /** How a service stands: running, stopped, or unknown for a name the system never registered nor started. */
    public String serviceStatus(final String name) {
        final SystemRun run = this.run();
        final ProgramService programs = this.services.programs();
        if (run == null) {
            return UNKNOWN;
        }
        final Integer up = run.started().get(name);
        if (up != null && programs != null && programs.running(up, "")) {
            return RUNNING;
        }
        return up != null || run.registered().containsKey(name) ? STOPPED : UNKNOWN;
    }

    /** Starts every service the system registered, in the order it registered them, as the system comes up. */
    public void startRegistered() {
        final SystemRun run = this.run();
        if (run != null) {
            for (final String name : List.copyOf(run.registered().keySet())) {
                this.startService(name);
            }
        }
    }

    /**
     * The desktop of that package, one of the mod's (KDE Plasma, GNOME, Cinnamon, CDE) or a player's, or null when
     * there is none of that name.
     */
    @Nullable
    public ResourceLocation desktopNamed(final String name) {
        final ResourceLocation player = PlayerDesktop.idOf(name);
        if (PlayerSystems.knownDesktop(player) != null) {
            return player;
        }
        final String path = name.toLowerCase(Locale.ROOT).replace('-', '_').replace(' ', '_');
        final ResourceLocation mod = ResourceLocation.fromNamespaceAndPath("jsc", path);
        return OsRegistry.getDesktop(mod) != null ? mod : null;
    }

    /**
     * Starts that desktop: at once on a system already up, or when it comes up when asked while it boots. A player's
     * desktop comes with the system that starts it, so one not yet on the machine is laid on it as its package
     * installs. Answers why it could not be, or null.
     */
    @Nullable
    public Text startDesktop(@Nullable final ResourceLocation id) {
        final SystemRun run = this.run();
        if (run == null) {
            return null;
        }
        // A desktop is drawn on the graphics screen, which a system with no graphics driver does not have.
        if (id != null && !this.drives(GRAPHICS)) {
            return NO_GRAPHICS.text();
        }
        final PlayerDesktop desktop = PlayerSystems.desktopWithFiles(id);
        if (desktop != null) {
            final ICliComputer.OpResult laid = this.services.packages().layDesktop(desktop);
            if (!laid.ok()) {
                return laid.message();
            }
        }
        run.desktop(id);
        if (run.done()) {
            this.machine.setBootedDesktopId(id);
        }
        this.machine.setChanged();
        return null;
    }

    /** Gives a process another priority, by its word: high, normal, low or idle. Answers whether it is here. */
    public boolean setPriority(final int pid, final String level) {
        final MachinePrograms programs = this.machine.programs();
        return programs != null && programs.setPriority(pid, ProgramPriority.named(level));
    }

    /** The room left on the disk the system is on, in megabytes. */
    public long diskFreeMb() {
        final ComputerInfoService info = this.services.computer();
        if (info == null || info.disks().isEmpty()) {
            return 0;
        }
        final ComputerInfoService.Disk disk = info.disks().getFirst();
        return Math.max(0, disk.capacityMb() - disk.usedMb());
    }

    /** What the disk the system is on holds in all, in megabytes. */
    public long diskSizeMb() {
        final ComputerInfoService info = this.services.computer();
        return info == null || info.disks().isEmpty() ? 0 : info.disks().getFirst().capacityMb();
    }

    /** The other disks and media the system reaches, by where they are mounted. */
    public List<String> mounted() {
        final ComputerInfoService info = this.services.computer();
        final List<String> out = new ArrayList<>();
        if (info != null) {
            for (final ComputerInfoService.Disk disk : info.disks()) {
                out.add(disk.mount());
            }
        }
        return out;
    }

    /** What a process holds, in megabytes; 0 for one that is not running. */
    public int heldBy(final int pid) {
        final ComputerInfoService info = this.services.computer();
        if (info != null) {
            for (final ComputerInfoService.Running running : info.running()) {
                if (running.id() == pid) {
                    return (int) (running.heldBytes() >> 20);
                }
            }
        }
        return 0;
    }

    /** The memory's ledger as the Task Manager lists it: each process, with what it holds. */
    public List<String> ledger() {
        final ComputerInfoService info = this.services.computer();
        final List<String> out = new ArrayList<>();
        if (info != null) {
            for (final ComputerInfoService.Running running : info.running()) {
                out.add(running.id() + " " + running.name() + " " + (running.heldBytes() >> 20) + " MB");
            }
        }
        return out;
    }

    /**
     * The devices the machine has that the system drives, each with its class: the text screen, the graphics cards,
     * what plays sound, the network adapters, the disks and the optical drives. A device with no driver is not there
     * for the system at all.
     */
    public List<Device> devices() {
        final List<Device> out = new ArrayList<>();
        final SystemFacts facts = this.facts();
        if (!(this.machine instanceof BlockEntity block) || !(block.getLevel() instanceof ServerLevel level)) {
            return out;
        }
        final DeviceMap map = DeviceMaps.of(level, this.machine);
        out.add(new Device("Console", "text"));
        for (final DeviceMap.VideoCard card : map.video()) {
            out.add(new Device(GameText.resolve(card.name()), "graphics"));
        }
        for (final DeviceMap.AudioSource source : map.audio()) {
            out.add(new Device(GameText.resolve(source.name()), "sound"));
        }
        for (final Text adapter : map.network()) {
            out.add(new Device(GameText.resolve(adapter), "network"));
        }
        for (final Text disk : map.disks()) {
            out.add(new Device(GameText.resolve(disk), "disks"));
        }
        for (final IMediaDrive drive : MachineMedia.of(this.machine, level).drives()) {
            if (drive.type().accepts(MediaFormat.CD) || drive.type().accepts(MediaFormat.DVD)) {
                out.add(new Device(GameText.resolve(drive.type().driveName()), "optical"));
            }
        }
        out.removeIf(device -> facts != null && !facts.kernel().drives(device.deviceClass()));
        return out;
    }

    /** Whether the machine has a device of that class the system drives. */
    public boolean has(final String deviceClass) {
        for (final Device device : this.devices()) {
            if (device.deviceClass().equals(deviceClass)) {
                return true;
            }
        }
        return false;
    }

    /* The system's own folder on its disk, with its slash, where its listings are. */
    /* A service's path as the system registered it, read from the disk's root when it names no root of its own. */
    private static String fromRoot(final String path) {
        final boolean rooted = path.startsWith("/") || path.startsWith("\\")
                || path.length() > 1 && path.charAt(1) == ':';
        return rooted ? path : "/" + path;
    }

    /* The system's folder from the disk's root, wherever the machine's prompt stands. */
    private String folder() {
        final OsDef os = this.machine.installedOs();
        final PlayerSystem system = os == null ? null : PlayerSystems.withFiles(os.id());
        return system == null ? "/" : "/" + system.folder() + "/";
    }

    /** A device the system drives, by name, with its class. */
    public record Device(String name, String deviceClass) {
    }
}
