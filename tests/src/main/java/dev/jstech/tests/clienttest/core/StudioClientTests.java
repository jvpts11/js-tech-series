/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Tech Series.
 */
package dev.jstech.tests.clienttest.core;

import dev.jstech.core.client.studio.BenchState;
import dev.jstech.core.client.studio.StudioClient;
import dev.jstech.core.client.studio.StudioScreen;
import dev.jstech.core.client.studio.StudioSheet;
import dev.jstech.core.client.studio.ThingKind;
import dev.jstech.tests.TestKitMachine;
import dev.jstech.tests.clienttest.ClientTest;
import dev.jstech.tests.clienttest.ClientTestContext;
import java.util.List;
import net.minecraft.resources.ResourceLocation;

/**
 * The studio: every mod's things by kind, a machine chosen on the bench in its states, what it is made of, each part
 * with its file and its state, and the checks across its parts.
 */
public final class StudioClientTests {

    private static final int WAIT = 60;
    private static final int SETTLE = 3;
    /* The studio reads every thing as it opens, and the ticks it held up then run with no frame between. */
    private static final int SHOT = 20;
    /* A machine of the test mod's, drawn from a states file and named in English. */
    private static final ResourceLocation MACHINE = TestKitMachine.BLOCK.getId();

    private StudioClientTests() {
    }

    @ClientTest(timeoutTicks = 400)
    public static void studio_showsWhatAMachineIsMadeOfOnItsBench(final ClientTestContext ctx) {
        ctx.afterTest(() -> ctx.mc().setScreen(null));
        ctx.then(0, StudioClient::open)
                .thenAwaitScreen(StudioScreen.class, WAIT)
                .then(SETTLE, () -> {
                    final StudioScreen studio = ctx.screen(StudioScreen.class);
                    ctx.assertTrue(studio.choose(MACHINE), "the test mod's machine is among the things");
                    ctx.assertTrue(studio.chosen().filter(thing -> thing.kind() == ThingKind.MACHINES).isPresent(),
                            "a block whose block entity is a machine's is under the machines");
                    final StudioSheet sheet = studio.sheet().orElseThrow();
                    final List<String> parts = sheet.parts().stream().map(StudioSheet.Part::name).toList();
                    ctx.assertTrue(parts.containsAll(List.of("Model", "Names", "Recipe", "Tags", "Drops", "Tests")),
                            "it is made of its model, names, recipe, tags, drops and tests: " + parts);
                    final StudioSheet.Part model = part(sheet, "Model");
                    ctx.assertTrue(model.detail().equals("blockstates/" + MACHINE.getPath() + ".json")
                                    && model.mark() == StudioSheet.Mark.FINE,
                            "its model is its states file, there: " + model);
                    final StudioSheet.Part names = part(sheet, "Names");
                    ctx.assertTrue(names.detail().contains("en_us") && names.mark() == StudioSheet.Mark.FINE,
                            "its name is in every language of its mod: " + names);
                    ctx.assertTrue(part(sheet, "Drops").detail().equals("itself"),
                            "it drops itself: " + part(sheet, "Drops"));
                    ctx.assertTrue(sheet.checks().stream().noneMatch(check -> check.mark()
                            == StudioSheet.Mark.ERROR), "nothing across its parts is wrong: " + sheet.checks());
                    ctx.assertTrue(studio.problems() >= sheet.checks().size(),
                            "the checks count every thing's problems");
                })
                .thenScreenshot(SHOT, "studio-working")
                .then(0, () -> ctx.screen(StudioScreen.class).showState(BenchState.JAMMED))
                .thenScreenshot(SHOT, "studio-jammed");
    }

    private static StudioSheet.Part part(final StudioSheet sheet, final String name) {
        return sheet.parts().stream().filter(part -> part.name().equals(name)).findFirst().orElseThrow();
    }
}
