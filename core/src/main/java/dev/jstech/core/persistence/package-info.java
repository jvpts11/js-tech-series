/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */

/**
 * What a world keeps and how it is read back: the layout each kind of saved thing is written in, with the version it
 * carries and the steps that bring an older save up to today's; the files a world keeps its saved data in; reading a
 * saved value with the reason it could not be read written to the log; and the saved data of the Core itself.
 *
 * <p>A kind of saved thing declares its layout once, a {@link SaveLayout} with its name, today's version and a step
 * from each older version ({@link ISaveUpgrade}: a compound rewritten, a key renamed or removed). Whatever was saved
 * before it carried a version is at version 0, and something older takes every step from its version on, in order,
 * before it is read. A compound that is the thing itself, such as a block entity's tag, carries its version beside its
 * own entries ({@code stamp}, {@code read}); a value saved whole is wrapped with its version ({@code wrap},
 * {@code unwrap}); a value a codec writes goes through the layout's {@code codec}. A state's builder and a block
 * entity's fields take the same steps. Something a newer version of its mod saved is read as far as it can be, and
 * {@link SaveFiles} keeps its file as the newer version left it beside the one written now, so going back to the newer
 * version loses nothing. {@link SavedValue} reads what a codec gives back and, when it cannot be read, writes why to
 * the log before handing back nothing. {@link UpgradeChain} counts the versions and runs the steps alone, the same for
 * saves and settings files. The Core's own saved data are which data networks exist in each dimension
 * ({@link NetworkRegistry}) and which have cables passing through each chunk ({@link CoreChunkData}).
 *
 * <p>How to version what a mod saves, from zero, is in {@code core/docs/STATE_AND_SAVES.md}. Examples that compile are
 * the Core's tests of layouts ({@code tests/src/main/java/dev/jstech/tests/gametest/core/SaveLayoutGameTests.java}) and
 * J's Computers' awards kept for players who are away, a state with a step from before versions
 * ({@code computers/src/main/java/dev/jstech/computers/advancement/PendingAwards.java}).
 */
@ApiStatus.Experimental
package dev.jstech.core.persistence;

import org.jetbrains.annotations.ApiStatus;
