/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.client.studio;

import dev.jstech.core.text.GameText;

/** The GameTests that name the thing, counted into its row of tests; "Run its tests" runs them. */
final class TestReader implements IStudioReader {

    @Override
    public void read(final StudioThing thing, final StudioContext context, final StudioSheet sheet) {
        final int count = context.tests().naming(thing.id().getPath()).size();
        if (count > 0) {
            sheet.tests(GameText.resolve(StudioTexts.TESTS_COUNT.with(count)));
        }
    }
}
