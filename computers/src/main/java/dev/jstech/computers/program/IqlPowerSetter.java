/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Computers.
 */
package dev.jstech.computers.program;

import dev.jstech.computers.blockentity.MainframeBlockEntity;
import dev.jstech.computers.machine.IqlTables;
import dev.jstech.computers.program.iql.IqlPowerStatement;
import dev.jstech.core.text.TextHolder;
import dev.jstech.core.text.TextKey;
import dev.jstech.core.uuid.NetworkUuid;
import net.minecraft.server.level.ServerLevel;

/**
 * Applies an {@code UPDATE power} statement to the bays of the Mainframe's network: every bay of a rack with a PDU
 * whose row of the {@code power} table the WHERE keeps is switched on or off, as its PWR switch is thrown by hand
 * (computers/docs/design/servers-and-racks.md). A job runs it as typed, which is how servers are turned on and off by
 * rule. A machine the cabinet has no energy for, or whose supply failed, does not start, and is counted apart.
 */
@TextHolder
public final class IqlPowerSetter {

    private static final TextKey SWITCHED_ON = TextKey.of("jsc.service.iql.power_on", "%s bays switched on");
    private static final TextKey SWITCHED_OFF = TextKey.of("jsc.service.iql.power_off", "%s bays switched off");
    private static final TextKey SOME_REFUSED = TextKey.of("jsc.service.iql.power_refused",
            "%s bays switched on; %s did not start, short of energy or with a failed supply");
    private static final TextKey NO_BAYS = TextKey.of("jsc.service.iql.power_none",
            "no bay of a rack with a PDU on this network is so");

    private IqlPowerSetter() {
    }

    /** Switches the bays {@code statement} keeps on the Mainframe's network. */
    public static IqlEngine.Outcome apply(final MainframeBlockEntity mainframe, final IqlPowerStatement statement) {
        final NetworkUuid network = mainframe.networkUuid();
        if (!(mainframe.getLevel() instanceof ServerLevel level) || network == null) {
            return IqlEngine.Outcome.fail(NO_BAYS.text());
        }
        int kept = 0;
        int refused = 0;
        for (final IqlTables.PowerBay bay : IqlTables.powerBays(level, network)) {
            if (!IqlTables.keeps(statement.where(), bay.row())) {
                continue;
            }
            kept++;
            if (!bay.rack().setBayPower(bay.slot(), statement.on())) {
                refused++;
            }
        }
        if (kept == 0) {
            return IqlEngine.Outcome.fail(NO_BAYS.text());
        }
        if (refused > 0) {
            return IqlEngine.Outcome.fail(SOME_REFUSED.with(kept - refused, refused));
        }
        return IqlEngine.Outcome.ok((statement.on() ? SWITCHED_ON : SWITCHED_OFF).with(kept));
    }
}
