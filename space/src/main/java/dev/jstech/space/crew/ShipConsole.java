/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Space.
 */
package dev.jstech.space.crew;

import dev.jstech.core.call.MenuCall;
import dev.jstech.core.call.MenuCalls;
import dev.jstech.core.id.StableCodecs;
import dev.jstech.core.team.CoreTeams;
import dev.jstech.core.text.GameText;
import dev.jstech.space.JsSpace;
import dev.jstech.space.SpaceContent;
import dev.jstech.space.ship.AirReserveEntity;
import io.netty.buffer.ByteBuf;
import java.util.UUID;
import net.minecraft.core.UUIDUtil;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.SimpleMenuProvider;
import net.minecraft.world.inventory.MenuType;
import net.neoforged.neoforge.registries.DeferredHolder;

/**
 * A small ship's flight console's screen, the display of the ship's console in the style of the flight computer's:
 * its crew page, where the ship's owner says who flies it, who works aboard and who only rides, and everyone else
 * aboard sees who is what. Opened with an empty hand on the console, aboard the ship.
 */
public final class ShipConsole {

    /** The longest name of a team the page shows. */
    public static final int LONGEST_TEAM = 32;
    /** Its screen's menu. */
    public static final DeferredHolder<MenuType<?>, MenuType<ShipConsoleMenu>> MENU =
            SpaceContent.CONTENT.menu("ship_console", ShipConsoleMenu::new);
    /** A role set on the crew page: the owner's team's, everyone else's, or a player named's. */
    public static final MenuCall<ShipConsoleMenu, RoleSet> SET = MenuCalls.declare(id("ship_console_set"),
            ShipConsoleMenu.class, RoleSet.STREAM_CODEC, (menu, player, set) -> {
                if (player instanceof ServerPlayer setting) {
                    menu.set(setting, set);
                }
            });
    /** A player named on the crew page, by the name typed, aboard as a passenger. */
    public static final MenuCall<ShipConsoleMenu, String> ADD = MenuCalls.declare(id("ship_console_add"),
            ShipConsoleMenu.class, ByteBufCodecs.stringUtf8(ShipCrew.LONGEST_NAME), (menu, player, name) -> {
                if (player instanceof ServerPlayer adding) {
                    menu.add(adding, name);
                }
            });

    private ShipConsole() {
    }

    /** Declares its menu and its screen's calls, once, from the mod's constructor. */
    public static void declare() {
        // Loading the class declares them.
    }

    /**
     * Opens the screen of {@code console} for {@code player}, who is told their own role aboard, whether they set the
     * roles, and the name of the owner's team.
     */
    public static void open(final ServerPlayer player, final AirReserveEntity console, final CrewRole role) {
        final boolean setsRoles = console.crew().map(crew -> crew.maySetRoles(player)).orElse(false);
        final String team = console.crew().map(crew -> teamName(player.server, crew)).orElse("");
        player.openMenu(new SimpleMenuProvider((id, inventory, opener) -> new ShipConsoleMenu(id, inventory, console,
                role, setsRoles, team), GameText.component(CrewTexts.TITLE)), buf -> {
                    buf.writeBlockPos(console.getBlockPos());
                    StableCodecs.byId(CrewRole.class, CrewRole.NONE).encode(buf, role);
                    buf.writeBoolean(setsRoles);
                    buf.writeUtf(team, LONGEST_TEAM);
                });
    }

    /**
     * The name the owner's team of {@code crew} is shown by, cut to {@link #LONGEST_TEAM}; empty for an owner on a team
     * of their own, the page then naming the team as the owner's.
     */
    public static String teamName(final MinecraftServer server, final ShipCrew crew) {
        final String name = CoreTeams.teamName(server, CoreTeams.teamOf(server, crew.owner())).orElse("");
        return name.length() > LONGEST_TEAM ? name.substring(0, LONGEST_TEAM) : name;
    }

    private static ResourceLocation id(final String path) {
        return ResourceLocation.fromNamespaceAndPath(JsSpace.MODID, path);
    }

    /**
     * A role set on the crew page.
     *
     * @param who    whose: {@link #TEAM}, {@link #OTHERS} or {@link #NAMED}
     * @param player the player named, for {@link #NAMED}
     * @param role   the role
     */
    public record RoleSet(int who, UUID player, CrewRole role) {

        /** The owner's team. */
        public static final int TEAM = 0;
        /** Everyone else. */
        public static final int OTHERS = 1;
        /** A player named. */
        public static final int NAMED = 2;

        public static final StreamCodec<ByteBuf, RoleSet> STREAM_CODEC = StreamCodec.composite(
                ByteBufCodecs.VAR_INT, RoleSet::who,
                UUIDUtil.STREAM_CODEC, RoleSet::player,
                StableCodecs.byId(CrewRole.class, CrewRole.NONE), RoleSet::role,
                RoleSet::new);
    }
}
