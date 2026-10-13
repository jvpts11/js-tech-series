/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Tech Series.
 */
package dev.jstech.tests.clienttest;

import dev.jstech.core.audio.SoundMixerTexts;
import dev.jstech.core.cable.Cables;
import dev.jstech.core.client.diagnostic.DiagnosticsClient;
import dev.jstech.core.energy.LevelEnergy;
import dev.jstech.core.text.GameText;
import dev.jstech.industrial.IndustrialModule;
import java.util.List;
import net.minecraft.core.BlockPos;

/**
 * The series on the game's debug screen: the sound system's panel under the Sound Mixer's name, and the time the
 * energy grid takes to move a tick's energy among the pieces of work timed.
 */
public final class DiagnosticsClientTests {

    private static final BlockPos CABLE = new BlockPos(0, 2, 3);

    private DiagnosticsClientTests() {
    }

    @ClientTest(timeoutTicks = 200)
    public static void debugScreen_showsThePanelsAndTheTimings(final ClientTestContext ctx) {
        ctx.thenServer(0, level -> Cables.lay(level, ctx.abs(CABLE), IndustrialModule.ENERGY_CABLE.get()))
                .thenWaitUntil(() -> DiagnosticsClient.lines().stream()
                        .anyMatch(line -> line.startsWith(LevelEnergy.TIMING + ": ")), 60,
                        "the energy grid's time on the debug screen")
                .thenAssert(0, () -> {
                    final List<String> lines = DiagnosticsClient.lines();
                    final String mixer = GameText.resolve(SoundMixerTexts.TITLE);
                    return lines.stream().anyMatch(line -> line.endsWith(mixer))
                            && lines.stream().anyMatch(line -> line.startsWith("Running: "));
                }, "the sound system's panel under the Sound Mixer's name")
                .thenServer(0, level -> level.removeBlock(ctx.abs(CABLE), false));
    }
}
