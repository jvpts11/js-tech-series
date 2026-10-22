/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.client.gui.data;

import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Function;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.MenuType;
import net.neoforged.neoforge.client.event.RegisterMenuScreensEvent;

/**
 * How a mod hands the game its screens declared in files. A menu's screen is registered with the menu screens:
 *
 * <pre>{@code
 * modBus.addListener(RegisterMenuScreensEvent.class, event ->
 *         DataScreens.register(event, PRESS_MENU.get(), rl("press"), menu -> screen -> screen
 *                 .onAction("start", () -> START.send(true))));
 * }</pre>
 *
 * <p>and a screen with no menu is opened where it is wanted, {@code DataScreens.open(rl("probe"), title, logic)}. The
 * file is {@code assets/<namespace>/jstech/screens/<path>.json}; the namespace is the mod whose style choice the screen
 * follows.
 */
public final class DataScreens {

    /* The menu each file's screen was registered for, which the studio checks the file's slots against. */
    private static final Map<ResourceLocation, MenuType<?>> MENUS = new ConcurrentHashMap<>();

    private DataScreens() {
    }

    /**
     * Registers the screen of {@code type} as declared in {@code file}, its code made for each menu by {@code logic}.
     */
    public static <M extends AbstractContainerMenu> void register(final RegisterMenuScreensEvent event,
                                                                  final MenuType<M> type, final ResourceLocation file,
                                                                  final Function<M, IScreenLogic<M>> logic) {
        MENUS.put(file, type);
        event.<M, DataContainerScreen<M>>register(type, (menu, inventory, title) -> new DataContainerScreen<>(menu,
                inventory, title, file, logic.apply(menu)));
    }

    /** The menu the screen declared in {@code file} was registered for, if one was. */
    public static Optional<MenuType<?>> menuOf(final ResourceLocation file) {
        return Optional.ofNullable(MENUS.get(file));
    }

    /** Opens the screen declared in {@code file}, with no menu, over the screen open now. */
    public static void open(final ResourceLocation file, final Component title, final IScreenLogic<Void> logic) {
        final Minecraft minecraft = Minecraft.getInstance();
        minecraft.setScreen(new DataScreen(file, title, logic, minecraft.screen));
    }

    /**
     * What the screen declared in {@code file} builds, with no code and no menu behind it, to be drawn somewhere other
     * than a screen of its own: a preview of it.
     */
    public static DataScreenView preview(final ResourceLocation file, final Component title) {
        return new DataScreenView(file, new ScreenWiring<>(null), null, () -> title);
    }
}
