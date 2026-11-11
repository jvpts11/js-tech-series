/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Computers.
 */
package dev.jstech.computers.menu;

import dev.jstech.computers.blockentity.MiningComputerBlockEntity;
import dev.jstech.computers.gui.layout.MiningComputerLayout;
import dev.jstech.computers.registry.ComputingMenus;
import dev.jstech.core.gui.layout.GuiLayout;
import dev.jstech.core.menu.MenuOpening;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;
import org.jetbrains.annotations.Nullable;

/**
 * Menu for the Mining Computer (computers/docs/design/teracoin.md): its board, processor, memory, supply and disk, as
 * every computer's assembly is, and its risers, one for each of the board's x1 slots; its cards are seated in the
 * world, so none of them is a slot here. A riser a card stands on stays until its card is taken off.
 */
public class MiningComputerMenu extends AbstractAssemblyComputerMenu {

    private final MiningComputerBlockEntity rig;

    /** The MINE AT START switch. */
    public static final int BUTTON_MINE_AT_START = 5;

    public MiningComputerMenu(final int containerId, final Inventory playerInventory,
                              final MiningComputerBlockEntity rig) {
        super(ComputingMenus.MINING_COMPUTER_MENU.get(), containerId, playerInventory, rig,
                MiningComputerLayout.geometry(rig.rigEra()), "gpu", (into, layout, onBoard) -> {
                    for (int i = 0; i < rig.riserCount() && layout.hasSlot("riser_" + i); i++) {
                        final GuiLayout.SlotPosition at = layout.slotAt("riser_" + i);
                        into.add(new RiserSlot(rig.risers(), i, at.x(), at.y(), onBoard,
                                index -> rig.cardPart(index) != null));
                    }
                });
        this.rig = rig;
        button(BUTTON_MINE_AT_START, p -> rig.toggleMineAtStart());
    }

    public static MiningComputerMenu fromNetwork(final int containerId, final Inventory playerInventory,
                                                 final RegistryFriendlyByteBuf buf) {
        return new MiningComputerMenu(containerId, playerInventory,
                MenuOpening.blockEntity(playerInventory, buf, MiningComputerBlockEntity.class));
    }

    /** The rig the menu assembles, for its screen. */
    public MiningComputerBlockEntity rig() {
        return rig;
    }

    /** How many risers the rig's frame carries. */
    public int riserCount() {
        return rig.riserCount();
    }

    /** The card on riser {@code index}, as the rig's part list tells every player who sees it; null if none. */
    @Nullable
    public ResourceLocation cardOn(final int index) {
        return rig.cardPart(index);
    }

    /** How many cards stand on the risers. */
    public int seatedCards() {
        return rig.seatedCards();
    }

    /** Whether riser {@code index} is in. */
    public boolean hasRiser(final int index) {
        return rig.hasRiser(index);
    }

    public long hashrate() {
        return rig.assemblyHashrate();
    }

    public boolean mining() {
        return rig.assemblyMining();
    }

    public boolean mineAtStart() {
        return rig.mineAtStart();
    }
}
