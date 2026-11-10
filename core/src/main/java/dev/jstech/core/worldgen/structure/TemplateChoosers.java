/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.worldgen.structure;

import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import net.minecraft.resources.ResourceLocation;

/**
 * The choosers of the {@code jscore:chosen} pool elements, by the id a pool names them by. A mod registers its own
 * from its constructor, before any world is generated:
 *
 * <pre>{@code
 * TemplateChoosers.register(rl("ruin"), (level, at, random, templates) ->
 *         new ITemplateChooser.Choice(templates.get(random.nextInt(templates.size())), List.of()));
 * }</pre>
 */
public final class TemplateChoosers {

    private static final Map<ResourceLocation, ITemplateChooser> CHOOSERS = new ConcurrentHashMap<>();

    private TemplateChoosers() {
    }

    /** Registers {@code chooser} under {@code id}; a second one under the same id is refused. */
    public static void register(final ResourceLocation id, final ITemplateChooser chooser) {
        if (CHOOSERS.putIfAbsent(id, chooser) != null) {
            throw new IllegalStateException("a template chooser is registered twice as " + id);
        }
    }

    /** The chooser registered under {@code id}. */
    public static Optional<ITemplateChooser> get(final ResourceLocation id) {
        return Optional.ofNullable(CHOOSERS.get(id));
    }
}
