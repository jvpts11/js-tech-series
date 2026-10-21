/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */

/**
 * Operations: the SQL-like requests a player or a program makes of a data network (search the storage, craft this,
 * move that; J's Computers' SELECT, INSERT, DELETE, CRAFT and VACUUM among them), the registry of OperationTypes every
 * mod declares its kinds of Operation in, the eight states a request goes through, and the dispatcher that runs their
 * work on virtual threads while every change to the world stays on the server's thread.
 *
 * <p>A kind of Operation is an {@link OperationType}: its id, saved in worlds; the record a request of it carries,
 * which implements {@link IOperationArgs}; the {@link OperationCategory} screens list it under; the lowest industrial
 * tier that can run it; the categories of member that may run it; and the {@link IOperationHandler} that does the work
 * on the spot, or starts it and answers {@code PENDING}. A mod registers its kinds once, while the game loads, through
 * {@code CoreRegisterEvent.operations()}, which is the {@link OperationTypeRegistry}: it refuses an id already taken
 * and closes once the game has loaded. A request goes from {@code PENDING} to one of the outcomes of
 * {@link OperationStatus}; one that failed says why as an {@link OperationFailure}, a sentence each player reads in
 * their own language; and the queues are handed out by {@link OperationPriority}, highest first.
 * {@link OperationDispatch} runs the CPU-bound work of an {@link IOperationTask} on a virtual thread, which goes
 * back to the server's thread for the world through its {@link IOperationContext} and waits on whole game ticks,
 * never on a clock. The engine's values a server tunes are in {@link OperationBalance}.
 *
 * <p>How to add a kind of Operation, from zero, is in {@code core/docs/OPERATIONS.md}. Examples that compile are the
 * test mod's Operation for its accelerator ({@code tests/src/main/java/dev/jstech/tests/TestNetworkMembers.java}) and
 * J's Computers' own ({@code computers/src/main/java/dev/jstech/computers/operation/ComputingOperations.java}).
 */
package dev.jstech.core.operation;
