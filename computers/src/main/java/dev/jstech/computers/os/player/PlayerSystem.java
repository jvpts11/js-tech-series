/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Computers.
 */
package dev.jstech.computers.os.player;

import dev.jstech.computers.os.FilesystemKind;
import dev.jstech.computers.os.InstallMode;
import dev.jstech.computers.os.KernelDef;
import dev.jstech.computers.os.OsCapability;
import dev.jstech.computers.os.OsDef;
import dev.jstech.computers.os.PackageManagerKind;
import dev.jstech.computers.os.Platform;
import dev.jstech.computers.os.SchedulerKind;
import dev.jstech.computers.os.ShellFamily;
import dev.jstech.computers.os.ShellKind;
import dev.jstech.computers.os.SoftwareHouse;
import dev.jstech.computers.os.SystemIsas;
import dev.jstech.computers.os.install.InstallerStyle;
import dev.jstech.computers.sigma.SystemFacts;
import dev.jstech.core.jstechseries.tier.Era;
import dev.jstech.core.jstechseries.tier.HardwareEra;
import net.minecraft.resources.ResourceLocation;

import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;

/**
 * An operating system a player wrote and built (computers/docs/design/player-systems.md): what it declared, the folder
 * it was built from, and its files, which its installer puts on a disk.
 *
 * <p>It is a system like the mod's own to everything that installs, boots and lists systems: {@link #os()} is what it
 * looks like to them, made from what it declared. A system on one of the mod's kernels counts as that kernel's family,
 * so it installs what that family installs; one with a kernel of its own stands on {@link #OWN_KERNEL}.
 *
 * @param id     the system, as disks and media name it
 * @param facts  what it declared
 * @param folder the folder it was built from, which it is installed into, so the paths it names stay right
 * @param files  its files, by their path inside the folder
 */
public record PlayerSystem(ResourceLocation id, SystemFacts facts, String folder, Map<String, String> files) {

    /** The kernel every system with a kernel of its own stands on; its policies are the system's own. */
    public static final ResourceLocation OWN_KERNEL = ResourceLocation.fromNamespaceAndPath("jsc", "own");
    /** The listing the system runs from, inside its folder. */
    public static final String LISTING = "system.asm";
    /** What a player is called as a house: themselves, with "community" said beside them. */
    public static final String COMMUNITY = "community";

    /* A megabyte, in the characters a file holds. */
    private static final int MEGABYTE = 1024 * 1024;

    public PlayerSystem {
        files = Map.copyOf(files);
    }

    /** The system as the installers, the firmware and the boot manager see it. */
    public OsDef os() {
        final SystemFacts.Kernel kernel = this.facts.kernel();
        final Platform platform = platformOf(kernel);
        return new OsDef(this.id, OsCapability.TERMINAL_ONLY, eraOf(this.facts.era()),
                kernel.own() ? OWN_KERNEL : ResourceLocation.fromNamespaceAndPath("jsc", kernel.base()), footprintMb(),
                Optional.empty(), platform, this.facts.label(), shellOf(platform), packagesOf(platform),
                InstallMode.GUIDED, Optional.empty(), new SoftwareHouse(this.facts.author(), this.facts.author()),
                this.facts.memoryMb(), InstallerStyle.PLAIN, 0,
                new SystemIsas(List.of(this.facts.isaId()), List.of()), List.of(), "");
    }

    /**
     * Its kernel as the machine's file system, scheduler and shell read it: one of its own made of what it chose, a
     * flat file system having no folders and the others one tree of them; null on one of the mod's kernels.
     */
    public KernelDef ownKernel() {
        final SystemFacts.Kernel kernel = this.facts.kernel();
        if (!kernel.own()) {
            return null;
        }
        final FilesystemKind files = "flat".equals(kernel.fileSystem()) ? FilesystemKind.FLAT
                : FilesystemKind.HIERARCHICAL;
        final ShellFamily shell = switch (kernel.shell()) {
            case "dos" -> ShellFamily.DOS;
            case "mc-net" -> ShellFamily.NET;
            default -> ShellFamily.POSIX;
        };
        final SchedulerKind scheduler = switch (kernel.scheduler()) {
            case "none" -> SchedulerKind.NONE;
            case "cooperative" -> SchedulerKind.COOPERATIVE;
            default -> SchedulerKind.PREEMPTIVE;
        };
        return new KernelDef(OWN_KERNEL, scheduler, files, shell);
    }

    /** What its files take on a disk, in whole megabytes, never less than one. */
    public int footprintMb() {
        long chars = 0;
        for (final String content : this.files.values()) {
            chars += content.length();
        }
        return (int) Math.max(1, (chars + MEGABYTE - 1) / MEGABYTE);
    }

    /** The id a system of that author, name and version is given: the same every time it is built again. */
    public static ResourceLocation idOf(final SystemFacts facts) {
        return ResourceLocation.fromNamespaceAndPath("jsc", "player/" + slug(facts.author(), 16) + "/"
                + slug(facts.name(), 20) + "/" + slug(facts.version(), 10));
    }

    /** The era a system names, as the machines know it. */
    public static Era eraOf(final String named) {
        return switch (named) {
            case "Vintage" -> HardwareEra.VINTAGE;
            case "Transition" -> HardwareEra.TRANSITION;
            case "Standard" -> HardwareEra.STANDARD;
            case "Advanced" -> HardwareEra.ADVANCED;
            default -> HardwareEra.LEGACY;
        };
    }

    /*
     * The family a kernel counts as: one of the mod's kernels is its own family; a kernel of the system's own is
     * read by the shell it speaks, POSIX for one of its own.
     */
    private static Platform platformOf(final SystemFacts.Kernel kernel) {
        final String family = kernel.own() ? kernel.shell() : kernel.base();
        return switch (family) {
            case "dos" -> Platform.MC_DOS;
            case "win9x", "nt" -> Platform.FRAMES;
            case "linux" -> Platform.LINUX;
            case "freebsd" -> Platform.FREEBSD;
            case "openbsd" -> Platform.OPENBSD;
            case "net_min", "mc-net" -> Platform.MC_NET;
            case "quince_classic", "wallace" -> Platform.QUINCE;
            default -> Platform.UNIX;
        };
    }

    private static ShellKind shellOf(final Platform platform) {
        return switch (platform) {
            case LINUX -> ShellKind.BASH;
            case OPENBSD -> ShellKind.KSH;
            case FREEBSD, UNIX, QUINCE -> ShellKind.SH;
            default -> ShellKind.CMD;
        };
    }

    /* The package manager of the family a system counts as: Linux's the one most of its distributions share. */
    private static PackageManagerKind packagesOf(final Platform platform) {
        return switch (platform) {
            case LINUX -> PackageManagerKind.APT;
            case FREEBSD -> PackageManagerKind.PKG;
            case OPENBSD -> PackageManagerKind.PKG_ADD;
            case QUINCE -> PackageManagerKind.KEG;
            default -> PackageManagerKind.NONE;
        };
    }

    /* A name as an id's piece: lower case, letters and digits, the rest as dashes, cut to {@code most}. */
    private static String slug(final String written, final int most) {
        final String slug = written.toLowerCase(Locale.ROOT).replaceAll("[^a-z0-9.]+", "-").replaceAll("^-|-$", "");
        final String cut = slug.length() > most ? slug.substring(0, most) : slug;
        return cut.isEmpty() ? "unnamed" : cut;
    }
}
