/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Computers.
 */
package dev.jstech.computers.item;

import dev.jstech.computers.blockentity.PartRefusals;
import dev.jstech.computers.registry.ComputingComponents;
import dev.jstech.computers.hardware.FormFactor;
import dev.jstech.computers.hardware.MotherboardSpec;
import dev.jstech.computers.rack.RackChassis;
import dev.jstech.core.text.TextKey;
import java.util.Set;
import net.minecraft.core.NonNullList;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.ItemContainerContents;
import net.neoforged.neoforge.items.IItemHandlerModifiable;
import org.jetbrains.annotations.Nullable;

/**
 * An item-handler view over the hardware of the Server item held in a player's hand, used by the Server assembly GUI.
 */
public final class ServerHardwareHandler implements IItemHandlerModifiable {

    /*
     * Since the racks rework a server carries NO drives: its disks live in the rack's front-panel
     * hotswap slots (the chassis decides how many it cables). The old disk slots (20-25) are gone;
     * anything a legacy stack still holds there is simply ignored (dev-only mod, no migration).
     */
    public static final int MOBO = 0;
    public static final int CPU_START = 1;
    public static final int CPU = 4;
    public static final int RAM_START = 5;
    public static final int RAM = 8;
    public static final int GPU_START = 13;
    public static final int GPU = 6;
    public static final int PSU = 19;
    public static final int SLOTS = 20;

    private final Player player;
    private final InteractionHand hand;

    public ServerHardwareHandler(final Player player, final InteractionHand hand) {
        this.player = player;
        this.hand = hand;
    }

    private ItemStack held() {
        return player.getItemInHand(hand);
    }

    private boolean writable() {
        return held().getItem() instanceof ServerItem;
    }

    private ItemContainerContents contents() {
        return ServerItem.hardware(held());
    }

    private void write(final NonNullList<ItemStack> items) {
        if (!writable()) {
            return; // the Server left the hand (picked up); ignore the write
        }
        held().set(ComputingComponents.SERVER_HARDWARE.get(), ItemContainerContents.fromItems(items));
    }

    private NonNullList<ItemStack> snapshot() {
        final NonNullList<ItemStack> items = NonNullList.withSize(SLOTS, ItemStack.EMPTY);
        final ItemContainerContents current = contents();
        for (int i = 0; i < SLOTS && i < current.getSlots(); i++) {
            items.set(i, current.getStackInSlot(i).copy());
        }
        return items;
    }

    @Override
    public int getSlots() {
        return SLOTS;
    }

    @Override
    public ItemStack getStackInSlot(final int slot) {
        if (slot < 0) {
            return ItemStack.EMPTY;
        }
        final ItemContainerContents current = contents();
        return slot < current.getSlots() ? current.getStackInSlot(slot) : ItemStack.EMPTY;
    }

    @Override
    public void setStackInSlot(final int slot, final ItemStack stack) {
        if (slot < 0 || slot >= SLOTS) {
            return; // out-of-range write; ignore rather than throw on the backing list
        }
        final NonNullList<ItemStack> items = snapshot();
        items.set(slot, stack.copyWithCount(Math.min(stack.getCount(), 1)));
        write(items);
    }

    @Override
    public int getSlotLimit(final int slot) {
        return 1; // one component per slot
    }

    /** Whether a board belongs to the era of the case it is going into (no case known: anything goes). */
    private boolean boardMatchesCaseEra(final ItemStack board) {
        final RackChassis chassis = ServerItem.chassisOf(held());
        return chassis == null || !(board.getItem() instanceof MotherboardItem item)
                || item.spec().era() == chassis.era();
    }

    @Override
    public boolean isItemValid(final int slot, final ItemStack stack) {
        if (slot == MOBO) {
            /*
             * A Server takes a server-class board: EEB across the eras, or the EATX workstation board
             * where the era offers one. It does not accept consumer or Mainframe-only boards, and the
             * case takes only a board of its own era, like every other computer's chassis.
             */
            return MotherboardItem.fits(stack,
                    Set.of(FormFactor.EEB,
                            FormFactor.EATX))
                    && boardMatchesCaseEra(stack);
        }
        if (slot == PSU) {
            return stack.getItem() instanceof PsuItem;
        }
        /*
         * A processor, memory or a card takes the board's socket, memory and bus, as in every other computer: a part
         * that cannot go in the board stays out of its slot rather than going in only to keep the server from
         * starting. With no board yet, anything of the kind is staged.
         */
        final MotherboardSpec board = getStackInSlot(MOBO).getItem() instanceof MotherboardItem seated
                ? seated.spec() : null;
        if (slot >= CPU_START && slot < CPU_START + CPU) {
            return stack.getItem() instanceof CpuItem cpu && (board == null
                    || cpu.spec().socket().equals(board.socket()) && cpu.spec().era() == board.era());
        }
        if (slot >= RAM_START && slot < RAM_START + RAM) {
            return stack.getItem() instanceof RamItem ram && (board == null
                    || board.acceptedRam().contains(ram.spec().generation()) && ram.spec().era() == board.era());
        }
        if (slot >= GPU_START && slot < GPU_START + GPU) {
            /*
             * The expansion slots take a GPU or a storage card in any server; the fibre comes into a rack through
             * a Fibre Optic Interface, never a server's network card. The crafting co-processor is the one card
             * reserved to a chassis: it goes only in a Supercomputer Node, whose single expansion slot
             * exists for it, and accepting GPUs alone here left that slot unable to seat anything.
             */
            if (stack.getItem() instanceof PhiCoprocessorItem) {
                final RackChassis chassis = ServerItem.chassisOf(held());
                return chassis != null && chassis.isSupercomputerNode() && !holdsCoprocessorOutside(slot);
            }
            return isServerCard(stack.getItem()) && stack.getItem() instanceof IExpansionCardItem card
                    && (board == null || board.takes(card.cardSpec()));
        }
        return false;
    }

    /**
     * Why {@code stack} stays out of {@code slot}, for the player holding it there: the rule of {@link #isItemValid}
     * it breaks, read in the order that method reads them; null when it goes in.
     */
    @Nullable
    public TextKey refusal(final int slot, final ItemStack stack) {
        if (stack.isEmpty() || isItemValid(slot, stack)) {
            return null;
        }
        final Item item = stack.getItem();
        if (slot == MOBO) {
            return item instanceof MotherboardItem ? PartRefusals.BOARD : PartRefusals.NOT_THIS_SLOT;
        }
        if (slot >= CPU_START && slot < CPU_START + CPU) {
            return item instanceof CpuItem ? PartRefusals.CPU : PartRefusals.NOT_THIS_SLOT;
        }
        if (slot >= RAM_START && slot < RAM_START + RAM) {
            return item instanceof RamItem ? PartRefusals.RAM : PartRefusals.NOT_THIS_SLOT;
        }
        if (slot >= GPU_START && slot < GPU_START + GPU) {
            if (item instanceof PhiCoprocessorItem) {
                final RackChassis chassis = ServerItem.chassisOf(held());
                return chassis != null && chassis.isSupercomputerNode() ? PartRefusals.ONE_PHI : PartRefusals.PHI;
            }
            return isServerCard(item) ? PartRefusals.CARD_BUS : PartRefusals.SERVER_CARD;
        }
        return PartRefusals.NOT_THIS_SLOT;
    }

    /* The kinds of card a server's expansion slots take, a bus that fits the board aside. */
    private static boolean isServerCard(final Item item) {
        return item instanceof GpuItem || item instanceof StorageCardItem;
    }

    /**
     * Whether another expansion slot already carries a co-processor. A node seats exactly one: the
     * cluster counts nodes, not cards, and a second card would sit there contributing nothing.
     */
    private boolean holdsCoprocessorOutside(final int slot) {
        final ItemContainerContents current = contents();
        final int end = Math.min(current.getSlots(), GPU_START + GPU);
        for (int i = GPU_START; i < end; i++) {
            if (i != slot && current.getStackInSlot(i).getItem() instanceof PhiCoprocessorItem) {
                return true;
            }
        }
        return false;
    }

    @Override
    public ItemStack insertItem(final int slot, final ItemStack stack, final boolean simulate) {
        if (stack.isEmpty() || !isItemValid(slot, stack) || !getStackInSlot(slot).isEmpty()) {
            return stack;
        }
        if (!simulate) {
            setStackInSlot(slot, stack.copyWithCount(1));
        }
        return stack.getCount() > 1 ? stack.copyWithCount(stack.getCount() - 1) : ItemStack.EMPTY;
    }

    @Override
    public ItemStack extractItem(final int slot, final int amount, final boolean simulate) {
        final ItemStack existing = getStackInSlot(slot);
        if (existing.isEmpty() || amount <= 0) {
            return ItemStack.EMPTY;
        }
        if (!simulate) {
            setStackInSlot(slot, ItemStack.EMPTY);
        }
        return existing.copyWithCount(1);
    }
}
