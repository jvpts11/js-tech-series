/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Computers.
 */
package dev.jstech.computers.block;

import dev.jstech.computers.blockentity.AbstractSmallComputerBlockEntity;
import dev.jstech.computers.blockentity.CaseFront;
import dev.jstech.computers.hardware.BayDriveSpec;
import dev.jstech.computers.hardware.CaseFrontLayout;
import dev.jstech.computers.item.DiskItem;
import dev.jstech.computers.os.media.DiscTray;
import dev.jstech.computers.os.media.EjectButton;
import dev.jstech.computers.os.media.FormattedMediaItem;
import dev.jstech.computers.os.media.MediaReaderBlock;
import dev.jstech.core.text.GameText;
import dev.jstech.core.text.Text;
import dev.jstech.core.text.TextHolder;
import dev.jstech.core.text.TextKey;
import net.minecraft.core.BlockPos;
import net.minecraft.world.Containers;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.Nullable;

/**
 * A click on the parts of a small computer's front, past its power button: the eject button of the drive in each bay,
 * which opens and closes its tray as an external drive's does; the disc laid on an open tray with a click and lifted
 * off it with a sneak-click; a USB flash drive plugged into a port with a click and pulled out with a sneak-click; and
 * a disk slid into the hot-swap bay with a click and taken out with a sneak-click, with the machine on or off.
 *
 * <p>Each answers only where it would do something, so a click anywhere else on the case is still the case's own: its
 * assembly screen, or with a sneak its side panel. What is refused says why, as the external drives do.
 */
@TextHolder
final class CaseFrontClicks {

    /*
     * How far in front of the face an open tray reaches, in sixty-fourths, halfway along: a look at the tray standing out
     * is followed to there, so a click on the tray finds its bay from above or from the side as well as head on.
     */
    static final int TRAY_REACH = 8;

    private static final TextKey PORT_TAKEN = TextKey.of("jsc.case_front.port_taken",
            "The port already holds a stick - sneak-click to pull it out.");
    private static final TextKey BAY_TAKEN = TextKey.of("jsc.case_front.bay_taken",
            "The bay already holds a disk - sneak-click to take it out.");
    private static final TextKey DISK_REFUSED = TextKey.of("jsc.case_front.disk_refused",
            "This computer does not take that disk.");

    private CaseFrontClicks() {
    }

    /**
     * Works the part of the front the click landed on, if any part answers it: on the server it acts, on both sides
     * it says whether the click was the part's.
     *
     * @param at    where the click landed on the front, in sixty-fourths from its top left as seen
     * @param ahead where the same look crossed the plane an open tray stands out to, or null when it did not
     * @param held  what the hand holds, empty for a sneak-click, which only an empty hand makes
     * @return whether a part of the front took the click
     */
    static boolean click(final AbstractSmallComputerBlockEntity computer, final CaseFrontLayout layout,
                         final double[] at, @Nullable final double[] ahead, final ItemStack held,
                         final Player player) {
        final CaseFront front = computer.front();
        final int bays = Math.min(front.bays(), layout.bays().size());
        // The eject buttons answer whatever the hand holds, as a real one does.
        for (int bay = 0; bay < bays; bay++) {
            final BayDriveSpec.Kind kind = front.kind(bay);
            final EjectButton button = kind == null ? null : layout.ejectButton(bay, kind);
            if (button != null && button.pressedAt(at[0], at[1])) {
                pressEject(computer, bay, player);
                return true;
            }
        }
        final int ports = Math.min(front.ports(), layout.ports().size());
        for (int port = 0; port < ports; port++) {
            if (layout.ports().get(port).contains(at[0], at[1]) && port(computer, port, held, player)) {
                return true;
            }
        }
        for (int bay = 0; bay < bays; bay++) {
            final CaseFrontLayout.Area area = layout.bays().get(bay);
            final boolean onTray = front.tray(bay).isOpen() && ahead != null && area.contains(ahead[0], ahead[1]);
            if ((area.contains(at[0], at[1]) || onTray) && bay(computer, bay, held, player)) {
                return true;
            }
        }
        return false;
    }

    /* The eject button of the drive in a bay: its tray rides out or back in, unless the computer is reading its disc. */
    private static void pressEject(final AbstractSmallComputerBlockEntity computer, final int bay,
                                   final Player player) {
        final Level level = computer.getLevel();
        if (level == null || level.isClientSide()) {
            return;
        }
        if (computer.front().reading(bay)) {
            tell(player, MediaReaderBlock.READING_WAIT.text());
            return;
        }
        computer.front().tray(bay).press(level, computer.getBlockPos());
    }

    /* A port: a stick in the hand plugs in, a sneak-click pulls out the one plugged in. */
    private static boolean port(final AbstractSmallComputerBlockEntity computer, final int port,
                                final ItemStack held, final Player player) {
        final CaseFront front = computer.front();
        final int slot = CaseFront.portSlot(port);
        if (player.isShiftKeyDown()) {
            if (front.stick(port).isEmpty()) {
                return false;
            }
            takeOut(computer, slot, player);
            return true;
        }
        if (!front.accepts(slot, held)) {
            return false;
        }
        if (!front.stick(port).isEmpty()) {
            tell(player, PORT_TAKEN.text());
            return true;
        }
        insert(computer, slot, held);
        return true;
    }

    /* A bay: what the part in it does with what the hand holds, or with a sneak-click. */
    private static boolean bay(final AbstractSmallComputerBlockEntity computer, final int bay, final ItemStack held,
                               final Player player) {
        final BayDriveSpec.Kind kind = computer.front().kind(bay);
        if (kind == null) {
            return false;
        }
        return kind == BayDriveSpec.Kind.HOT_SWAP ? hotSwap(computer, bay, held, player)
                : drive(computer, bay, kind, held, player);
    }

    /* The hot-swap bay: a disk the computer takes slides in, a sneak-click takes the one in it out. */
    private static boolean hotSwap(final AbstractSmallComputerBlockEntity computer, final int bay,
                                   final ItemStack held, final Player player) {
        final CaseFront front = computer.front();
        if (player.isShiftKeyDown()) {
            if (front.held(bay).isEmpty()) {
                return false;
            }
            // What the system wrote is on the disk before it leaves, as when it is taken out on the assembly screen.
            if (isServer(computer)) {
                computer.setChanged();
            }
            takeOut(computer, bay, player);
            return true;
        }
        if (!(held.getItem() instanceof DiskItem)) {
            return false;
        }
        if (!front.held(bay).isEmpty()) {
            tell(player, BAY_TAKEN.text());
        } else if (!front.accepts(bay, held)) {
            tell(player, DISK_REFUSED.text());
        } else {
            insert(computer, bay, held);
        }
        return true;
    }

    /* An internal optical drive: a disc goes on its open tray and comes off it, as on an external drive. */
    private static boolean drive(final AbstractSmallComputerBlockEntity computer, final int bay,
                                 final BayDriveSpec.Kind kind, final ItemStack held, final Player player) {
        final CaseFront front = computer.front();
        final DiscTray tray = front.tray(bay);
        if (player.isShiftKeyDown()) {
            if (front.held(bay).isEmpty()) {
                return false;
            }
            if (!tray.isOpen()) {
                tell(player, DiscTray.CLOSED.text());
            } else {
                takeOut(computer, bay, player);
            }
            return true;
        }
        if (!(held.getItem() instanceof FormattedMediaItem disc) || !disc.format().onTray()) {
            return false;
        }
        // A refused disc says why: a silent click looks like a broken block.
        if (!front.accepts(bay, held)) {
            tell(player, MediaReaderBlock.CANNOT_READ.with(kind.reads().inSentence()));
        } else if (!tray.isOpen()) {
            tell(player, DiscTray.CLOSED.text());
        } else if (!front.held(bay).isEmpty()) {
            tell(player, MediaReaderBlock.ALREADY_HOLDS.text());
        } else {
            insert(computer, bay, held);
        }
        return true;
    }

    /* One of what the hand holds goes in, on the server, which tells the hand of it. */
    private static void insert(final AbstractSmallComputerBlockEntity computer, final int slot, final ItemStack held) {
        if (isServer(computer) && computer.front().insert(slot, held.copyWithCount(1)).isEmpty()) {
            held.shrink(1);
        }
    }

    /*
     * What a slot holds comes out, on the server, which tells the case of it: to the player, or falling by the case
     * when the player has no room.
     */
    private static void takeOut(final AbstractSmallComputerBlockEntity computer, final int slot, final Player player) {
        if (!isServer(computer)) {
            return;
        }
        final ItemStack out = computer.front().take(slot);
        if (out.isEmpty() || player.addItem(out)) {
            return;
        }
        final BlockPos at = computer.getBlockPos();
        Containers.dropItemStack(computer.getLevel(), at.getX() + 0.5, at.getY() + 1.0, at.getZ() + 0.5, out);
    }

    /* A line on the action bar, sent by the server. */
    private static void tell(final Player player, final Text text) {
        if (!player.level().isClientSide()) {
            player.displayClientMessage(GameText.component(text), true);
        }
    }

    private static boolean isServer(final AbstractSmallComputerBlockEntity computer) {
        return computer.getLevel() != null && !computer.getLevel().isClientSide();
    }
}
