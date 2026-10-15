/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.client.gui.data;

import dev.jstech.core.client.gui.component.Label;
import dev.jstech.core.gui.flex.FlexBox;
import dev.jstech.core.gui.flex.Role;
import dev.jstech.core.gui.screen.ElementSpec;
import dev.jstech.core.gui.screen.ElementType;
import dev.jstech.core.util.DeclarationError;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * What builds each kind of component on the player's game, by kind. The Core registers its own; a mod that declares a
 * kind registers what builds it here, from client setup.
 */
public final class ElementBuilders {

    private static final Map<ElementType<?>, IElementBuilder<?>> BY_TYPE = new ConcurrentHashMap<>();

    private ElementBuilders() {
    }

    /**
     * Registers what builds {@code type}.
     *
     * @throws DeclarationError when something builds it already
     */
    public static <P> void register(final ElementType<P> type, final IElementBuilder<P> builder) {
        if (BY_TYPE.putIfAbsent(type, builder) != null) {
            throw DeclarationError.of(type.id().getNamespace(), "the builder of the screen component " + type.id(),
                    "is registered twice", "register it once, from client setup");
        }
    }

    /** Whether something builds {@code type}. */
    public static boolean has(final ElementType<?> type) {
        return BY_TYPE.containsKey(type);
    }

    /**
     * {@code spec} built by its kind's builder; a kind nobody builds on this game is a line saying so, where the
     * component would have been.
     */
    @SuppressWarnings("unchecked")
    public static BuiltNode build(final ElementSpec spec, final BuildContext context) {
        final IElementBuilder<Object> builder = (IElementBuilder<Object>) BY_TYPE.get(spec.type());
        if (builder == null) {
            final String said = "[" + spec.type() + "]";
            final Label missing = new Label(said);
            return context.node(spec, FlexBox.leaf("missing " + spec.type(), context.text(1f, false).of(said))
                    .role(Role.TEXT), missing, null, List.of());
        }
        return builder.build(spec, spec.props(), context);
    }
}
