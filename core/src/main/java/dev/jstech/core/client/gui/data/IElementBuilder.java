/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.client.gui.data;

import dev.jstech.core.gui.screen.ElementSpec;

/**
 * What builds one kind of component of a screen's file on the player's game: its box in the layout and what it draws,
 * from what the file says of it. Registered with {@link ElementBuilders#register} from client setup.
 *
 * @param <P> what the kind's own fields are read into
 */
@FunctionalInterface
public interface IElementBuilder<P> {

    /**
     * The component {@code spec} declares, its own fields {@code props}. Use {@link BuildContext#node} to make the
     * node, so the fields every component has (its size, its room, when it shows) are honoured alike.
     */
    BuiltNode build(ElementSpec spec, P props, BuildContext context);
}
