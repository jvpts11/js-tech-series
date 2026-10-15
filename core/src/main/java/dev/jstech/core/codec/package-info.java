/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
/**
 * Codecs without the boilerplate. A codec says how a value is written into a save, a data file or a message and read
 * back (it is the game's own Codec, from Mojang's DataFixerUpper). Here a record's codec is written one line a
 * component, in the record's order, checked against the record as it is built; and the codec a value travels the
 * network with is taken from the one it is saved with, so the two never disagree.
 */
package dev.jstech.core.codec;
