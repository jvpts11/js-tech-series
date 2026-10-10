/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Computers.
 */
package dev.jstech.computers.menu;

import dev.jstech.computers.block.RedstoneInterfaceBlock;
import dev.jstech.computers.blockentity.RedstoneInterfaceBlockEntity;
import dev.jstech.computers.gui.layout.RedstoneInterfaceLayout;
import dev.jstech.computers.registry.ComputingMenus;
import dev.jstech.core.menu.CoreMenu;
import dev.jstech.core.menu.MenuValidity;
import dev.jstech.core.tier.HardwareEra;
import net.minecraft.core.BlockPos;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;

/**
 * A Redstone Interface's screen: its name, the computer it hangs from, what it reads or emits, its mode and the
 * strength it emits. The name and the computer come with the screen; whether a name asked for clashes is the
 * interface's own field, carried while the screen is open, and the rest is the client's copy of the same block entity.
 *
 * <p>The mode and the strength are the menu's buttons: {@link #BUTTON_IN}, {@link #BUTTON_OUT}, and
 * {@link #BUTTON_STRENGTH} plus the strength. A setting made here is a player's, so it clears the mark a program left.
 */
public class RedstoneInterfaceMenu extends CoreMenu {

    private final RedstoneInterfaceBlockEntity sensor;
    private final Opening opening;

    /** Makes it read. */
    public static final int BUTTON_IN = 0;
    /** Makes it emit, at the strength it was last set to. */
    public static final int BUTTON_OUT = 1;
    /** Sets the strength it emits: this plus the strength, 0 to 15; only while it is live and emitting. */
    public static final int BUTTON_STRENGTH = 2;

    /** The server's menu, reading the interface as it is; the client's, once it resolved its own block entity too. */
    public RedstoneInterfaceMenu(final int containerId, final Inventory playerInventory,
                                 final RedstoneInterfaceBlockEntity sensor, final Opening opening) {
        super(ComputingMenus.REDSTONE_INTERFACE_MENU.get(), containerId, playerInventory,
                MenuValidity.block(sensor.getLevel(), sensor.getBlockPos(), RedstoneInterfaceBlock.class));
        this.sensor = sensor;
        this.opening = opening;
        data(sensor.fields().menuData());
        button(BUTTON_IN, player -> sensor.read(""));
        button(BUTTON_OUT, player -> sensor.emit(sensor.strength(), ""));
        for (int strength = 0; strength < RedstoneInterfaceLayout.CELLS; strength++) {
            final int chosen = strength;
            button(BUTTON_STRENGTH + strength, player -> {
                if (sensor.live() && sensor.emits()) {
                    sensor.emit(chosen, "");
                }
            });
        }
    }

    /** The client's menu, on the client's own copy of the interface standing at the opening's position. */
    public static RedstoneInterfaceMenu fromNetwork(final int containerId, final Inventory playerInventory,
                                                    final RegistryFriendlyByteBuf buf) {
        final Opening opening = Opening.read(buf);
        if (!(playerInventory.player.level().getBlockEntity(opening.pos())
                instanceof RedstoneInterfaceBlockEntity sensor)) {
            throw new IllegalStateException("a menu was opened on a Redstone Interface at " + opening.pos()
                    + " the client does not have");
        }
        return new RedstoneInterfaceMenu(containerId, playerInventory, sensor, opening);
    }

    /** The screen closed: the name typed on it is taken, unless another interface of its computer has it. */
    @Override
    public void removed(final Player player) {
        super.removed(player);
        sensor.takeAskedName();
    }

    public BlockPos sensorPos() {
        return opening.pos();
    }

    /** What its screen opened with. */
    public Opening opening() {
        return opening;
    }

    /** The interface as the side this menu lives on sees it. */
    public RedstoneInterfaceBlockEntity sensor() {
        return sensor;
    }

    /** Whether the name last typed is one another interface of its computer already has. */
    public boolean nameClashes() {
        return sensor.nameClashes();
    }

    /**
     * What a Redstone Interface's screen opens with.
     *
     * @param pos          where the interface stands
     * @param name         the name a player gave it, empty for none
     * @param computerName the name a player gave its computer, empty for none
     * @param computerKind the translation key of its computer's block, empty when it is not linked to one
     * @param era          the era of its housing, whose skin the screen wears
     */
    public record Opening(BlockPos pos, String name, String computerName, String computerKind, HardwareEra era) {

        public void write(final RegistryFriendlyByteBuf buf) {
            new NamedDeviceOpening(pos, name, computerName, computerKind, era)
                    .write(buf, RedstoneInterfaceBlockEntity.MAX_NAME);
        }

        public static Opening read(final RegistryFriendlyByteBuf buf) {
            final NamedDeviceOpening read = NamedDeviceOpening.read(buf, RedstoneInterfaceBlockEntity.MAX_NAME);
            return new Opening(read.pos(), read.name(), read.computerName(), read.computerKind(), read.era());
        }
    }
}
