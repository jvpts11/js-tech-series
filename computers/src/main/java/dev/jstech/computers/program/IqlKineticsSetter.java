/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Computers.
 */
package dev.jstech.computers.program;

import dev.jstech.computers.blockentity.MainframeBlockEntity;
import dev.jstech.computers.integration.create.KineticTable;
import dev.jstech.computers.machine.IqlStateTables;
import dev.jstech.computers.machine.IqlTables;
import dev.jstech.computers.program.iql.IqlKineticsStatement;
import dev.jstech.core.text.TextHolder;
import dev.jstech.core.text.TextKey;
import dev.jstech.core.uuid.NetworkUuid;
import java.util.Locale;
import net.minecraft.server.level.ServerLevel;

/**
 * Runs an {@code UPDATE create_kinetics} statement on the network of the Mainframe that runs it: every Rotation Speed
 * Controller of Create's that a computer of the network reads through the industrial control cable, whose row the WHERE
 * keeps, aims at the speed it gives (computers/docs/design/implementation.md, "Create"). Clutches and gearshifts are
 * no part of it: a Redstone Interface works them.
 */
@TextHolder
public final class IqlKineticsSetter {

    private static final TextKey SET = TextKey.of("jsc.service.iql.kinetics_set",
            "%s Rotation Speed Controllers aim at %s RPM");
    private static final TextKey SOME_NOT_CONTROLLERS = TextKey.of("jsc.service.iql.kinetics_not_controllers",
            "%s Rotation Speed Controllers aim at %s RPM; %s of the blocks kept are no Rotation Speed Controller");
    private static final TextKey NONE = TextKey.of("jsc.service.iql.kinetics_none",
            "no block of Create's this network's computers read is so");
    private static final TextKey NO_CREATE = TextKey.of("jsc.service.iql.kinetics_no_create",
            "the create_kinetics table is only there with Create");

    private IqlKineticsSetter() {
    }

    /** Sets the Rotation Speed Controllers {@code statement} keeps on the Mainframe's network. */
    public static IqlEngine.Outcome apply(final MainframeBlockEntity mainframe, final IqlKineticsStatement statement) {
        if (!KineticTable.declared()) {
            return IqlEngine.Outcome.fail(NO_CREATE.text());
        }
        final NetworkUuid network = mainframe.networkUuid();
        if (!(mainframe.getLevel() instanceof ServerLevel level) || network == null) {
            return IqlEngine.Outcome.fail(NONE.text());
        }
        int kept = 0;
        int set = 0;
        for (final KineticTable.Read read : KineticTable.read(level, network)) {
            if (!IqlTables.keeps(statement.where(), IqlStateTables.cellsOf(KineticTable.row(level, read)))) {
                continue;
            }
            kept++;
            if (read.values().write(KineticTable.TARGET_SPEED, statement.speed())) {
                set++;
            }
        }
        if (kept == 0) {
            return IqlEngine.Outcome.fail(NONE.text());
        }
        final String speed = speedOf(statement.speed());
        if (set < kept) {
            return IqlEngine.Outcome.fail(SOME_NOT_CONTROLLERS.with(set, speed, kept - set));
        }
        return IqlEngine.Outcome.ok(SET.with(set, speed));
    }

    /* A speed as a player wrote it: a whole number without its point. */
    private static String speedOf(final double speed) {
        return speed == Math.rint(speed) ? Long.toString((long) speed) : String.format(Locale.ROOT, "%.1f", speed);
    }
}
