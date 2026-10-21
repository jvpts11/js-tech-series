/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.guide;

import dev.jstech.core.JsCore;
import dev.jstech.core.config.ConfigFile;
import dev.jstech.core.config.ConfigKey;
import dev.jstech.core.config.ConfigSide;
import dev.jstech.core.config.format.ConfigFormats;
import dev.jstech.core.notification.NotificationKind;
import dev.jstech.core.notification.NotificationLevel;
import dev.jstech.core.notification.Notifications;
import dev.jstech.core.text.TextHolder;
import dev.jstech.core.text.TextKey;
import net.minecraft.resources.ResourceLocation;

/**
 * The tip at the right time: the first time a player picks up an item a manual explains, a quiet notification of
 * information says the manual has its page, and a click on it opens there. Once an item and a player, each player's
 * game keeping which items it told of; the player's own settings, in {@code jstech-manuals.toml}, turn it off, or on
 * in creative too.
 */
@TextHolder
public final class GuideTips {

    public static final TextKey KIND_NAME = TextKey.of("jscore.notification.kind.manual_tip", "Pages of the manuals");
    /** The tip: the item's name, and why it comes now. */
    public static final TextKey TITLE = TextKey.of("jscore.guide.tip.title", "In the manual: %s");
    public static final TextKey PICKED_UP = TextKey.of("jscore.guide.tip.picked_up", "Picked up for the first time");
    /** The kind of the tip, a notification of information, which the history and the silenced kinds name. */
    public static final NotificationKind TIP = Notifications.kind(ResourceLocation.fromNamespaceAndPath(JsCore.MODID,
            "manual_tip"), NotificationLevel.INFO, KIND_NAME).declare();
    private static final String SECTION = "manuals";
    /** Whether the first item of a kind picked up brings the tip of its page. */
    public static final ConfigKey<Boolean> ON_FIRST_PICKUP = ConfigKey.flag(SECTION + ".tip_on_first_pickup", true)
            .comment("Whether picking up an item a manual explains for the first time brings a quiet notification "
                    + "that opens its page.")
            .named("Tip when an item is first picked up");
    /** Whether the tip comes in creative too, where items are taken from the menu rather than picked up. */
    public static final ConfigKey<Boolean> IN_CREATIVE = ConfigKey.flag(SECTION + ".tip_in_creative", false)
            .comment("Whether the tip also comes in creative, where items are taken from the menu rather than "
                    + "picked up.")
            .named("Tips in creative too").activeWhen(ON_FIRST_PICKUP);
    /** The player's file. */
    public static final ConfigFile FILE = ConfigFile.builder("jstech-manuals", ConfigSide.CLIENT, ConfigFormats.TOML)
            .comment("How the manuals of the mods built on J's Core come to a player on this game.")
            .sectionComment(SECTION, "The tips of the manuals.")
            .sectionNamed(SECTION, "Manuals")
            .key(ON_FIRST_PICKUP, value -> onFirstPickup = value)
            .key(IN_CREATIVE, value -> inCreative = value)
            .build();

    private static volatile boolean onFirstPickup = true;
    private static volatile boolean inCreative;

    private GuideTips() {
    }

    /** Declares the tip's kind, from the Core's constructor. */
    public static void declare() {
        // Loading the class declares it.
    }

    /** Whether the player wants the tip when they first pick up an item a manual explains. */
    public static boolean onFirstPickup() {
        return onFirstPickup;
    }

    /** Whether the player wants the tip in creative too. */
    public static boolean inCreative() {
        return inCreative;
    }
}
