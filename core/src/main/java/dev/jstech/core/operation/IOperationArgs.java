/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.operation;

/**
 * What one request of a kind of Operation carries: the item to look for, how many to make, where to put them.
 *
 * <p>Each kind of Operation has a record of its own that implements this, named by its {@link OperationType}, and
 * the kind's handler is handed exactly that record. The interface has nothing in it; it only marks which records
 * are arguments of an Operation, so a handler can never be handed the arguments of another kind by mistake.
 *
 * <pre>{@code
 * public record CountItemsArgs(String item) implements IOperationArgs {
 * }
 * }</pre>
 */
public interface IOperationArgs {
    // Marker interface, no methods required.
}
