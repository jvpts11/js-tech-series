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
 * @param id     its short id, as {@code Operations.List} names it
 * @param status how it settled: done, partial, failed or discarded
 */
public record SettledOperation(String id, String type, String item, long moved, long requested, String status,
                               String priority) {

    public SettledOperation {
        id = id == null ? "" : id;
        type = type == null ? "" : type;
        item = item == null ? "" : item;
        status = status == null ? "" : status;
        priority = priority == null ? "" : priority;
    }
}
