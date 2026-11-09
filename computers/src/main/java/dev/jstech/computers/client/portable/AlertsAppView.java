/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Computers.
 */
package dev.jstech.computers.client.portable;

import dev.jstech.computers.client.portable.IAppKit.Tone;
import dev.jstech.computers.operation.payload.NetworkItemEntry;
import dev.jstech.computers.operation.payload.portable.PortableActionPayload;
import dev.jstech.computers.operation.payload.portable.PortableActionPayload.Action;
import dev.jstech.computers.operation.payload.portable.PortableNetworkPayload;
import dev.jstech.computers.operation.payload.portable.PortableViewPayload;
import dev.jstech.computers.portable.AlertKind;
import dev.jstech.computers.portable.MobileApp;
import dev.jstech.computers.portable.PortableState;
import dev.jstech.computers.storage.StorageKey;
import dev.jstech.core.text.Text;
import dev.jstech.core.text.TextKey;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.UUID;
import org.jetbrains.annotations.Nullable;

/**
 * The Alerts app (computers/docs/design/wireless.md): which of the network's five kinds of alert reach this device,
 * each switched on or off, and the items it watches, each with the level below which it is told; an item is chosen
 * to watch from the network's stock. A rule for the whole network is the automation's, not this.
 */
final class AlertsAppView implements IMobileAppView {

    private static final int ASK_EVERY = 100;

    @Override
    public int askEvery() {
        return ASK_EVERY;
    }

    /* The watched items' names come from the network's stock, and choosing a new one needs it. */
    @Override
    @Nullable
    public PortableActionPayload ask(final UUID device, final MobileAppState state) {
        return PortableActionPayload.of(device, Action.NETWORK);
    }

    @Override
    public void paint(final AppPage page) {
        final MobileAppState state = page.state();
        final PortableViewPayload system = PortableViews.of(page.device());
        final boolean inner = state.pickingWatch() || state.watching() != null;
        final float y = page.kit().bar(page.c(), MobileApp.ALERTS, page.top(), MobileParts.name(MobileApp.ALERTS),
                null, inner);
        if (system == null) {
            return;
        }
        if (state.pickingWatch()) {
            pick(page, y);
        } else if (state.watching() != null) {
            level(page, system, state.watching(), y);
        } else {
            settings(page, system, y);
        }
    }

    /* The five kinds, each on or off, then the items watched and the way to watch another. */
    private static void settings(final AppPage page, final PortableViewPayload system, final float top) {
        final MobileCanvas c = page.c();
        final IAppKit kit = page.kit();
        float y = kit.section(c, top + 2, say(AppTexts.SEND_TO_THIS_DEVICE));
        for (final AlertKind kind : AlertKind.values()) {
            y = kit.toggle(c, y, say(nameOf(kind)), system.alertsOn().contains(kind), MobileTap.run(() ->
                    page.send(PortableActionPayload.named(page.device(), Action.TOGGLE_ALERT,
                            kind.serializedName()))));
        }
        y = kit.section(c, y + 3, say(AppTexts.WATCHED_ITEMS));
        for (final PortableViewPayload.Watch watch : system.watched()) {
            if (y + kit.rowPitch() > page.bottom() - 14) {
                break;
            }
            y = kit.row(c, y, watch.key().stack(1), watch.key().displayName().getString(),
                    say(AppTexts.UNDER.with(MobileParts.number(watch.level()))), Tone.DIM,
                    MobileTap.run(() -> page.state().watching(watch.key())), false);
        }
        if (system.watched().size() < PortableState.MOST_WATCHED) {
            c.text(say(AppTexts.WATCH_AN_ITEM), 5, y + 3, kit.ink(Tone.ACCENT), MobileCanvas.SMALL);
            c.press(0, y, c.width(), 13, MobileTap.run(() -> page.state().pickWatch(true)));
        } else {
            kit.note(c, y + 3, say(AppTexts.WATCHES_FULL), Tone.DIM);
        }
    }

    /* Choosing an item to watch: the network's stock, searched, each watched at the level a new watch starts at. */
    private static void pick(final AppPage page, final float top) {
        final MobileCanvas c = page.c();
        final IAppKit kit = page.kit();
        final MobileAppState state = page.state();
        float y = kit.section(c, top + 2, say(AppTexts.CHOOSE_AN_ITEM));
        y = kit.field(c, 4, y, c.width() - 8, state.search(), say(AppTexts.SEARCH),
                state.typing() == MobileAppState.Field.SEARCH,
                MobileTap.run(() -> state.typeInto(MobileAppState.Field.SEARCH))) + 3;
        final PortableNetworkPayload view = PortableApps.network(page.device());
        if (view == null || view.network().isEmpty()) {
            kit.note(c, y + 8, say(AppTexts.OUT_OF_REACH), Tone.DIM);
            return;
        }
        final List<NetworkItemEntry> items = new ArrayList<>();
        final String wanted = state.search().strip().toLowerCase(Locale.ROOT);
        for (final NetworkItemEntry entry : view.stock()) {
            if (entry.key().isItem() && entry.key().displayName().getString().toLowerCase(Locale.ROOT)
                    .contains(wanted)) {
                items.add(entry);
            }
        }
        final int rows = Math.max(1, (int) ((page.bottom() - y) / kit.rowPitch()));
        final int from = Math.min(page.frame().scroll(), Math.max(0, items.size() - rows));
        for (int n = from; n < items.size() && n < from + rows; n++) {
            final NetworkItemEntry entry = items.get(n);
            y = kit.row(c, y, entry.key().stack(1), entry.key().displayName().getString(),
                    MobileParts.number(entry.total()), Tone.DIM, MobileTap.run(() -> {
                        page.send(PortableActionPayload.item(page.device(), Action.WATCH, entry.key(),
                                MobileAppState.DEFAULT_LEVEL));
                        state.watching(entry.key());
                    }), false);
        }
    }

    /* One watched item: its level, halved or doubled, and the way to stop watching it. */
    private static void level(final AppPage page, final PortableViewPayload system, final StorageKey key,
                              final float top) {
        final MobileCanvas c = page.c();
        final IAppKit kit = page.kit();
        final float w = c.width();
        final long level = levelOf(system, key);
        kit.card(c, 5, top + 2, w - 10, 70);
        c.item(key.stack(1), w / 2 - 10, top + 6, 20);
        kit.note(c, top + 29, key.displayName().getString(), Tone.TEXT);
        kit.note(c, top + 40, say(AppTexts.TELL_ME_BELOW), Tone.DIM);
        final float y = top + 51;
        kit.button(c, 12, y, 16, "-", false, MobileTap.run(() -> page.send(PortableActionPayload.item(
                page.device(), Action.WATCH, key, Math.max(1L, level / 2)))));
        kit.button(c, w - 28, y, 16, "+", false, MobileTap.run(() -> page.send(PortableActionPayload.item(
                page.device(), Action.WATCH, key, Math.min(Integer.MAX_VALUE, level * 2)))));
        c.text(MobileParts.number(level), w / 2, y + 1, kit.ink(Tone.TEXT), MobileCanvas.NORMAL,
                MobileCanvas.Align.CENTRE, true);
        kit.button(c, 8, top + 80, w - 16, say(AppTexts.STOP_WATCHING), false, MobileTap.run(() -> {
            page.send(PortableActionPayload.item(page.device(), Action.UNWATCH, key, 0L));
            page.state().watching(null);
        }));
    }

    /* The level {@code key} is watched at, or the level a new watch starts at while it is on its way. */
    private static long levelOf(final PortableViewPayload system, final StorageKey key) {
        for (final PortableViewPayload.Watch watch : system.watched()) {
            if (watch.key().equals(key)) {
                return watch.level();
            }
        }
        return MobileAppState.DEFAULT_LEVEL;
    }

    private static TextKey nameOf(final AlertKind kind) {
        return switch (kind) {
            case ITEMS -> AppTexts.KIND_ITEMS;
            case OPERATIONS -> AppTexts.KIND_OPERATIONS;
            case POWER -> AppTexts.KIND_POWER;
            case UPS -> AppTexts.KIND_UPS;
            case CONFLICT -> AppTexts.KIND_CONFLICT;
        };
    }

    private static String say(final TextKey key) {
        return MobileTexts.say(key);
    }

    private static String say(final Text text) {
        return MobileTexts.say(text);
    }
}
