/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.client.developer;

import dev.jstech.core.client.gui.data.DataScreenView;
import dev.jstech.core.client.gui.data.DataScreens;
import java.util.List;
import net.minecraft.client.gui.Font;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;

/**
 * Whether a screen declared in a file holds its words in each of the series' languages: the screen built as it would
 * open, and each text measured in each language against its room, as the inspector measures it.
 */
public final class ScreenFit {

    /** How tall the screen is laid out, a window of the smallest the series is made for. */
    private static final int HEIGHT = 240;

    private ScreenFit() {
    }

    /** Each text of {@code screen} past its room in a language. */
    public static List<Overflow> of(final ResourceLocation screen, final Font font) {
        final DataScreenView view = DataScreens.preview(screen, Component.literal(screen.getPath()));
        try {
            view.build(font);
            view.layout(0, 0, HEIGHT);
            return new InspectorModel(view).tooLong(font).stream().map(found -> new Overflow(found.component(),
                    found.language(), found.over())).toList();
        } finally {
            view.dispose();
        }
    }

    /**
     * A text past its room.
     *
     * @param component the component, by its name or its kind
     * @param language  the language it does not fit in
     * @param over      by how many pixels
     */
    public record Overflow(String component, String language, int over) {
    }
}
