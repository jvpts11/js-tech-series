/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Space.
 */
package dev.jstech.space.hangar;

import dev.jstech.core.call.MenuCall;
import dev.jstech.core.call.MenuCalls;
import dev.jstech.core.text.GameText;
import dev.jstech.space.JsSpace;
import dev.jstech.space.SpaceContent;
import java.util.function.Predicate;
import net.minecraft.core.BlockPos;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.SimpleMenuProvider;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.level.Level;
import net.neoforged.neoforge.registries.DeferredHolder;

/**
 * The Rocket Design, a formed Rocket Hangar's design screen, opened at its terminal or at the Rocket Design table in
 * its control room: its menu, and the calls its screen makes, the design as it stands sent at each change, saved as
 * one of the hangar's plans, a plan deleted, and the design sent to the bay to be built.
 */
public final class RocketDesigner {

    /** Its screen's menu. */
    public static final DeferredHolder<MenuType<?>, MenuType<RocketDesignMenu>> MENU =
            SpaceContent.CONTENT.menu("rocket_design", RocketDesignMenu::new);
    /** The design as it stands on the screen, kept by the hangar. */
    public static final MenuCall<RocketDesignMenu, RocketDesign> DESIGN = MenuCalls.declare(id("rocket_design_set"),
            RocketDesignMenu.class, RocketDesign.STREAM_CODEC, (menu, player, design) -> menu.terminal()
                    .design(design));
    /** The design saved as one of the hangar's plans, over the plan of its name. */
    public static final MenuCall<RocketDesignMenu, Boolean> SAVE = MenuCalls.declare(id("rocket_design_save"),
            RocketDesignMenu.class, ByteBufCodecs.BOOL, (menu, player, any) -> menu.terminal().save());
    /** A plan deleted, by its place among the hangar's plans. */
    public static final MenuCall<RocketDesignMenu, Integer> DELETE = MenuCalls.declare(id("rocket_design_delete"),
            RocketDesignMenu.class, ByteBufCodecs.VAR_INT, (menu, player, place) -> menu.terminal().delete(place));
    /** The design sent to the bay to be built, when it is ready. */
    public static final MenuCall<RocketDesignMenu, Boolean> CONFIRM = MenuCalls.declare(id("rocket_design_confirm"),
            RocketDesignMenu.class, ByteBufCodecs.BOOL, (menu, player, any) -> menu.terminal().confirm());
    /* How far past a player's reach the block the screen was opened at may be, as a machine's screen allows. */
    private static final double REACH_MARGIN = 4.0;

    private RocketDesigner() {
    }

    /** Declares its menu and its screen's calls, once, from the mod's constructor. */
    public static void declare() {
        // Loading the class declares them.
    }

    /**
     * Opens the design screen of {@code terminal}'s hangar for {@code player}, used at {@code at}: the terminal, or
     * the table; it stays open while the terminal stands and the player can reach where they opened it.
     */
    public static void open(final ServerPlayer player, final HangarTerminalBlockEntity terminal, final BlockPos at) {
        final Level level = player.level();
        final Predicate<Player> valid = viewer -> !terminal.isRemoved() && viewer.level() == level
                && level.getBlockEntity(terminal.getBlockPos()) == terminal
                && viewer.canInteractWithBlock(at, REACH_MARGIN);
        player.openMenu(new SimpleMenuProvider((id, inventory, opener) -> new RocketDesignMenu(id, inventory,
                terminal, valid), GameText.component(HangarTexts.DESIGN)),
                buf -> buf.writeBlockPos(terminal.getBlockPos()));
    }

    private static ResourceLocation id(final String path) {
        return ResourceLocation.fromNamespaceAndPath(JsSpace.MODID, path);
    }
}
