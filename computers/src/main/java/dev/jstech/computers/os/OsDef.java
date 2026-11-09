/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Computers.
 */
package dev.jstech.computers.os;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import dev.jstech.computers.api.ComputersRegisterEvent;
import dev.jstech.computers.hardware.EraStorage;
import dev.jstech.computers.hardware.IsaSpec;
import dev.jstech.computers.hardware.IsaTranslator;
import dev.jstech.computers.hardware.IsaTranslators;
import dev.jstech.computers.hardware.Isas;
import dev.jstech.computers.os.install.InstallerStyle;
import dev.jstech.core.id.StableCodecs;
import dev.jstech.core.jstechseries.tier.Era;
import dev.jstech.core.jstechseries.tier.EraCodecs;
import dev.jstech.core.jstechseries.tier.HardwareEra;
import net.minecraft.resources.ResourceLocation;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.Nullable;

/**
 * Immutable descriptor for an operating system registered with the mod.
 *
 * <p>Each OS declares the capability tier it provides, the minimum hardware era it requires,
 * the kernel it runs on, the disk footprint it consumes on installation, an optional install
 * media item that carries the OS installer payload, and its display name (which datagen writes to
 * {@link #titleKey()}). The {@link #CODEC} keeps this JSON-serialisable, so the built-in OSes can
 * later move to a datapack; today the Java registration through {@link ComputersRegisterEvent} is the source.
 *
 * @param id             unique registry key for this OS (e.g. {@code jsc:mc_dos})
 * @param capability     the capability tier this OS provides to programs and the player
 * @param minEra         the minimum hardware era required to install this OS; the era acts as a
 *                       minimum, so installing on hardware at this era or later is accepted, older
 *                       hardware is rejected
 * @param kernelId       registry key of the kernel this OS runs on
 * @param footprintMb    the size of the installed system in megabytes; what that costs in items follows
 *                       from the era of the disk it lands on ({@link #footprintItemsOn})
 * @param installMediaId optional registry key of the install media item that delivers this OS;
 *                       empty when the OS is provisioned programmatically (e.g. server auto-provision)
 * @param platform       the platform (family) this OS is, which programs are gated against
 * @param displayName    the human name (English), the value datagen writes to {@link #titleKey()}
 * @param shell          the interactive shell this OS ships ({@code cmd}, {@code bash}, {@code zsh}); it
 *                       flavours the prompt, while the command syntax comes from the kernel's shell family
 * @param packageManager the package manager the OS installs programs with ({@link PackageManagerKind#NONE}
 *                       for media-installed platforms)
 * @param installMode    how booting this OS's medium installs it (guided, or the real manual steps)
 * @param bundledDesktop the desktop environment this OS ships with, if any (the Frames editions bundle their
 *                       own; a Linux distribution boots to a TTY until one is installed)
 * @param house          who wrote it: the name on its banner, its copyright line and its install disc
 * @param ramMb          megabytes the running system holds for itself before any program opens; a program
 *                       bundled with it weighs a share of this ({@link RamLedger#bundledWeightMb})
 * @param familyRank     where this system sits in its own family's order, counting from one, so a program can
 *                       ask for a system of that family no older than a given one. Zero, the default, means
 *                       the family has no order: nothing is newer or older than anything else in it, and a
 *                       program is decided by the platform alone
 * @param isas           the instruction sets the system is built for and the translators it carries; a system
 *                       that names none is built for its first era's processors ({@link #builtFor()})
 */
@ApiStatus.Experimental
public record OsDef(
        ResourceLocation id,
        OsCapability capability,
        Era minEra,
        ResourceLocation kernelId,
        int footprintMb,
        Optional<ResourceLocation> installMediaId,
        Platform platform,
        String displayName,
        ShellKind shell,
        PackageManagerKind packageManager,
        InstallMode installMode,
        Optional<ResourceLocation> bundledDesktop,
        SoftwareHouse house,
        int ramMb,
        InstallerStyle installerStyle,
        int familyRank,
        SystemIsas isas
) {

    public static final Codec<OsDef> CODEC = RecordCodecBuilder.create(inst -> inst.group(
            ResourceLocation.CODEC.fieldOf("id").forGetter(OsDef::id),
            StableCodecs.byName(OsCapability.class).fieldOf("capability").forGetter(OsDef::capability),
            EraCodecs.CODEC.fieldOf("min_era").forGetter(OsDef::minEra),
            ResourceLocation.CODEC.fieldOf("kernel").forGetter(OsDef::kernelId),
            Codec.INT.optionalFieldOf("footprint_mb", 0).forGetter(OsDef::footprintMb),
            ResourceLocation.CODEC.optionalFieldOf("install_media").forGetter(OsDef::installMediaId),
            StableCodecs.byName(Platform.class).fieldOf("platform").forGetter(OsDef::platform),
            Codec.STRING.optionalFieldOf("display_name", "").forGetter(OsDef::displayName),
            StableCodecs.byName(ShellKind.class).optionalFieldOf("shell", ShellKind.CMD).forGetter(OsDef::shell),
            StableCodecs.byName(PackageManagerKind.class).optionalFieldOf("package_manager", PackageManagerKind.NONE)
                    .forGetter(OsDef::packageManager),
            StableCodecs.byName(InstallMode.class).optionalFieldOf("install_mode", InstallMode.GUIDED)
                    .forGetter(OsDef::installMode),
            ResourceLocation.CODEC.optionalFieldOf("bundled_desktop").forGetter(OsDef::bundledDesktop),
            SoftwareHouse.CODEC.optionalFieldOf("house", SoftwareHouse.MIDSOFT).forGetter(OsDef::house),
            Codec.INT.optionalFieldOf("ram_mb", 0).forGetter(OsDef::ramMb),
            StableCodecs.byName(InstallerStyle.class)
                    .optionalFieldOf("installer", InstallerStyle.PLAIN)
                    .forGetter(OsDef::installerStyle),
            Tail.CODEC.forGetter(os -> new Tail(os.familyRank(), os.isas()))
    ).apply(inst, (id, capability, minEra, kernelId, footprintMb, installMediaId, platform, displayName, shell,
            packageManager, installMode, bundledDesktop, house, ramMb, installerStyle, tail) -> new OsDef(id,
            capability, minEra, kernelId, footprintMb, installMediaId, platform, displayName, shell, packageManager,
            installMode, bundledDesktop, house, ramMb, installerStyle, tail.familyRank(), tail.isas())));

    /**
     * The media-installed, guided, desktop-bundling form used by the DOS-family systems: {@code cmd} shell, no
     * package manager, guided install, and the given bundled desktop (empty for a terminal or network OS).
     */
    public static OsDef mediaInstalled(final ResourceLocation id, final OsCapability capability,
                                       final Era minEra, final ResourceLocation kernelId,
                                       final int footprintMb, final Platform platform,
                                       final String displayName, final Optional<ResourceLocation> bundledDesktop,
                                       final SoftwareHouse house) {
        /*
         * A Frames edition ships pckmgr; anything else installed from media (MC-NET, MC-DOS) still
         * takes its programs from a disc in a linked drive.
         */
        return new OsDef(id, capability, minEra, kernelId, footprintMb, Optional.empty(), platform,
                displayName, ShellKind.CMD,
                platform == Platform.FRAMES ? PackageManagerKind.PCKMGR : PackageManagerKind.NONE,
                InstallMode.GUIDED, bundledDesktop, house, 0,
                InstallerStyle.PLAIN, 0, SystemIsas.UNNAMED);
    }

    /** A Linux distribution: TTY capability on the Linux kernel, no bundled desktop, installable from the Legacy era. */
    public static OsDef linuxDistro(final ResourceLocation id, final int footprintMb, final String displayName,
                                    final ShellKind shell, final PackageManagerKind packageManager,
                                    final InstallMode installMode, final SoftwareHouse house) {
        return terminalSystem(id, ResourceLocation.fromNamespaceAndPath("jsc", "linux"), Platform.LINUX,
                HardwareEra.LEGACY, footprintMb, displayName, shell, packageManager, installMode, house);
    }

    /**
     * A system that comes up at a terminal on a kernel and a platform of its own, and takes a desktop as a
     * package afterwards if it takes one at all: what a Linux distribution is, and what the systems that are
     * no Linux but are met at the same kind of prompt are too.
     */
    public static OsDef terminalSystem(final ResourceLocation id, final ResourceLocation kernelId,
                                       final Platform platform, final Era minEra, final int footprintMb,
                                       final String displayName, final ShellKind shell,
                                       final PackageManagerKind packageManager, final InstallMode installMode,
                                       final SoftwareHouse house) {
        return new OsDef(id, OsCapability.TERMINAL_ONLY, minEra, kernelId, footprintMb, Optional.empty(), platform,
                displayName, shell, packageManager, installMode, Optional.empty(), house, 0,
                InstallerStyle.PLAIN, 0, SystemIsas.UNNAMED);
    }

    /**
     * A system that comes up at a desktop of its own, the one under its own id, with the shell its terminal opens
     * and the manager its packages come through: what each of Quince's systems is.
     */
    public static OsDef desktopSystem(final ResourceLocation id, final ResourceLocation kernelId,
                                      final Platform platform, final Era minEra, final int footprintMb,
                                      final String displayName, final ShellKind shell,
                                      final PackageManagerKind packageManager, final SoftwareHouse house) {
        return new OsDef(id, OsCapability.FULL_DESKTOP, minEra, kernelId, footprintMb, Optional.empty(), platform,
                displayName, shell, packageManager, InstallMode.GUIDED, Optional.of(id), house, 0,
                InstallerStyle.PLAIN, 0, SystemIsas.UNNAMED);
    }

    /** The same system, holding {@code megabytes} of RAM for itself while it runs. */
    public OsDef withRam(final int megabytes) {
        return new OsDef(id, capability, minEra, kernelId, footprintMb, installMediaId, platform, displayName,
                shell, packageManager, installMode, bundledDesktop, house, megabytes, installerStyle,
                familyRank, isas);
    }

    /**
     * The same system, built for those instruction sets: a machine starts it when its processor runs one of them, as
     * a system built for two processors at once starts on either (computers/docs/design/quince.md).
     */
    public OsDef withIsas(final IsaSpec... instructionSets) {
        final List<String> ids = new ArrayList<>();
        for (final IsaSpec one : instructionSets) {
            ids.add(one.id());
        }
        return new OsDef(id, capability, minEra, kernelId, footprintMb, installMediaId, platform, displayName,
                shell, packageManager, installMode, bundledDesktop, house, ramMb, installerStyle, familyRank,
                new SystemIsas(ids, isas.translators()));
    }

    /** The same system, carrying those translators for the programs built for another processor. */
    public OsDef withTranslators(final IsaTranslator... carried) {
        final List<String> ids = new ArrayList<>();
        for (final IsaTranslator one : carried) {
            ids.add(one.id());
        }
        return new OsDef(id, capability, minEra, kernelId, footprintMb, installMediaId, platform, displayName,
                shell, packageManager, installMode, bundledDesktop, house, ramMb, installerStyle, familyRank,
                new SystemIsas(isas.builtFor(), ids));
    }

    /**
     * The ids of the instruction sets this system is built for: the ones it names, or, when it names none, the one
     * this mod's processors of its first era are built on, which is what every system was built for before any other.
     */
    public List<String> builtFor() {
        return isas.builtFor().isEmpty() ? List.of(Isas.of(minEra).id()) : isas.builtFor();
    }

    /** Whether a machine whose processor is of {@code isa} starts this system; one with no processor says nothing. */
    public boolean runsOn(@Nullable final IsaSpec isa) {
        if (isa == null) {
            return true;
        }
        for (final String one : builtFor()) {
            if (isa.runs(one)) {
                return true;
            }
        }
        return false;
    }

    /** The translators this system carries, those a mod has brought; one nobody brought is left out. */
    public List<IsaTranslator> translators() {
        final List<IsaTranslator> carried = new ArrayList<>();
        for (final String one : isas.translators()) {
            IsaTranslators.byId(one).ifPresent(carried::add);
        }
        return carried;
    }

    /**
     * The same system, placed at {@code rank} in its family's order, counting from one.
     *
     * <p>A family whose systems say nothing here has no order, which is the answer for most of them: one
     * Linux distribution is not newer than another in any way a program can ask about. A family that does
     * have an order says so here, once, on each of its systems, and that is where the answer lives. It used
     * to be a list of three names written into the registry, which meant a family the mod did not ship could
     * never have an order at all.
     */
    public OsDef withRank(final int rank) {
        return new OsDef(id, capability, minEra, kernelId, footprintMb, installMediaId, platform, displayName,
                shell, packageManager, installMode, bundledDesktop, house, ramMb, installerStyle, rank, isas);
    }

    /**
     * The same system, installed by an installer of its own rather than the plain one.
     *
     * <p>A system that says nothing here gets the plain installer, which is what an addon's system and anything
     * still waiting for a pass of its own are drawn with.
     */
    public OsDef withInstaller(final InstallerStyle style) {
        return new OsDef(id, capability, minEra, kernelId, footprintMb, installMediaId, platform, displayName,
                shell, packageManager, installMode, bundledDesktop, house, ramMb, style, familyRank, isas);
    }

    public OsDef {
        if (house == null) {
            house = SoftwareHouse.MIDSOFT;
        }
        if (displayName == null || displayName.isBlank()) {
            displayName = id.getPath();
        }
        if (shell == null) {
            shell = ShellKind.CMD;
        }
        if (packageManager == null) {
            packageManager = PackageManagerKind.NONE;
        }
        if (installMode == null) {
            installMode = InstallMode.GUIDED;
        }
        if (bundledDesktop == null) {
            bundledDesktop = Optional.empty();
        }
        if (ramMb < 0) {
            ramMb = 0;
        }
        if (installerStyle == null) {
            installerStyle = InstallerStyle.PLAIN;
        }
        // A rank below zero would read as older than a family's own first system, which nothing can be.
        if (familyRank < 0) {
            familyRank = 0;
        }
        if (isas == null) {
            isas = SystemIsas.UNNAMED;
        }
    }

    /**
     * How many items this system takes on a disk of {@code diskEra}: its size in megabytes at that era's
     * cost per item, rounded up. A 20 GB system is 80 items on a standard disk and more than a whole
     * vintage drive.
     */
    public long footprintItemsOn(final Era diskEra) {
        return EraStorage.itemsFor(diskEra, footprintMb);
    }

    /** The translation key for this OS's display name, in vanilla {@code os.<ns>.<path>} form. */
    public String titleKey() {
        return "os." + id.getNamespace() + "." + id.getPath();
    }

    /*
     * The fields after the fifteenth, read and written beside the others: one codec group takes sixteen at most, so
     * the family's rank and the instruction sets go in as the sixteenth, side by side in the same object.
     */
    private record Tail(int familyRank, SystemIsas isas) {

        static final MapCodec<Tail> CODEC = RecordCodecBuilder.mapCodec(inst -> inst.group(
                Codec.INT.optionalFieldOf("family_rank", 0).forGetter(Tail::familyRank),
                SystemIsas.MAP_CODEC.forGetter(Tail::isas)
        ).apply(inst, Tail::new));
    }
}
