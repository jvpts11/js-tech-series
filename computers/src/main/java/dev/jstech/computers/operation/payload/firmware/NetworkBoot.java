/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Computers.
 */
package dev.jstech.computers.operation.payload.firmware;

import dev.jstech.computers.blockentity.MainframeBlockEntity;
import dev.jstech.computers.os.IOsHost;
import dev.jstech.computers.os.InstallMode;
import dev.jstech.computers.os.OsDef;
import dev.jstech.computers.os.OsGating;
import dev.jstech.computers.os.OsRegistry;
import dev.jstech.computers.os.boot.NetworkAddresses;
import dev.jstech.computers.os.install.Installers;
import dev.jstech.computers.os.install.OsInstallJob;
import dev.jstech.computers.os.install.SetupTiming;
import dev.jstech.computers.os.media.InstallMedia;
import dev.jstech.computers.os.media.LiveMedium;
import dev.jstech.computers.program.install.LiveInstallState;
import dev.jstech.computers.sigma.pack.Manifest;
import dev.jstech.computers.sigma.pack.Packed;
import dev.jstech.core.jstechseries.tier.Era;
import dev.jstech.core.jstechseries.tier.HardwareEra;
import dev.jstech.core.text.Text;
import dev.jstech.core.text.TextHolder;
import dev.jstech.core.text.TextKey;
import dev.jstech.core.uuid.NodeUuid;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

/**
 * Booting from the network: the firmware asks the Mainframe's Mirror for the systems it serves and boots the
 * installer of the one chosen, which then runs as from a medium (computers/docs/design/operating-systems.md).
 *
 * <p>The Legacy's BIOS does it through its card's PXE agent and the Standard's UEFI over HTTP; the Vintage cannot.
 * The Mirror lists only what the machine runs, its era and its processor, so nothing on the menu is refused after it
 * is chosen. A guided installer copies over the network with no drive to lose; a live medium (Linux Mint's desktop,
 * the shells of Arch Linux and Gentoo) comes up as it would from its disc, and stands without one.
 */
@TextHolder
public final class NetworkBoot {

    /** What the firmware says when it asks and nothing on the network serves systems. */
    public static final TextKey NO_MIRROR = TextKey.of("jsc.firmware.network.no_mirror",
            "No Mirror answered on the network");
    /** A choice the Mirror does not serve to this computer, or no longer does. */
    public static final TextKey NOT_SERVED = TextKey.of("jsc.firmware.network.not_served",
            "The Mirror does not serve that system to this computer");
    /** A system's image, by the medium its installer is made for: "DVD image". */
    private static final TextKey IMAGE = TextKey.of("jsc.firmware.network.image", "%s image");

    private NetworkBoot() {
    }

    /**
     * What the Mirror serves this machine, with the addresses its card reads out; null when no Mirror answers.
     *
     * <p>The systems come in the order they were registered, which keeps each house's together and puts the Frames
     * editions in the order they came out.
     */
    @Nullable
    public static Listing listing(final ServerLevel level, final IOsHost machine) {
        final String mirror = Installers.mirrorHost(machine, level);
        if (mirror.isEmpty()) {
            return null;
        }
        final Era era = eraOf(machine);
        final List<Offer> offers = new ArrayList<>();
        for (final OsDef os : OsRegistry.oses()) {
            if (OsGating.canInstall(os.minEra(), era) && machine.runsSystem(os)) {
                offers.add(new Offer(os.id().toString(), os.displayName(), os.house().name(), false,
                        IMAGE.with(InstallMedia.forSystem(os.minEra()).text())));
            }
        }
        // The players' own, as they published them, each under its author.
        final MainframeBlockEntity mainframe = Installers.mirrorOf(machine, level);
        for (final String shelved : mainframe == null ? List.<String>of() : mainframe.shelvedPackages().values()) {
            final Packed packed = Packed.read(shelved);
            final OsDef os = packed == null || !Manifest.SYSTEM.equals(packed.manifest().kind()) ? null
                    : OsRegistry.getOs(ResourceLocation.tryParse(packed.manifest().entry()));
            if (os != null && OsGating.canInstall(os.minEra(), era) && machine.runsSystem(os)) {
                offers.add(new Offer(os.id().toString(), os.displayName(), os.house().name(), true,
                        IMAGE.with(InstallMedia.forSystem(os.minEra()).text())));
            }
        }
        final long seed = seedOf(machine);
        return new Listing(mirror, NetworkAddresses.card(seed), NetworkAddresses.host(seed), offers);
    }

    /**
     * Boots the installer of the system {@code id} from the Mirror: the machine restarts into it, as it would into a
     * medium. Answers why it could not, or null once the machine is on its way.
     */
    @Nullable
    public static Text boot(final ServerLevel level, final IOsHost machine, final String id) {
        final Listing listing = listing(level, machine);
        if (listing == null) {
            return NO_MIRROR.text();
        }
        final OsDef os = listing.serves(id) ? OsRegistry.getOs(ResourceLocation.tryParse(id)) : null;
        if (os == null) {
            return NOT_SERVED.text();
        }
        if (os.installerStyle().bootsLive() && machine.startLiveSession(os.id(), true)) {
            machine.setNeedsPost(true);
            machine.setChanged();
            return null;
        }
        final LiveInstallState.Distro distro = LiveMedium.distroOf(os.id());
        if (distro != null && os.installMode() != InstallMode.GUIDED) {
            machine.setNeedsPost(true);
            machine.console().startLiveInstall(distro, true);
            machine.setChanged();
            return null;
        }
        // The self-test first, which clears what the machine held, and the copy after it, so the copy survives it.
        machine.setNeedsPost(true);
        final Text failure = install(level, machine, os, machine.defaultInstallSlot());
        if (failure != null) {
            machine.setNeedsPost(false);
        }
        return failure;
    }

    /**
     * Starts copying {@code os} from the Mirror onto disk {@code targetSlot}: what a guided installer booted from the
     * network does, and what a live desktop booted from it installs from. Answers why it did not start, or null.
     */
    @Nullable
    public static Text install(final ServerLevel level, final IOsHost machine, final OsDef os, final int targetSlot) {
        if (Installers.mirrorHost(machine, level).isEmpty()) {
            return NO_MIRROR.text();
        }
        final Text refused = FirmwarePayloads.refusal(machine, os, eraOf(machine));
        if (refused != null) {
            return refused;
        }
        return FirmwarePayloads.copy(level, machine, os,
                SetupTiming.networkInstallRate(machine.cpuCores(), machine.maxCpuMhz()), OsInstallJob.NO_READER, 0,
                targetSlot);
    }

    /* The era the machine installs for, as its installer asks it. */
    private static Era eraOf(final IOsHost machine) {
        return machine.installedEra() != null ? machine.installedEra() : HardwareEra.STANDARD;
    }

    /* What the machine's addresses are worked out from: its number on the network, which stays with it. */
    private static long seedOf(final IOsHost machine) {
        final NodeUuid node = machine.nodeUuid();
        return node == null ? 0L : node.value().getMostSignificantBits() ^ node.value().getLeastSignificantBits();
    }

    /**
     * One system the Mirror serves.
     *
     * @param id        the system, as the firmware names it back
     * @param name      what it is called, with its version
     * @param house     who made it: a house, or a player for one of theirs
     * @param community whether a player made it, which the menu says beside their name
     * @param image     the medium its installer is made for, as the UEFI's list names it
     */
    public record Offer(String id, String name, String house, boolean community, Text image) {
    }

    /**
     * What the Mirror answered.
     *
     * @param mirror  the Mainframe serving it, by the name it goes by
     * @param card    the machine's card's hardware address
     * @param address the address the network handed the machine
     * @param offers  the systems it serves this machine
     */
    public record Listing(String mirror, String card, String address, List<Offer> offers) {

        public Listing {
            offers = List.copyOf(offers);
        }

        /** Whether the Mirror serves the system {@code id} to this machine. */
        public boolean serves(final String id) {
            for (final Offer offer : this.offers) {
                if (offer.id().equals(id)) {
                    return true;
                }
            }
            return false;
        }
    }
}
