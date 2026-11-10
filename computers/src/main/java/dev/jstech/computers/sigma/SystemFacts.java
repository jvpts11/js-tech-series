/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Computers.
 */
package dev.jstech.computers.sigma;

import java.util.List;
import java.util.Locale;

/**
 * What an operating system a player wrote says it is (computers/docs/design/player-systems.md): its name, version and
 * author, the era and instruction set it is built for, the memory it keeps for itself, and its kernel.
 *
 * <p>Read by the compiler from the system's properties, which are written with an arrow and values the compiler
 * knows, so a system is known before anything of it runs. This record is pure and carries no Minecraft dependency.
 *
 * @param name     what the system is called
 * @param version  its version, as written
 * @param author   who wrote it
 * @param era      the era it is built for, as the language names it: "Legacy"
 * @param isa      the instruction set it is built for, as the language names it: "IA32"
 * @param memoryMb the memory it keeps for itself, in megabytes
 * @param kernel   the kernel it stands on
 * @param services the services its sources hold, by the names of their classes: each a script the build writes out as
 *                 a listing of its own, which the system starts by that name
 */
public record SystemFacts(String name, String version, String author, String era, String isa, int memoryMb,
                          Kernel kernel, List<String> services) {

    public SystemFacts {
        services = List.copyOf(services);
    }

    /** The schedulers a kernel of its own may hand the machine's credits out with. */
    public static final List<String> SCHEDULERS = List.of("none", "cooperative", "preemptive");
    /** The kinds of file system a kernel of its own may keep. */
    public static final List<String> FILE_SYSTEMS = List.of("dos", "posix", "flat");
    /** The classes of device a kernel of its own may drive. */
    public static final List<String> DRIVER_CLASSES = List.of("text", "graphics", "sound", "network", "disks",
            "optical", "usb", "printer");
    /** What a kernel of its own does when memory runs out. */
    public static final List<String> MEMORY_RULES = List.of("refuse", "end", "down");
    /** The shells a kernel of its own may speak: one of the mod's families, or one of the system's own. */
    public static final List<String> SHELLS = List.of("dos", "posix", "mc-net", "own");
    /** The slice a preemptive kernel takes turns in by default, the round the mod's own systems use. */
    public static final int DEFAULT_SLICE = 64;
    /** The shortest and the longest slice a preemptive kernel may take turns in. */
    public static final int LEAST_SLICE = 16;
    public static final int MOST_SLICE = 256;

    /* The namespace the mod's instruction sets are named in, as the machines know them. */
    private static final String ISA_NAMESPACE = "jsc:";

    /** The system as one line: "Lantern OS 1.2". */
    public String label() {
        return this.name + " " + this.version;
    }

    /** The instruction set it is built for, by the id the machines know it by: "jsc:x86" for IA32. */
    public String isaId() {
        return ISA_NAMESPACE + switch (this.isa) {
            case "IA16" -> "ia_16";
            case "X86_64" -> "x86_64";
            case "M68K" -> "m68k";
            case "PowerPC" -> "powerpc";
            case "ARM64" -> "arm64";
            default -> "x86";
        };
    }

    /** The instruction set it is built for, as a person writes it: "IA-32". */
    public String isaWritten() {
        return switch (this.isa) {
            case "IA16" -> "IA-16";
            case "X86_64" -> "x86-64";
            case "M68K" -> "68k";
            case "PowerPC" -> "PowerPC";
            case "ARM64" -> "ARM64";
            default -> "IA-32";
        };
    }

    /** The package it is published as, its name in lower case with dashes: "lantern-os". */
    public String packageName() {
        final String slug = this.name.toLowerCase(Locale.ROOT).replaceAll("[^a-z0-9]+", "-")
                .replaceAll("^-|-$", "");
        return slug.isEmpty() ? "system" : slug;
    }

    /**
     * A kernel: one of the mod's, by its name ({@code base}), taking all its behaviour, or one of the system's own,
     * each of its policies chosen. A kernel of its own has an empty {@code base}.
     *
     * @param base       the mod's kernel it is, "linux", or empty for a kernel of its own
     * @param scheduler  how it hands out the machine's credits: none, cooperative or preemptive
     * @param slice      the instructions in a preemptive turn
     * @param fileSystem the kind of file system it keeps: dos, posix or flat
     * @param drivers    the classes of device it drives
     * @param memoryRule what it does when memory runs out: refuse, end what holds the most, or go down
     * @param shell      the shell it speaks: one of the mod's families, or "own"
     */
    public record Kernel(String base, String scheduler, int slice, String fileSystem, List<String> drivers,
                         String memoryRule, String shell) {

        /** A kernel of its own with nothing chosen yet: the mod's defaults for a preemptive POSIX system. */
        public static final Kernel OWN = new Kernel("", "preemptive", DEFAULT_SLICE, "posix", DRIVER_CLASSES,
                "end", "posix");

        public Kernel {
            drivers = List.copyOf(drivers);
        }

        /** One of the mod's kernels, by its name. */
        public static Kernel of(final String base) {
            return new Kernel(base.toLowerCase(Locale.ROOT), "", 0, "", List.of(), "", "");
        }

        /** Whether this is a kernel of the system's own rather than one of the mod's. */
        public boolean own() {
            return this.base.isEmpty();
        }

        /** Whether this kernel drives devices of that class. */
        public boolean drives(final String deviceClass) {
            return !own() || this.drivers.contains(deviceClass);
        }
    }
}
