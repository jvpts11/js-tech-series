/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.text;

import java.util.ArrayList;
import java.util.List;
import net.minecraft.locale.Language;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.contents.TranslatableContents;

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

    /** A sentence with nothing put into it, in the language this side has loaded. */
    public static String resolve(final TextKey key) {
        return key.text().resolve(LOADED);
    }

    /** The game's component for it, for a screen or a message that takes one, ready to be styled. */
    public static MutableComponent component(final Text text) {
        return switch (text) {
            case Text.Literal literal -> Component.literal(literal.value());
            case Text.Translated translated -> Component.translatableWithFallback(translated.key().key(),
                    translated.key().english(), arguments(translated.args()));
        };
    }

    /** The game's component for a sentence with nothing put into it, ready to be styled. */
    public static MutableComponent component(final TextKey key) {
        return component(key.text());
    }

    /**
     * A game component as text, so a name the game translates (an item's, a block's) travels as its key and is read
     * in the language of whoever it reaches, rather than in the one this side resolved it in.
     *
     * <p>What this side reads the component as stands in for its English, for the machine's own records.
     */
    public static Text of(final Component component) {
        if (component.getContents() instanceof TranslatableContents translatable && component.getSiblings().isEmpty()) {
            final Object[] given = translatable.getArgs();
            final List<Text> args = new ArrayList<>(given.length);
            for (final Object arg : given) {
                args.add(arg instanceof Component nested ? of(nested) : Text.of(arg));
            }
            try {
                return new Text.Translated(new TextKey(translatable.getKey(), component.getString()), args);
            } catch (final IllegalArgumentException notAKey) {
                return Text.literal(component.getString());
            }
        }
        return Text.literal(component.getString());
    }

    private static Object[] arguments(final List<Text> args) {
        final Object[] out = new Object[args.size()];
        for (int i = 0; i < out.length; i++) {
            out[i] = component(args.get(i));
        }
        return out;
    }
}
