/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Computers.
 */
package dev.jstech.computers.program.install;

import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.jetbrains.annotations.Nullable;

/**
 * The build options a source-based system keeps in {@code /etc/portage/make.conf}, read for the one of them
 * that decides how long everything takes: how many jobs a build may run at once.
 *
 * <p>The file is the player's to write, while installing the system and for as long as it is used, so it is
 * read the forgiving way a shell would read it: the last thing it says about jobs is what counts, and a file
 * that says nothing, or is not there, means one.
 */
public final class MakeOpts {

    /** Where the file is, by the system's own name for it. */
    public static final String PATH = "/etc/portage/make.conf";

    /** The line that sets them, from the start of a line so that one commented out sets nothing. */
    private static final Pattern JOBS = Pattern.compile("(?m)^\\s*MAKEOPTS\\s*=\\s*\"?[^\"\\n]*-j\\s*(\\d+)");

    private MakeOpts() {
    }

    /** How many jobs that file asks for; one when it asks for nothing, or is not there at all. */
    public static int jobs(@Nullable final String conf) {
        if (conf == null) {
            return 1;
        }
        int jobs = 1;
        final Matcher found = JOBS.matcher(conf);
        while (found.find()) {
            jobs = Math.max(1, Integer.parseInt(found.group(1)));
        }
        return jobs;
    }
}
