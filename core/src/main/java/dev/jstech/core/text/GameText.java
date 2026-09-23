/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.text;

import java.util.List;
import net.minecraft.locale.Language;
import net.minecraft.network.chat.Component;

/**
 * Text as the game holds it: resolved in the language this side of the game has loaded, and turned into the game's
 * own components for the screens and messages that take those.
 *
 * <p>The language loaded on a client is its player's; on a server it is English, since every mod's English file is
 * loaded there too. So what a server resolves is what a machine writes down, and what a client resolves is what its
 * player reads.
 */
public final class GameText {

    /** The language this side of the game has loaded. */
    public static final ITextLanguage LOADED = key -> {
        final Language language = Language.getInstance();
        return language.has(key) ? language.getOrDefault(key) : null;
    };

    private GameText() {
    }

    /** What it says in the language this side has loaded. */
    public static String resolve(final Text text) {
        return text.resolve(LOADED);
    }

    /** The game's component for it, for a screen or a message that takes one. */
    public static Component component(final Text text) {
        return switch (text) {
            case Text.Literal literal -> Component.literal(literal.value());
            case Text.Translated translated -> Component.translatableWithFallback(translated.key().key(),
                    translated.key().english(), arguments(translated.args()));
        };
    }

    /** The game's component for a sentence with nothing put into it. */
    public static Component component(final TextKey key) {
        return component(key.text());
    }

    private static Object[] arguments(final List<Text> args) {
        final Object[] out = new Object[args.size()];
        for (int i = 0; i < out.length; i++) {
            out[i] = component(args.get(i));
        }
        return out;
    }
}
