/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.client.config;

import dev.jstech.core.config.ConfigDraft;
import dev.jstech.core.config.ConfigFile;
import dev.jstech.core.config.ConfigRule;
import dev.jstech.core.config.ConfigScreenTexts;
import dev.jstech.core.gui.style.StyleRole;
import dev.jstech.core.text.GameText;
import dev.jstech.core.text.Text;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import net.minecraft.Util;

/**
 * What the settings screen says of itself: the banner under the tabs (a rule broken, an operator changing a server's
 * settings, settings only read here) and the footer's line (news for a while, how many changes wait and how many of
 * them a restart, what waits for a restart already kept).
 */
final class ConfigStatus {

    private final ConfigScreenModel model;
    private String notice = "";
    private boolean noticeWrong;
    private long noticeUntil;

    /** How long a line of news (a preset taken, settings imported) stays in the footer. */
    private static final long NOTICE_MILLIS = 6000L;

    ConfigStatus(final ConfigScreenModel model) {
        this.model = model;
    }

    /**
     * What a line says, and its colour.
     *
     * @param text what it says
     * @param role its colour
     * @param lock whether a padlock stands before it
     */
    record Line(String text, StyleRole role, boolean lock) {
    }

    /** Says {@code said} in the footer for a while, as news or as something wrong. */
    void say(final String said, final boolean wrong) {
        notice = said;
        noticeWrong = wrong;
        noticeUntil = Util.getMillis() + NOTICE_MILLIS;
    }

    /** The rules the changes in hand break, of every file the screen changes. */
    List<ConfigRule> brokenRules() {
        final List<ConfigRule> out = new ArrayList<>();
        for (final Map.Entry<ConfigFile, ConfigDraft> entry : model.drafts().entrySet()) {
            if (ConfigScreenModel.editable(entry.getKey())) {
                out.addAll(entry.getValue().broken());
            }
        }
        return out;
    }

    /**
     * What the banner says, by what matters most: a rule broken, then how the open section's file is changed here.
     *
     * @param open    the open section, if one is
     * @param inWorld whether the player is in a world
     */
    Optional<Line> banner(final Optional<ConfigScreenModel.Section> open, final boolean inWorld) {
        final List<ConfigRule> broken = brokenRules();
        if (!broken.isEmpty()) {
            return Optional.of(new Line(GameText.resolve(broken.get(0).message()), StyleRole.WARNING, false));
        }
        if (open.isEmpty()) {
            return Optional.empty();
        }
        return switch (ConfigScreenModel.access(open.get().file())) {
            case REMOTE -> Optional.of(new Line(GameText.resolve(ConfigScreenTexts.OPERATOR), StyleRole.HEADING,
                    true));
            case READ_ONLY -> Optional.of(new Line(GameText.resolve(inWorld ? ConfigScreenTexts.SERVER_READ_ONLY
                    : ConfigScreenTexts.WORLD_ONLY), StyleRole.NOTICE, true));
            case LOCAL -> Optional.empty();
        };
    }

    /** What the footer says: news for a while, else how many changes wait, how many on a restart, or what is due. */
    Line footer() {
        if (!notice.isEmpty() && Util.getMillis() < noticeUntil) {
            return new Line(notice, noticeWrong ? StyleRole.WARNING : StyleRole.INFO, false);
        }
        int waiting = 0;
        int onRestart = 0;
        int due = 0;
        for (final Map.Entry<ConfigFile, ConfigDraft> entry : model.drafts().entrySet()) {
            if (ConfigScreenModel.editable(entry.getKey())) {
                waiting += entry.getValue().changeCount();
                onRestart += entry.getValue().pendingOnRestart();
            }
            due += entry.getKey().restartDue().size();
        }
        if (waiting > 0) {
            final Text said = onRestart > 0 ? ConfigScreenTexts.CHANGED_ON_RESTART.with(waiting, onRestart)
                    : ConfigScreenTexts.CHANGED.with(waiting);
            return new Line(GameText.resolve(said), StyleRole.NOTICE, false);
        }
        if (due > 0) {
            return new Line(GameText.resolve(ConfigScreenTexts.RESTART_DUE.with(due)), StyleRole.NOTICE, false);
        }
        return new Line(GameText.resolve(ConfigScreenTexts.NOTHING_CHANGED), StyleRole.MENU_DIM, false);
    }
}
