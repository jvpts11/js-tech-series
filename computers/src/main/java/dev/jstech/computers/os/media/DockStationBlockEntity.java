/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Computers.
 */
package dev.jstech.computers.os.media;

import com.mojang.serialization.Codec;
import dev.jstech.computers.ComputingModule;
import dev.jstech.computers.audio.ComputingSounds;
import dev.jstech.computers.audio.SystemSound;
import dev.jstech.computers.hardware.StorageTier;
import dev.jstech.computers.item.DiskItem;
import dev.jstech.computers.machine.DriveTable;
import dev.jstech.computers.os.IOsHost;
import dev.jstech.computers.printer.Printers;
import dev.jstech.core.audio.Audio;
import dev.jstech.core.blockentity.FieldItemHandler;
import dev.jstech.core.blockentity.ValueField;
import dev.jstech.core.util.Loaded;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.items.ItemStackHandler;
import software.bernie.geckolib.animation.AnimatableManager;

/**
 * The Dock Station: a reader on the desk that takes a disk of any era besides the flash drive in its port. Its three
 * trays take the disks by size, a hard disk in the 3.5" bay (the IDE disks of old and the Keep), a SATA disk in the
 * 2.5" bay (the Link and the Swift), an NVMe in the M.2 tray (the Bolt); the stick goes in the USB port, which is the
 * reader's own slot, so a system installs from it as it did. The computer it is linked to sees each docked disk as an
 * external drive, to open and copy to.
 *
 * <p>The players who see it are sent the disks in its trays and the letter each has on the computer, which its window
 * shows; the window takes the disks in and out too.
 */
public class DockStationBlockEntity extends MediaReaderBlockEntity {

    /* A tray takes one disk, of the size it is cut for. */
    private final FieldItemHandler bays = fields().items("Bays", BAYS).save().toClient().dropsWhenBroken()
            .slotLimit(1).accepts((bay, stack) -> bayOf(stack) == bay).onChange(this::bayMoved)
            .onLoad(this::settleBays);
    /** The letter of each tray's disk and the stick on the linked computer, a space for none, kept a second old. */
    private final ValueField<String> letters = fields().value("Letters", Codec.STRING, "    ").toClient();
    /** The host name of the computer it is docked to, empty while it is docked to none. */
    private final ValueField<String> host = fields().value("Host", Codec.STRING, "").toClient();
    /** What each tray held a moment ago, so a change is heard as a disk going in or coming out. */
    private final ItemStack[] held = {ItemStack.EMPTY, ItemStack.EMPTY, ItemStack.EMPTY};
    /** Whether the block is being broken, when its disks leave without the sound of a tray. */
    private boolean breaking;

    /** The trays, in the order they stand on the front, top to bottom. */
    public static final int BAY_HDD = 0;
    public static final int BAY_SSD = 1;
    public static final int BAY_NVME = 2;
    public static final int BAYS = 3;
    /** Where the stick's letter is kept, after the trays'. */
    public static final int USB = 3;
    /** How often the letters are read again off the linked computer. */
    private static final int LETTER_TICKS = 20;

    public DockStationBlockEntity(final BlockPos pos, final BlockState state) {
        super(ComputingModule.DOCK_STATION_BE.get(), pos, state);
        fields().whenBroken((level, at) -> breaking = true);
    }

    public static void dockTick(final Level level, final BlockPos pos, final BlockState state,
                                final DockStationBlockEntity dock) {
        serverTick(level, pos, state, dock);
        if (level instanceof ServerLevel server && server.getGameTime() % LETTER_TICKS == 0) {
            dock.readLetters(server);
        }
    }

    /** The tray a disk goes in by its size, or -1 for what is not a disk. */
    public static int bayOf(final ItemStack stack) {
        if (!(stack.getItem() instanceof DiskItem disk)) {
            return -1;
        }
        return bayOf(disk.spec().tier());
    }

    /** The tray a size of disk goes in. */
    public static int bayOf(final StorageTier tier) {
        return switch (tier) {
            case HDD -> BAY_HDD;
            case SSD -> BAY_SSD;
            case NVME -> BAY_NVME;
        };
    }

    public ItemStackHandler bays() {
        return bays;
    }

    /** The disk in a tray, empty when it holds none. */
    public ItemStack disk(final int bay) {
        return bay >= 0 && bay < BAYS ? bays.getStackInSlot(bay) : ItemStack.EMPTY;
    }

    /** Puts a disk in its tray; what is left over, the disk itself when its tray is taken or it is no disk. */
    public ItemStack insertDisk(final ItemStack stack) {
        final int bay = bayOf(stack);
        return bay < 0 ? stack : bays.insertItem(bay, stack, false);
    }

    /** Takes the disk out of a tray, or nothing when it is empty. */
    public ItemStack ejectDisk(final int bay) {
        if (bay < 0 || bay >= BAYS) {
            return ItemStack.EMPTY;
        }
        final ItemStack out = bays.getStackInSlot(bay);
        bays.setStackInSlot(bay, ItemStack.EMPTY);
        return out;
    }

    /** The letter the linked computer gives a tray's disk, or the stick's at {@link #USB}; a space for none. */
    public char letter(final int slot) {
        final String now = letters.get();
        return slot >= 0 && slot < now.length() ? now.charAt(slot) : ' ';
    }

    /** The host name of the computer the dock is docked to, empty for none. */
    public String hostName() {
        return host.get();
    }

    /** Saves and shows a disk whose files were just changed in place, as the drives' are. */
    public void diskChanged() {
        setChanged();
        fields().syncToClients();
    }

    /* The dock has no clips; its lamps and the stick are shown and hidden by its renderer. */
    @Override
    public void registerControllers(final AnimatableManager.ControllerRegistrar controllers) {
    }

    /*
     * A disk going into a tray or coming out sounds like a sled sliding in or out of a server rack, and the linked
     * computer's system hears a device come or go, as it hears the stick.
     */
    private void bayMoved(final int bay) {
        final ItemStack now = bays.getStackInSlot(bay);
        final boolean in = held[bay].isEmpty() && !now.isEmpty();
        final boolean out = !held[bay].isEmpty() && now.isEmpty();
        held[bay] = now.copy();
        if (breaking || !(level instanceof ServerLevel server) || !(in || out)) {
            return;
        }
        Audio.at(server, worldPosition, in ? ComputingSounds.RACK_SLIDE_IN : ComputingSounds.RACK_SLIDE_OUT);
        final BlockPos owner = ownerPos();
        if (owner != null && Loaded.blockEntity(server, owner) instanceof IOsHost machine && machine.isRunning()
                && machine.bootedDesktopId() != null && !machine.isDisabled(worldPosition.asLong())) {
            machine.systemSound(server, in ? SystemSound.DEVICE_CONNECT : SystemSound.DEVICE_DISCONNECT);
        }
    }

    /* What the trays hold as they come back from a save or a packet, which is not a disk going in. */
    private void settleBays() {
        for (int bay = 0; bay < BAYS; bay++) {
            held[bay] = bays.getStackInSlot(bay).copy();
        }
    }

    /* Each docked disk's letter and the stick's, as the linked computer letters its drives. */
    private void readLetters(final ServerLevel server) {
        final char[] found = {' ', ' ', ' ', ' '};
        final BlockPos owner = ownerPos();
        final BlockEntity computer = owner == null ? null : Loaded.blockEntity(server, owner);
        host.set(computer instanceof IOsHost machine ? Printers.machineName(machine) : "");
        if (computer instanceof IOsHost machine && !machine.isDisabled(worldPosition.asLong())) {
            for (final DriveTable.Drive drive : DriveTable.of(computer, server).all()) {
                for (int bay = 0; bay < BAYS; bay++) {
                    if (!disk(bay).isEmpty() && drive.disk() == disk(bay)) {
                        found[bay] = drive.drive();
                    }
                }
                if (!mediaSlot().getStackInSlot(0).isEmpty() && drive.disk() == mediaSlot().getStackInSlot(0)) {
                    found[USB] = drive.drive();
                }
            }
        }
        letters.set(new String(found));
    }
}
