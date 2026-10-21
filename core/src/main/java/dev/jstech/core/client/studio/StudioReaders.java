/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.client.studio;

import dev.jstech.core.log.CoreLogs;
import dev.jstech.core.text.GameText;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.CopyOnWriteArrayList;
import org.slf4j.Logger;

/**
 * Who reads what things are made of: the Core's readers, in the order the studio shows their rows (model, screen,
 * effects, sounds, names, recipe, manual page, tags, drops, settings, and the creative tabs and GameTests), and then
 * every reader a mod adds for a part of its own, from its client's setup.
 *
 * <pre>{@code
 * StudioReaders.add((thing, context, sheet) -> programOf(thing).ifPresent(program -> sheet.part(...)));
 * }</pre>
 */
public final class StudioReaders {

    private static final Logger LOGGER = CoreLogs.DEVELOPER.logger();
    private static final List<IStudioReader> READERS = new CopyOnWriteArrayList<>(List.of(new ModelReader(),
            new ScreenReader(), new EffectReader(), new SoundReader(), new NameReader(), new RecipeReader(),
            new ManualReader(), new TagReader(), new DropReader(), new SettingReader(), new TabReader(),
            new TestReader()));

    private StudioReaders() {
    }

    /** Adds a reader, after the Core's and those added before it. */
    public static void add(final IStudioReader reader) {
        READERS.add(Objects.requireNonNull(reader, "reader"));
    }

    /**
     * What {@code thing} is made of, read by every reader; a reader that fails is written down as a problem of the
     * thing's, so one broken part never hides the rest.
     */
    public static StudioSheet read(final StudioThing thing, final StudioContext context) {
        final StudioSheet sheet = new StudioSheet();
        for (final IStudioReader reader : READERS) {
            try {
                reader.read(thing, context, sheet);
            } catch (final RuntimeException failed) {
                LOGGER.warn("The studio's reader {} failed on {}", reader.getClass().getName(), thing.id(), failed);
                sheet.check(new StudioSheet.Check(StudioSheet.Mark.ERROR, GameText.resolve(StudioTexts.READER_FAILED),
                        GameText.resolve(StudioTexts.READER_FAILED_DETAIL.with(reader.getClass().getSimpleName(),
                                String.valueOf(failed.getMessage()))), Optional.empty()));
            }
        }
        return sheet;
    }
}
