/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.audio.media;

import com.mojang.brigadier.arguments.IntegerArgumentType;
import dev.jstech.core.command.SeriesCommands;
import dev.jstech.core.text.GameText;
import java.io.IOException;
import java.util.Locale;
import java.util.Optional;
import java.util.concurrent.TimeUnit;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;

/**
 * {@code /jstech media}: how much the server keeps of the recordings players brought, and
 * {@code /jstech media prune <days>}, which takes out every one nothing has used for that many days. What a mod still
 * offers and what is playing stays, and whoever brought a recording that goes gets its room back.
 */
public final class MediaCommand {

    /* A century, which is further back than any server has been running. */
    private static final int MOST_DAYS = 36_500;
    private static final String DAYS = "days";

    private MediaCommand() {
    }

    /** Declares the command, from the Core's constructor; clearing the store is the server's own maintenance. */
    public static void declare() {
        SeriesCommands.declare("media", SeriesCommands.ADMINS, (node, context) -> node
                .executes(source -> held(source.getSource()))
                .then(Commands.literal("prune")
                        .then(Commands.argument(DAYS, IntegerArgumentType.integer(0, MOST_DAYS))
                                .executes(source -> prune(source.getSource(),
                                        IntegerArgumentType.getInteger(source, DAYS))))));
    }

    private static int held(final CommandSourceStack source) {
        final Optional<MediaStore> store = MediaStore.current();
        if (store.isEmpty()) {
            source.sendFailure(GameText.component(MediaTexts.STORE_CLOSED));
            return 0;
        }
        final MediaStore.Held held = store.get().size();
        source.sendSuccess(() -> GameText.component(MediaTexts.HELD.with(held.count(), megabytes(held.bytes()))),
                false);
        return held.count();
    }

    private static int prune(final CommandSourceStack source, final int days) {
        final Optional<MediaStore> store = MediaStore.current();
        if (store.isEmpty()) {
            source.sendFailure(GameText.component(MediaTexts.STORE_CLOSED));
            return 0;
        }
        try {
            final MediaStore.Held pruned = store.get().prune(TimeUnit.DAYS.toMillis(days), MediaKeepers.kept());
            source.sendSuccess(() -> GameText.component(MediaTexts.PRUNED.with(pruned.count(), days,
                    megabytes(pruned.bytes()))), true);
            return pruned.count();
        } catch (final IOException failed) {
            source.sendFailure(GameText.component(MediaTexts.PRUNE_FAILED.with(String.valueOf(failed.getMessage()))));
            return 0;
        }
    }

    /* Bytes as megabytes to a tenth. */
    private static String megabytes(final long bytes) {
        return String.format(Locale.ROOT, "%.1f", bytes / (1024.0 * 1024.0));
    }
}
