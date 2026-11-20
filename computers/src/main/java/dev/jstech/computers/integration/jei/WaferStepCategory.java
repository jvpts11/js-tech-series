/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Computers.
 */
package dev.jstech.computers.integration.jei;

import dev.jstech.computers.JsComputers;
import dev.jstech.computers.chain.machine.ChainFaceWord;
import dev.jstech.computers.chain.machine.ChainFaces;
import dev.jstech.computers.chain.machine.ChainMachine;
import dev.jstech.computers.chain.machine.ChainMachineBlockEntity;
import dev.jstech.computers.chain.machine.ChainMachines;
import dev.jstech.computers.chain.machine.WaferPages;
import dev.jstech.core.client.machine.MachineArt;
import dev.jstech.core.client.machine.MachineChrome;
import dev.jstech.core.client.machine.PictureFace;
import dev.jstech.core.machine.FacePicture;
import dev.jstech.core.machine.MachineScreenLayout;
import dev.jstech.core.machine.MachineScreenTexts;
import dev.jstech.core.material.Material;
import dev.jstech.core.material.MaterialProperties;
import dev.jstech.core.text.GameText;
import dev.jstech.core.text.TextKey;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import mezz.jei.api.gui.builder.IRecipeLayoutBuilder;
import mezz.jei.api.gui.drawable.IDrawable;
import mezz.jei.api.gui.ingredient.IRecipeSlotsView;
import mezz.jei.api.helpers.IGuiHelper;
import mezz.jei.api.recipe.IFocusGroup;
import mezz.jei.api.recipe.RecipeIngredientRole;
import mezz.jei.api.recipe.RecipeType;
import mezz.jei.api.recipe.category.IRecipeCategory;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.ItemLike;
import org.jetbrains.annotations.Nullable;

/**
 * A wafer's steps in JEI (computers/docs/design/survival.md, "How a chip is made"), each page the step running on its
 * machine's own face: the face plays its loop, the step's items lie in the face's own slots, still, for each to be
 * looked at and looked up, its liquids and gases fill the face's sight glasses, and three lines under it say the layer
 * and the next step, the time and what it uses, and what else the machine needs ({@link WaferPages}). One category a
 * step: lithography, etching, implanting, deposition and packaging.
 */
final class WaferStepCategory implements IRecipeCategory<WaferPages.Page> {

    private final WaferPages.Step step;
    private final IDrawable icon;
    /* Each machine's block entity, made once, to read its class plate from. */
    private final Map<ChainMachine, String> ratings = new HashMap<>();
    /* Each page's name, by its place in the pages, as JEI asks each page's. */
    private final Map<WaferPages.Page, Integer> numbers = new IdentityHashMap<>();

    /** The JEI recipe type of each step. */
    static final Map<WaferPages.Step, RecipeType<WaferPages.Page>> TYPES = types();
    /* The face, a pixel in from the page's corner; the rule and the lines under it. */
    private static final int FACE = 1;
    private static final int RULE_Y = FACE + MachineScreenLayout.FACE_HEIGHT + 3;
    private static final int LINES_Y = RULE_Y + 4;
    private static final int LINE = 10;
    private static final int WIDTH = MachineScreenLayout.FACE_WIDTH + 2 * FACE;
    private static final int HEIGHT = LINES_Y + 3 * LINE;
    /* How full the glasses and how far up the meter's scale a still shows. */
    private static final double FULL = 0.6;

    /** The category of {@code step}, whose pages are {@code pages}, numbered in their order for their names. */
    WaferStepCategory(final IGuiHelper gui, final WaferPages.Step step, final List<WaferPages.Page> pages) {
        this.step = step;
        this.icon = gui.createDrawableItemStack(new ItemStack(ChainMachines.terminalOf(ChainMachines.byId(
                iconMachine(step)).orElseThrow()).get()));
        for (int i = 0; i < pages.size(); i++) {
            this.numbers.put(pages.get(i), i);
        }
    }

    /** Every page of every step, by its step. */
    static Map<WaferPages.Step, List<WaferPages.Page>> pages() {
        final Map<WaferPages.Step, List<WaferPages.Page>> out = new EnumMap<>(WaferPages.Step.class);
        for (final WaferPages.Step each : WaferPages.Step.values()) {
            out.put(each, new ArrayList<>());
        }
        for (final WaferPages.Page page : WaferPages.all()) {
            out.get(page.step()).add(page);
        }
        return out;
    }

    /** The machines {@code pages} are worked on, which show them from their terminals. */
    static List<ItemLike> machines(final List<WaferPages.Page> pages) {
        final List<ItemLike> out = new ArrayList<>();
        for (final WaferPages.Page page : pages) {
            final ItemLike terminal = ChainMachines.terminalOf(page.machine()).get();
            if (!out.contains(terminal)) {
                out.add(terminal);
            }
        }
        return out;
    }

    @Override
    public RecipeType<WaferPages.Page> getRecipeType() {
        return TYPES.get(this.step);
    }

    @Override
    public Component getTitle() {
        return GameText.component(title(this.step));
    }

    @Override
    public IDrawable getIcon() {
        return this.icon;
    }

    @Override
    public int getWidth() {
        return WIDTH;
    }

    @Override
    public int getHeight() {
        return HEIGHT;
    }

    @Override
    public void setRecipe(final IRecipeLayoutBuilder builder, final WaferPages.Page page, final IFocusGroup focuses) {
        final FacePicture picture = ChainFaces.of(page.machine()).orElseThrow();
        for (final FacePicture.Port port : picture.ports()) {
            final List<ItemStack> held = page.slots().get(port.slot());
            if (held == null || held.isEmpty()) {
                continue;
            }
            // A port's slot is an 18-pixel recess; the item stands a pixel in from its corner.
            final RecipeIngredientRole role = page.machine().layout().isOutput(port.slot())
                    ? RecipeIngredientRole.OUTPUT : RecipeIngredientRole.INPUT;
            builder.addSlot(role, FACE + port.x() + 1, FACE + port.y() + 1).addItemStacks(held);
        }
    }

    @Override
    public void draw(final WaferPages.Page page, final IRecipeSlotsView slots, final GuiGraphics g,
                     final double mouseX, final double mouseY) {
        final Font font = Minecraft.getInstance().font;
        final ChainMachine machine = page.machine();
        final FacePicture picture = ChainFaces.of(machine).orElseThrow();
        final List<Optional<PictureFace.Fill>> fills = new ArrayList<>();
        for (final FacePicture.Glass glass : picture.glasses()) {
            fills.add(Optional.ofNullable(page.tanks().get(glass.tank())).map(fill -> new PictureFace.Fill(
                    0xFF << 24 | fill.material().colour(), FULL, symbol(fill.material()))));
        }
        final String name = picture.plate().map(WaferStepCategory::say).orElse(machine.english());
        final PictureFace.Still still = new PictureFace.Still(name, rating(machine), List.of(
                GameText.resolve(MachineScreenTexts.LCD_WORKING), GameText.resolve(JeiTexts.SECONDS.with(
                machine.seconds()))), FULL, fills);
        PictureFace.drawStill(g, font, FACE, FACE, ResourceLocation.fromNamespaceAndPath(JsComputers.MODID,
                "textures/gui/machine/face/" + machine.id() + ".png"), picture, still, WaferStepCategory::say);
        // The pages' ink is the viewer's other pages' of the chain, Mask Writing's.
        final MaskWritingLook.Colours ink = MaskWritingLook.PALETTE.get();
        g.fill(FACE, RULE_Y, WIDTH - FACE, RULE_Y + 1, ink.rule());
        for (int k = 0; k < page.lines().size(); k++) {
            MachineArt.text(g, font, GameText.resolve(page.lines().get(k)), FACE + 2, LINES_Y + k * LINE,
                    k == 0 ? ink.text() : ink.dim());
        }
    }

    @Override
    public @Nullable ResourceLocation getRegistryName(final WaferPages.Page page) {
        final Integer number = this.numbers.get(page);
        return number == null ? null : ResourceLocation.fromNamespaceAndPath(JsComputers.MODID, "wafer/"
                + this.step.name().toLowerCase(Locale.ROOT) + "/" + number);
    }

    /* What a machine's class plate says, read from a block entity of it made once. */
    private String rating(final ChainMachine machine) {
        return this.ratings.computeIfAbsent(machine, each -> {
            final ChainMachineBlockEntity made = ChainMachines.typeOf(each).create(BlockPos.ZERO,
                    ChainMachines.terminalOf(each).get().defaultBlockState());
            return made == null ? "" : MachineChrome.rating(made);
        });
    }

    /* A word of a face, translated: its stencilled words and its name plate. */
    private static String say(final String written) {
        final Optional<ChainFaceWord> whole = ChainFaceWord.of(written);
        if (whole.isPresent()) {
            return GameText.resolve(whole.get().text().text());
        }
        final String[] words = written.split(" ", -1);
        for (int i = 0; i < words.length; i++) {
            final Optional<ChainFaceWord> word = ChainFaceWord.of(words[i]);
            if (word.isPresent()) {
                words[i] = GameText.resolve(word.get().text().text());
            }
        }
        return String.join(" ", words);
    }

    /* A material as short as a panel writes it: its formula or its symbol, else its name. */
    private static String symbol(final Material material) {
        return material.value(MaterialProperties.FORMULA).or(() -> material.value(MaterialProperties.SYMBOL))
                .orElseGet(() -> GameText.resolve(material.name()).toUpperCase(Locale.ROOT));
    }

    private static TextKey title(final WaferPages.Step step) {
        return switch (step) {
            case LITHOGRAPHY -> JeiTexts.LITHOGRAPHY;
            case ETCH -> JeiTexts.ETCH;
            case IMPLANT -> JeiTexts.IMPLANT;
            case DEPOSITION -> JeiTexts.DEPOSITION;
            case PACKAGING -> JeiTexts.PACKAGING;
        };
    }

    /* The machine whose terminal is a step's icon. */
    private static String iconMachine(final WaferPages.Step step) {
        return switch (step) {
            case LITHOGRAPHY -> "lithography_legacy";
            case ETCH -> "plasma_etcher";
            case IMPLANT -> "ion_implanter";
            case DEPOSITION -> "deposition_chamber";
            case PACKAGING -> "packaging_line";
        };
    }

    private static Map<WaferPages.Step, RecipeType<WaferPages.Page>> types() {
        final Map<WaferPages.Step, RecipeType<WaferPages.Page>> out = new EnumMap<>(WaferPages.Step.class);
        for (final WaferPages.Step each : WaferPages.Step.values()) {
            out.put(each, RecipeType.create(JsComputers.MODID, "wafer_" + each.name().toLowerCase(Locale.ROOT),
                    WaferPages.Page.class));
        }
        return out;
    }
}
