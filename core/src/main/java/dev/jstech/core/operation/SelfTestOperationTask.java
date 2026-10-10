/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.operation;

/**
 * A diagnostic Operation that does a bounded amount of deterministic CPU-bound work on its virtual thread and then succeeds.
 * It is a dev-only load generator, so the loop does not poll {@link IOperationContext#isActive()}: a long run ends on
 * its own even if the dispatcher shuts down meanwhile.
 */
public record SelfTestOperationTask(int workUnits) implements IOperationTask {

    /* Written once per run so the JIT cannot discard the loop as dead code. */
    private static volatile long sink;

    public SelfTestOperationTask {
        if (workUnits < 0) {
            throw new IllegalArgumentException("workUnits must be >= 0; got " + workUnits);
        }
    }

    @Override
    public IOperationResult run(final IOperationContext context) {
        /*
         * Pure, allocation-free arithmetic over an immutable input; it never touches
         * the world, exactly as an Operation's Layer-A work must behave.
         */
        long accumulator = 0L;
        for (int i = 1; i <= workUnits; i++) {
            accumulator += (long) (Math.sqrt(i) * 1024.0) ^ (long) i;
        }
        sink = accumulator;
        return IOperationResult.success();
    }
}
