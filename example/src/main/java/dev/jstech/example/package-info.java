/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Example.
 */

/**
 * J's Example: a mod built on J's Core alone, and only on what the Core promises, to be read as a tutorial.
 *
 * <p>Everything it adds is declared in {@link ExampleContent}: a generator that burns fuel, a battery, a crusher whose
 * recipes are files of data, a tank and the oil it holds, an energy cable and a fluid pipe in the Core's shared cable
 * block, and a sensor mounted on a cable that reads how full the block beside it is. Its settings are a file the player
 * can edit ({@link ExampleSettings}), it keeps what it remembers per server and per player ({@link ExampleState}), and
 * its logbook opens a screen declared in a file.
 *
 * <p>Its unit test holds it to the promise: anything it reaches in the Core has to be in the Core's API photo, and it
 * may reach nothing of J's Computers or J's Industrial at all.
 */
package dev.jstech.example;
