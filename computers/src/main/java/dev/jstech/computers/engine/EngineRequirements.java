/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Computers.
 */
package dev.jstech.computers.engine;

import dev.jstech.computers.blockentity.MainframeBlockEntity;
import dev.jstech.computers.os.ProgramRequirement;
import dev.jstech.computers.os.ProgramSpec;
import dev.jstech.computers.program.Programs;
import dev.jstech.core.text.Text;
import dev.jstech.core.text.TextHolder;
import dev.jstech.core.text.TextKey;
import java.util.List;
import org.jetbrains.annotations.Nullable;

/**
 * Whether a program finds what it needs of its network's engine, asked when the program is opened.
 *
 * <p>A house's tool written for one engine finds it only when that engine, in that version or a newer one, is the
 * one running; a portable program finds what it needs on any engine that offers the capabilities it names. A
 * program that does not find it stays installed, and says what it did not find.
 */
@TextHolder
public final class EngineRequirements {

    private static final TextKey NO_COMPATIBLE =
            TextKey.of("jsc.engine.no_compatible", "No compatible %s was found on this network.");
    private static final TextKey MISSING_CAPABILITIES =
            TextKey.of("jsc.engine.missing_capabilities", "%s needs an engine that offers %s.");

    private EngineRequirements() {
    }

    /**
     * Why {@code program} cannot be opened on the network whose Mainframe is {@code mainframe}, or {@code null}
     * when it finds what it needs there. A computer on no network with a Mainframe finds no engine at all.
     */
    @Nullable
    public static Text unmet(final ProgramSpec program, @Nullable final MainframeBlockEntity mainframe) {
        final ProgramRequirement needs = program.requires();
        if (needs.none()) {
            return null;
        }
        final INetworkEngine running = mainframe == null ? null : mainframe.runningEngine();
        if (needs.engine() != null) {
            final boolean found = running != null && running.def().program().equals(needs.engine())
                    && (needs.minVersion().isEmpty() || EngineVersions.atLeast(
                            mainframe.installedEngines().getOrDefault(needs.engine(), ""), needs.minVersion()));
            if (!found) {
                final ProgramSpec wanted = Programs.get(needs.engine());
                return NO_COMPATIBLE.with(wanted == null ? Text.literal(needs.engine().toString())
                        : wanted.name().text());
            }
        }
        if (!needs.capabilities().isEmpty()
                && (running == null || !running.def().extras().containsAll(needs.capabilities()))) {
            // The capabilities go by their own names, which are data a program is written against.
            final List<String> names = needs.capabilities().stream().map(Enum::name).sorted().toList();
            return MISSING_CAPABILITIES.with(program.name().text(), Text.literal(String.join(", ", names)));
        }
        return null;
    }
}
