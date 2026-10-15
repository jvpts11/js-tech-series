/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
/**
 * Work done away from the tick: each task runs on a virtual thread of its own, where it must not touch the world, and
 * its result is handed back on the main thread, where it may, a few at a time within a budget of each tick so a burst
 * of finished work never stretches one. A task can be cancelled, and then nothing of it is handed back. The server's
 * tasks are {@code Tasks.onServer}; a runner of one's own is drained wherever its main thread is.
 */
package dev.jstech.core.task;
