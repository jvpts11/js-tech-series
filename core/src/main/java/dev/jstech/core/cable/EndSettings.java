/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.cable;

import dev.jstech.core.network.payload.ClientPayloads;
import dev.jstech.core.registry.CoreItems;
import dev.jstech.core.text.GameText;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.SimpleMenuProvider;
import net.minecraft.world.inventory.MenuType;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.registries.DeferredHolder;

/**
 * The screen of one end of a line, where everything an end has is set: its mode, priority, rate, channel and filter.
 * Any wrench opens it on an end of a line that routes what it carries ({@link #open}); the Basic Wrench does when its
 * player sneaks.
 */
public final class EndSettings {

    /** The menu of an end's screen. */
    public static final DeferredHolder<MenuType<?>, MenuType<CableEndMenu>> MENU = CoreItems.CONTENT.menu(
            "cable_end", CableEndMenu::new);

    private static final String NETWORK_VERSION = "1";

    private EndSettings() {
    }

    /** Declares the menu, from the Core's constructor, while the Core's content is declared. */
    public static void declare() {
        // Loading the class declares it.
    }

    /** Hands the mod's bus what this answers, once, from the Core's constructor. */
    public static void listen(final IEventBus modBus) {
        modBus.addListener(RegisterPayloadHandlersEvent.class, event -> ClientPayloads.onMenu(
                event.registrar(NETWORK_VERSION), EndNumberPayload.TYPE, EndNumberPayload.STREAM_CODEC,
                CableEndMenu.class, CableEndMenu::pos, EndNumberPayload::pos,
                (payload, menu, player, level) -> menu.typed(payload.rate(), payload.value())));
    }

    /** Whether a wire's ends have a screen: those of a line that routes what it carries, item by item or by the mB. */
    public static boolean hasScreen(final Wire wire) {
        return wire.type().grid().policy().routesEnds();
    }

    /** Opens the screen of the end of {@code wire} that faces {@code face}, in {@code cable}, for {@code player}. */
    public static void open(final ServerPlayer player, final CableBlockEntity cable, final Wire wire,
                            final Direction face) {
        final Component title = GameText.component(EndTexts.TITLE.with(GameText.of(wire.type().item()
                .getDescription()), EndTexts.faceInline(face).text()));
        player.openMenu(new SimpleMenuProvider((id, inventory, opener) -> new CableEndMenu(id, inventory, cable,
                wire.slot(), face), title), buf -> {
                    buf.writeBlockPos(cable.getBlockPos());
                    buf.writeUtf(wire.slot().serializedName());
                    buf.writeVarInt(face.get3DDataValue());
                });
    }
}
