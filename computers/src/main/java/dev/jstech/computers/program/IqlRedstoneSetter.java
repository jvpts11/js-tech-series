/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Computers.
 */
package dev.jstech.computers.program;

import dev.jstech.computers.blockentity.RedstoneInterfaceBlockEntity;
import dev.jstech.computers.program.cli.ICliComputer;
import dev.jstech.computers.program.iql.IqlRedstoneStatement;
import dev.jstech.core.peripheral.IPeripheralOwner;
import dev.jstech.core.text.TextHolder;
import dev.jstech.core.text.TextKey;
import net.minecraft.server.level.ServerLevel;

/**
 * Carries out a {@code SET REDSTONE} statement on the machine that runs it: the Redstone Interface linked to it that
 * answers to the name reads, or emits the strength, marked with whoever asked, as a program marks it. A machine with
 * no interface answering to the name refuses, naming it.
 */
@TextHolder
public final class IqlRedstoneSetter {

    private static final TextKey READS = TextKey.of("jsc.service.iql.redstone_reads", "Redstone Interface %s reads");
    private static final TextKey EMITS = TextKey.of("jsc.service.iql.redstone_emits",
            "Redstone Interface %s emits %s");
    private static final TextKey NONE = TextKey.of("jsc.service.iql.redstone_missing",
            "no Redstone Interface named %s is linked to this machine");

    private IqlRedstoneSetter() {
    }

    /**
     * Sets the interface {@code statement} names among those linked to {@code machine}.
     *
     * @param by what the setting is marked with: the job that ran it, or {@link IqlBusSetter#TYPED} for a statement
     *           typed at a prompt
     */
    public static ICliComputer.OpResult apply(final ServerLevel level, final Object machine,
                                              final IqlRedstoneStatement statement, final String by) {
        final RedstoneInterfaceBlockEntity sensor = machine instanceof IPeripheralOwner owner
                ? RedstoneInterfaceBlockEntity.linkedTo(level, owner, statement.target()) : null;
        if (sensor == null) {
            return ICliComputer.OpResult.fail(NONE.with(statement.target()));
        }
        if (statement.emits()) {
            sensor.emit(statement.strength(), by);
            return ICliComputer.OpResult.ok(EMITS.with(sensor.answersTo(), statement.strength()));
        }
        sensor.read(by);
        return ICliComputer.OpResult.ok(READS.with(sensor.answersTo()));
    }
}
