/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.diagnostic;

import java.util.function.Supplier;

/*
 * The frame a section's work runs in: a class of its own, named after the section, so a profiler's samples, the
 * watchdog's stacks and a crash report's show the section by name.
 */
interface ISectionFrame {

    void run(Runnable work);

    Object call(Supplier<?> work);
}
