/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Computers.
 */
package dev.jstech.computers.os.install;

import dev.jstech.computers.blockentity.MainframeBlockEntity;
import dev.jstech.computers.hardware.DiskSpec;
import dev.jstech.computers.hardware.EraStorage;
import dev.jstech.computers.os.FileSystemType;
import dev.jstech.computers.os.IOsHost;
import dev.jstech.computers.os.OsDef;
import dev.jstech.computers.os.OsDisks;
import dev.jstech.computers.os.OsRegistry;
import dev.jstech.computers.os.ProgramKind;
import dev.jstech.computers.os.ProgramSpec;
import dev.jstech.computers.os.disk.DiskLayout;
import dev.jstech.computers.os.disk.DiskPartitions;
import dev.jstech.computers.os.disk.PartitionEntry;
import dev.jstech.computers.os.disk.PartitionRole;
import dev.jstech.computers.os.disk.PartitionScheme;
import dev.jstech.computers.os.disk.ZfsMirrors;
import dev.jstech.computers.program.Programs;
import dev.jstech.computers.registry.ComputingComponents;
import dev.jstech.computers.registry.HardwareRegistry;
import dev.jstech.computers.terminal.IComputerTerminalHost;
import dev.jstech.core.jstechseries.network.NetworkSystem;
import dev.jstech.core.jstechseries.tier.Era;
import dev.jstech.core.jstechseries.tier.HardwareEra;
import dev.jstech.core.text.Text;
import dev.jstech.core.util.Loaded;
import dev.jstech.core.uuid.NetworkUuid;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.stream.Stream;

/**
 * Reads a machine for the things its installer has to ask about: the disks it has, what each already holds, and
 * the desktops a Mirror could serve along with the system.
 *
 * <p>Everything here is read the moment it is wanted, and nothing of it is saved. What is saved with the machine
 * are the answers, so an installer that comes back after a reload is built again from the machine as it is now:
 * a disk pulled out while nobody was looking is gone from the list, which is the truth.
 */
public final class Installers {

    private Installers() {
    }

    /**
     * The disks the installer can offer, in slot order, each named the way its maker called it, or, when
     * {@code adaNames} is set, the way {@code bsdinstall} names one instead: {@code ada0}, {@code ada1} and so on,
     * counted the same way {@link Installers#adaLabel} and the boot lines count them, so the two never disagree
     * about which drive is which.
     *
     * <p>What is free on a disk is what would really fit on it, so the free room is counted in whole units of
     * whatever this generation of disk stores a file in, and the system already on it is part of what is taken.
     * Counting it any other way would let a page offer a disk that the write then refuses.
     */
    public static List<InstallerFlow.Disk> disksOf(final IOsHost machine, final InstallerStyle style) {
        final List<InstallerFlow.Disk> disks = new ArrayList<>();
        int drive = 0;
        int ide = 0;
        int sata = 0;
        for (int slot = 0; slot < machine.diskSlots(); slot++) {
            final ItemStack stack = machine.diskInSlot(slot);
            if (!(HardwareRegistry.disk(stack) instanceof DiskSpec disk)) {
                continue;
            }
            final Era era = disk.era();
            final long sizeMb = disk.capacityItems() * EraStorage.mbPerItem(era);
            final List<DiskSpot> spots = spotsOf(stack);
            long freeMb = 0L;
            for (final DiskSpot spot : spots) {
                freeMb += spot.isFree() || spot.holdsData() ? spot.freeMb() : 0L;
            }
            final String label = switch (style) {
                case BSD_INSTALL -> adaLabel(drive);
                case OPENBSD -> ideDisk(era) ? "wd" + ide++ : "sd" + sata++;
                default -> stack.getHoverName().getString();
            };
            disks.add(new InstallerFlow.Disk(slot, label,
                    (int) Math.min(Integer.MAX_VALUE, sizeMb), (int) Math.min(Integer.MAX_VALUE, freeMb),
                    holderOf(stack, spots), disk.tier().speedMultiplier(), DiskPartitions.layout(stack).scheme(),
                    spots));
            drive++;
        }
        return disks;
    }

    /**
     * What the installer knows of the machine beyond its pages (computers/docs/design/storage-devices.md): the file
     * system the system's installer formats with on this machine, the ones it installs onto as they stand, the swap
     * partition it makes (as big as the memory, on Linux, UNIX and the BSDs) and the table it writes on a disk it
     * takes whole, MBR under a BIOS and GPT under UEFI.
     */
    public static InstallPlan.Facts factsFor(final IOsHost machine, final OsDef system) {
        final Era era = machine.installedEra();
        final List<FileSystemType> carriers = new ArrayList<>();
        for (final FileSystemType type : FileSystemType.values()) {
            if (type.carriesSystem(system.platform(), era) && type.formattableIn(era)) {
                carriers.add(type);
            }
        }
        final boolean swaps = switch (system.platform()) {
            case LINUX, FREEBSD, OPENBSD, UNIX -> true;
            default -> false;
        };
        return new InstallPlan.Facts(OsRegistry.fileSystemOf(system, era), carriers,
                swaps ? machine.ramTotalMb() : 0, DiskPartitions.schemeFor(era));
    }

    /**
     * Writes on the disk where the installer puts the system, as the copy starts (computers/docs/design/
     * storage-devices.md): the whole disk erased when it is taken whole, the table the plan leaves, every partition
     * made or formatted for it emptied, and the system's partition opened, so the copy writes there. A ZFS mirror's
     * second disk is laid out the same way. Nothing is written when the plan refuses.
     *
     * @return whether the disk was laid out for the system
     */
    public static boolean prepareDisk(final IOsHost machine, final InstallerFlow flow) {
        final InstallPlan.Plan plan = flow.plan();
        if (!plan.ok() || plan.layout() == null) {
            return false;
        }
        write(machine, plan);
        final InstallPlan.Plan mirror = InstallPlan.mirror(flow);
        if (mirror != null && mirror.ok() && mirror.layout() != null) {
            write(machine, mirror);
            ZfsMirrors.pair(machine.diskInSlot(plan.slot()), plan.systemPartition(),
                    machine.diskInSlot(mirror.slot()), mirror.systemPartition());
        }
        machine.markChanged();
        return true;
    }

    /**
     * Deletes a partition from the disk step, at once and with what it held, as Frames XP's D and Frames 7 to 11's
     * Delete did once the player had said yes.
     *
     * @return whether the table changed
     */
    public static boolean deletePartition(final IOsHost machine, final InstallerFlow flow, final int slot,
                                          final int number) {
        final ItemStack disk = editable(machine, flow, slot);
        if (disk.isEmpty()) {
            return false;
        }
        return rewrite(machine, flow, disk, DiskPartitions.layout(disk).delete(number), false);
    }

    /**
     * Makes a partition of all the free space starting at {@code startMb}, formatted with the installer's own file
     * system, as Frames 7 to 11's New did; under UEFI the EFI system partition comes first when the disk has none.
     *
     * @return whether the table changed
     */
    public static boolean newPartition(final IOsHost machine, final InstallerFlow flow, final int slot,
                                       final long startMb) {
        final ItemStack disk = editable(machine, flow, slot);
        if (disk.isEmpty()) {
            return false;
        }
        DiskLayout layout = DiskPartitions.layout(disk);
        long at = startMb;
        long room = 0L;
        for (final DiskLayout.FreeSpace gap : layout.free()) {
            if (gap.startMb() == startMb) {
                room = gap.sizeMb();
            }
        }
        if (flow.facts().scheme() == PartitionScheme.GPT && layout.scheme() == PartitionScheme.GPT
                && !InstallPlan.hasEfi(layout) && room > InstallPlan.EFI_MB) {
            final DiskLayout.Change efi = layout.create(at, InstallPlan.EFI_MB, FileSystemType.FAT32,
                    PartitionRole.EFI);
            if (efi.ok()) {
                layout = efi.layout();
                at += InstallPlan.EFI_MB;
                room -= InstallPlan.EFI_MB;
            }
        }
        final FileSystemType fs = flow.facts().fileSystem();
        return rewrite(machine, flow, disk, layout.create(at, Math.min(room, fs.mostPartitionMb()), fs,
                PartitionRole.DATA), true);
    }

    /**
     * Formats a partition with the installer's own file system, emptying it, as Frames 7 to 11's Format did once the
     * player had said yes.
     *
     * @return whether the partition was formatted
     */
    public static boolean formatPartition(final IOsHost machine, final InstallerFlow flow, final int slot,
                                          final int number) {
        final ItemStack disk = editable(machine, flow, slot);
        if (disk.isEmpty()) {
            return false;
        }
        return rewrite(machine, flow, disk, DiskPartitions.layout(disk).format(number, flow.facts().fileSystem(),
                PartitionRole.DATA), true);
    }

    /**
     * Grows a partition into all the free space right after it, as Frames 7 to 11's Extend did; one whose file system
     * cannot grow stays as it is.
     *
     * @return whether the partition grew
     */
    public static boolean extendPartition(final IOsHost machine, final InstallerFlow flow, final int slot,
                                          final int number) {
        final ItemStack disk = editable(machine, flow, slot);
        if (disk.isEmpty()) {
            return false;
        }
        final DiskLayout layout = DiskPartitions.layout(disk);
        final PartitionEntry partition = layout.find(number);
        if (partition == null) {
            return false;
        }
        final long room = layout.roomAfter(partition);
        return room > 0L && rewrite(machine, flow, disk, layout.resize(number, partition.sizeMb() + room, false,
                0L), false);
    }

    /** Remembers where the player put the system with the rest of the installer's answers. */
    public static void savePlacement(final CompoundTag pages, final InstallerFlow flow) {
        final Placement placement = flow.placement();
        pages.putLong("Place", placement.startMb());
        pages.putString("Fs", placement.fileSystem());
        pages.putLong("Shrink", placement.shrinkToMb());
        pages.putInt("MirrorSlot", placement.mirrorSlot());
    }

    /** Puts back where the player had put the system; a save from before there was any reads as none. */
    public static void restorePlacement(final CompoundTag memo, final InstallerFlow flow) {
        if (!memo.contains("Place")) {
            return;
        }
        flow.restorePlacement(new Placement(memo.getLong("Place"), memo.getString("Fs"), memo.getLong("Shrink"),
                memo.getInt("MirrorSlot")));
    }

    /**
     * A disk drive by FreeBSD's own name for one, {@code ada<n>}: shared here rather than kept inline wherever a
     * drive is named this way, so {@code bsdinstall}'s disk page and the boot lines never spell the same drive
     * two different ways.
     */
    public static String adaLabel(final int driveIndex) {
        return "ada" + driveIndex;
    }

    /**
     * Whether a disk of that era is on IDE, which OpenBSD names {@code wd}, rather than SATA, which it names
     * {@code sd}: the Vintage and Legacy disks are IDE, the later ones SATA (computers/docs/design/storage-devices.md).
     */
    public static boolean ideDisk(final Era era) {
        return era.isAtMost(HardwareEra.LEGACY);
    }

    /**
     * The desktops a Mirror could serve with that system on a machine of that generation.
     *
     * <p>A desktop cannot predate the hardware it was written for, and it has to be a desktop the system's own
     * platform runs, which is why a machine of the first age is offered none at all.
     */
    public static List<InstallerFlow.Desktop> desktopsFor(final OsDef system, final Era era,
                                                          final int eraFactor) {
        final List<InstallerFlow.Desktop> desktops = new ArrayList<>();
        for (final ProgramSpec spec : OsRegistry.programs()) {
            if (spec.kind() != ProgramKind.DESKTOP_ENVIRONMENT || OsRegistry.getDesktop(spec.id()) == null) {
                continue;
            }
            if (!spec.platforms().contains(system.platform()) || era == null || era.level() < spec.minEra().level()) {
                continue;
            }
            desktops.add(new InstallerFlow.Desktop(spec.id().toString(), spec.displayName(), spec.minDiskMb(),
                    SetupTiming.networkTicks(spec.minDiskMb(), false, eraFactor)));
        }
        return desktops;
    }

    /** The Mainframe whose Mirror serves this machine, or null when none answers. */
    @Nullable
    public static MainframeBlockEntity mirrorOf(final IOsHost machine, final ServerLevel level) {
        final MainframeBlockEntity mainframe = mainframeOf(machine, level);
        return mainframe == null || !mainframe.isMirrorActive() ? null : mainframe;
    }

    /** The Mirror serving this machine, by the name its Mainframe goes by; empty when none answers. */
    public static String mirrorHost(final IOsHost machine, final ServerLevel level) {
        final MainframeBlockEntity mainframe = mainframeOf(machine, level);
        return mainframe == null || !mainframe.isMirrorActive() ? "" : nameOf(mainframe);
    }

    /**
     * The Mainframe of the network this machine is on, by the name it goes by; empty when it is on none.
     *
     * <p>Being on a network and being served by a Mirror are two different things, and a machine can be the
     * first without the second: the Mainframe may simply not be running one.
     */
    public static String networkHost(final IOsHost machine, final ServerLevel level) {
        final MainframeBlockEntity mainframe = mainframeOf(machine, level);
        return mainframe == null ? "" : nameOf(mainframe);
    }

    /**
     * What the Mainframe of this machine's network is running, named, or nothing when it runs nothing.
     *
     * <p>Beside {@link #networkHost} and {@link #mirrorHost} because it answers the same kind of question about
     * the same Mainframe: what a machine finds when it looks up its own network.
     *
     * <p>The services are named as they are sold, which is data in every language.
     */
    public static Text servicesOn(final IOsHost machine, final ServerLevel level) {
        final MainframeBlockEntity mainframe = mainframeOf(machine, level);
        if (mainframe == null) {
            return Text.EMPTY;
        }
        final ProgramSpec engine = mainframe.activeEngine() == null ? null : Programs.get(mainframe.activeEngine());
        return Text.literal(String.join(", ", Stream.of(
                        engine == null ? null : engine.name().english(),
                        mainframe.isAutomationEngineInstalled() ? "Automation Engine" : null,
                        mainframe.isMirrorInstalled() ? "Mirror" : null)
                .filter(Objects::nonNull).toList()));
    }

    /** The Mainframe orchestrating this machine's network, or null when it belongs to none. */
    @Nullable
    private static MainframeBlockEntity mainframeOf(final IOsHost machine, final ServerLevel level) {
        final NetworkUuid net = machine.networkUuid();
        if (net == null) {
            return null;
        }
        return NetworkSystem.get(level).mainframePositionOf(net)
                .map(pos -> Loaded.blockEntity(level, BlockPos.of(pos)) instanceof MainframeBlockEntity mf ? mf : null)
                .orElse(null);
    }

    /** A Mainframe by the name it goes by, or the plain word when it was never given one. */
    private static String nameOf(final MainframeBlockEntity mainframe) {
        final String name = mainframe.console() == null ? "" : mainframe.console().computerName();
        return name == null || name.isBlank() ? "mainframe" : name;
    }

    /**
     * An installation about to be set up on this machine, with the disk and the name it suggests already filled
     * in and the desktops it could offer gathered.
     *
     * @param baseRate what this machine copies at with the disk left out, in megabytes a second: the medium it
     *                 reads from and the processor that unpacks it. The disk joins it when one is chosen
     */
    public static InstallerFlow beginning(final IOsHost machine, final ServerLevel level, final OsDef system,
                                          final double baseRate) {
        final Era era = machine.installedEra();
        final String mirror = system.installerStyle().asksMirror() ? mirrorHost(machine, level) : "";
        final List<InstallerFlow.Desktop> desktops = mirror.isEmpty()
                ? List.of() : desktopsFor(system, era, SetupTiming.eraFactor(era));
        final InstallerFlow flow = InstallerFlow.beginning(system.installerStyle(), system.id().toString(),
                system.displayName(), system.footprintMb(), baseRate, disksOf(machine, system.installerStyle()),
                machineName(machine), desktops, mirror);
        flow.setFacts(factsFor(machine, system));
        flow.suggestDisk();
        // OpenBSD asks where the sets come from, and offers the medium first, as its [cd0] does.
        if (system.installerStyle() == InstallerStyle.OPENBSD) {
            flow.setUseMirror(false);
        }
        return flow;
    }

    /**
     * An installation put back on a machine as it stands now, with the answers it had been given, including
     * bsdinstall's own pages.
     *
     * <p>The disks and the desktops are read again rather than remembered, so a disk pulled out while the world
     * was away is simply not on the list any more.
     */
    public static InstallerFlow restored(final IOsHost machine, final ServerLevel level, final OsDef system,
                                         final int copyTicks, final int stageIndex, final int targetSlot,
                                         final String computerName, final String desktopId, final int eraseSlot,
                                         final boolean portsSelected, final boolean useMirror,
                                         final boolean cronEnabled, final boolean sshdEnabled, final int options) {
        final Era era = machine.installedEra();
        final String mirror = system.installerStyle().asksMirror() ? mirrorHost(machine, level) : "";
        final List<InstallerFlow.Desktop> desktops = mirror.isEmpty()
                ? List.of() : desktopsFor(system, era, SetupTiming.eraFactor(era));
        final InstallerFlow flow = InstallerFlow.restored(system.installerStyle(), system.id().toString(),
                system.displayName(), system.footprintMb(), copyTicks, disksOf(machine, system.installerStyle()),
                desktops, mirror, stageIndex, targetSlot, computerName, desktopId, eraseSlot, portsSelected, useMirror,
                cronEnabled, sshdEnabled, options);
        flow.setFacts(factsFor(machine, system));
        return flow;
    }

    /**
     * What this computer calls itself: the name it was given, else the one on the assembly screen, else the
     * plain word.
     *
     * <p>Not the host name, which is this lowered and hyphenated for the network. This is the name a player
     * typed and should see written back to them.
     */
    public static String machineName(final IOsHost machine) {
        final String console = machine.console() == null ? "" : machine.console().computerName();
        if (console != null && !console.isBlank()) {
            return console;
        }
        final String custom = machine.customName();
        return custom == null || custom.isBlank() ? "computer" : custom;
    }

    /**
     * The name this computer answers to on a network and at a Unix prompt: its own name lowered and hyphenated,
     * or its system's when it was never given one. What a system writes about its host name on its way up has
     * to be this, since it is what the prompt will say a moment later.
     */
    public static String hostName(final IOsHost machine) {
        return machine instanceof IComputerTerminalHost terminal ? terminal.hostname() : machineName(machine);
    }

    /*
     * The disk's partitions and the free space between them, in the order they lie, each with what it holds: its free
     * room, its system and its label read off the partition itself.
     */
    private static List<DiskSpot> spotsOf(final ItemStack disk) {
        final DiskLayout layout = DiskPartitions.layout(disk);
        final List<DiskSpot> spots = new ArrayList<>();
        for (final PartitionEntry partition : layout.partitions()) {
            final String fs = partition.fs() == null ? "" : partition.fs().serializedName();
            if (!partition.holdsData()) {
                spots.add(new DiskSpot(partition.number(), partition.startMb(), partition.sizeMb(),
                        partition.sizeMb(), fs, "", "", partition.role(), partition.active()));
                continue;
            }
            final ItemStack volume = DiskPartitions.view(disk, partition.number());
            final long freeMb = DiskPartitions.freeMbOf(volume);
            final String label = volume.getOrDefault(ComputingComponents.VOLUME_LABEL.get(), "");
            spots.add(new DiskSpot(partition.number(), partition.startMb(), partition.sizeMb(), freeMb, fs,
                    systemNameOn(volume), label, partition.role(), partition.active()));
        }
        for (final DiskLayout.FreeSpace gap : layout.free()) {
            spots.add(DiskSpot.free(gap.startMb(), gap.sizeMb()));
        }
        spots.sort(Comparator.comparingLong(DiskSpot::startMb));
        return spots;
    }

    /*
     * The disk in that slot, when the installer may still change its table: before the copy starts, and only a disk
     * the installer offers. Empty otherwise.
     */
    private static ItemStack editable(final IOsHost machine, final InstallerFlow flow, final int slot) {
        if (flow.started() || flow.diskAt(slot) == null || slot < 0 || slot >= machine.diskSlots()) {
            return ItemStack.EMPTY;
        }
        return machine.diskInSlot(slot);
    }

    /*
     * Writes a change the disk step made to a table, emptying the partition it touched when {@code empties} (a
     * partition made or formatted), and reads the disks again so the page lists them as they now are.
     */
    private static boolean rewrite(final IOsHost machine, final InstallerFlow flow, final ItemStack disk,
                                   final DiskLayout.Change change, final boolean empties) {
        if (!change.ok() || change.layout() == null) {
            return false;
        }
        DiskPartitions.relayout(disk, change.layout());
        if (empties) {
            DiskPartitions.wipe(disk, change.number());
        }
        flow.refreshDisks(disksOf(machine, flow.style()));
        machine.markChanged();
        return true;
    }

    /* Writes a plan on its disk: erased first when it takes the disk whole, then laid out, emptied and opened. */
    private static void write(final IOsHost machine, final InstallPlan.Plan plan) {
        if (plan.wholeDisk()) {
            machine.formatDisk(plan.slot());
        }
        final ItemStack disk = machine.diskInSlot(plan.slot());
        DiskPartitions.relayout(disk, plan.layout());
        for (final int number : plan.made()) {
            DiskPartitions.wipe(disk, number);
        }
        DiskPartitions.open(disk, plan.systemPartition());
    }

    /**
     * The system on that disk, by the name a person reads: the one on the partition it starts from, else the first on
     * any other, or empty when it carries none.
     */
    private static String holderOf(final ItemStack disk, final List<DiskSpot> spots) {
        final String open = systemNameOn(disk);
        if (!open.isEmpty()) {
            return open;
        }
        for (final DiskSpot spot : spots) {
            if (spot.hasSystem()) {
                return spot.holds();
            }
        }
        return "";
    }

    /** The system on that volume, by the name a person reads, or empty when it carries none. */
    private static String systemNameOn(final ItemStack volume) {
        final ResourceLocation osId = OsDisks.systemOn(volume);
        @Nullable final OsDef held = osId == null ? null : OsRegistry.getOs(osId);
        return held == null ? "" : held.displayName();
    }
}
