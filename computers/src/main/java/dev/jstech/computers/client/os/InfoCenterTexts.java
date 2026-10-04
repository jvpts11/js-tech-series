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

/**
 * What KDE's Info Center says around its pages: the list of pages and the button of "Devices by port". Kept apart
 * from the window so the language generator can read it on a server too.
 */
@TextHolder
final class InfoCenterTexts {

    static final TextKey ABOUT_THIS_SYSTEM = TextKey.of("jsc.info_center.about_this_system", "About this System");
    static final TextKey DEVICES_BY_PORT = TextKey.of("jsc.info_center.devices_by_port", "Devices by port");
    static final TextKey DISABLE_SELECTED = TextKey.of("jsc.info_center.disable_selected",
            "Disable the selected device");
    static final TextKey ENABLE_SELECTED = TextKey.of("jsc.info_center.enable_selected",
            "Enable the selected device");

    private InfoCenterTexts() {
    }
}
