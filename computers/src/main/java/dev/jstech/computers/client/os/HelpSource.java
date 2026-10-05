/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Computers.
 */
package dev.jstech.computers.client.os;

import dev.jstech.computers.gui.help.HelpCommand;
import dev.jstech.computers.gui.help.HelpTexts;
import dev.jstech.computers.gui.help.IHelpSource;
import dev.jstech.core.client.guide.GuideRecipes;
import dev.jstech.core.guide.ManualReader;
import dev.jstech.core.text.GameText;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import net.minecraft.core.BlockPos;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.Nullable;

/**
 * What a help program of a machine reads from on the player's side: the manuals of the player's game, and the
 * machine's commands and their pages as the machine sends them, each page asked for once.
 */
final class HelpSource implements IHelpSource, HelpAnswers.IListener {

    @Nullable
    private final BlockPos machine;
    /** Told when the machine has answered, so whoever shows the help lays it out again. */
    private final Runnable onAnswer;
    private final Map<String, List<String>> pages = new HashMap<>();
    private final Set<String> asked = new HashSet<>();
    private final GuideRecipes recipes = new GuideRecipes();
    private List<HelpCommand> commands = List.of();

    /** Starts listening, and asks the machine for its commands. */
    HelpSource(@Nullable final BlockPos machine, final Runnable onAnswer) {
        this.machine = machine;
        this.onAnswer = onAnswer;
        HelpAnswers.listen(this);
        if (machine != null) {
            HelpAnswers.ask(machine, "");
        }
    }

    /** Stops listening, when the help program is put away. */
    void close() {
        HelpAnswers.stop(this);
    }

    /** Listens again and asks the machine afresh, for a window come back after its desktop was away. */
    void refresh() {
        HelpAnswers.listen(this);
        this.pages.clear();
        this.asked.clear();
        if (this.machine != null) {
            HelpAnswers.ask(this.machine, "");
        }
    }

    @Override
    public List<ManualReader> manuals() {
        return HelpBooks.all();
    }

    @Override
    public List<HelpCommand> commands() {
        return this.commands;
    }

    @Override
    @Nullable
    public List<String> commandPage(final String name) {
        final String key = name.toLowerCase(Locale.ROOT);
        final List<String> page = this.pages.get(key);
        if (page == null && this.machine != null && this.asked.add(key)) {
            HelpAnswers.ask(this.machine, name);
        }
        return page;
    }

    @Override
    public List<String> recipeLines(final String type, final String output) {
        return recipeLines(this.recipes, type, output);
    }

    /** The recipes of that type making that item, as a window draws them, with their items. */
    public List<GuideRecipes.View> recipeViews(final String type, final String output) {
        return this.recipes.of(type, output);
    }

    /** One line per recipe of that type making that item: what goes in, what comes out, and its time and energy. */
    static List<String> recipeLines(final GuideRecipes recipes, final String type, final String output) {
        final List<String> lines = new ArrayList<>();
        for (final GuideRecipes.View view : recipes.of(type, output)) {
            final List<String> inputs = new ArrayList<>();
            for (final List<ItemStack> choices : view.inputs()) {
                if (!choices.isEmpty()) {
                    inputs.add(named(choices.getFirst()));
                }
            }
            final List<String> outputs = new ArrayList<>();
            for (final ItemStack stack : view.outputs()) {
                outputs.add(named(stack));
            }
            final List<String> cost = new ArrayList<>();
            if (!view.time().getString().isEmpty()) {
                cost.add(view.time().getString());
            }
            if (!view.energy().getString().isEmpty()) {
                cost.add(view.energy().getString());
            }
            final String in = String.join(" + ", inputs);
            final String out = String.join(" + ", outputs);
            lines.add(GameText.resolve(cost.isEmpty() ? HelpTexts.RECIPE.with(in, out)
                    : HelpTexts.RECIPE_COST.with(in, out, String.join(", ", cost))));
        }
        return lines;
    }

    @Override
    public void answered(final BlockPos host, final List<HelpCommand> answeredCommands, final String page,
                         final List<String> lines) {
        if (this.machine == null || !this.machine.equals(host)) {
            return;
        }
        this.commands = answeredCommands;
        if (!page.isEmpty()) {
            this.pages.put(page.toLowerCase(Locale.ROOT), lines);
        }
        this.onAnswer.run();
    }

    /** An item as a recipe line names it: its name, after how many when more than one. */
    private static String named(final ItemStack stack) {
        final String name = stack.getHoverName().getString();
        return stack.getCount() > 1 ? stack.getCount() + " " + name : name;
    }
}
