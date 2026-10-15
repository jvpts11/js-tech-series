/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
/**
 * Logs by system: each part of a mod that writes to the log does it under a name of its own, {@code mod/system}, so a
 * line says which part of which mod wrote it, and how much each part writes can be changed while the game runs, from
 * code or with {@code /jstech log}, to follow a fault in one part without the rest filling the log.
 */
package dev.jstech.core.log;
