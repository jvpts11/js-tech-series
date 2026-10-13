/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Computers.
 */
package dev.jstech.computers.client.os;

import dev.jstech.core.text.TextHolder;
import dev.jstech.core.text.TextKey;

/**
 * What the Application Manager is called, and what its group window says about how many programs a group holds.
 */
@TextHolder
final class ApplicationManagerTexts {

    static final TextKey NAME = TextKey.of("jsc.application_manager.name", "Application Manager");
    static final TextKey GROUP_ONE_PROGRAM =
            TextKey.of("jsc.application_manager.group_one_program", "%s, %s program");
    static final TextKey GROUP_PROGRAMS = TextKey.of("jsc.application_manager.group_programs", "%s, %s programs");

    private ApplicationManagerTexts() {
    }
}
