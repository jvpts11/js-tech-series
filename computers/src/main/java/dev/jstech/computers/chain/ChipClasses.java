/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Computers.
 */
package dev.jstech.computers.chain;

import dev.jstech.computers.JsComputers;
import dev.jstech.computers.registry.ComputingComponents;
import dev.jstech.core.machine.ProcessingRecipe;
import dev.jstech.core.palette.Palette;
import dev.jstech.core.palette.PaletteHolder;
import dev.jstech.core.palette.Palettes;
import dev.jstech.core.text.GameText;
import dev.jstech.core.text.TextHolder;
import dev.jstech.core.text.TextKey;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.item.crafting.RecipeManager;
import net.neoforged.neoforge.common.crafting.SizedIngredient;
import net.neoforged.neoforge.event.entity.player.ItemTooltipEvent;
import org.jetbrains.annotations.Nullable;

/**
 * The classes a chip's dies are sorted into at the Packaging Line, as a fab grades its dies (computers/docs/design/
 * survival.md, "How a chip is made"): A, the dies that tested best, down to F. Each model of a family takes its own
 * class of the chip that makes it, so no recipe's lot holds another's. A chip has as many classes as the recipes ask
 * for, read from the recipes themselves; a plain chip, with no class, goes to everything that only needs the chip.
 * The class is marked as testers mark a die, a dot of paint on the package's corner, each class its own colour.
 */
@TextHolder
@PaletteHolder
public final class ChipClasses {

    /** The most classes a chip's dies are sorted into: A to F. */
    public static final int MOST = 6;
    /** The item property a chip's model reads to paint its class's dot: 1 for a chip of a class, 0 for a plain one. */
    public static final ResourceLocation PROPERTY = ResourceLocation.fromNamespaceAndPath(JsComputers.MODID,
            "chip_class");
    /** Where a chip's dot is drawn, its paint and its shade, item/chip_class/<chip>_1 and _2. */
    public static final String MARKS = "chip_class";
    public static final TextKey CLASS_OF = TextKey.of("jsc.chip_class.of", "Class %s of %s");
    private static final TextKey GRADE_A = TextKey.of("jsc.chip_class.grade_a",
            "Of each wafer, the dies that tested best");
    private static final TextKey GRADE_B = TextKey.of("jsc.chip_class.grade_b",
            "Of each wafer, the second grade of die");
    private static final TextKey GRADE_C = TextKey.of("jsc.chip_class.grade_c",
            "Of each wafer, the third grade of die");
    private static final TextKey GRADE_D = TextKey.of("jsc.chip_class.grade_d",
            "Of each wafer, the fourth grade of die");
    private static final TextKey GRADE_E = TextKey.of("jsc.chip_class.grade_e",
            "Of each wafer, the fifth grade of die");
    private static final TextKey GRADE_F = TextKey.of("jsc.chip_class.grade_f",
            "Of each wafer, the sixth grade of die");
    private static final String LETTERS = "ABCDEF";
    /* Each class's paint and its shade, as the approved design painted the dots. */
    private static final Palette<Paint> PAINT = Palettes.declare(JsComputers.MODID, "item/chip_classes", new Paint(
            0xFF << 24 | 0xFFD640, 0xFF << 24 | 0xC49614, 0xFF << 24 | 0x58D6FF, 0xFF << 24 | 0x1E8CC8,
            0xFF << 24 | 0xFF6EAA, 0xFF << 24 | 0xBE326E, 0xFF << 24 | 0x82EC60, 0xFF << 24 | 0x3CA028,
            0xFF << 24 | 0xFF9640, 0xFF << 24 | 0xC45A14, 0xFF << 24 | 0xBA84FF, 0xFF << 24 | 0x7846C8));

    /* The recipes last read and how many classes each chip they take has; read again once the recipes change. */
    private static volatile @Nullable Counted counted;

    private ChipClasses() {
    }

    /** The class of {@code stack}, 1 to {@link #MOST} for A to F, or 0 for a plain chip and anything else. */
    public static int of(final ItemStack stack) {
        final Integer graded = stack.get(ComputingComponents.CHIP_CLASS.get());
        return graded == null ? 0 : graded;
    }

    /** {@code count} of {@code chip} of class {@code grade}, or plain ones for 0. */
    public static ItemStack stack(final Item chip, final int grade, final int count) {
        final ItemStack out = new ItemStack(chip, count);
        if (grade > 0) {
            out.set(ComputingComponents.CHIP_CLASS.get(), grade);
        }
        return out;
    }

    /** The letter of class {@code grade}, 1 to {@link #MOST}. */
    public static String letter(final int grade) {
        return String.valueOf(LETTERS.charAt(grade - 1));
    }

    /**
     * How many classes {@code chip}'s dies are sorted into: the highest class any of {@code recipes} takes of it, none
     * for a chip no recipe takes a class of.
     */
    public static int count(final Item chip, final RecipeManager recipes) {
        // The game swaps the whole collection when it reads its recipes again, on the server and on the client alike.
        final Collection<RecipeHolder<?>> all = recipes.getRecipes();
        Counted held = counted;
        if (held == null || held.recipes() != all) {
            held = new Counted(all, countAll(all));
            counted = held;
        }
        return held.classes().getOrDefault(chip, 0);
    }

    /** A classed chip's lines under its name: its class of how many, then which of each wafer's dies it is. */
    public static void onTooltip(final ItemTooltipEvent event) {
        final ItemStack stack = event.getItemStack();
        final int grade = of(stack);
        if (grade == 0) {
            return;
        }
        final List<Component> tooltip = event.getToolTip();
        final int at = Math.min(1, tooltip.size());
        final Player player = event.getEntity();
        final int of = player == null ? grade : Math.max(grade, count(stack.getItem(),
                player.level().getRecipeManager()));
        tooltip.add(at, GameText.component(grade(grade)).withStyle(ChatFormatting.GRAY));
        tooltip.add(at, GameText.component(CLASS_OF.with(letter(grade), of)).withColor(paint(grade, 1)));
    }

    /**
     * The colour of layer {@code layer} of a chip's dot of class {@code grade}: its paint for the first, its shade for
     * the second; none (white, the texture's own) for anything else.
     */
    public static int paint(final int grade, final int layer) {
        if (grade < 1 || grade > MOST || layer < 1 || layer > 2) {
            return 0xFF << 24 | 0xFFFFFF;
        }
        final Paint p = PAINT.get();
        final int[] colours = {p.a(), p.aShade(), p.b(), p.bShade(), p.c(), p.cShade(), p.d(), p.dShade(), p.e(),
                p.eShade(), p.f(), p.fShade()};
        return colours[(grade - 1) * 2 + layer - 1];
    }

    private static TextKey grade(final int grade) {
        return switch (grade) {
            case 1 -> GRADE_A;
            case 2 -> GRADE_B;
            case 3 -> GRADE_C;
            case 4 -> GRADE_D;
            case 5 -> GRADE_E;
            default -> GRADE_F;
        };
    }

    private static Map<Item, Integer> countAll(final Collection<RecipeHolder<?>> all) {
        final Map<Item, Integer> out = new HashMap<>();
        for (final RecipeHolder<?> holder : all) {
            if (!(holder.value() instanceof ProcessingRecipe recipe)) {
                continue;
            }
            for (final SizedIngredient input : recipe.inputs()) {
                for (final ItemStack shown : input.ingredient().getItems()) {
                    final int grade = of(shown);
                    if (grade > 0) {
                        out.merge(shown.getItem(), grade, Math::max);
                    }
                }
            }
        }
        return Map.copyOf(out);
    }

    /* The recipes read, and how many classes each chip they take has. */
    private record Counted(Collection<RecipeHolder<?>> recipes, Map<Item, Integer> classes) {
    }

    /* Each class's paint and its shade, A to F. */
    private record Paint(int a, int aShade, int b, int bShade, int c, int cShade, int d, int dShade, int e, int eShade,
                         int f, int fShade) {
    }
}
