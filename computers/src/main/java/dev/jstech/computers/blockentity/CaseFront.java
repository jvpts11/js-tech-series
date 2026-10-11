/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Computers.
 */
package dev.jstech.computers.blockentity;

import dev.jstech.computers.audio.ComputingSounds;
import dev.jstech.computers.audio.MediaBaySounds;
import dev.jstech.computers.audio.SystemSound;
import dev.jstech.computers.hardware.BayDriveSpec;
import dev.jstech.computers.item.BayDriveItem;
import dev.jstech.computers.os.media.DiscTray;
import dev.jstech.computers.os.media.FormattedMediaItem;
import dev.jstech.computers.os.media.MediaFormat;
import dev.jstech.core.audio.Audio;
import dev.jstech.core.blockentity.BlockEntityFields;
import dev.jstech.core.blockentity.FieldItemHandler;
import dev.jstech.core.util.BlockDrops;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import org.jetbrains.annotations.Nullable;

/**
 * The front of a small computer's case: what its 5.25-inch bays and its USB ports hold. A bay holds the part installed
 * in it on the assembly screen. An internal drive there has a tray its eject button opens and closes, and the disc on
 * the tray is read only with the tray in, as on an external drive. The hot-swap bay takes a disk from the front, in or
 * out with the machine on or off, and that disk is one of the computer's own. A USB flash drive plugs into a port.
 *
 * <p>What the bays and ports hold is saved, sent to the players who see the case, who see the disc on its tray, the
 * stick in its port and the caddy in the hot-swap bay, and spilled with the hardware when the case is broken. A part
 * taken out of a bay, or swapped for another, lets fall what it held.
 */
public final class CaseFront {

    private final AbstractSmallComputerBlockEntity computer;
    /* What each bay holds, then what each port holds: one item each. */
    private final FieldItemHandler held;
    private final DiscTray[] trays = new DiscTray[BAYS];
    /* What each slot sounds like taking a medium in and giving it back. */
    private final MediaBaySounds[] sounds = new MediaBaySounds[SLOTS];
    /* Whether the case is being broken, when what it holds leaves without the sound of an eject. */
    private boolean breaking;

    /** The most bays and ports any case has. */
    public static final int BAYS = 2;
    public static final int PORTS = 2;
    public static final int SLOTS = BAYS + PORTS;
    /* The trays' clips, by bay and by kind of drive, made once: a tray asks for them every frame. */
    private static final List<Map<BayDriveSpec.Kind, DiscTray.Clips>> CLIPS = clips();

    CaseFront(final AbstractSmallComputerBlockEntity computer, final BlockEntityFields fields) {
        this.computer = computer;
        for (int slot = 0; slot < SLOTS; slot++) {
            sounds[slot] = new MediaBaySounds();
        }
        this.held = fields.items("Front", SLOTS).save().toClient().slotLimit(1).accepts(this::accepts)
                .onChange(this::moved).onLoad(this::settle);
        for (int bay = 0; bay < BAYS; bay++) {
            trays[bay] = new DiscTray(fields, "Bay" + bay + "Tray");
        }
    }

    /** The slot of port {@code port}, after the bays'. */
    public static int portSlot(final int port) {
        return BAYS + port;
    }

    /** How many 5.25-inch bays the case has. */
    public int bays() {
        return Math.min(BAYS, computer.caseBays());
    }

    /** How many USB ports the case has on its front. */
    public int ports() {
        return Math.min(PORTS, computer.casePorts());
    }

    /**
     * The part installed in bay {@code bay}, by its kind, or null for an empty bay or one the case does not have. Read
     * from what the players who see the case are told, which the server keeps in step with the hardware, so both sides
     * answer alike.
     */
    @Nullable
    public BayDriveSpec.Kind kind(final int bay) {
        if (bay < 0 || bay >= bays()) {
            return null;
        }
        final ResourceLocation part = computer.installedPart(computer.hardwareLayout().bayStart() + bay);
        return part != null && BuiltInRegistries.ITEM.get(part) instanceof BayDriveItem drive ? drive.spec().kind()
                : null;
    }

    /** What slot {@code slot} holds, read or not. */
    public ItemStack held(final int slot) {
        return slot >= 0 && slot < SLOTS ? held.getStackInSlot(slot) : ItemStack.EMPTY;
    }

    /** The disc the drive in bay {@code bay} reads: the one on its tray, with the tray in. */
    public ItemStack readable(final int bay) {
        final BayDriveSpec.Kind kind = kind(bay);
        final ItemStack disc = held(bay);
        return kind != null && kind.reads() != null && trays[bay].reads(disc) ? disc : ItemStack.EMPTY;
    }

    /** The disk in the hot-swap bay {@code bay}, or nothing when the bay holds another part or no disk. */
    public ItemStack hotSwapDisk(final int bay) {
        return kind(bay) == BayDriveSpec.Kind.HOT_SWAP ? held(bay) : ItemStack.EMPTY;
    }

    /** The stick in port {@code port}, or nothing. */
    public ItemStack stick(final int port) {
        return port >= 0 && port < ports() ? held(portSlot(port)) : ItemStack.EMPTY;
    }

    /** The tray of the drive in bay {@code bay}. */
    public DiscTray tray(final int bay) {
        return trays[bay];
    }

    /**
     * Whether slot {@code slot} takes {@code stack}: a bay's drive a disc of a format it reads, the hot-swap bay a disk
     * the computer takes in its own disk bays, a port a USB flash drive; a bay or a port the case does not have, or a
     * bay with no part in it, nothing.
     */
    public boolean accepts(final int slot, final ItemStack stack) {
        if (slot >= BAYS) {
            return slot - BAYS < ports() && stack.getItem() instanceof FormattedMediaItem stick
                    && stick.format() == MediaFormat.USB;
        }
        final BayDriveSpec.Kind kind = kind(slot);
        if (kind == null) {
            return false;
        }
        if (kind == BayDriveSpec.Kind.HOT_SWAP) {
            return computer.acceptsDisk(stack);
        }
        return stack.getItem() instanceof FormattedMediaItem disc && disc.format().onTray()
                && kind.reads().accepts(disc.format());
    }

    /**
     * Puts {@code stack} in slot {@code slot}.
     *
     * @return what is left over: empty when it went in, the stack unchanged when the slot takes it not or is full
     */
    public ItemStack insert(final int slot, final ItemStack stack) {
        return held.insertItem(slot, stack, false);
    }

    /** Takes out and returns what slot {@code slot} holds, or nothing when it is empty. */
    public ItemStack take(final int slot) {
        final ItemStack out = held(slot);
        if (!out.isEmpty()) {
            held.setStackInSlot(slot, ItemStack.EMPTY);
        }
        return out;
    }

    /**
     * The clips the tray of the drive in bay {@code bay} plays, named by the kind of drive the model shows there, or
     * null for a bay with no drive that has a tray.
     */
    @Nullable
    public DiscTray.Clips trayClips(final int bay) {
        final BayDriveSpec.Kind kind = kind(bay);
        return kind == null || kind.reads() == null ? null : CLIPS.get(bay).get(kind);
    }

    /**
     * The part the model shows in bay {@code bay}, as its bones are named: {@code cd}, {@code dvd}, {@code bd}, and
     * the hot-swap bay {@code hotswap} with a disk in it or {@code hotswap_empty}; null for an empty bay.
     */
    @Nullable
    public String shown(final int bay) {
        final BayDriveSpec.Kind kind = kind(bay);
        if (kind == null) {
            return null;
        }
        return kind == BayDriveSpec.Kind.HOT_SWAP ? (held(bay).isEmpty() ? "hotswap_empty" : "hotswap")
                : boneName(kind);
    }

    /**
     * The parts in the bays changed from {@code before} to {@code after}, the item in each hardware slot: a bay whose
     * part is gone or is another lets fall what it held, and its tray is in again, as the next part comes.
     */
    void partsChanged(final List<ResourceLocation> before, final List<ResourceLocation> after) {
        if (before.isEmpty() || !(computer.getLevel() instanceof ServerLevel level)) {
            return; // the record just came from the save: nothing was taken out
        }
        final int start = computer.hardwareLayout().bayStart();
        for (int bay = 0; bay < BAYS; bay++) {
            final int slot = start + bay;
            if (slot < before.size() && slot < after.size() && before.get(slot).equals(after.get(slot))) {
                continue;
            }
            final ItemStack out = take(bay);
            if (!out.isEmpty()) {
                Block.popResource(level, computer.getBlockPos(), out);
            }
            if (trays[bay].isOpen()) {
                trays[bay].press(level, computer.getBlockPos());
            }
        }
    }

    /** Spills what the bays and ports hold, as the case breaks: no sound of an eject. */
    void spill(final ServerLevel level, final BlockPos pos) {
        breaking = true;
        BlockDrops.spill(level, pos, held);
    }

    /*
     * A medium went in or came out: a stick is heard in its port and is a device coming or going for the system, a disk
     * slid into the hot-swap bay or out of it is heard and is one too; a disc only lies on its tray, whose ride is heard.
     */
    private void moved(final int slot) {
        if (breaking || !(computer.getLevel() instanceof ServerLevel level)) {
            sounds[slot].settle(held(slot));
            return;
        }
        final ItemStack now = held(slot);
        if (slot < BAYS && kind(slot) == BayDriveSpec.Kind.HOT_SWAP) {
            final boolean in = !now.isEmpty();
            sounds[slot].settle(now);
            Audio.at(level, computer.getBlockPos(), in ? ComputingSounds.RACK_SLIDE_IN : ComputingSounds.RACK_SLIDE_OUT);
            deviceMoved(level, in);
            return;
        }
        final MediaBaySounds.Move move = sounds[slot].changed(level, computer.getBlockPos(), now);
        if (move != null && move.format() == MediaFormat.USB) {
            deviceMoved(level, move.in());
        }
    }

    /* A device plugged in or pulled out, which the computer's system says with its own sound when it is up. */
    private void deviceMoved(final ServerLevel level, final boolean in) {
        if (computer.isRunning() && computer.bootedDesktopId() != null) {
            computer.systemSound(level, in ? SystemSound.DEVICE_CONNECT : SystemSound.DEVICE_DISCONNECT);
        }
    }

    private void settle() {
        for (int slot = 0; slot < SLOTS; slot++) {
            sounds[slot].settle(held(slot));
        }
    }

    /* The name a drive of that kind goes by in the model's bones. */
    private static String boneName(final BayDriveSpec.Kind kind) {
        return switch (kind) {
            case CD -> "cd";
            case DVD -> "dvd";
            case BLU_RAY -> "bd";
            case HOT_SWAP -> "hotswap";
        };
    }

    private static List<Map<BayDriveSpec.Kind, DiscTray.Clips>> clips() {
        final List<Map<BayDriveSpec.Kind, DiscTray.Clips>> out = new ArrayList<>(BAYS);
        for (int bay = 0; bay < BAYS; bay++) {
            final Map<BayDriveSpec.Kind, DiscTray.Clips> ofBay = new EnumMap<>(BayDriveSpec.Kind.class);
            for (final BayDriveSpec.Kind kind : BayDriveSpec.Kind.values()) {
                final String suffix = "_" + bay + "_" + boneName(kind);
                ofBay.put(kind, new DiscTray.Clips("animation.computer.open_tray" + suffix,
                        "animation.computer.close_tray" + suffix, "animation.computer.tray_out" + suffix));
            }
            out.add(ofBay);
        }
        return List.copyOf(out);
    }
}
