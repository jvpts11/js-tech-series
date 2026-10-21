/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Example.
 */
package dev.jstech.example.screen;

import dev.jstech.core.menu.CoreMenu;
import dev.jstech.core.menu.MenuValue;
import dev.jstech.core.menu.SyncedValue;
import dev.jstech.example.ExampleContent;
import dev.jstech.example.ExampleState;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Inventory;

/**
 * The logbook's menu: no slots, two values by name that its screen's file shows, and one button.
 *
 * <p>Its screen is {@code assets/jsexample/jstech/screens/logbook.json}. The file says where each thing stands and
 * which value of this menu each label shows; this class says what the values are. A value is worked out on the server
 * and sent to the player's game while the screen is open; the energy is a synced value because it outgrows what the
 * game's own menu numbers carry.
 */
public final class LogbookMenu extends CoreMenu {

    private final SyncedValue<Long> made;
    private final MenuValue opened;

    /** The button that starts the player's count over, as the screen's file names it. */
    public static final int START_OVER = 0;

    public LogbookMenu(final int id, final Inventory inventory) {
        super(ExampleContent.LOGBOOK_MENU.get(), id, inventory, player -> true);
        this.made = synced("made", ByteBufCodecs.VAR_LONG, 0L, () -> {
            final MinecraftServer server = inventory.player.getServer();
            return server == null ? 0L : ExampleState.ENERGY_MADE.get(server);
        });
        this.opened = value("opened", () -> {
            final MinecraftServer server = inventory.player.getServer();
            return server == null ? 0 : ExampleState.LOGBOOK_OPENED.get(server, inventory.player.getUUID());
        });
        button(START_OVER, player -> {
            if (player instanceof ServerPlayer reader) {
                ExampleState.LOGBOOK_OPENED.set(reader.server, reader.getUUID(), 0);
            }
        });
    }

    /** The energy made on the server, as this copy of the menu has it. */
    public long made() {
        return made.get();
    }

    /** How many times its player opened the logbook, as this copy of the menu has it. */
    public int opened() {
        return opened.get();
    }
}
