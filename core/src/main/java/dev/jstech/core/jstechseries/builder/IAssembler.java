/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.jstechseries.builder;

import dev.jstech.core.jstechseries.operation.IItemNeeds;
import dev.jstech.core.jstechseries.operation.IOperationRequest;
import dev.jstech.core.jstechseries.operation.OperationStatus;
import org.jetbrains.annotations.ApiStatus;

/**
 * A machine of the network that builds a plan by itself, a crane over a rocket in its hangar or robot arms over a ship
 * in its dock, given the work as a request of {@link Assembly#ASSEMBLE}: its block entity implements this and carries
 * that kind out. It reports on the request as it builds: {@link OperationStatus#PROCESSING PROCESSING} while it lays
 * blocks, {@link OperationStatus#WAITING WAITING} while it waits for them (and says which as an {@link IItemNeeds}),
 * and {@link OperationStatus#COMPLETED COMPLETED} once the plan is whole.
 */
@ApiStatus.Experimental
public interface IAssembler {

    /**
     * Takes {@code request}, building the plan it names among the machine's own, or the plan the machine was given
     * already when it names none.
     *
     * @return the state the request is in now; the machine reports on it from then on
     */
    OperationStatus assemble(IOperationRequest<Assembly.Args> request);
}
