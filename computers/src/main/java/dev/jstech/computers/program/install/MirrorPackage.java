/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Computers.
 */
package dev.jstech.computers.program.install;

import java.util.List;

/**
 * A program the Mirror has, as a package manager sees it: what it is called, how big it is, and everything
 * that is built to get it.
 *
 * <p>Most programs are one piece. A desktop on a system that builds what it installs is several: the toolkit
 * it is drawn with, the frameworks it is written on, and its own parts, with the package asked for last.
 *
 * @param id      the program this is, the way the machine that ends up with it records it
 * @param name    the name a package manager that does not file things by category knows it by
 * @param version the version the Mirror has
 * @param sizeMb  how big it is once installed
 * @param pieces  what is built to get it, in the order it is built, the program itself last
 */
public record MirrorPackage(String id, String name, String version, double sizeMb, List<Piece> pieces) {

    /** Where packages are looked up: the Mirror a machine reaches, or a handful of them standing in for it. */
    @FunctionalInterface
    public interface IShelf {

        /**
         * The package known by that name, or null when there is none.
         *
         * @param byCategory whether the asking system files its packages by category, and so also knows them
         *                   by those names
         */
        MirrorPackage find(String typed, boolean byCategory);
    }

    /**
     * One package of what a program is built from.
     *
     * @param atom     its category and name
     * @param archive  the file its source comes in, empty for one that is only a list of others
     * @param sizeMb   how big that file is
     * @param compiles whether it has anything of its own to compile
     */
    public record Piece(String atom, String version, String archive, double sizeMb, String flagsOn,
                        String flagsOff, boolean compiles) {
    }
}
