/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Space.
 */
package dev.jstech.space.shipyard;

import dev.jstech.core.call.MenuCall;
import dev.jstech.core.call.MenuCalls;
import dev.jstech.core.text.GameText;
import dev.jstech.core.text.TextHolder;
import dev.jstech.core.text.TextKey;
import dev.jstech.space.JsSpace;
import dev.jstech.space.SpaceContent;
import dev.jstech.space.ship.ShipDesign;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.SimpleMenuProvider;
import net.minecraft.world.inventory.MenuType;
import net.neoforged.neoforge.registries.DeferredHolder;

/**
 * The Starship Design, a formed Small Shipyard's builder screen, opened at its terminal: its menu, and the calls its
 * screen makes, the design as it stands sent at each change, saved as one of the shipyard's plans, and sent to the dock
 * to be built.
 */
@TextHolder
public final class ShipDesigner {

    /** The screen's name. */
    public static final TextKey DESIGN = TextKey.of("jsspace.shipyard.design", "Starship Design");
    /** Its screen's menu. */
    public static final DeferredHolder<MenuType<?>, MenuType<ShipDesignMenu>> MENU =
            SpaceContent.CONTENT.menu("ship_design", ShipDesignMenu::new);
    /** The design as it stands on the screen, kept by the shipyard. */
    public static final MenuCall<ShipDesignMenu, ShipDesign> SET = MenuCalls.declare(id("ship_design_set"),
            ShipDesignMenu.class, ShipDesign.STREAM_CODEC, (menu, player, design) -> menu.terminal().design(design));
    /** The design saved as one of the shipyard's plans, over the plan of its name. */
    public static final MenuCall<ShipDesignMenu, Boolean> SAVE = MenuCalls.declare(id("ship_design_save"),
            ShipDesignMenu.class, ByteBufCodecs.BOOL, (menu, player, any) -> menu.terminal().save());
    /** The design sent to the dock to be built, when it is ready. */
    public static final MenuCall<ShipDesignMenu, Boolean> CONFIRM = MenuCalls.declare(id("ship_design_confirm"),
            ShipDesignMenu.class, ByteBufCodecs.BOOL, (menu, player, any) -> menu.terminal().confirm());

    private ShipDesigner() {
    }

    /** Declares its menu and its screen's calls, once, from the mod's constructor. */
    public static void declare() {
        // Loading the class declares them.
    }

    /** Opens the builder screen of {@code terminal}'s shipyard for {@code player}. */
    public static void open(final ServerPlayer player, final ShipyardTerminalBlockEntity terminal) {
        player.openMenu(new SimpleMenuProvider((id, inventory, opener) -> new ShipDesignMenu(id, inventory,
                terminal), GameText.component(DESIGN)), buf -> buf.writeBlockPos(terminal.getBlockPos()));
    }

    private static ResourceLocation id(final String path) {
        return ResourceLocation.fromNamespaceAndPath(JsSpace.MODID, path);
    }
}
