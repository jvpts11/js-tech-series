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
 *
 * <p>How to write one, from zero, is in {@code core/docs/TOOLKIT.md}. An example that compiles is the test mod's
 * codec kit test ({@code tests/src/main/java/dev/jstech/tests/gametest/core/CodecKitGameTests.java}), which writes a
 * record's codec and takes the network's from it.
 */
package dev.jstech.core.codec;
