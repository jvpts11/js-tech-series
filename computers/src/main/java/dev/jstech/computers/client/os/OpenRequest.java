/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Computers.
 */
package dev.jstech.computers.client.os;

import java.util.List;

/**
 * Something another screen or a running program asked the desktop to open. Most are a program, but a few carry what
 * to open it on: a folder for the file manager, a file with or without the program to open it in, a program to run
 * or lines to type at the prompt, or a file whose Properties to show.
 */
sealed interface OpenRequest {

    /** A program, by the key its window goes by or by the name it reads as on this desktop. */
    record Program(String key) implements OpenRequest {
    }

    /** The file manager, opened at a folder: a drive, a share, or one on the desktop. */
    record FilesAt(String dir) implements OpenRequest {
    }

    /** A compiled program, run at the terminal, which is what double-clicking one does. */
    record RunAtTerminal(String path) implements OpenRequest {
    }

    /** Lines typed at the terminal, one after the other. */
    record TypeAtTerminal(List<String> lines) implements OpenRequest {

        public TypeAtTerminal {
            lines = List.copyOf(lines);
        }
    }

    /** A file, opened in a program by its id, or in the program that opens its kind when the id is empty. */
    record OpenFile(String programId, String path) implements OpenRequest {
    }

    /** A file whose program the player is asked to choose, as Choose another program does. */
    record ChooseOpener(String path) implements OpenRequest {
    }

    /** The Properties window of a file on the desktop, which the file manager shows. */
    record Properties(String path) implements OpenRequest {
    }
}
