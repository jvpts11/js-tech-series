/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Computers.
 */
package dev.jstech.computers.program.cli;

import dev.jstech.computers.advancement.JscEvents;
import dev.jstech.computers.os.KernelNames;
import dev.jstech.computers.os.Platform;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import net.minecraft.resources.ResourceLocation;

/**
 * What the Unix systems say about themselves, and the package managers of each family.
 *
 * <p>Moved here from PosixCommands, which keeps the lists that say which system gets which command.
 */
final class PosixSystemCommands {

    private PosixSystemCommands() {
    }

    static final class Uname implements ICliCommand {

        /** Every letter the tool takes: all of it, the kernel, the machine's name, the release, the architecture. */
        private static final String LETTERS = "asnrm";

        @Override public CommandScope scope() {
            return CommandScope.on(CommandScope.UNIX_SYSTEMS);
        }

        @Override public String name() { return "uname"; }

        @Override public String summary() { return "print system information"; }

        @Override public String usage() { return "[-a|-s|-r|-m|-sr]"; }

        /*
         * The letters may come together or apart, as they always could: -sr is -s -r. What is printed is in
         * the order the tool has always printed it, whatever order it was asked in.
         */
        @Override public void run(final CliContext ctx) {
            final Platform platform = ctx.computer().platform();
            final int bits = ctx.computer().processorBits();
            final StringBuilder asked = new StringBuilder();
            for (int i = 0; i < ctx.argCount(); i++) {
                if (!ctx.arg(i).startsWith("-") || ctx.arg(i).length() < 2) {
                    ctx.out().error("uname: extra operand '" + ctx.arg(i) + "'");
                    return;
                }
                asked.append(ctx.arg(i).substring(1));
            }
            for (int i = 0; i < asked.length(); i++) {
                if (LETTERS.indexOf(asked.charAt(i)) < 0) {
                    ctx.out().error("uname: invalid option -- '" + asked.charAt(i) + "'");
                    return;
                }
            }
            if (asked.indexOf("a") >= 0) {
                ctx.out().line(KernelNames.everything(platform, ctx.computer().hostname(), bits));
                return;
            }
            // Asked nothing, it names the kernel, which is what -s alone says.
            final String wanted = asked.isEmpty() ? "s" : asked.toString();
            final List<String> said = new ArrayList<>();
            if (wanted.indexOf('s') >= 0) {
                said.add(KernelNames.name(platform));
            }
            if (wanted.indexOf('n') >= 0) {
                said.add(ctx.computer().hostname());
            }
            if (wanted.indexOf('r') >= 0) {
                said.add(KernelNames.release(platform));
            }
            if (wanted.indexOf('m') >= 0) {
                said.add(KernelNames.architecture(platform, bits));
            }
            ctx.out().line(String.join(" ", said));
        }
    }

    static final class Hostname implements ICliCommand {
        @Override public CommandScope scope() {
            return CommandScope.on(CommandScope.UNIX_SYSTEMS);
        }

        @Override public String name() { return "hostname"; }

        @Override public String summary() { return "print this computer's host name"; }

        @Override public void run(final CliContext ctx) {
            ctx.out().line(ctx.computer().hostname());
        }
    }

    static final class Screenfetch implements ICliCommand {
        /** A package a player installs, on the systems whose art it draws. */
        @Override public CommandScope scope() {
            return CommandScope.on(CommandScope.UNIX_SYSTEMS).fromPackage("jsc:screenfetch");
        }


        // One small ASCII mark per distribution, printed beside the system readout.
        private static final Map<String, String[]> LOGOS = Map.of(
                "ubuntu", new String[]{
                        "                          ./+o+-",
                        "                  yyyyy- -yyyyyy+",
                        "               ://+//////-yyyyyyo",
                        "           .++ .:/++++++/-.+sss/`",
                        "         .:++o:  /++++++++/:--:/-",
                        "        o:+o+:++.`..```.-/oo+++++/",
                        "       .:+o:+o/.          `+sssoo+/",
                        "  .++/+:+oo+o:`             /sssooo.",
                        " /+++//+:`oo+o               /::--:.",
                        " \\+/+o+++`o++o               ++////.",
                        "  .++.o+++oo+:`             /dddhhh.",
                        "       .+.o+oo:.          `oddhhhh+",
                        "        \\+.++o+o``-````.:ohdhhhhh+",
                        "         `:o+++ `ohhhhhhhhyo++os:",
                        "           .o:`.syhhhhhhh/.oo++o`",
                        "               /osyyyyyyo++ooo+++/",
                        "              ````` +oo+++o\\:",
                        "                          `oo++."},
                "debian", new String[]{
                        "       _,met$$$$$gg.",
                        "    ,g$$$$$$$$$$$$$$$P.",
                        "  ,g$$P\"\"       \"\"\"Y$$.\".",
                        " ,$$P'              `$$$.",
                        "',$$P       ,ggs.     `$$b:",
                        "`d$$'     ,$P\"'   .    $$$",
                        " $$P      d$'     ,    $$P",
                        " $$:      $$.   -    ,d$$'",
                        " $$;      Y$b._   _,d$P'",
                        " Y$$.    `.`\"Y$$$$P\"'",
                        " `$$b      \"-.__",
                        "  `Y$$",
                        "   `Y$$.",
                        "     `$$b.",
                        "       `Y$$b.",
                        "          `\"Y$b._",
                        "              `\"\"\""},
                "fedora", new String[]{
                        "          /:-------------:\\",
                        "       :-------------------::",
                        "     :-----------/shhOHbmp---:\\",
                        "   /-----------omMMMNNNMMD  ---:",
                        "  :-----------sMMMMNMNMP.    ---:",
                        " :-----------:MMMdP-------    ---\\",
                        ",------------:MMMd--------    ---:",
                        ":------------:MMMd-------    .---:",
                        ":----    oNMMMMMMMMMNho     .----:",
                        ":--     .+shhhMMMmhhy++   .------/",
                        ":-    -------:MMMd--------------:",
                        ":-   --------/MMMd-------------;",
                        ":-    ------/hMMMy------------:",
                        ":-- :dMNdhhdNMMNo------------;",
                        ":---:sdNMMMMNds:------------:",
                        ":------:://:-------------::",
                        ":---------------------://"},
                "arch", new String[]{
                        "                   -`",
                        "                  .o+`",
                        "                 `ooo/",
                        "                `+oooo:",
                        "               `+oooooo:",
                        "               -+oooooo+:",
                        "             `/:-:++oooo+:",
                        "            `/++++/+++++++:",
                        "           `/++++++++++++++:",
                        "          `/+++ooooooooooooo/`",
                        "         ./ooosssso++osssssso+`",
                        "        .oossssso-````/ossssss+`",
                        "       -osssssso.      :ssssssso.",
                        "      :osssssss/        osssso+++.",
                        "     /ossssssss/        +ssssooo/-",
                        "   `/ossssso+/:-        -:/+osssso+-",
                        "  `+sso+:-`                 `.-/+oso:",
                        " `++:.                           `-/+/",
                        " .`                                 `/"},
                "gentoo", new String[]{
                        "         -/oyddmdhs+:.",
                        "     -odNMMMMMMMMNNmhy+-`",
                        "   -yNMMMMMMMMMMMNNNmmdhy+-",
                        " `omMMMMMMMMMMMMNmdmmmmddhhy/`",
                        " omMMMMMMMMMMMNhhyyyohmdddhhhdo`",
                        ".ydMMMMMMMMMMdhs++so/smdddhhhhdm+`",
                        " oyhdmNMMMMMMMNdyooydmddddhhhhyhNd.",
                        "  :oyhhdNNMMMMMMMNNNmmdddhhhhhyymMh",
                        "    .:+sydNMMMMMNNNmmmdddhhhhhhmMmy",
                        "       /mMMMMMMNNNmmmdddhhhhhmMNhs:",
                        "    `oNMMMMMMMNNNmmmddddhhdmMNhs+`",
                        "  `sNMMMMMMMMNNNmmmdddddmNMmhs/.",
                        " /NMMMMMMMMNNNNmmmdddmNMNdso:`",
                        "+MMMMMMMNNNNNmmmmdmNMNdso/-",
                        "yMMNNNNNNNmmmmmNNMmhs+/-`",
                        "/hMMNNNNNNNNMNdhs++/-`",
                        "`/ohdmmddhys+++/:.`",
                        "  `-//////:--."},
                "freebsd", new String[]{
                        "   ```                        `",
                        "  s` `.....---.......--.```   -/",
                        "  +o   .--`         /y:`      +.",
                        "   yo`:.            :o      `+-",
                        "    y/               -/`   -o/",
                        "   .-                  ::/sy+:.",
                        "   /                     `--  /",
                        "  `:                          :`",
                        "  `:                          :`",
                        "   /                          /",
                        "   .-                        -.",
                        "    --                      -.",
                        "     `:`                  `:`",
                        "       .--             `--.",
                        "          .---.....----."});

        private static final String[] DEFAULT_LOGO = {
                "  .--------.  ",
                "  |  .--.  |  ",
                "  |  |  |  |  ",
                "  |  '--'  |  ",
                "  |  JSC   |  ",
                "  '--------'  "};

        // Each system paints in its brand color, the way real fetch tools color their logo block.
        private static final Map<String, CliStyle>
                COLORS = Map.of(
                        "ubuntu", CliStyle.ORANGE,
                        "debian", CliStyle.MAGENTA,
                        "fedora", CliStyle.BLUE,
                        "arch", CliStyle.CYAN,
                        "gentoo", CliStyle.PURPLE,
                        "freebsd", CliStyle.RED);

        @Override public String name() { return "screenfetch"; }

        @Override public List<String> aliases() { return List.of("neofetch"); }

        @Override public String summary() { return "show the system logo and information"; }

        @Override public String usage() { return ""; }

        @Override public boolean available(final ICliComputer computer) {
            /*
             * A package the Mirror serves, not a built-in: 'command not found' until it is installed,
             * the classic first thing to apt install on a fresh system.
             */
            return computer.hasProgram(
                    ResourceLocation.fromNamespaceAndPath("jsc", "screenfetch"));
        }

        @Override public void run(final CliContext ctx) {
            final ICliComputer.SystemInfo info = ctx.computer().systemInfo();
            if (info == null) {
                ctx.out().error("screenfetch: no operating system installed");
                return;
            }
            ctx.computer().report(JscEvents.SCREENFETCH, info.distroId());
            final String[] logo = LOGOS.getOrDefault(info.distroId(), DEFAULT_LOGO);
            final CliStyle color = COLORS.getOrDefault(
                    info.distroId(), CliStyle.ACCENT);
            final List<String> lines = new ArrayList<>();
            final String user = "player@" + info.hostname();
            lines.add(user);
            lines.add("-".repeat(user.length()));
            lines.add("OS: " + info.os() + " "
                    + KernelNames.architecture(ctx.computer().platform(), ctx.computer().processorBits()));
            lines.add("Kernel: " + info.kernel());
            lines.add("Uptime: " + uptime(info.uptimeTicks()));
            lines.add("Packages: " + info.packages() + " (" + ctx.computer().packageManager().command() + ")");
            lines.add("Shell: " + info.shell());
            lines.add("DE: " + info.desktop());
            lines.add("CPU: " + info.cpu());
            lines.add("RAM: " + info.ramMb() + " MB");
            lines.add("Disk: " + info.diskUsedMb() + " MB / " + info.diskTotalMb() + " MB");
            final int rows = Math.max(logo.length, lines.size());
            // The info column starts two spaces past the widest logo row (the arts differ in width).
            int logoW = 0;
            for (final String row : logo) {
                logoW = Math.max(logoW, row.length());
            }
            logoW += 2;
            for (int i = 0; i < rows; i++) {
                final String left = i < logo.length ? logo[i] : "";
                final String right = i < lines.size() ? lines.get(i) : "";
                final String padded = left + " ".repeat(Math.max(0, logoW - left.length()));
                ctx.out().styled((padded + right).stripTrailing(), color);
            }
        }

        /** The world's uptime as {@code Xd Xh Xm} (in-game time, and the machine has been part of it). */
        private static String uptime(final long ticks) {
            final long minutes = ticks / (20L * 60L);
            final long days = minutes / (60 * 24);
            final long hours = (minutes / 60) % 24;
            final long mins = minutes % 60;
            if (days > 0) {
                return days + "d " + hours + "h " + mins + "m";
            }
            return hours > 0 ? hours + "h " + mins + "m" : mins + "m";
        }
    }

}
