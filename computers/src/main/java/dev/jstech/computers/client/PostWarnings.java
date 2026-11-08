/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Computers.
 */
package dev.jstech.computers.client;

import dev.jstech.computers.hardware.PsuCondition;
import dev.jstech.computers.hardware.PsuSpec;
import dev.jstech.computers.hardware.PsuTexts;
import dev.jstech.computers.operation.payload.FirmwareStatePayload;
import dev.jstech.core.text.GameText;
import dev.jstech.core.text.Text;
import dev.jstech.core.text.TextHolder;
import dev.jstech.core.text.TextKey;
import java.util.ArrayList;
import java.util.List;
import org.jetbrains.annotations.Nullable;

/**
 * What a machine's self-test warns of while it still boots, each in the voice of its firmware: the text BIOS a line,
 * the blue BIOS a WARNING! before it, the UEFI a box under its bar with a title and two lines. A warning only stays
 * written; the boot goes on. A supply that has failed never reaches the self-test: the machine does not start.
 */
@TextHolder
final class PostWarnings {

    static final TextKey CLI_DEGRADED = TextKey.of("jsc.post.cli_psu_degraded",
            "Warning: power supply degraded, %s%% worn");
    static final TextKey CLI_UNITS = TextKey.of("jsc.post.cli_psu_units",
            "Warning: redundant power supply, %s of %s units failed");
    static final TextKey BIOS_WARNING = TextKey.of("jsc.post.bios_warning", "WARNING! ");
    static final TextKey BIOS_DEGRADED = TextKey.of("jsc.post.bios_psu_degraded", "Power Supply degraded (%s%% worn)");
    static final TextKey BIOS_UNITS = TextKey.of("jsc.post.bios_psu_units",
            "Redundant Power Supply: %s of %s units failed");
    static final TextKey UEFI_DEGRADED = TextKey.of("jsc.post.uefi_psu_degraded", "Power supply degraded");
    static final TextKey UEFI_DEGRADED_LINE = TextKey.of("jsc.post.uefi_psu_degraded_line",
            "%s: %s%% worn. Replace it soon.");
    static final TextKey UEFI_UNITS = TextKey.of("jsc.post.uefi_psu_units",
            "Redundant power supply: %s of %s units failed");
    static final TextKey UEFI_UNITS_LINE = TextKey.of("jsc.post.uefi_psu_units_line",
            "%s: unit %s failed, unit %s carries the load.");
    static final TextKey UEFI_UNITS_ADVICE = TextKey.of("jsc.post.uefi_psu_units_advice", "Replace unit %s.");

    private PostWarnings() {
    }

    /**
     * One warning, in every firmware's words.
     *
     * @param cli        the text BIOS's line
     * @param bios       what the blue BIOS writes after its WARNING!
     * @param uefiTitle  the UEFI box's title
     * @param uefiLine   the box's first line
     * @param uefiAdvice the box's second line, empty when the first says it all
     */
    record Warning(Text cli, Text bios, Text uefiTitle, Text uefiLine, Text uefiAdvice) {
    }

    /** What the self-test of a machine with the power supply {@code psu} warns of, in order; none for a sound one. */
    static List<Warning> of(@Nullable final FirmwareStatePayload.Psu psu) {
        final List<Warning> warnings = new ArrayList<>();
        if (psu == null) {
            return warnings;
        }
        final PsuSpec spec = psu.spec();
        final PsuCondition condition = psu.condition();
        if (condition.failed(spec) || !condition.degraded(spec)) {
            return warnings;
        }
        final String supply = GameText.resolve(psu.name());
        final int failed = condition.failedUnits(spec);
        if (failed > 0) {
            final String gone = letters(spec, condition, true);
            warnings.add(new Warning(CLI_UNITS.with(failed, spec.units()), BIOS_UNITS.with(failed, spec.units()),
                    UEFI_UNITS.with(failed, spec.units()), UEFI_UNITS_LINE.with(supply, gone,
                    letters(spec, condition, false)), UEFI_UNITS_ADVICE.with(gone)));
        } else {
            final int worn = PsuTexts.percent(condition.worst(spec));
            warnings.add(new Warning(CLI_DEGRADED.with(worn), BIOS_DEGRADED.with(worn), UEFI_DEGRADED.text(),
                    UEFI_DEGRADED_LINE.with(supply, worn), Text.EMPTY));
        }
        return warnings;
    }

    /* The letters of the units that failed, or of those that carry on, joined. */
    private static String letters(final PsuSpec spec, final PsuCondition condition, final boolean failed) {
        final StringBuilder out = new StringBuilder();
        for (int unit = 0; unit < spec.units(); unit++) {
            if ((condition.wearOf(unit) >= 1f) == failed) {
                if (!out.isEmpty()) {
                    out.append(", ");
                }
                out.append(PsuTexts.letter(unit));
            }
        }
        return out.toString();
    }
}
