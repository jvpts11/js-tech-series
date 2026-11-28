/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Computers.
 */
package dev.jstech.computers.vm.program;

/**
 * An Operation of the network that has settled, as a program hears it through {@code Operations.OnSettled}: the same
 * facts {@code Operations.Get} gives of one still in flight, each written as {@code OperationInfo} has it.
 *
 * @param id      its short id, as {@code Operations.List} names it
 * @param status  how it settled: done, partial, failed or discarded
 * @param cause   why it fell short, as the operations log says it; empty where it did not, or did not say
 * @param waited  the ticks it waited in the queue
 * @param ran     the ticks it ran
 * @param settled the game tick it settled at
 * @param by      who asked for it, as the operations log names them
 */
public record SettledOperation(String id, String type, String item, long moved, long requested, String status,
                               String priority, String cause, long waited, long ran, long settled, String by) {

    public SettledOperation {
        id = id == null ? "" : id;
        type = type == null ? "" : type;
        item = item == null ? "" : item;
        status = status == null ? "" : status;
        priority = priority == null ? "" : priority;
        cause = cause == null ? "" : cause;
        by = by == null ? "" : by;
    }
}
