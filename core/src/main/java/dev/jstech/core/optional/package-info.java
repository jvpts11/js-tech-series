/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
/**
 * Working with another mod when it is installed and without it when it is not. The work with the other mod lives in
 * an adapter under an {@code integration} package, started through a guard that loads nothing of it unless the mod is
 * there and leaves it out, with a line in the log, if it fails to start. A check any mod runs over its own sources
 * keeps an optional mod from becoming a required one by an import in the wrong place.
 *
 * <p>How to write one, from zero, is in {@code core/docs/TOOLKIT.md}. An example that compiles is the Core's own work
 * with Curios ({@code core/src/main/java/dev/jstech/core/integration/curios/CuriosIntegration.java}).
 */
package dev.jstech.core.optional;
