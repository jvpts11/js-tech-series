/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Computers.
 */
package dev.jstech.computers.chain.machine;

import dev.jstech.computers.block.AbstractSmallComputerBlock;
import dev.jstech.computers.blockentity.AbstractSmallComputerBlockEntity;
import dev.jstech.computers.item.ServerItem;
import dev.jstech.computers.os.BoardFirmware;
import dev.jstech.computers.os.OsDef;
import dev.jstech.computers.os.OsDisks;
import dev.jstech.computers.os.OsGating;
import dev.jstech.computers.os.OsRegistry;
import dev.jstech.computers.os.ProgramSpec;
import dev.jstech.computers.os.ProgramVersions;
import dev.jstech.computers.os.install.SetupGate;
import dev.jstech.computers.os.media.FormattedMediaItem;
import dev.jstech.computers.os.media.MediaItem;
import dev.jstech.computers.os.media.MediaKind;
import dev.jstech.computers.program.ComputerConsoleState;
import dev.jstech.computers.registry.ComputingComponents;
import dev.jstech.computers.registry.HardwareRegistry;
import dev.jstech.core.id.IStableName;
import dev.jstech.core.text.Text;
import dev.jstech.core.text.TextHolder;
import dev.jstech.core.text.TextKey;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.TreeSet;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.ItemContainerContents;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.neoforged.neoforge.items.ItemStackHandler;
import org.jetbrains.annotations.Nullable;

/**
 * What the Preinstaller writes (computers/docs/design/survival.md), one job at a time: onto an assembled computer its
 * board's firmware, then a system and programs onto its system disk; onto a loose motherboard its firmware; onto a
 * loose disk a system and programs, which is how a rack server's bay drives get their system; onto a rack server its
 * board's firmware. The firmware is the board's era's; the system and the programs come from the install media put in
 * it, read and never used up, and are written as an installer would write them, by its rules. Where there is nothing
 * to write, or the rules refuse, it says why.
 */
@TextHolder
public final class Preinstalls {

    public static final TextKey NOTHING_TO_WRITE = TextKey.of("jsc.preinstaller.nothing_to_write",
            "Nothing to write: its firmware is written, and no medium brings a system or a program it lacks");
    public static final TextKey NO_DISK = TextKey.of("jsc.preinstaller.no_disk", "No disk in it to write %s onto");
    public static final TextKey NO_ROOM = TextKey.of("jsc.preinstaller.no_room", "No room on its disk for %s");
    public static final TextKey WRONG_PROCESSOR = TextKey.of("jsc.preinstaller.wrong_processor",
            "%s does not run on its processor");
    public static final TextKey NEEDS_ERA = TextKey.of("jsc.preinstaller.needs_era",
            "%s needs a computer of the %s or later");
    public static final TextKey MISSING_DISKS = TextKey.of("jsc.preinstaller.missing_disks",
            "%s comes on %s disks, and %s of them are in");
    public static final TextKey TWO_SYSTEMS = TextKey.of("jsc.preinstaller.two_systems",
            "Its media bring two systems, %s and %s: keep the one to write");
    public static final TextKey NEEDS_SYSTEM = TextKey.of("jsc.preinstaller.needs_system",
            "%s needs a system on the disk first");
    public static final TextKey WRONG_PLATFORM = TextKey.of("jsc.preinstaller.wrong_platform",
            "%s does not run on %s");
    /* Of a job, a second more for every hundred megabytes it writes, on top of its machine's step. */
    private static final int MEGABYTES_A_SECOND = 100;

    private Preinstalls() {
    }

    /** Whether {@code stack} is something it writes onto: a computer with its parts, a board, a disk, a server. */
    public static boolean isWork(final ItemStack stack) {
        return HardwareRegistry.board(stack) != null || HardwareRegistry.disk(stack) != null
                || stack.getItem() instanceof ServerItem || isComputer(stack);
    }

    /** Whether {@code stack} is an install medium it reads: a system's or a program's. */
    public static boolean isMedium(final ItemStack stack) {
        return stack.getItem() instanceof FormattedMediaItem && MediaItem.payload(stack) != null
                && MediaItem.kind(stack) != MediaKind.DATA;
    }

    /** The job of writing onto {@code work} from {@code media}: what comes out and in how long, or why it will not. */
    public static Job prepare(final ItemStack work, final List<ItemStack> media) {
        final ItemStack one = work.copyWithCount(1);
        final Given given = Given.of(media);
        if (!given.refusal().isEmpty()) {
            return Job.refused(given.refusal());
        }
        if (HardwareRegistry.board(one) != null) {
            return BoardFirmware.blank(one) ? new Job(BoardFirmware.written(one), Station.BOARD_FIXTURE, 0, true,
                    Text.EMPTY) : Job.refused(NOTHING_TO_WRITE.text());
        }
        if (HardwareRegistry.disk(one) != null) {
            return disk(one, given);
        }
        if (one.getItem() instanceof ServerItem) {
            if (!BoardFirmware.blank(ServerItem.board(one))) {
                return Job.refused(NOTHING_TO_WRITE.text());
            }
            ServerItem.replaceBoard(one, BoardFirmware.written(ServerItem.board(one)));
            return new Job(one, Station.CRADLE, 0, true, Text.EMPTY);
        }
        return computer(one, given);
    }

    /* A loose disk: the system of its media and their programs, written as an installer writes them. */
    private static Job disk(final ItemStack disk, final Given given) {
        final ItemStack[] held = {disk};
        int megabytes = 0;
        boolean wrote = false;
        if (given.os() != null && !OsDisks.systemsOn(disk).has(given.os().id())) {
            if (!OsDisks.installOs(1, slot -> held[0], (stack, slot) -> held[0] = stack, given.os().id(), 0)) {
                return Job.refused(NO_ROOM.with(given.os().displayName()));
            }
            final ComputerConsoleState console = consoleOf(held[0]);
            OsDisks.installBundledSpace(given.os(), console);
            keep(held[0], console);
            megabytes += given.os().footprintMb();
            wrote = true;
        }
        final ResourceLocation systemId = OsDisks.systemOn(held[0]);
        final OsDef system = systemId == null ? null : OsRegistry.getOs(systemId);
        final ComputerConsoleState console = consoleOf(held[0]);
        for (final ProgramSpec program : given.programs()) {
            if (program.preinstalled() || console.isInstalled(program.id().toString())) {
                continue;
            }
            if (system == null) {
                return Job.refused(NEEDS_SYSTEM.with(program.displayName()));
            }
            if (!program.platforms().contains(system.platform())) {
                return Job.refused(WRONG_PLATFORM.with(program.displayName(), system.displayName()));
            }
            install(console, program);
            megabytes += program.minDiskMb();
            wrote = true;
        }
        keep(held[0], console);
        return wrote ? new Job(held[0], Station.DISK_DOCK, megabytes, false, Text.EMPTY)
                : Job.refused(NOTHING_TO_WRITE.text());
    }

    /*
     * An assembled computer: its board's firmware, then the system of its media onto its system disk and their
     * programs, by the rules the computer's own installer keeps; a computer that stands nowhere is asked.
     */
    private static Job computer(final ItemStack item, final Given given) {
        if (!(item.getItem() instanceof BlockItem blockItem)
                || !(blockItem.getBlock() instanceof AbstractSmallComputerBlock<?> block)
                || !(block.newBlockEntity(BlockPos.ZERO, block.defaultBlockState())
                instanceof AbstractSmallComputerBlockEntity computer)) {
            return Job.refused(NOTHING_TO_WRITE.text());
        }
        final ItemContainerContents parts = item.getOrDefault(ComputingComponents.ASSEMBLED_HARDWARE.get(),
                ItemContainerContents.EMPTY);
        final ItemStackHandler seats = computer.getHardware();
        for (int slot = 0; slot < parts.getSlots() && slot < seats.getSlots(); slot++) {
            seats.setStackInSlot(slot, parts.getStackInSlot(slot).copy());
        }
        int megabytes = 0;
        boolean wrote = false;
        boolean firmware = false;
        final int boardSlot = computer.hardwareLayout().motherboardSlot();
        if (BoardFirmware.blank(seats.getStackInSlot(boardSlot))) {
            seats.setStackInSlot(boardSlot, BoardFirmware.written(seats.getStackInSlot(boardSlot)));
            firmware = true;
            wrote = true;
        }
        final OsDef os = given.os();
        if (os != null && (computer.installedOs() == null || !computer.installedOs().id().equals(os.id()))) {
            final Optional<Text> refused = systemRefusal(computer, os);
            if (refused.isPresent()) {
                return Job.refused(refused.get());
            }
            megabytes += os.footprintMb();
            wrote = true;
        }
        for (final ProgramSpec program : given.programs()) {
            if (program.preinstalled() || computer.console().isInstalled(program.id().toString())) {
                continue;
            }
            final Optional<Text> refused = SetupGate.refusal(computer, program, false, true);
            if (refused.isPresent()) {
                return Job.refused(refused.get());
            }
            install(computer.console(), program);
            megabytes += program.minDiskMb();
            wrote = true;
        }
        // What was written goes back onto the system disk, as every change to the software does.
        computer.setChanged();
        if (!wrote) {
            return Job.refused(NOTHING_TO_WRITE.text());
        }
        final ItemStack done = item.copy();
        done.set(ComputingComponents.ASSEMBLED_HARDWARE.get(), computer.collectComponents()
                .getOrDefault(ComputingComponents.ASSEMBLED_HARDWARE.get(), ItemContainerContents.EMPTY));
        return new Job(done, Station.CRADLE, megabytes, firmware, Text.EMPTY);
    }

    /* Writes {@code os} onto the computer's system disk; why not, when its era, its processor or its disk refuse. */
    private static Optional<Text> systemRefusal(final AbstractSmallComputerBlockEntity computer, final OsDef os) {
        boolean disk = false;
        for (int slot = 0; slot < computer.diskSlots() && !disk; slot++) {
            disk = !computer.diskInSlot(slot).isEmpty();
        }
        if (!disk) {
            return Optional.of(NO_DISK.with(os.displayName()));
        }
        if (computer.displayEra() != null && !OsGating.canInstall(os.minEra(), computer.displayEra())) {
            return Optional.of(NEEDS_ERA.with(os.displayName(), os.minEra().named()));
        }
        if (!computer.runsSystem(os)) {
            return Optional.of(WRONG_PROCESSOR.with(os.displayName()));
        }
        return computer.installOs(os.id()) ? Optional.empty() : Optional.of(NO_ROOM.with(os.displayName()));
    }

    private static boolean isComputer(final ItemStack stack) {
        return stack.getItem() instanceof BlockItem item && item.getBlock() instanceof AbstractSmallComputerBlock<?>
                && stack.has(ComputingComponents.ASSEMBLED_HARDWARE.get());
    }

    private static void install(final ComputerConsoleState console, final ProgramSpec program) {
        final String id = program.id().toString();
        console.install(id);
        console.setInstalledVersion(id, ProgramVersions.of(id));
    }

    /* The software state written on a loose disk, or a fresh one for a disk that keeps none. */
    private static ComputerConsoleState consoleOf(final ItemStack disk) {
        final ComputerConsoleState console = new ComputerConsoleState();
        final CompoundTag saved = disk.get(ComputingComponents.DISK_CONSOLE.get());
        if (saved != null) {
            console.load(saved);
        }
        return console;
    }

    private static void keep(final ItemStack disk, final ComputerConsoleState console) {
        final CompoundTag saved = new CompoundTag();
        console.save(saved);
        disk.set(ComputingComponents.DISK_CONSOLE.get(), saved);
    }

    /** Which of its stations does a job: the cradle a computer, the fixture a board, the dock a disk. */
    public enum Station implements IStableName {
        CRADLE("cradle"),
        BOARD_FIXTURE("board_fixture"),
        DISK_DOCK("disk_dock");

        private final String serializedName;

        Station(final String serializedName) {
            this.serializedName = serializedName;
        }

        @Override
        public String serializedName() {
            return serializedName;
        }
    }

    /**
     * One job: what comes out, at which station, how much it writes, whether it writes the firmware; or why it will
     * not do it.
     *
     * @param result    what comes out; empty when it was refused
     * @param station   the station that does it
     * @param megabytes how much of a system and programs it writes
     * @param firmware  whether it writes a board's firmware first
     * @param refusal   why it will not; empty when it will
     */
    public record Job(ItemStack result, Station station, int megabytes, boolean firmware, Text refusal) {

        /** Whether it does the job. */
        public boolean done() {
            return !result.isEmpty();
        }

        /** How long it takes, in seconds: the machine's step, and a second more for every hundred megabytes. */
        public int seconds(final int step) {
            return step + (megabytes + MEGABYTES_A_SECOND - 1) / MEGABYTES_A_SECOND;
        }

        static Job refused(final Text why) {
            return new Job(ItemStack.EMPTY, Station.CRADLE, 0, false, why);
        }
    }

    /* What its media bring: one system, whole on its disks, and programs; or why they cannot be read together. */
    private record Given(@Nullable OsDef os, List<ProgramSpec> programs, Text refusal) {

        static Given of(final List<ItemStack> media) {
            OsDef os = null;
            final TreeSet<Integer> parts = new TreeSet<>();
            final List<ProgramSpec> programs = new ArrayList<>();
            for (final ItemStack medium : media) {
                final ResourceLocation payload = MediaItem.payload(medium);
                if (payload == null) {
                    continue;
                }
                if (MediaItem.kind(medium) == MediaKind.PROGRAM_INSTALL) {
                    final ProgramSpec program = OsRegistry.getProgram(payload);
                    if (program != null && !programs.contains(program)) {
                        programs.add(program);
                    }
                    continue;
                }
                final OsDef system = OsRegistry.getOs(payload);
                if (system == null) {
                    continue;
                }
                if (os != null && !os.id().equals(system.id())) {
                    return new Given(null, List.of(), TWO_SYSTEMS.with(os.displayName(), system.displayName()));
                }
                os = system;
                parts.add(MediaItem.part(medium));
            }
            if (os != null) {
                final int needed = Math.max(1, os.installerStyle().disks().size());
                if (parts.size() < needed || parts.first() != 1 || parts.last() != needed) {
                    return new Given(null, List.of(), MISSING_DISKS.with(os.displayName(), needed, parts.size()));
                }
            }
            return new Given(os, List.copyOf(programs), Text.EMPTY);
        }
    }
}
