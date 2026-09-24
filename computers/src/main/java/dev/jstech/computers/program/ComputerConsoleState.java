/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Computers.
 */
package dev.jstech.computers.program;

import dev.jstech.computers.os.PackageManagerKind;
import dev.jstech.computers.os.install.InstallerFlow;
import dev.jstech.computers.os.install.SetupJob;
import dev.jstech.computers.program.install.LiveInstallState;
import dev.jstech.computers.program.job.JobStorage;
import dev.jstech.computers.program.job.MachineJobs;
import dev.jstech.core.id.StableNames;
import dev.jstech.core.text.TextTags;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Deque;
import java.util.List;
import java.util.Set;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.StringTag;
import net.minecraft.nbt.Tag;
import org.jetbrains.annotations.Nullable;

/**
 * The per-computer state behind the machine's prompt and desktop, held on the host BlockEntity and saved with its
 * NBT: the command history (so it survives closing the prompt or the Monitor, and a world reload), the programs
 * installed on the computer, the work it does on its own, and its settings.
 *
 * <p>The installed programs are the heart of it and are asked of it directly; they are kept by
 * {@link InstalledPrograms}. Where the shells stand is {@link ShellLocations}, how the desktop is laid out is
 * {@link DesktopLayout}, and how the settings are saved is {@link SettingsStorage}.
 */
public final class ComputerConsoleState {

    private final Deque<String> history = new ArrayDeque<>();
    private final InstalledPrograms programs = new InstalledPrograms();
    /**
     * The work this machine does with nobody at it: lines left running and lines to be run at an hour.
     *
     * <p>They belong to the computer and not to whoever typed them, so they go with it through a save and
     * carry on while the player is away, which is the whole reason for having them.
     */
    private final MachineJobs jobs = new MachineJobs();
    private final ComputerSettings settings = new ComputerSettings();
    private final ShellLocations locations = new ShellLocations();
    private final DesktopLayout desktop = new DesktopLayout();
    /*
     * The tool running in front of this machine's terminal, if one is: a fetch, an unpack, a compile. Kept
     * apart from this class because it is a thing of its own, with its own rules about what is written down.
     */
    private final TerminalForeground foreground = new TerminalForeground();
    private String computerName = "";
    /**
     * Which run of this machine is on the glass, counted up every time the machine starts over.
     *
     * <p>What a terminal has printed belongs to the run of the machine that printed it. A restart ends that
     * run, and the lines from before it are not the new system's: a machine that had an installation typed
     * into it came up showing the whole installation, and a disk swapped for a blank one came up showing the
     * session of the disk that had been taken out.
     *
     * <p>Deliberately not saved. A machine reloaded from disk is a machine whose terminal nobody is looking
     * at, and counting from zero again only means the first terminal opened after a reload starts clean.
     */
    private long session;
    /*
     * The program being set up right now, if any. One at a time: a machine installs one thing and
     * then the next, and a second request while one runs is told the machine is busy.
     */
    @Nullable
    private SetupJob setup;
    /*
     * A live installation medium booted on this computer (the manual Arch / Gentoo install), until it reboots
     * into the installed system. Persisted so a half-done install survives a reload.
     */
    @Nullable
    private LiveInstallState liveInstall;
    /*
     * The machine this session is currently ssh'd into, as a packed block position, or null when the
     * shell is local. In memory like the rest of the session: closing the terminal drops the remote
     * shell, exactly as hanging up a real one does.
     */
    @Nullable
    private Long sshTarget;

    public static final int MAX_HISTORY = 100;

    private static final StableNames<PackageManagerKind> MANAGERS = StableNames.of(PackageManagerKind.class);

    /**
     * A program written by a player and installed from the Mirror.
     *
     * <p>It is not a {@code ProgramSpec}: those are the mod's own and are registered when the game
     * starts, and there is no registering something a player wrote yesterday on a server. So the little
     * the desktop needs in order to give it an icon and run it is kept here, with the machine that
     * installed it.
     *
     * @param entry the listing to run, as a path on this machine's disk
     */
    public record Community(String name, String version, String house, String icon, String entry) {
    }

    /** The per-computer settings owned by the Settings app and the {@code config} command. */
    public ComputerSettings settings() {
        return settings;
    }

    /** The work this machine does with nobody at it. */
    public MachineJobs jobs() {
        return this.jobs;
    }

    /** Where this machine's shells stand. */
    public ShellLocations locations() {
        return this.locations;
    }

    /** How this machine's desktop is laid out. */
    public DesktopLayout desktop() {
        return this.desktop;
    }

    /** The command history, oldest first. */
    public List<String> history() {
        return new ArrayList<>(history);
    }

    /** Appends a command, de-duplicating so a repeated command moves to the end, capped at {@link #MAX_HISTORY}. */
    public void pushHistory(final String line) {
        if (line == null || line.isBlank()) {
            return;
        }
        history.remove(line);
        history.addLast(line);
        while (history.size() > MAX_HISTORY) {
            history.removeFirst();
        }
    }

    public Set<String> installed() {
        return programs.installed();
    }

    public boolean isInstalled(final String programId) {
        return programs.isInstalled(programId);
    }

    /** Installs a program by id; returns false if it was already installed. A fresh copy is a package. */
    public boolean install(final String programId) {
        return programs.install(programId);
    }

    public boolean uninstall(final String programId) {
        return programs.uninstall(programId);
    }

    /** The version a package was installed at, or {@code ""} when it predates version tracking. */
    public String installedVersion(final String programId) {
        return programs.version(programId);
    }

    public void setInstalledVersion(final String programId, final String version) {
        programs.setVersion(programId, version);
    }

    /**
     * Every installed package behind the version this build ships for it, each against its own version: a
     * version is part of a program's face, so an update brings each one up to what this build of it is.
     */
    public List<String> outdatedPackages() {
        return programs.outdated();
    }

    /** Whether that installed program was built on this machine from source. */
    public boolean builtFromSource(final String programId) {
        return programs.builtFromSource(programId);
    }

    /** Every installed program built on this machine from source. */
    public Set<String> builtFromSource() {
        return programs.builtFromSource();
    }

    /** Records that an installed program was built here from source; a program not installed is not recorded. */
    public void markBuiltFromSource(final String programId) {
        programs.markBuiltFromSource(programId);
    }

    /** Every player-written program installed here. */
    public Collection<Community> community() {
        return programs.community();
    }

    /** One of them, or null. */
    @Nullable
    public Community communityProgram(final String name) {
        return programs.communityProgram(name);
    }

    /** Records one as installed. */
    public void addCommunity(final Community program) {
        programs.addCommunity(program);
    }

    /** Forgets one; false when it was not installed. */
    public boolean removeCommunity(final String name) {
        return programs.removeCommunity(name);
    }

    /** What the machine is setting up, or null when nothing. */
    @Nullable
    public SetupJob setup() {
        return setup;
    }

    /** Starts a setup; the caller has already checked the machine can take the program. */
    public void beginSetup(final SetupJob job) {
        this.setup = job;
    }

    /** Forgets the setup, done or cancelled. */
    public void clearSetup() {
        this.setup = null;
    }

    /** The live installation in progress, or null when the computer is not booted from a live medium. */
    @Nullable
    public LiveInstallState liveInstall() {
        return liveInstall;
    }

    /** Boots a live medium: starts a fresh manual installation of the given distribution. */
    public void startLiveInstall(final LiveInstallState.Distro distro) {
        this.liveInstall = new LiveInstallState(distro);
    }

    /** Ends the live session (the install completed, or the medium was abandoned). */
    public void clearLiveInstall() {
        this.liveInstall = null;
    }

    /** What is running in front of the terminal, which may be nothing. */
    public TerminalForeground foreground() {
        return this.foreground;
    }

    /** Which run of this machine is on the glass. */
    public long session() {
        return this.session;
    }

    /** The machine started over: whatever a terminal printed belongs to the run that has just ended. */
    public void newSession() {
        this.session++;
        /* Whatever was running in front of it went down with the machine, unfinished. */
        this.foreground.clear();
    }

    /** The player-given computer name ({@code ""} means unset). */
    public String computerName() {
        return computerName;
    }

    /**
     * Names the computer, cut to the length a name may be.
     *
     * <p>Cut here, at the machine that keeps it, rather than only where it is typed. The name rides on
     * several packets to the screens that show it, some of which refuse a string past their own length by
     * throwing rather than by trimming, so a name that was never cut at the source would take the player's
     * desktop down instead of simply reading short.
     */
    public void setComputerName(final String name) {
        final String given = name == null ? "" : name;
        this.computerName = given.length() <= InstallerFlow.MOST_NAME_LETTERS ? given
                : given.substring(0, InstallerFlow.MOST_NAME_LETTERS);
    }

    /** The packed position of the machine this session is connected to, or null when local. */
    @Nullable
    public Long sshTarget() {
        return sshTarget;
    }

    public void setSshTarget(@Nullable final Long packedPos) {
        this.sshTarget = packedPos;
    }

    /**
     * Erases everything the software layer remembered, because the disk it conceptually lived on was just
     * formatted: command history, where the shells stood, installed programs, and any builds. The next system
     * starts from a genuinely clean console.
     */
    public void wipeSoftware() {
        history.clear();
        locations.clear();
        // What a player installed from the Mirror, which version of each program, and which were built here, were
        // on that disk too.
        programs.clear();
        // Whatever was being built went with the system it was being built for.
        foreground.clear();
    }

    public void save(final CompoundTag tag) {
        final ListTag historyTag = new ListTag();
        for (final String line : history) {
            historyTag.add(StringTag.valueOf(line));
        }
        tag.put("History", historyTag);
        JobStorage.save(this.jobs, tag);
        if (setup != null) {
            // A setup half done goes with the machine, so the world coming back finds it still copying.
            final CompoundTag job = new CompoundTag();
            job.putString("Program", setup.programId());
            job.putString("Name", setup.name());
            job.putString("House", setup.house());
            job.putInt("SizeMb", setup.sizeMb());
            job.put("Source", TextTags.write(setup.source()));
            job.putBoolean("Removing", setup.removing());
            job.putInt("Total", setup.ticksTotal());
            job.putInt("Left", setup.ticksLeft());
            job.putString("Via", setup.manager().serializedName());
            job.putString("Package", setup.packageName());
            tag.put("Setup", job);
        }
        programs.save(tag);
        if (liveInstall != null) {
            tag.putString("LiveInstall", liveInstall.serialize());
        }
        if (foreground.running()) {
            final CompoundTag front = new CompoundTag();
            foreground.save(front);
            tag.put("Foreground", front);
        }
        desktop.save(tag);
        if (!computerName.isEmpty()) {
            tag.putString("ComputerName", computerName);
        }
        SettingsStorage.save(settings, tag);
    }

    /**
     * Resets every field to its empty value. Defined as loading an empty tag so it can never drift from
     * {@link #load}: a field added there is reset here for free.
     */
    public void clear() {
        load(new CompoundTag());
    }

    public void load(final CompoundTag tag) {
        history.clear();
        JobStorage.load(this.jobs, tag);
        for (final Tag entry : tag.getList("History", Tag.TAG_STRING)) {
            history.addLast(entry.getAsString());
        }
        // A tampered or legacy tag may hold more entries than the live cap; keep only the most recent.
        while (history.size() > MAX_HISTORY) {
            history.removeFirst();
        }
        programs.load(tag);
        setup = null;
        if (tag.contains("Setup")) {
            final CompoundTag job = tag.getCompound("Setup");
            setup = new SetupJob(job.getString("Program"), job.getString("Name"),
                    job.getString("House"), job.getInt("SizeMb"), TextTags.read(job.getCompound("Source")),
                    job.getBoolean("Removing"), job.getInt("Total"), job.getInt("Left"),
                    MANAGERS.find(job.getString("Via")), job.getString("Package"));
        }
        liveInstall = tag.contains("LiveInstall")
                ? LiveInstallState.deserialize(tag.getString("LiveInstall"))
                : null;
        foreground.load(tag.getCompound("Foreground"));
        desktop.load(tag);
        computerName = tag.getString("ComputerName");
        SettingsStorage.load(settings, tag);
    }
}
