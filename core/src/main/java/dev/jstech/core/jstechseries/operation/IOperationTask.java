/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.jstechseries.operation;

import org.jetbrains.annotations.ApiStatus;

/**
 * The CPU-bound work of an Operation, executed on a virtual thread by the {@link OperationDispatch}.
 */
@ApiStatus.Experimental
@FunctionalInterface
public interface IOperationTask {

    IOperationResult run(IOperationContext context);
}
