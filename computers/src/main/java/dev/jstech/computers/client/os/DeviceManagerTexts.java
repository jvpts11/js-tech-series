/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Computers.
 */
package dev.jstech.computers.client.os;

import dev.jstech.core.text.TextHolder;
import dev.jstech.core.text.TextKey;

/** What the Device Manager of each Frames edition says. */
@TextHolder
final class DeviceManagerTexts {

    static final TextKey TITLE = TextKey.of("jsc.device_manager.title", "Device Manager");
    static final TextKey SYSTEM_PROPERTIES = TextKey.of("jsc.device_manager.system_properties", "System Properties");
    static final TextKey READING = TextKey.of("jsc.device_manager.reading", "Reading the machine...");

    static final TextKey TAB_GENERAL = TextKey.of("jsc.device_manager.tab.general", "General");
    static final TextKey TAB_DEVICE_MANAGER = TextKey.of("jsc.device_manager.tab.device_manager", "Device Manager");
    static final TextKey TAB_PROFILES = TextKey.of("jsc.device_manager.tab.profiles", "Hardware Profiles");
    static final TextKey TAB_PERFORMANCE = TextKey.of("jsc.device_manager.tab.performance", "Performance");

    static final TextKey RADIO_TYPE = TextKey.of("jsc.device_manager.radio.type", "View devices by type");
    static final TextKey RADIO_CONNECTION = TextKey.of("jsc.device_manager.radio.connection",
            "View devices by connection");
    static final TextKey RADIO_PORT = TextKey.of("jsc.device_manager.radio.port", "View devices by port");

    static final TextKey PROPERTIES = TextKey.of("jsc.device_manager.properties", "Properties");
    static final TextKey REFRESH = TextKey.of("jsc.device_manager.refresh", "Refresh");
    static final TextKey DISABLE = TextKey.of("jsc.device_manager.disable", "Disable");
    static final TextKey ENABLE = TextKey.of("jsc.device_manager.enable", "Enable");
    static final TextKey PRINT = TextKey.of("jsc.device_manager.print", "Print...");
    static final TextKey OK = TextKey.of("jsc.device_manager.ok", "OK");
    static final TextKey CANCEL = TextKey.of("jsc.device_manager.cancel", "Cancel");

    static final TextKey MENU_FILE = TextKey.of("jsc.device_manager.menu.file", "File");
    static final TextKey MENU_ACTION = TextKey.of("jsc.device_manager.menu.action", "Action");
    static final TextKey MENU_VIEW = TextKey.of("jsc.device_manager.menu.view", "View");
    static final TextKey MENU_HELP = TextKey.of("jsc.device_manager.menu.help", "Help");
    static final TextKey EXIT = TextKey.of("jsc.device_manager.exit", "Exit");
    static final TextKey VIEW_TYPE = TextKey.of("jsc.device_manager.view.type", "Devices by type");
    static final TextKey VIEW_CONNECTION = TextKey.of("jsc.device_manager.view.connection", "Devices by connection");
    static final TextKey VIEW_PORT = TextKey.of("jsc.device_manager.view.port", "Devices by port");
    static final TextKey SHOW_HIDDEN = TextKey.of("jsc.device_manager.show_hidden", "Show hidden devices");
    static final TextKey ABOUT = TextKey.of("jsc.device_manager.about", "About Device Manager");
    static final TextKey UPDATE_DRIVER = TextKey.of("jsc.device_manager.update_driver", "Update Driver...");
    static final TextKey UNINSTALL = TextKey.of("jsc.device_manager.uninstall", "Uninstall");
    /** The mark before the view a menu shows is the one in front. */
    static final TextKey CHOSEN = TextKey.of("jsc.device_manager.chosen", "• %s");

    static final TextKey SEGMENT_PORT = TextKey.of("jsc.device_manager.segment.port", "By port");
    static final TextKey SEGMENT_TYPE = TextKey.of("jsc.device_manager.segment.type", "By type");
    static final TextKey SEGMENT_CONNECTION = TextKey.of("jsc.device_manager.segment.connection", "By connection");

    static final TextKey STATUS_DISABLED = TextKey.of("jsc.device_manager.status.disabled",
            "%s is disabled: the computer stops reading and writing it.");
    static final TextKey STATUS_COUNT = TextKey.of("jsc.device_manager.status.count", "%s devices");

    static final TextKey PROPS_TITLE = TextKey.of("jsc.device_manager.props.title", "%s Properties");
    static final TextKey PROPS_LOCATION = TextKey.of("jsc.device_manager.props.location", "Location: %s");
    static final TextKey PROPS_WORKING = TextKey.of("jsc.device_manager.props.working",
            "This device is working properly.");
    static final TextKey PROPS_DISABLED = TextKey.of("jsc.device_manager.props.disabled", "This device is disabled.");

    private DeviceManagerTexts() {
    }
}
