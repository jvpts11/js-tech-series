/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Space.
 */
package dev.jstech.space.crew;

import com.mojang.authlib.GameProfile;
import dev.jstech.core.id.StableCodecs;
import dev.jstech.core.menu.CoreMenu;
import dev.jstech.core.menu.MenuOpening;
import dev.jstech.core.menu.MenuValidity;
import dev.jstech.core.text.GameText;
import dev.jstech.space.ship.AirReserveEntity;
import java.util.Optional;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Inventory;

/**
 * The ship console's menu: no slots, since a console holds nothing here; its screen reads the console's crew, which is
 * sent to the players who see it, sets a role through {@link ShipConsole#SET} and names a player through
 * {@link ShipConsole#ADD}, both the owner's alone to do.
 */
public final class ShipConsoleMenu extends CoreMenu {

    private final AirReserveEntity console;
    private final CrewRole viewer;
    private final boolean setsRoles;
    private final String teamName;

    public ShipConsoleMenu(final int containerId, final Inventory inventory, final AirReserveEntity console,
                           final CrewRole viewer, final boolean setsRoles, final String teamName) {
        super(ShipConsole.MENU.get(), containerId, inventory, MenuValidity.blockEntity(console));
        this.console = console;
        this.viewer = viewer;
        this.setsRoles = setsRoles;
        this.teamName = teamName;
    }

    /**
     * The menu on the player's side, for the console whose place the server wrote, their role aboard, whether they
     * set the roles, and the name of the owner's team.
     */
    public ShipConsoleMenu(final int containerId, final Inventory inventory, final RegistryFriendlyByteBuf buf) {
        this(containerId, inventory, MenuOpening.blockEntity(inventory, buf, AirReserveEntity.class),
                StableCodecs.byId(CrewRole.class, CrewRole.NONE).decode(buf), buf.readBoolean(),
                buf.readUtf(ShipConsole.LONGEST_TEAM));
    }

    /** The console. */
    public AirReserveEntity console() {
        return this.console;
    }

    /** The role aboard of the player who opened it, as the server found it. */
    public CrewRole viewer() {
        return this.viewer;
    }

    /** Whether the player who opened it sets the roles: the owner, and whoever passes the locks. */
    public boolean setsRoles() {
        return this.setsRoles;
    }

    /** The name the owner's team is shown by; empty for an owner on a team of their own. */
    public String teamName() {
        return this.teamName;
    }

    /** {@code player} sets a role on the crew page: the owner's to do, the others told so. */
    public void set(final ServerPlayer player, final ShipConsole.RoleSet set) {
        final Optional<ShipCrew> crew = owned(player);
        if (crew.isEmpty()) {
            return;
        }
        final ShipCrew next = switch (set.who()) {
            case ShipConsole.RoleSet.TEAM -> crew.get().withTeam(set.role());
            case ShipConsole.RoleSet.OTHERS -> crew.get().withOthers(set.role());
            default -> crew.get().named().stream().filter(member -> member.id().equals(set.player())).findFirst()
                    .map(member -> crew.get().withNamed(member.id(), member.name(), set.role())).orElse(crew.get());
        };
        this.console.crew(next);
    }

    /**
     * {@code player} names a player by {@code name} on the crew page, aboard as a passenger: one on the server now, or
     * one who has played on it before; the owner's to do.
     */
    public void add(final ServerPlayer player, final String name) {
        final Optional<ShipCrew> crew = owned(player);
        if (crew.isEmpty() || name.isBlank()) {
            return;
        }
        final ServerPlayer online = player.server.getPlayerList().getPlayerByName(name.trim());
        Optional<GameProfile> found = online == null ? Optional.empty() : Optional.of(online.getGameProfile());
        if (found.isEmpty() && player.server.getProfileCache() != null) {
            found = player.server.getProfileCache().get(name.trim());
        }
        if (found.isEmpty()) {
            player.displayClientMessage(GameText.component(CrewTexts.UNKNOWN_PLAYER), true);
            return;
        }
        this.console.crew(crew.get().withNamed(found.get().getId(), found.get().getName(), CrewRole.PASSENGER));
    }

    /* The console's crew, when {@code player} may set its roles; else they are told whose it is to do. */
    private Optional<ShipCrew> owned(final ServerPlayer player) {
        final Optional<ShipCrew> crew = this.console.crew();
        if (crew.isPresent() && !crew.get().maySetRoles(player)) {
            player.displayClientMessage(GameText.component(CrewTexts.OWNER_ONLY), true);
            return Optional.empty();
        }
        return crew;
    }
}
