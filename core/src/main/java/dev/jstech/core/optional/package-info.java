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
 */
package dev.jstech.core.optional;
