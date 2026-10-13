/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.guide;

import dev.jstech.core.audio.AudioChannels;
import dev.jstech.core.audio.SoundKey;
import dev.jstech.core.registry.CoreItems;
import net.minecraft.resources.ResourceLocation;

/**
 * What the manuals sound like: the game's own sounds of a book, a page turning and a cover laid down, and the game's
 * own click for a button, played from the player's screen in the Manuals channel, so a reader can turn the books
 * down apart from everything else. None is a file of the Core's own; the game already has the right ones.
 */
public final class GuideSounds {

    /** A page turning, one of the game's three flips of a book's page. */
    public static final SoundKey PAGE = CoreItems.CONTENT.sound("manual/page").onScreen()
            .channel(AudioChannels.MANUALS).file(game("item/book/open_flip1")).file(game("item/book/open_flip2"))
            .file(game("item/book/open_flip3")).subtitle("Manual page turns").register();

    /** A manual opened: a book's page flipped from its cover. */
    public static final SoundKey OPEN = CoreItems.CONTENT.sound("manual/open").onScreen()
            .channel(AudioChannels.MANUALS).file(game("item/book/open_flip1")).file(game("item/book/open_flip3"))
            .subtitle("Manual opens").register();

    /** A manual closed: a book laid down. */
    public static final SoundKey CLOSE = CoreItems.CONTENT.sound("manual/close").onScreen()
            .channel(AudioChannels.MANUALS).file(game("item/book/close_put1")).file(game("item/book/close_put2"))
            .subtitle("Manual closes").register();

    /** A button, a tab or a link clicked: the game's own button click. */
    public static final SoundKey CLICK = CoreItems.CONTENT.sound("manual/click").onScreen()
            .channel(AudioChannels.MANUALS).file(game("random/click_stereo")).subtitle("Manual clicks").register();

    private GuideSounds() {
    }

    /** Declares the manuals' sounds while the Core's content is declared. */
    public static void declare() {
        // Loading the class declares them.
    }

    private static ResourceLocation game(final String path) {
        return ResourceLocation.withDefaultNamespace(path);
    }
}
