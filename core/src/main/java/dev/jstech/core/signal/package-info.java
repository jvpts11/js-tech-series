/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */

/**
 * Redstone and signals for any mod: every sensor, and every machine that reads something, speaks redstone through one
 * capability, and signals travel without wire by frequency, from a transmitter to every receiver within its reach,
 * sent again by relays. Nothing is worked out on every tick, so a large build weighs nothing on the game.
 *
 * <p>What a block reads is a list of readings ({@link Reading}), each with its value and its span, from the value
 * that gives no signal to the one that gives the full 15. A block offers them as an {@link ISignalOutput} on the
 * capability of {@link Signals}, through which anything reads every sensor alike, and keeps the redstone it gives
 * in a {@link SignalLevel}, so its neighbours hear of it only when it changes. A sensor is a {@link SensorBlock}
 * whose {@link SensorBlockEntity} a mod writes with what it reads. A radio is a {@link RadioBlock} of one role, a
 * transmitter, a receiver or a relay, all of a mod's radios sharing one {@link RadioBlockEntity} type, and a block
 * entity of a mod's own may be an {@link IRadio} too. What a radio is tuned to is a {@link Tuning}, in a {@link Band}
 * of its owner's, of its owner's team's or of a pair's own; how far it carries is {@link RadioReach}, less for every
 * solid block in the way; and {@link SignalRadio} keeps who hears whom across the server, a second transmitter on one
 * frequency being a clash.
 *
 * <p>How sensors and radios work, from zero, is in {@code core/docs/SIGNALS.md}. An example that compiles is the test
 * mod's transmitter, receiver, relay and light sensor ({@code tests/src/main/java/dev/jstech/tests/TestRadios.java}).
 */
@ApiStatus.Experimental
package dev.jstech.core.signal;

import org.jetbrains.annotations.ApiStatus;
