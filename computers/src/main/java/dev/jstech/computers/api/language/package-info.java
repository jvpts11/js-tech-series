/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Computers.
 */

/**
 * The programming languages of J's Computers' machines: what a language is to the machines, the programs it runs, the
 * view of its machine a running program is given, and the registry of every language a world knows.
 *
 * <p>A language implements {@link IProgrammingLanguage}: its id, which every saved program names it by; its name; the
 * extensions of the files a person writes in it and of those it runs; how it compiles source; and how an editor
 * colours it. A language may do nothing but compile, to a listing the machines run themselves. One that runs its own
 * files also starts and restores its programs, each an {@link ILanguageProcess} that spends a budget of instructions
 * each tick, never blocks, and saves itself with the world; a program reads its machine's clock, writes the lines a
 * person reads and learns how much memory it may hold through its {@link IMachineView}. A mod registers its language
 * once, while the game loads, through {@code ComputersRegisterEvent.languages()}, which is the
 * {@link LanguageRegistry}: it finds every file's language by its extension, refuses an extension another language
 * already claims, lets an add-on take a language away, and closes once the game has loaded. The machines then give the
 * language their prompt, file explorer, terminal, task manager, saving and tick budget, the budget set by
 * {@link ExecutionBalance}.
 *
 * <p>Examples that compile are the test mod's toy language
 * ({@code tests/src/main/java/dev/jstech/tests/testkit/ToyLanguage.java}) and J's Computers' Sigma and Sigma Sharp,
 * which only compile ({@code computers/src/main/java/dev/jstech/computers/machine/SigmaLanguage.java}).
 */
@ApiStatus.Experimental
package dev.jstech.computers.api.language;

import org.jetbrains.annotations.ApiStatus;
