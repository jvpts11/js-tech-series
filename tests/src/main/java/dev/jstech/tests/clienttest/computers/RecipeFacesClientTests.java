/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Tech Series.
 */
package dev.jstech.tests.clienttest.computers;

import dev.jstech.computers.chain.machine.ChainMachines;
import dev.jstech.computers.chain.machine.WaferPages;
import dev.jstech.computers.client.chain.ChainRecipePages;
import dev.jstech.core.client.recipeview.ProcessingRecipeViews;
import dev.jstech.core.client.recipeview.RecipeFaces;
import dev.jstech.core.client.recipeview.ViewKinds;
import dev.jstech.core.integration.emi.CoreEmiPlugin;
import dev.jstech.core.integration.jei.CoreJeiPlugin;
import dev.jstech.core.machine.MachineScreenLayout;
import dev.jstech.core.machine.ProcessingKind;
import dev.jstech.core.machine.ProcessingRecipe;
import dev.jstech.core.recipeview.RecipeView;
import dev.jstech.core.recipeview.RecipeViewKind;
import dev.jstech.testkit.client.ClientTestHelper;
import dev.jstech.tests.clienttest.ClientTest;
import dev.jstech.tests.clienttest.ClientTestContext;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.neoforged.fml.ModList;

/**
 * The machines' own faces in the recipe viewers: each kind of a chain machine's work is laid on that machine's face,
 * every recipe of it in the face's ports and sight glasses; the Optical Polisher's fibres on its drawing tower; a
 * bench's kinds keep their plain pages; a page draws the machine at work, its loop playing, its words said for the
 * recipe on it; and the pages no recipe holds (a wafer's steps, the air separated, Mask Writing) are the Core's kinds.
 */
public final class RecipeFacesClientTests {

    private static final int SETTLE = 4;
    private static final int VIEWER_WAIT = 400;
    /* How often the drawn face is read, in ticks, and how many times: past a few of the crusher's frames. */
    private static final int SAMPLE_GAP = 3;
    private static final int SAMPLES = 6;
    /* Every other point of the face is read for its picture, nearly all of them drawn on. */
    private static final int STEP = 2;
    private static final double DRAWN_SHARE = 0.9;

    private RecipeFacesClientTests() {
    }

    @ClientTest(timeoutTicks = 600)
    public static void recipeFaces_layEveryRecipeOfEachChainMachinesKindOnItsFace(final ClientTestContext ctx) {
        ctx.thenWaitUntil(() -> ctx.mc().level != null, VIEWER_WAIT, "the client's world")
                .then(0, () -> {
                    final Set<ResourceLocation> faced = RecipeFaces.kinds();
                    for (final String machine : List.of("crusher", "chemical_reactor", "electrolytic_cell",
                            "optical_polisher", "induction_furnace")) {
                        ctx.assertTrue(faced.contains(kindOf(machine).id()), machine + "'s kind is on its face: "
                                + faced);
                    }
                    ctx.assertTrue(faced.stream().noneMatch(id -> id.getPath().endsWith("_electronics")),
                            "a bench's kinds keep their plain pages: " + faced);
                    final List<String> wrong = new ArrayList<>();
                    for (final ProcessingKind kind : ProcessingRecipeViews.kinds()) {
                        if (!faced.contains(kind.id())) {
                            continue;
                        }
                        for (final RecipeHolder<ProcessingRecipe> holder : recipes(ctx, kind)) {
                            if (RecipeFaces.of(kind.id(), holder.value()).isEmpty()) {
                                wrong.add(holder.id().toString());
                            }
                        }
                    }
                    ctx.assertTrue(wrong.isEmpty(), "every recipe of a faced kind fits its face: " + wrong);
                    final ProcessingKind polisher = kindOf("optical_polisher");
                    for (final RecipeHolder<ProcessingRecipe> holder : recipes(ctx, polisher)) {
                        final String drawn = RecipeFaces.of(polisher.id(), holder.value()).orElseThrow().frames()
                                .getPath();
                        final boolean fibre = holder.value().inputs().size() > 1;
                        ctx.assertTrue(drawn.endsWith(fibre ? "optical_polisher_fibre.png" : "optical_polisher.png"),
                                holder.id() + " is drawn on " + drawn);
                    }
                });
    }

    /*
     * The pages no recipe holds are the Core's kinds, for every viewer: a wafer's steps on their machines' faces, a
     * step's liquid in its sight glass as a fluid and its gases on the face; the Air Separation Unit's four gases given
     * out of its tanks, nothing taken; and Mask Writing's page with its rule and its three needs as bare icons.
     */
    @ClientTest(timeoutTicks = 600)
    public static void pages_ofTheWaferMasksAndAirAreTheCoresKinds(final ClientTestContext ctx) {
        ctx.thenWaitUntil(() -> ctx.mc().level != null, VIEWER_WAIT, "the client's world")
                .then(0, () -> {
                    for (final WaferPages.Step step : WaferPages.Step.values()) {
                        ctx.assertTrue(ChainRecipePages.step(step).map(kind -> !kind.recipes(ctx.mc().level)
                                .isEmpty()).orElse(false), step + " is a kind with its pages");
                    }
                    final RecipeViewKind<WaferPages.Page> etch = ChainRecipePages.step(WaferPages.Step.ETCH)
                            .orElseThrow();
                    final RecipeView acid = etch.view(etch.recipes(ctx.mc().level).getFirst());
                    ctx.assertTrue(acid.face().isPresent() && acid.slots(RecipeView.Role.INPUT).stream()
                            .anyMatch(RecipeView.Slot::holdsFluid), "the Vintage's etching on its face, its acid a "
                            + "fluid in its glass: " + acid.slots());
                    final RecipeView plasma = etch.view(etch.recipes(ctx.mc().level).getLast());
                    ctx.assertTrue(plasma.face().map(face -> face.matter().size() == 2).orElse(false),
                            "the Advanced's chlorine and argon on its face: " + plasma.face());
                    final RecipeViewKind<?> air = ViewKinds.all().stream().filter(kind -> kind.id().equals(
                            ResourceLocation.fromNamespaceAndPath("jsc", "air_separation"))).findFirst()
                            .orElseThrow(() -> new IllegalStateException("no air separation kind"));
                    final RecipeView gases = first(air, ctx);
                    ctx.assertTrue(gases.slots().isEmpty() && gases.face().map(face -> face.matter().size() == 4
                            && face.matter().keySet().stream().noneMatch(tank -> RecipeFaces.taking(gases, tank)
                            .orElse(true))).orElse(false), "four gases given out, nothing taken: " + gases.face());
                    final RecipeViewKind<ChainRecipePages.MaskPage> masks = ChainRecipePages.maskWriting()
                            .orElseThrow();
                    final RecipeView mask = masks.view(masks.recipes(ctx.mc().level).getFirst());
                    ctx.assertTrue(mask.rules().size() == 1 && mask.slots(RecipeView.Role.CATALYST).size() == 3
                            && mask.slots(RecipeView.Role.CATALYST).stream().noneMatch(RecipeView.Slot::framed),
                            "Mask Writing's rule and its three needs as bare icons: " + mask);
                });
    }

    @ClientTest(timeoutTicks = 600)
    public static void recipeFaces_drawTheMachineAtWorkItsLoopPlaying(final ClientTestContext ctx) {
        final List<Long> pictures = new ArrayList<>();
        final RecipeView[] laid = new RecipeView[1];
        ctx.afterTest(() -> ctx.mc().setScreen(null));
        ctx.thenWaitUntil(() -> ctx.mc().level != null, VIEWER_WAIT, "the client's world")
                .then(0, () -> {
                    final ProcessingKind crusher = kindOf("crusher");
                    final RecipeHolder<ProcessingRecipe> holder = recipes(ctx, crusher).stream()
                            .filter(each -> !each.value().byproducts().isEmpty()).findFirst().orElseThrow();
                    final RecipeFaces.RecipeFace face = RecipeFaces.of(crusher.id(), holder.value()).orElseThrow();
                    final RecipeView.Builder page = new RecipeView.Builder();
                    RecipeFaces.lay(face, holder.value(), page);
                    laid[0] = page.build();
                    final Optional<RecipeView.Face> on = laid[0].face();
                    ctx.assertTrue(on.isPresent(), "the page is laid on the crusher's face");
                    final String chance = on.get().said().get("{byproduct}");
                    ctx.assertTrue(chance != null && chance.trim().endsWith("%") && !chance.contains("-"),
                            "the face says the recipe's byproduct chance: " + chance);
                    ctx.assertTrue(laid[0].slots(RecipeView.Role.OUTPUT).size() == holder.value().outputs().size()
                            + holder.value().byproducts().size(), "its outputs and byproducts lie in its ports");
                    ctx.mc().setScreen(new FaceSheet(laid[0]));
                })
                .thenScreenshot(SETTLE, "recipe-face-crusher");
        for (int k = 0; k < SAMPLES; k++) {
            ctx.then(SAMPLE_GAP, () -> pictures.add(picture(ctx)));
        }
        ctx.then(0, () -> {
            ctx.assertTrue(new HashSet<>(pictures).size() > 1, "the face's loop plays: " + pictures);
            try (ClientTestHelper.FrameSampler frame = ctx.sampleFrame()) {
                int drawn = 0;
                int read = 0;
                for (int y = 0; y < MachineScreenLayout.FACE_HEIGHT; y += STEP) {
                    for (int x = 0; x < MachineScreenLayout.FACE_WIDTH; x += STEP) {
                        read++;
                        drawn += frame.pixel(FaceSheet.LEFT + x, FaceSheet.TOP + y) != FaceSheet.GROUND ? 1 : 0;
                    }
                }
                ctx.assertTrue(drawn > read * DRAWN_SHARE, "the face is drawn over its whole page: " + drawn
                        + " of " + read);
            }
        });
    }

    @ClientTest(timeoutTicks = 900)
    public static void viewer_showsChainMachinesRecipesOnTheirFaces(final ClientTestContext ctx) {
        if (!ModList.get().isLoaded("jei") && !emi()) {
            ctx.then(0, () -> { });
            return;
        }
        ctx.afterTest(() -> ctx.mc().setScreen(null));
        for (final String machine : List.of("crusher", "chemical_reactor", "electrolytic_cell", "optical_polisher")) {
            final ResourceLocation kind = kindOf(machine).id();
            ctx.thenWaitUntil(() -> shown(kind) > 0, VIEWER_WAIT, "the viewer's pages of " + kind)
                    .then(0, () -> ctx.assertTrue(show(kind), "the viewer opens on " + kind))
                    .thenWaitUntil(() -> ctx.mc().screen != null, VIEWER_WAIT, "the viewer's recipe screen")
                    .thenScreenshot(SETTLE * 2, "viewer-face-" + machine)
                    .then(SETTLE, () -> ctx.mc().setScreen(null));
        }
    }

    /* The kind the chain machine {@code id} works. */
    /* The page of the first recipe {@code kind} lists in the client's world. */
    private static <T> RecipeView first(final RecipeViewKind<T> kind, final ClientTestContext ctx) {
        return kind.view(kind.recipes(ctx.mc().level).getFirst());
    }

    private static ProcessingKind kindOf(final String id) {
        return ChainMachines.workedBy(ChainMachines.byId(id).orElseThrow()).getFirst().kind();
    }

    private static List<RecipeHolder<ProcessingRecipe>> recipes(final ClientTestContext ctx,
                                                                final ProcessingKind kind) {
        return ctx.mc().level.getRecipeManager().getAllRecipesFor(kind.type());
    }

    /* What the face drawn on the sheet looks like now, as one number. */
    private static long picture(final ClientTestContext ctx) {
        long hash = 1;
        try (ClientTestHelper.FrameSampler frame = ctx.sampleFrame()) {
            for (int y = 0; y < MachineScreenLayout.FACE_HEIGHT; y += STEP) {
                for (int x = 0; x < MachineScreenLayout.FACE_WIDTH; x += STEP) {
                    hash = 31 * hash + frame.pixel(FaceSheet.LEFT + x, FaceSheet.TOP + y);
                }
            }
        }
        return hash;
    }

    private static boolean emi() {
        return ModList.get().isLoaded("emi");
    }

    private static int shown(final ResourceLocation kind) {
        return (emi() ? CoreEmiPlugin.recipesShown(kind) : CoreJeiPlugin.recipesShown(kind)).orElse(0);
    }

    private static boolean show(final ResourceLocation kind) {
        return emi() ? CoreEmiPlugin.showRecipes(kind) : CoreJeiPlugin.showRecipes(kind);
    }

    /* A grey sheet with one recipe's page on its machine's face, its top left at a known place. */
    private static final class FaceSheet extends Screen {

        private final RecipeView page;

        private static final int LEFT = 4;
        private static final int TOP = 4;
        private static final int GROUND = 0xFF8B8B8B;

        private FaceSheet(final RecipeView page) {
            super(Component.empty());
            this.page = page;
        }

        @Override
        public void render(final GuiGraphics g, final int mouseX, final int mouseY, final float partialTick) {
            g.fill(0, 0, this.width, this.height, GROUND);
            RecipeFaces.draw(g, this.page, LEFT, TOP);
        }
    }
}
