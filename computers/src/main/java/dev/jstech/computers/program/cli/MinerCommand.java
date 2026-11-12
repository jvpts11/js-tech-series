/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Computers.
 */
package dev.jstech.computers.program.cli;

import dev.jstech.computers.os.ProgramVersions;
import dev.jstech.computers.program.Programs;
import dev.jstech.computers.teracoin.MinerConsole;
import dev.jstech.computers.teracoin.MinerSettings;
import dev.jstech.computers.teracoin.MinerTexts;
import dev.jstech.computers.teracoin.MinerTool;
import dev.jstech.computers.teracoin.TeracoinAddress;
import dev.jstech.core.text.Text;
import java.util.List;
import java.util.Locale;

/**
 * {@code miner}: Blockwright's miner at the prompt (computers/docs/design/teracoin.md, "Mining"). Alone it mines in
 * front of the terminal until Q; {@code start} keeps it mining in the background, {@code status} says how it does,
 * {@code stop} stops it; {@code --address}, {@code --pool} and {@code --alone} set whom it pays and mines for.
 */
public final class MinerCommand implements ICliCommand {

    /* The miner's package, by its id as text: a command's scope is read where no game is loaded. */
    private static final String PACKAGE = "jsc:miner";

    @Override
    public String name() {
        return "miner";
    }

    @Override
    public CommandGroup group() {
        return CommandGroup.NETWORK;
    }

    @Override
    public Text summary() {
        return MinerTexts.SUMMARY.text();
    }

    @Override
    public Text usage() {
        return MinerTexts.USAGE.text();
    }

    @Override
    public List<Text> description() {
        return List.of(MinerTexts.DESCRIPTION.text(), MinerTexts.DESCRIPTION_BACKGROUND.text());
    }

    @Override
    public List<Option> options() {
        return List.of(new Option("--address ADDRESS", MinerTexts.OPTION_ADDRESS),
                new Option("--pool NAME", MinerTexts.OPTION_POOL), new Option("--alone", MinerTexts.OPTION_ALONE));
    }

    @Override
    public List<Example> examples() {
        return List.of(new Example("miner start", MinerTexts.EXAMPLE_FIRST),
                new Example("miner status", MinerTexts.EXAMPLE_STATUS));
    }

    @Override
    public CommandScope scope() {
        return CommandScope.everywhere().fromPackage(PACKAGE);
    }

    @Override
    public void run(final CliContext ctx) {
        final MinerConsole console = ctx.computer().miner();
        if (console == null) {
            ctx.out().error(CliTexts.SAID_BY.with(name(), MinerTexts.CANNOT.text()));
            return;
        }
        final MinerSettings miner = console.settings();
        String verb = "";
        boolean changed = false;
        Text told = Text.EMPTY;
        for (int i = 0; i < ctx.argCount(); i++) {
            final String arg = ctx.arg(i);
            switch (arg.toLowerCase(Locale.ROOT)) {
                case "--address" -> {
                    final String address = ctx.arg(++i).toLowerCase(Locale.ROOT);
                    if (!TeracoinAddress.valid(address)) {
                        ctx.out().error(CliTexts.SAID_BY.with(name(), MinerTexts.BAD_ADDRESS.with(address)));
                        return;
                    }
                    miner.setAddress(address);
                    changed = true;
                    told = MinerTexts.ADDRESS_SET.text();
                }
                case "--pool" -> {
                    final String pool = ctx.arg(++i);
                    miner.setPool(pool);
                    changed = true;
                    told = pool.isEmpty() ? MinerTexts.FIRST_POOL_SET.text() : MinerTexts.POOL_SET.with(pool);
                }
                case "--alone" -> {
                    miner.setAlone();
                    changed = true;
                    told = MinerTexts.ALONE_SET.text();
                }
                case "status", "start", "stop" -> verb = arg.toLowerCase(Locale.ROOT);
                default -> {
                    ctx.out().error(CliTexts.USAGE.with(name(), usage()));
                    return;
                }
            }
        }
        if (changed) {
            console.changed();
        }
        switch (verb) {
            case "status" -> status(ctx, console);
            case "start" -> {
                if (!TeracoinAddress.valid(miner.address())) {
                    ctx.out().line(console.stall(MinerSettings.Status.NO_ADDRESS, ""));
                    return;
                }
                miner.setBackground(true);
                console.changed();
                ctx.out().ok(MinerTexts.STARTED);
            }
            case "stop" -> {
                miner.setBackground(false);
                miner.toolStopped();
                console.changed();
                ctx.out().ok(MinerTexts.BACKGROUND_STOPPED);
            }
            default -> {
                if (miner.background()) {
                    if (changed) {
                        ctx.out().ok(told);
                    } else {
                        status(ctx, console);
                    }
                    return;
                }
                foreground(ctx, console);
            }
        }
    }

    private static void status(final CliContext ctx, final MinerConsole console) {
        if (!console.settings().background() && console.settings().status() == MinerSettings.Status.OFF) {
            ctx.out().dim(MinerTexts.NOT_RUNNING);
            return;
        }
        for (final CliLine line : console.status()) {
            ctx.out().line(line);
        }
    }

    private static void foreground(final CliContext ctx, final MinerConsole console) {
        if (!TeracoinAddress.valid(console.settings().address())) {
            ctx.out().line(console.stall(MinerSettings.Status.NO_ADDRESS, ""));
            return;
        }
        // Blockwright's gold, which on a terminal is its amber; the extended orange reads as a red there.
        ctx.out().line(new CliLine(MinerTexts.BANNER.with(ProgramVersions.of(Programs.MINER)), CliStyle.WARN));
        ctx.out().line(console.whom());
        for (final CliLine line : console.devices()) {
            ctx.out().line(line);
        }
        ctx.out().start(new MinerTool(console));
    }
}
