/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Computers.
 */
package dev.jstech.computers.program.cli;

import dev.jstech.computers.sigma.SigmaVersions;
import dev.jstech.computers.vm.listing.AsmProgram;
import dev.jstech.computers.vm.listing.AsmReader;
import dev.jstech.computers.vm.program.Debugger;
import dev.jstech.computers.vm.program.ProgramImage;
import dev.jstech.core.text.Text;
import java.util.Locale;

/**
 * sgdb: the debugger at the terminal, which speaks gdb's commands (computers/docs/design/sigma.md).
 *
 * <p>It is given a listing, holds the terminal until it is quit, and starts the program when it is told to run it,
 * stopping it at breakpoints and stepping it a line at a time. What the program prints comes out between the
 * debugger's own lines, as it does under gdb. It comes with the Σ# compiler, as a debugger comes with the compiler it
 * reads the builds of.
 */
public final class SgdbCommand implements ICliCommand {

    private static final String ASSEMBLY = ".asm";

    @Override
    public String name() {
        return "sgdb";
    }

    @Override
    public CommandGroup group() {
        return CommandGroup.PROGRAMMING;
    }

    @Override
    public Text summary() {
        return SgdbTexts.SUMMARY.text();
    }

    @Override
    public Text usage() {
        return SgdbTexts.USAGE.text();
    }

    @Override
    public CommandScope scope() {
        return CommandScope.everywhere().fromPackage(SigmaCommands.COMPILER);
    }

    @Override
    public void run(final CliContext ctx) {
        final String path = ctx.arg(0);
        if (path.isEmpty()) {
            ctx.out().error(CliTexts.USAGE.with(this.name(), this.usage()));
            return;
        }
        final ICliComputer.FsResult read = ctx.computer().readFile(path);
        if (!read.ok()) {
            ctx.out().error(CliTexts.SAID_BY.with(this.name(), read.message()));
            return;
        }
        final ProgramImage image = path.toLowerCase(Locale.ROOT).endsWith(ASSEMBLY)
                ? readImage(new AsmReader(read.message().english())) : null;
        if (image == null) {
            ctx.out().error(CliTexts.SAID_BY.with(this.name(), SgdbTexts.UNREADABLE.with(path)));
            return;
        }
        final int version = SigmaVersions.ofPackage(ctx.computer().installedVersion(SigmaCommands.COMPILER));
        ctx.out().dim(SgdbTexts.BANNER.with(version));
        if (!Debugger.hasLines(image)) {
            ctx.out().line(SgdbTexts.NO_LINES.with(path));
        }
        ctx.out().start(new SgdbTool(ctx.computer(), path, image, ctx.args().subList(1, ctx.args().size())));
    }

    /* The program a listing holds, or null for a listing that does not read, has problems, or starts nowhere. */
    private static ProgramImage readImage(final AsmReader reader) {
        final AsmProgram program = reader.read();
        if (reader.hasProblems() || program.entryPoint() == null) {
            return null;
        }
        final ProgramImage image = ProgramImage.of(program);
        return image.problems().isEmpty() ? image : null;
    }
}
