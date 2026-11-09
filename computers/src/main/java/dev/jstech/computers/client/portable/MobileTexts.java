/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Computers.
 */
package dev.jstech.computers.client.portable;

import dev.jstech.core.text.GameText;
import dev.jstech.core.text.Text;
import dev.jstech.core.text.TextHolder;
import dev.jstech.core.text.TextKey;

/** The words on the portables' screens, in each system's own way of putting them. */
@TextHolder
public final class MobileTexts {

    public static final TextKey DAY = TextKey.of("jsc.mobile.day", "Day %s");
    public static final TextKey NOW = TextKey.of("jsc.mobile.now", "now");
    public static final TextKey START = TextKey.of("jsc.mobile.frames_pocket.start", "Start");
    public static final TextKey TODAY = TextKey.of("jsc.mobile.today", "Today");
    public static final TextKey NETWORK_NAMED = TextKey.of("jsc.mobile.network_named", "Network %s");
    public static final TextKey NETWORK_HEADER = TextKey.of("jsc.mobile.network_header", "NETWORK %s");
    public static final TextKey NETWORK_CAPS = TextKey.of("jsc.mobile.network_caps", "NETWORK");
    public static final TextKey NO_NETWORK = TextKey.of("jsc.mobile.no_network", "No network");
    public static final TextKey LINKED = TextKey.of("jsc.mobile.linked", "Linked: %s");
    public static final TextKey ALERT_COUNT = TextKey.of("jsc.mobile.alert_count", "%s alerts");
    public static final TextKey ONE_ALERT = TextKey.of("jsc.mobile.one_alert", "1 alert");
    public static final TextKey NO_ALERTS = TextKey.of("jsc.mobile.no_alerts", "No alerts");
    public static final TextKey NEW_ALERTS = TextKey.of("jsc.mobile.new_alerts", "%s new alerts");
    public static final TextKey CLOCK_AND_DAY = TextKey.of("jsc.mobile.clock_and_day", "%s, Day %s");
    public static final TextKey DAY_AND_ALERTS = TextKey.of("jsc.mobile.day_and_alerts", "Day %s · %s alerts");
    public static final TextKey NO_SLEEP = TextKey.of("jsc.mobile.no_sleep", "No sleep set");
    public static final TextKey OWNER = TextKey.of("jsc.mobile.owner", "Owner: %s");
    public static final TextKey BATTERY = TextKey.of("jsc.mobile.battery", "Battery %s%%");
    public static final TextKey BATTERY_COLON = TextKey.of("jsc.mobile.battery_colon", "Battery: %s%%");
    public static final TextKey BATTERY_WORD = TextKey.of("jsc.mobile.battery_word", "Battery");
    public static final TextKey PERCENT = TextKey.of("jsc.mobile.percent", "%s%%");
    public static final TextKey NEW = TextKey.of("jsc.mobile.frames_pocket.new", "New");
    public static final TextKey MARKETPLACE = TextKey.of("jsc.mobile.frames_pocket.marketplace", "Marketplace");
    public static final TextKey SETTINGS = TextKey.of("jsc.mobile.settings", "Settings");
    public static final TextKey PROGRAMS = TextKey.of("jsc.mobile.frames_pocket.programs", "Programs");
    public static final TextKey DEVICE_LOCKED = TextKey.of("jsc.mobile.frames_pocket.locked", "Device locked");
    public static final TextKey UNLOCK_TO_SEE = TextKey.of("jsc.mobile.frames_pocket.unlock_to_see",
            "Unlock to see %s alerts");
    public static final TextKey UNLOCK = TextKey.of("jsc.mobile.frames_pocket.unlock", "Unlock");
    public static final TextKey NETWORK_ALERT = TextKey.of("jsc.mobile.frames_pocket.network_alert",
            "Network alert");
    public static final TextKey DISMISS = TextKey.of("jsc.mobile.frames_pocket.dismiss", "Dismiss");
    public static final TextKey SHOW = TextKey.of("jsc.mobile.frames_pocket.show", "Show");
    public static final TextKey DONE = TextKey.of("jsc.mobile.frames_pocket.done", "Done");
    public static final TextKey PERSONAL = TextKey.of("jsc.mobile.frames_pocket.personal", "Personal");
    public static final TextKey SYSTEM = TextKey.of("jsc.mobile.frames_pocket.system", "System");
    public static final TextKey LINKS = TextKey.of("jsc.mobile.frames_pocket.links", "Links");
    public static final TextKey WIFI = TextKey.of("jsc.mobile.wifi", "Wi-Fi");
    public static final TextKey POWER = TextKey.of("jsc.mobile.frames_pocket.power", "Power");
    public static final TextKey CLOCK = TextKey.of("jsc.mobile.frames_pocket.clock", "Clock");
    public static final TextKey ABOUT = TextKey.of("jsc.mobile.frames_pocket.about", "About");
    public static final TextKey WIFI_NAMED = TextKey.of("jsc.mobile.wifi_named", "Wi-Fi: %s");
    public static final TextKey WIFI_SPEED = TextKey.of("jsc.mobile.wifi_speed", "%s · %s items per tick");
    public static final TextKey HARDWARE_SHORT = TextKey.of("jsc.mobile.hardware_short", "%s · %s · %s");
    public static final TextKey STORAGE_CARD = TextKey.of("jsc.mobile.storage_card", "Storage card: %s");
    public static final TextKey FROM_THE_MIRROR = TextKey.of("jsc.mobile.from_the_mirror", "From the Mirror");
    public static final TextKey THE_MIRROR = TextKey.of("jsc.mobile.the_mirror", "the Mirror");
    public static final TextKey SEARCH_THE_MIRROR = TextKey.of("jsc.mobile.search_the_mirror", "Search the Mirror");
    public static final TextKey MIRROR_OUT_OF_REACH = TextKey.of("jsc.mobile.mirror_out_of_reach",
            "The Mirror is out of reach");
    public static final TextKey FREE = TextKey.of("jsc.mobile.free", "Free");
    public static final TextKey INSTALL = TextKey.of("jsc.mobile.install", "Install");
    public static final TextKey OPEN = TextKey.of("jsc.mobile.open", "Open");
    public static final TextKey GET_CAPS = TextKey.of("jsc.mobile.qos.get", "GET");
    public static final TextKey OPEN_CAPS = TextKey.of("jsc.mobile.qos.open", "OPEN");
    public static final TextKey SLIDE_TO_UNLOCK = TextKey.of("jsc.mobile.qos.slide_to_unlock", "slide to unlock");
    public static final TextKey SLIDE_TO_UNLOCK_ARROW = TextKey.of("jsc.mobile.qos.slide_to_unlock_arrow",
            "slide to unlock  >");
    public static final TextKey ALL = TextKey.of("jsc.mobile.qos.all", "All");
    public static final TextKey MISSED = TextKey.of("jsc.mobile.qos.missed", "Missed");
    public static final TextKey ALERTS_ROW = TextKey.of("jsc.mobile.alerts", "Alerts");
    public static final TextKey STORE = TextKey.of("jsc.mobile.store", "Store");
    public static final TextKey STORAGE = TextKey.of("jsc.mobile.storage", "Storage");
    public static final TextKey OF = TextKey.of("jsc.mobile.of", "%s of %s");
    public static final TextKey ABOUT_CAPS = TextKey.of("jsc.mobile.qos.about", "ABOUT");
    public static final TextKey MODEL = TextKey.of("jsc.mobile.qos.model", "Model");
    public static final TextKey CHIP = TextKey.of("jsc.mobile.qos.chip", "Chip");
    public static final TextKey MEMORY = TextKey.of("jsc.mobile.qos.memory", "Memory");
    public static final TextKey NOT_CONNECTED = TextKey.of("jsc.mobile.not_connected", "Not connected");
    public static final TextKey SEARCH = TextKey.of("jsc.mobile.droidix.search", "Search");
    public static final TextKey UNLOCK_WORD = TextKey.of("jsc.mobile.droidix.unlock", "unlock");
    public static final TextKey SWIPE_TO_UNLOCK = TextKey.of("jsc.mobile.droidix.swipe_to_unlock",
            "Swipe to unlock");
    public static final TextKey CLEAR_ALL = TextKey.of("jsc.mobile.droidix.clear_all", "CLEAR ALL");
    public static final TextKey CONNECTIONS = TextKey.of("jsc.mobile.droidix.connections", "Connections");
    public static final TextKey WIFI_THROUGH = TextKey.of("jsc.mobile.droidix.wifi_through", "Wi-Fi · %s");
    public static final TextKey NOTIFICATIONS = TextKey.of("jsc.mobile.droidix.notifications", "Notifications");
    public static final TextKey KINDS_ON = TextKey.of("jsc.mobile.droidix.kinds_on", "%s kinds on");
    public static final TextKey BATTERY_LEFT = TextKey.of("jsc.mobile.droidix.battery_left", "%s%%, %s");
    public static final TextKey DAYS_HOURS = TextKey.of("jsc.mobile.days_hours", "%s d %s h");
    public static final TextKey HOURS = TextKey.of("jsc.mobile.hours", "%s h");
    public static final TextKey ABOUT_DEVICE = TextKey.of("jsc.mobile.droidix.about_device", "About device");

    private MobileTexts() {
    }

    /** What {@code text} says in the player's language. */
    public static String say(final Text text) {
        return GameText.resolve(text);
    }

    /** What {@code key} says in the player's language. */
    public static String say(final TextKey key) {
        return GameText.resolve(key.text());
    }
}
