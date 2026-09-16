/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Computers.
 */
package dev.jstech.computers.config;

import dev.jstech.computers.JsComputers;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.config.ModConfig;
import net.neoforged.fml.event.config.ModConfigEvent;
import net.neoforged.neoforge.common.ModConfigSpec;

/**
 * The settings of the computers themselves, {@code jscomputers-server.toml} next to the world save.
 *
 * <p>Apart from the series' balance file, which the Core owns: what is in here is about how the machines of this
 * mod behave, and the Core has no business knowing that a boot menu exists.
 *
 * <p>Every value is copied into a plain field as the file is read, so the things that ask for them, which are
 * machines in the middle of starting, read a field rather than going through the config library each time.
 */
public final class ComputersServerConfig {

    public static final String FILE_NAME = "jscomputers-server.toml";

    public static final ModConfigSpec SPEC;

    private static final ModConfigSpec.BooleanValue SHOW_BOOT_MENU_VALUE;

    /** Held apart from the file so a machine can ask while the world is still coming up. */
    private static boolean showBootMenu = true;

    static {
        final ModConfigSpec.Builder builder = new ModConfigSpec.Builder();
        builder.comment("How the computers of J's Computers behave. Balance of the Operations engine lives in the "
                + "series' own file beside this one.");
        builder.push("boot");
        SHOW_BOOT_MENU_VALUE = builder
                .comment("Whether a machine with a Linux system shows its boot menu every time it starts.",
                        "Turning this off boots the chosen system at once, the way a machine with the menu hidden "
                                + "does.")
                .define("show_boot_menu", true);
        builder.pop();
        SPEC = builder.build();
    }

    private ComputersServerConfig() {
    }

    /** Puts the file beside the world save and keeps the fields in step with it. */
    public static void register(final IEventBus modEventBus, final ModContainer modContainer) {
        modContainer.registerConfig(ModConfig.Type.SERVER, SPEC, FILE_NAME);
        modEventBus.addListener(ComputersServerConfig::onLoad);
        modEventBus.addListener(ComputersServerConfig::onReload);
    }

    /** Whether a machine with a Linux system stops at its boot menu on the way up. */
    public static boolean showBootMenu() {
        return showBootMenu;
    }

    private static void onLoad(final ModConfigEvent.Loading event) {
        apply(event.getConfig());
    }

    private static void onReload(final ModConfigEvent.Reloading event) {
        apply(event.getConfig());
    }

    private static void apply(final ModConfig config) {
        // Only our own file: every other mod's config raises the same events.
        if (config.getSpec() != SPEC) {
            return;
        }
        showBootMenu = SHOW_BOOT_MENU_VALUE.get();
        JsComputers.LOGGER.debug("Boot menu is {}", showBootMenu ? "shown" : "hidden");
    }
}
