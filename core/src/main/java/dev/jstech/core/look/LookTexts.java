/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.look;

import dev.jstech.core.text.TextHolder;
import dev.jstech.core.text.TextKey;

/** What the Core's own block entities say to a player who looks at them. */
@TextHolder
public final class LookTexts {

    /** A machine at work, and how far through the piece of work it is. */
    public static final TextKey WORKING = TextKey.of("jscore.look.working", "Working: %s%%");
    /** A machine with nothing to work on, or no room for what it would make. */
    public static final TextKey IDLE = TextKey.of("jscore.look.idle", "Idle");
    /** Whose a thing is. */
    public static final TextKey OWNER = TextKey.of("jscore.look.owner", "Owner: %s");
    /** The switch in Jade's settings for the lines above, under the key Jade reads it from. */
    public static final TextKey JADE_OPTION = TextKey.of("config.jade.plugin_jscore.described",
            "What machines are doing, and whose they are");

    private LookTexts() {
    }
}
