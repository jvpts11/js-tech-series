/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Computers.
 */
package dev.jstech.computers.integration.jei;

import dev.jstech.computers.JsComputers;
import dev.jstech.computers.chain.machine.ChainFaces;
import dev.jstech.computers.chain.machine.ChainMachine;
import dev.jstech.computers.chain.machine.ChainMachineBlockEntity;
import dev.jstech.computers.chain.machine.ChainMachines;
import dev.jstech.computers.chain.machine.WaferPages;
import dev.jstech.computers.client.chain.ChainFaceScreens;
import dev.jstech.core.client.machine.MachineChrome;
import dev.jstech.core.client.machine.PictureFace;
import dev.jstech.core.client.machine.StandardFace;
import dev.jstech.core.machine.FacePicture;
import dev.jstech.core.machine.MachineScreenLayout;
import dev.jstech.core.machine.MachineScreenTexts;
import dev.jstech.core.machine.MachineTexts;
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
import mezz.jei.api.gui.builder.ITooltipBuilder;
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
 * looked at and looked up, and its liquids and gases fill the face's sight glasses, each glass saying on its tooltip
 * how much the step takes ({@link WaferPages}). One category a step: lithography, etching, implanting, deposition and
 * packaging.
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
    /* How full the glasses and how far up the meter's scale a still shows. */
    private static final double FULL = 0.6;
    /* How far round a sight glass its tooltip reaches: over its brackets. */
    private static final int GLASS_REACH_X = 3;
    private static final int GLASS_REACH_Y = 4;

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
        return MachineScreenLayout.FACE_WIDTH;
    }

    @Override
    public int getHeight() {
        return MachineScreenLayout.FACE_HEIGHT;
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
            builder.addSlot(role, port.x() + 1, port.y() + 1).addItemStacks(held);
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
                    0xFF << 24 | fill.material().colour(), FULL, StandardFace.written(fill.material()))));
        }
        final String name = picture.plate().map(ChainFaceScreens::translated).orElse(machine.english());
        final PictureFace.Still still = new PictureFace.Still(name, rating(machine), List.of(
                GameText.resolve(MachineScreenTexts.LCD_WORKING), GameText.resolve(MachineTexts.RECIPE_TIME.with(
                machine.seconds())).toUpperCase(Locale.ROOT)), FULL, fills);
        PictureFace.drawStill(g, font, 0, 0, ResourceLocation.fromNamespaceAndPath(JsComputers.MODID,
                "textures/gui/machine/face/" + machine.id() + ".png"), picture, still, ChainFaceScreens::translated);
    }

    @Override
    public void getTooltip(final ITooltipBuilder tooltip, final WaferPages.Page page, final IRecipeSlotsView slots,
                           final double mouseX, final double mouseY) {
        for (final FacePicture.Glass glass : ChainFaces.of(page.machine()).orElseThrow().glasses()) {
            final WaferPages.Fill fill = page.tanks().get(glass.tank());
            if (fill != null && MachineScreenLayout.on(mouseX, mouseY, glass.x() - GLASS_REACH_X,
                    glass.y() - GLASS_REACH_Y, glass.w() + 2 * GLASS_REACH_X, glass.h() + 2 * GLASS_REACH_Y)) {
                tooltip.add(GameText.component(MachineTexts.RECIPE_MATTER_IN.with(fill.amount(),
                        fill.material().name())));
            }
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
