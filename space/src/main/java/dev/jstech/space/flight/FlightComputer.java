/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Space.
 */
package dev.jstech.space.flight;

import dev.jstech.core.call.MenuCall;
import dev.jstech.core.call.MenuCalls;
import dev.jstech.space.JsSpace;
import dev.jstech.space.SpaceContent;
import dev.jstech.space.rocket.RocketKitBlocks;
import net.minecraft.core.BlockPos;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.neoforged.neoforge.registries.DeferredHolder;

/**
 * The Flight Computer, the block of a capsule and of a lander's cabin that every rocket carries: it plans the transfer
 * to a world, shows the system with its worlds going round on its transfer page, and on its landing page the map of
 * the world it goes to, drawn as the game's own map item draws a world, with the spot marked to land on and the ellipse
 * the rocket will come down in. Its screen is the approved flat display, its keys round the screen.
 */
public final class FlightComputer {

    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<FlightComputerBlockEntity>> ENTITY =
            SpaceContent.CONTENT.blockEntity("flight_computer", FlightComputerBlockEntity::new,
                    RocketKitBlocks.FLIGHT_COMPUTER);
    /** Its screen's menu. */
    public static final DeferredHolder<MenuType<?>, MenuType<FlightComputerMenu>> MENU =
            SpaceContent.CONTENT.menu("flight_computer", FlightComputerMenu::new);
    /** A key of its screen pressed, by its number ({@link FlightComputerBlockEntity#press}). */
    public static final MenuCall<FlightComputerMenu, Integer> PRESS = MenuCalls.declare(id("flight_computer_press"),
            FlightComputerMenu.class, ByteBufCodecs.VAR_INT, (menu, player, key) -> menu.computer().press(key));
    /** A spot of its landing map clicked, marked to land on. */
    public static final MenuCall<FlightComputerMenu, BlockPos> MARK = MenuCalls.declare(id("flight_computer_mark"),
            FlightComputerMenu.class, BlockPos.STREAM_CODEC, (menu, player, at) -> menu.computer().markAt(at.getX(),
                    at.getZ()));
    /** Its landing map asked for, drawn as it stands now and sent to the player who asked. */
    public static final MenuCall<FlightComputerMenu, Boolean> MAP = MenuCalls.declare(id("flight_computer_map"),
            FlightComputerMenu.class, ByteBufCodecs.BOOL, (menu, player, any) -> {
                if (player instanceof ServerPlayer asking) {
                    LandingMaps.send(asking, menu.computer());
                }
            });

    private FlightComputer() {
    }

    /** Declares its block entity, its menu and its screen's calls, once, from the mod's constructor. */
    public static void declare() {
        // Loading the class declares them.
    }

    private static ResourceLocation id(final String path) {
        return ResourceLocation.fromNamespaceAndPath(JsSpace.MODID, path);
    }
}
