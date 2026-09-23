/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Computers.
 */
package dev.jstech.computers.datagen.advancement;

import dev.jstech.computers.ComputingModule;
import dev.jstech.computers.advancement.JscEvents;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Items;

/**
 * Programming in Sigma: installing the compiler, the first program, the mistakes every programmer makes once, and
 * the programs that run the network on their own.
 */
public final class SigmaAdvancements extends AdvancementTab {

    /* The package of the Sigma# compiler, whose install opens the tab. */
    private static final String COMPILER = "sgsc";

    public SigmaAdvancements() {
        super("sigma", ResourceLocation.withDefaultNamespace("textures/block/purpur_block.png"));
        this.root(Items.WRITABLE_BOOK, "Sigma", "Install the Σ# Compiler",
                on(JscEvents.PROGRAM_INSTALLED, COMPILER));

        this.task("erm_what_the_sigma", "root", Items.WRITABLE_BOOK, "Erm, What the Sigma?",
                "Run a Sigma program", on(JscEvents.SIGMA_RUN));
        this.secret("expected_semicolon", "root", Items.PAPER, "Expected ';' but Found '}'",
                "Fail to compile a program", on(JscEvents.SIGMA_COMPILE_ERROR));
        this.goal("back_in_my_day", "root", ComputingModule.FLOPPY_DISK.get(), "Back in My Day",
                "Compile a Σ program on MC-DOS", on(JscEvents.SIGMA_ON_DOS));

        this.secret("stack_overflow", "erm_what_the_sigma", Items.LADDER, "Stack Overflow",
                "Have a program halt 1,024 calls deep", on(JscEvents.SIGMA_HALTED, JscEvents.HALT_STACK));
        this.secret("undefined_behaviour", "erm_what_the_sigma", Items.SPIDER_EYE, "Undefined Behaviour",
                "Have a program halt dividing by zero", on(JscEvents.SIGMA_HALTED, JscEvents.HALT_DIVIDE));
        this.task("two_problems", "erm_what_the_sigma", Items.STRING, "Now You Have Two Problems",
                "Start a second thread in a program", on(JscEvents.SIGMA_THREAD));
        this.task("look_ma_no_console", "erm_what_the_sigma", Items.GLASS_PANE, "Look Ma, No Console",
                "Open a window from a program", on(JscEvents.SIGMA_WINDOW));
        this.task("stonks", "erm_what_the_sigma", Items.GOLD_INGOT, "Stonks",
                "Have a program woken by a watch on network stock", on(JscEvents.SIGMA_WATCH));
        this.task("works_on_my_machine", "erm_what_the_sigma", ComputingModule.PERSONAL_COMPUTER.item(),
                "Works on My Machine", "Run the same program on two computers", on(JscEvents.SIGMA_TWO_MACHINES));
        this.goal("ship_it", "erm_what_the_sigma", Items.MINECART, "Ship It",
                "Pack a program with sgpack and publish it to the Mirror", on(JscEvents.SIGMA_PUBLISHED));
        this.goal("automate_the_boring_stuff", "erm_what_the_sigma", Items.PISTON, "Automate the Boring Stuff",
                "Have a program set an Operation going on the network", on(JscEvents.SIGMA_OPERATION));
        this.challenge("five_nines", "automate_the_boring_stuff", Items.NETHER_STAR, "Five Nines",
                "Keep a program running for seven days without stopping", on(JscEvents.SIGMA_UPTIME));
    }
}
