/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Computers.
 */
package dev.jstech.computers.program.cli;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * The logos screenfetch draws beside its readout, and the colours it draws them and the readout in.
 *
 * <p>The art and its colours are neofetch's, taken as written from its 7.1.0 release, the one the distributions
 * ship, and Frames 11's four panes from its later script, the only one that has them. neofetch is MIT licensed,
 * Copyright (c) 2015-2021 Dylan Araps; its notice travels with the mod in {@code THIRD_PARTY_NOTICES.md}. A change
 * of colour inside a row is written the way neofetch writes it, {@code ${c1}} for the logo's first colour and so
 * on, so every row here can be set beside neofetch's own and be seen to be the same.
 *
 * <p>The readout follows neofetch's rule for a logo's colours: the user and the host in its first, the labels in
 * its second, or in its first when the second is white, and the values plain.
 */
final class ScreenfetchLogos {

    /** How a colour change is written inside a row, as neofetch writes it. */
    private static final Pattern COLOUR = Pattern.compile("\\$\\{c(\\d)}");

    private static final String[] UBUNTU = {
            "${c1}            .-/+oossssoo+/-.",
            "${c1}        `:+ssssssssssssssssss+:`",
            "${c1}      -+ssssssssssssssssssyyssss+-",
            "${c1}    .ossssssssssssssssss${c2}dMMMNy${c1}sssso.",
            "${c1}   /sssssssssss${c2}hdmmNNmmyNMMMMh${c1}ssssss/",
            "${c1}  +sssssssss${c2}hm${c1}yd${c2}MMMMMMMNddddy${c1}ssssssss+",
            "${c1} /ssssssss${c2}hNMMM${c1}yh${c2}hyyyyhmNMMMNh${c1}ssssssss/",
            "${c1}.ssssssss${c2}dMMMNh${c1}ssssssssss${c2}hNMMMd${c1}ssssssss.",
            "${c1}+ssss${c2}hhhyNMMNy${c1}ssssssssssss${c2}yNMMMy${c1}sssssss+",
            "${c1}oss${c2}yNMMMNyMMh${c1}ssssssssssssss${c2}hmmmh${c1}ssssssso",
            "${c1}oss${c2}yNMMMNyMMh${c1}sssssssssssssshmmmh${c1}ssssssso",
            "${c1}+ssss${c2}hhhyNMMNy${c1}ssssssssssss${c2}yNMMMy${c1}sssssss+",
            "${c1}.ssssssss${c2}dMMMNh${c1}ssssssssss${c2}hNMMMd${c1}ssssssss.",
            "${c1} /ssssssss${c2}hNMMM${c1}yh${c2}hyyyyhdNMMMNh${c1}ssssssss/",
            "${c1}  +sssssssss${c2}dm${c1}yd${c2}MMMMMMMMddddy${c1}ssssssss+",
            "${c1}   /sssssssssss${c2}hdmNNNNmyNMMMMh${c1}ssssss/",
            "${c1}    .ossssssssssssssssss${c2}dMMMNy${c1}sssso.",
            "${c1}      -+sssssssssssssssss${c2}yyy${c1}ssss+-",
            "${c1}        `:+ssssssssssssssssss+:`",
            "${c1}            .-/+oossssoo+/-."};

    private static final String[] DEBIAN = {
            "${c2}       _,met$$$$$gg.",
            "${c2}    ,g$$$$$$$$$$$$$$$P.",
            "${c2}  ,g$$P\"     \"\"\"Y$$.\".",
            "${c2} ,$$P'              `$$$.",
            "${c2}',$$P       ,ggs.     `$$b:",
            "${c2}`d$$'     ,$P\"'   ${c1}.${c2}    $$$",
            "${c2} $$P      d$'     ${c1},${c2}    $$P",
            "${c2} $$:      $$.   ${c1}-${c2}    ,d$$'",
            "${c2} $$;      Y$b._   _,d$P'",
            "${c2} Y$$.    ${c1}`.${c2}`\"Y$$$$P\"'",
            "${c2} `$$b      ${c1}\"-.__",
            "${c2}  `Y$$",
            "${c2}   `Y$$.",
            "${c2}     `$$b.",
            "${c2}       `Y$$b.",
            "${c2}          `\"Y$b._",
            "${c2}              `\"\"\""};

    private static final String[] FEDORA = {
            "${c1}          /:-------------:\\",
            "${c1}       :-------------------::",
            "${c1}     :-----------${c2}/shhOHbmp${c1}---:\\",
            "${c1}   /-----------${c2}omMMMNNNMMD  ${c1}---:",
            "${c1}  :-----------${c2}sMMMMNMNMP${c1}.    ---:",
            "${c1} :-----------${c2}:MMMdP${c1}-------    ---\\",
            "${c1},------------${c2}:MMMd${c1}--------    ---:",
            "${c1}:------------${c2}:MMMd${c1}-------    .---:",
            "${c1}:----    ${c2}oNMMMMMMMMMNho${c1}     .----:",
            "${c1}:--     .${c2}+shhhMMMmhhy++${c1}   .------/",
            "${c1}:-    -------${c2}:MMMd${c1}--------------:",
            "${c1}:-   --------${c2}/MMMd${c1}-------------;",
            "${c1}:-    ------${c2}/hMMMy${c1}------------:",
            "${c1}:--${c2} :dMNdhhdNMMNo${c1}------------;",
            "${c1}:---${c2}:sdNMMMMNds:${c1}------------:",
            "${c1}:------${c2}:://:${c1}-------------::",
            "${c1}:---------------------://"};

    private static final String[] ARCH = {
            "${c1}                   -`",
            "${c1}                  .o+`",
            "${c1}                 `ooo/",
            "${c1}                `+oooo:",
            "${c1}               `+oooooo:",
            "${c1}               -+oooooo+:",
            "${c1}             `/:-:++oooo+:",
            "${c1}            `/++++/+++++++:",
            "${c1}           `/++++++++++++++:",
            "${c1}          `/+++o${c2}oooooooo${c1}oooo/`",
            "${c2}         ${c1}./${c2}ooosssso++osssssso${c1}+`",
            "${c2}        .oossssso-````/ossssss+`",
            "${c2}       -osssssso.      :ssssssso.",
            "${c2}      :osssssss/        osssso+++.",
            "${c2}     /ossssssss/        +ssssooo/-",
            "${c2}   `/ossssso+/:-        -:/+osssso+-",
            "${c2}  `+sso+:-`                 `.-/+oso:",
            "${c2} `++:.                           `-/+/",
            "${c2} .`                                 `/"};

    private static final String[] GENTOO = {
            "${c1}         -/oyddmdhs+:.",
            "${c1}     -o${c2}dNMMMMMMMMNNmhy+${c1}-`",
            "${c1}   -y${c2}NMMMMMMMMMMMNNNmmdhy${c1}+-",
            "${c1} `o${c2}mMMMMMMMMMMMMNmdmmmmddhhy${c1}/`",
            "${c1} om${c2}MMMMMMMMMMMN${c1}hhyyyo${c2}hmdddhhhd${c1}o`",
            "${c1}.y${c2}dMMMMMMMMMMd${c1}hs++so/s${c2}mdddhhhhdm${c1}+`",
            "${c1} oy${c2}hdmNMMMMMMMN${c1}dyooy${c2}dmddddhhhhyhN${c1}d.",
            "${c1}  :o${c2}yhhdNNMMMMMMMNNNmmdddhhhhhyym${c1}Mh",
            "${c1}    .:${c2}+sydNMMMMMNNNmmmdddhhhhhhmM${c1}my",
            "${c1}       /m${c2}MMMMMMNNNmmmdddhhhhhmMNh${c1}s:",
            "${c1}    `o${c2}NMMMMMMMNNNmmmddddhhdmMNhs${c1}+`",
            "${c1}  `s${c2}NMMMMMMMMNNNmmmdddddmNMmhs${c1}/.",
            "${c1} /N${c2}MMMMMMMMNNNNmmmdddmNMNdso${c1}:`",
            "${c1}+M${c2}MMMMMMNNNNNmmmmdmNMNdso${c1}/-",
            "${c1}yM${c2}MNNNNNNNmmmmmNNMmhs+/${c1}-`",
            "${c1}/h${c2}MMNNNNNNNNMNdhs++/${c1}-`",
            "${c1}`/${c2}ohdmmddhys+++/:${c1}.`",
            "${c1}  `-//////:--."};

    /* The waving four-colour flag of Frames 95 and XP. */
    private static final String[] FRAMES_FLAG = {
            "${c1}        ,.=:!!t3Z3z.,",
            "${c1}       :tt:::tt333EE3",
            "${c1}       Et:::ztt33EEEL${c2} @Ee.,      ..,",
            "${c1}      ;tt:::tt333EE7${c2} ;EEEEEEttttt33#",
            "${c1}     :Et:::zt333EEQ.${c2} $EEEEEttttt33QL",
            "${c1}     it::::tt333EEF${c2} @EEEEEEttttt33F",
            "${c1}    ;3=*^```\"*4EEV${c2} :EEEEEEttttt33@.",
            "${c3}    ,.=::::!t=., ${c1}`${c2} @EEEEEEtttz33QF",
            "${c3}   ;::::::::zt33)${c2}   \"4EEEtttji3P*",
            "${c3}  :t::::::::tt33.${c4}:Z3z..${c2}  ``${c4} ,..g.",
            "${c3}  i::::::::zt33F${c4} AEEEtttt::::ztF",
            "${c3} ;:::::::::t33V${c4} ;EEEttttt::::t3",
            "${c3} E::::::::zt33L${c4} @EEEtttt::::z3F",
            "${c3}{3=*^```\"*4E3)${c4} ;EEEtttt:::::tZ`",
            "${c3}             `${c4} :EEEEtttt::::z7",
            "${c4}                 \"VEzjt:;;z>*`"};

    /* Frames 11's four flat panes. */
    private static final String[] FRAMES_PANES = {
            "${c1}################  ################",
            "${c1}################  ################",
            "${c1}################  ################",
            "${c1}################  ################",
            "${c1}################  ################",
            "${c1}################  ################",
            "${c1}################  ################",
            "",
            "${c1}################  ################",
            "${c1}################  ################",
            "${c1}################  ################",
            "${c1}################  ################",
            "${c1}################  ################",
            "${c1}################  ################",
            "${c1}################  ################"};

    /* FreeBSD's as the mod has always drawn it, in one colour with its readout: left exactly as it is. */
    private static final String[] FREEBSD = {
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
            "          .---.....----."};

    private static final String[] DEFAULT = {
            "  .--------.  ",
            "  |  .--.  |  ",
            "  |  |  |  |  ",
            "  |  '--'  |  ",
            "  |  JSC   |  ",
            "  '--------'  "};

    private static final Map<String, Logo> BY_SYSTEM = Map.of(
            "ubuntu", Logo.drawn(UBUNTU, CliStyle.RED, CliStyle.BRIGHT, CliStyle.YELLOW),
            "debian", Logo.drawn(DEBIAN, CliStyle.RED, CliStyle.BRIGHT, CliStyle.YELLOW),
            "fedora", Logo.drawn(FEDORA, CliStyle.BLUE, CliStyle.BRIGHT, CliStyle.RED),
            "arch", Logo.drawn(ARCH, CliStyle.CYAN, CliStyle.CYAN, CliStyle.BRIGHT, CliStyle.RED),
            "gentoo", Logo.drawn(GENTOO, CliStyle.MAGENTA, CliStyle.BRIGHT),
            "freebsd", Logo.plain(FREEBSD, CliStyle.RED),
            "frames_95", Logo.drawn(FRAMES_FLAG, CliStyle.RED, CliStyle.GREEN, CliStyle.BLUE, CliStyle.YELLOW),
            "frames_xp", Logo.drawn(FRAMES_FLAG, CliStyle.RED, CliStyle.GREEN, CliStyle.BLUE, CliStyle.YELLOW),
            "frames_11", Logo.drawn(FRAMES_PANES, CliStyle.CYAN, CliStyle.BRIGHT));

    private static final Logo FALLBACK = Logo.plain(DEFAULT, CliStyle.ACCENT);

    private ScreenfetchLogos() {
    }

    /** The logo of the system with that id's path, such as {@code ubuntu}; a plain mark for one it has none for. */
    static Logo of(final String system) {
        return BY_SYSTEM.getOrDefault(system, FALLBACK);
    }

    /**
     * One logo: its rows as coloured runs, and the colours of the readout beside it.
     *
     * @param oneColour whether the logo and its whole readout are printed in {@link #title()} alone, which is how
     *                  FreeBSD's has always been drawn and is kept
     */
    record Logo(List<List<CliSpan>> rows, CliStyle title, CliStyle label, boolean oneColour) {

        Logo {
            rows = rows.stream().map(List::copyOf).toList();
        }

        /** A logo in neofetch's colours: {@code colours[0]} is its {@code ${c1}}, and so on. */
        static Logo drawn(final String[] art, final CliStyle... colours) {
            final List<List<CliSpan>> rows = new ArrayList<>(art.length);
            for (final String row : art) {
                rows.add(runs(row, colours));
            }
            final boolean secondIsWhite = colours.length < 2 || colours[1] == CliStyle.BRIGHT;
            return new Logo(rows, colours[0], secondIsWhite ? colours[0] : colours[1], false);
        }

        /** A logo printed, with its readout, in the one colour. */
        static Logo plain(final String[] art, final CliStyle colour) {
            final List<List<CliSpan>> rows = new ArrayList<>(art.length);
            for (final String row : art) {
                rows.add(List.of(new CliSpan(row, colour)));
            }
            return new Logo(rows, colour, colour, true);
        }

        /** How many columns the widest row takes. */
        int width() {
            int widest = 0;
            for (final List<CliSpan> row : this.rows) {
                widest = Math.max(widest, columns(row));
            }
            return widest;
        }

        static int columns(final List<CliSpan> row) {
            int count = 0;
            for (final CliSpan span : row) {
                count += span.text().length();
            }
            return count;
        }

        /* A row split where its colour changes, each run in the colour its marker names. */
        private static List<CliSpan> runs(final String row, final CliStyle[] colours) {
            final List<CliSpan> out = new ArrayList<>();
            final Matcher marker = COLOUR.matcher(row);
            CliStyle colour = colours[0];
            int at = 0;
            while (marker.find()) {
                if (marker.start() > at) {
                    out.add(new CliSpan(row.substring(at, marker.start()), colour));
                }
                colour = colours[Integer.parseInt(marker.group(1)) - 1];
                at = marker.end();
            }
            if (at < row.length()) {
                out.add(new CliSpan(row.substring(at), colour));
            }
            return out;
        }
    }
}
