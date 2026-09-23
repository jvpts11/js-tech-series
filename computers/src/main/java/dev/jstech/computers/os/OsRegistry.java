/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Computers.
 */
package dev.jstech.computers.os;

import dev.jstech.computers.JsComputers;
import net.minecraft.resources.ResourceLocation;

import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Every kernel, operating system, program and desktop the computers know.
 *
 * <p>They are added while the game loads, at the one moment the mods are asked for them, and the registry
 * is closed once the loading is done. That is what lets a world be sure that what it can install does not
 * change under it while somebody plays it.
 *
 * <p>Two of the same id are refused rather than one quietly replacing the other, because which of the two
 * won would otherwise depend on the order the mods happened to load in. They are kept in the order they
 * arrived, so what a list shows is the same every time.
 */
public final class OsRegistry {

    private static final Map<ResourceLocation, KernelDef> KERNELS = new LinkedHashMap<>();
    private static final Map<ResourceLocation, OsDef> OSES = new LinkedHashMap<>();
    private static final Map<ResourceLocation, ProgramSpec> PROGRAMS = new LinkedHashMap<>();
    private static final Map<ResourceLocation, DesktopEnvironmentDef> DESKTOPS = new LinkedHashMap<>();
    private static final Map<ResourceLocation, OperatingSpaceDef> SPACES = new LinkedHashMap<>();

    private static volatile boolean frozen;

    private OsRegistry() {}

    public static void registerKernel(final KernelDef def) {
        add(KERNELS, def == null ? null : def.id(), def, "kernel");
    }

    public static void registerOs(final OsDef def) {
        add(OSES, def == null ? null : def.id(), def, "operating system");
    }

    public static void registerProgram(final ProgramSpec def) {
        add(PROGRAMS, def == null ? null : def.id(), def, "program");
    }

    public static void registerDesktop(final DesktopEnvironmentDef def) {
        add(DESKTOPS, def == null ? null : def.id(), def, "desktop");
    }

    public static void registerSpace(final OperatingSpaceDef def) {
        add(SPACES, def == null ? null : def.id(), def, "operating space");
    }

    /** Closes the registry, as the mod does once every mod has loaded: nothing is added after. */
    public static void freeze() {
        frozen = true;
    }

    /** Whether the registry has been closed. */
    public static boolean isFrozen() {
        return frozen;
    }

    /**
     * Puts one in, and refuses an id something already answers for.
     *
     * <p>The two refusals are not the same. A taken id can only be two mods declaring the same thing, which
     * is a mistake in one of them and is worth stopping the load over while somebody is there to read why.
     * Adding one after the loading is done can happen in a world already being played, and no addon's
     * mistake is worth ending somebody's game over, so that one is refused with a word in the log.
     */
    private static <T> void add(final Map<ResourceLocation, T> into, final ResourceLocation id, final T def,
                                final String what) {
        if (def == null || id == null) {
            return;
        }
        if (frozen) {
            JsComputers.LOGGER.warn("The {} {} was not registered: they are only added while the game loads",
                    what, id);
            return;
        }
        if (into.containsKey(id)) {
            throw new IllegalStateException("A " + what + " is already registered as " + id);
        }
        into.put(id, def);
    }

    /** Returns the {@link DesktopEnvironmentDef} registered under {@code id}, or {@code null} if absent. */
    public static DesktopEnvironmentDef getDesktop(ResourceLocation id) {
        return id == null ? null : DESKTOPS.get(id);
    }

    /** Returns an unmodifiable view of all registered desktop environments. */
    public static Collection<DesktopEnvironmentDef> desktops() {
        return Collections.unmodifiableCollection(DESKTOPS.values());
    }

    /** Returns the {@link OperatingSpaceDef} registered under {@code id}, or {@code null} if absent. */
    public static OperatingSpaceDef getSpace(ResourceLocation id) {
        return id == null ? null : SPACES.get(id);
    }

    /** Returns an unmodifiable view of all registered operating spaces. */
    public static Collection<OperatingSpaceDef> spaces() {
        return Collections.unmodifiableCollection(SPACES.values());
    }

    // Lookup

    /**
     * Returns the {@link KernelDef} registered under {@code id}, or {@code null} if absent.
     */
    public static KernelDef getKernel(ResourceLocation id) {
        return KERNELS.get(id);
    }

    /**
     * Returns the {@link OsDef} registered under {@code id}, or {@code null} if absent.
     */
    public static OsDef getOs(ResourceLocation id) {
        return OSES.get(id);
    }

    /**
     * Returns the {@link ProgramSpec} registered under {@code id}, or {@code null} if absent.
     */
    public static ProgramSpec getProgram(ResourceLocation id) {
        return PROGRAMS.get(id);
    }

    /**
     * Whether the OS installed under {@code osId} lets the program {@code progId} <em>run</em> on a
     * computer with the given best CPU clock and total VRAM.
     *
     * <p>Null-safe by design: a program with no registered {@link ProgramSpec} declares no requirement
     * and is always allowed (so existing/third-party programs are unaffected). A registered program
     * needs a known installed OS whose platform it supports and a computer that meets its CPU/VRAM
     * minimums; an absent or unknown OS denies it.
     *
     * @param osId   the installed OS id, or {@code null} when none is installed
     * @param progId the program id
     * @param cpuMhz the computer's best CPU clock in MHz
     * @param vramMb the computer's total VRAM in MB
     */
    public static boolean canRunProgram(ResourceLocation osId, ResourceLocation progId,
                                        int cpuMhz, int vramMb) {
        return canRunProgram(osId, progId, cpuMhz, vramMb, false);
    }

    /**
     * The same, for a copy the computer may have built from source, which asks for a little less of a processor
     * ({@link SourceAdvantage}).
     *
     * @param builtHere whether the computer built the program from source rather than installing a built package
     */
    public static boolean canRunProgram(final ResourceLocation osId, final ResourceLocation progId,
                                        final int cpuMhz, final int vramMb, final boolean builtHere) {
        final ProgramSpec prog = getProgram(progId);
        if (prog == null) {
            return true;
        }
        final OsDef os = osId == null ? null : getOs(osId);
        if (os == null) {
            return false;
        }
        /*
         * The version rank gate only bites WITHIN the Frames family (ranks 1/2/3); a non-Frames OS (rank 0)
         * is decided by the platform gate alone, so a headless service still runs on an MC-DOS Mainframe.
         */
        final int rank = osVersionRank(osId);
        return (rank == 0 || rank >= prog.minOsRank())
                && OsGating.canRunProgram(os.platform(), prog.platforms(), cpuMhz, vramMb,
                SourceAdvantage.of(prog.minCpuMhz(), builtHere), prog.minVramMb());
    }

    /**
     * Where a system sits in its family's order, as the system itself declares, so a program can ask for one
     * no older than a given one within the same platform.
     *
     * <p>Zero for a system that declares nothing, for one nobody has registered, and for no system at all: a
     * family with no order means the platform alone decides, which is the answer for every family but one.
     *
     * <p>This used to be three names written out here, which meant the mod's own three systems were the only
     * ones that could ever be ordered: a fourth, or an addon's, had no way to say it was newer than anything.
     */
    public static int osVersionRank(final ResourceLocation osId) {
        final OsDef os = osId == null ? null : getOs(osId);
        return os == null ? 0 : os.familyRank();
    }

    /**
     * The name of the system at that place in its family's order, for a message saying what is needed.
     *
     * <p>Falls back to words rather than a name where nothing answers to that rank, which is what happens if a
     * program asks for a rank past the end of its family.
     */
    public static String systemOfRank(final int rank) {
        for (final OsDef os : oses()) {
            if (os.familyRank() == rank) {
                return os.displayName();
            }
        }
        return "a newer system";
    }

    /**
     * Whether the program {@code progId} may be <em>installed</em> on the OS under {@code osId} on a
     * computer with the given hardware: it must satisfy {@link #canRunProgram} and the system disk
     * must have at least the program's disk footprint free.
     *
     * @param osId       the installed OS id, or {@code null} when none is installed
     * @param progId     the program id
     * @param cpuMhz     the computer's best CPU clock in MHz
     * @param vramMb     the computer's total VRAM in MB
     * @param freeDiskMb the free space on the system disk in MB
     */
    public static boolean canInstallProgram(ResourceLocation osId, ResourceLocation progId,
                                            int cpuMhz, int vramMb, long freeDiskMb) {
        return canInstallProgram(osId, progId, cpuMhz, vramMb, freeDiskMb, false);
    }

    /**
     * The same, for a program about to be built from source on the computer, which asks for a little less of a
     * processor and of the disk ({@link SourceAdvantage}).
     *
     * @param fromSource whether the program is being built from source rather than installed as a built package
     */
    public static boolean canInstallProgram(final ResourceLocation osId, final ResourceLocation progId,
                                            final int cpuMhz, final int vramMb, final long freeDiskMb,
                                            final boolean fromSource) {
        final ProgramSpec prog = getProgram(progId);
        if (prog == null) {
            return true;
        }
        final OsDef os = osId == null ? null : getOs(osId);
        if (os == null) {
            return false;
        }
        final int rank = osVersionRank(osId);
        return (rank == 0 || rank >= prog.minOsRank())
                && OsGating.canInstallProgram(os.platform(), prog.platforms(), cpuMhz, vramMb, freeDiskMb,
                SourceAdvantage.of(prog.minCpuMhz(), fromSource), prog.minVramMb(),
                SourceAdvantage.of(prog.minDiskMb(), fromSource));
    }

    // Listing

    /** Returns an unmodifiable view of all registered kernels. */
    public static Collection<KernelDef> kernels() {
        return Collections.unmodifiableCollection(KERNELS.values());
    }

    /** Returns an unmodifiable view of all registered operating systems. */
    public static Collection<OsDef> oses() {
        return Collections.unmodifiableCollection(OSES.values());
    }

    /** Returns an unmodifiable view of all registered programs. */
    public static Collection<ProgramSpec> programs() {
        return Collections.unmodifiableCollection(PROGRAMS.values());
    }
}
