/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Space.
 */
package dev.jstech.space.crawler;

import dev.jstech.core.text.TextHolder;
import dev.jstech.core.text.TextKey;

/** What a crawler-transporter tells the player who uses its cab, when it cannot go. */
@TextHolder
public final class CrawlerTexts {

    public static final TextKey NOT_WHOLE = TextKey.of("jsspace.crawler.not_whole",
            "The crawler is not whole: every track, jack, deck plate and cab must be in its place round the engine.");
    public static final TextKey MOVING = TextKey.of("jsspace.crawler.moving", "The crawler is already on its way.");
    public static final TextKey NO_WAY = TextKey.of("jsspace.crawler.no_way",
            "There is no crawlerway to follow: a lane of gravel under each side's trucks, along the crawler.");
    public static final TextKey BLOCKED = TextKey.of("jsspace.crawler.blocked",
            "The way is blocked at %s, %s, %s.");
    public static final TextKey NOT_BUILT = TextKey.of("jsspace.crawler.not_built",
            "The rocket on the launcher is not finished yet.");
    public static final TextKey NO_COMPUTER = TextKey.of("jsspace.crawler.no_computer",
            "The rocket on the launcher has no flight computer to take its plan with it.");
    public static final TextKey TOO_BIG = TextKey.of("jsspace.crawler.too_big",
            "The launcher and its rocket are more than the crawler can carry.");

    private CrawlerTexts() {
    }
}
