/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Computers.
 */
package dev.jstech.computers.client.portable;

import dev.jstech.computers.client.portable.IAppKit.Tone;
import dev.jstech.computers.client.portable.MobileCanvas.Align;
import dev.jstech.computers.operation.payload.MessengerStatePayload;
import dev.jstech.computers.operation.payload.portable.PortableActionPayload;
import dev.jstech.computers.operation.payload.portable.PortableActionPayload.Action;
import dev.jstech.computers.portable.MobileApp;
import dev.jstech.computers.program.MessengerLog;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import net.minecraft.client.Minecraft;
import net.neoforged.neoforge.network.PacketDistributor;
import org.jetbrains.annotations.Nullable;

/**
 * The Messenger on a portable (computers/docs/design/wireless.md): the same service a computer's Messenger talks to,
 * run by a server of the device's network. Its rooms and the people on it first; a room shows what was said in it,
 * the player's own words on the right, and takes what they type.
 */
final class MessengerAppView implements IMobileAppView {

    private static final int ASK_EVERY = 40;
    private static final String[] SEND = {"X...", "XXX.", "XXXX", "XXX.", "X..."};
    private static final float LINE_H = 9.0F;

    @Override
    public int askEvery() {
        return ASK_EVERY;
    }

    @Override
    @Nullable
    public PortableActionPayload ask(final UUID device, final MobileAppState state) {
        return PortableActionPayload.named(device, Action.MESSENGER, state.room());
    }

    @Override
    public void paint(final AppPage page) {
        final MobileAppState state = page.state();
        final MessengerStatePayload view = PortableApps.messenger();
        final String me = me();
        final boolean inRoom = !state.room().isEmpty();
        final float y = page.kit().bar(page.c(), MobileApp.MESSENGER, page.top(),
                inRoom ? roomName(state.room(), me) : MobileParts.name(MobileApp.MESSENGER),
                inRoom ? MobileParts.network(page.frame()) : null, inRoom);
        if (view == null) {
            return;
        }
        if (!view.service().online()) {
            page.kit().note(page.c(), y + 20, MobileTexts.say(AppTexts.NO_MESSENGER), Tone.DIM);
            return;
        }
        if (inRoom) {
            room(page, view, y);
        } else {
            rooms(page, view, me, y);
        }
    }

    /** Says what was typed in the room open, and starts the next message. */
    void send(final UUID device, final MobileAppState state) {
        if (!state.room().isEmpty() && !state.draft().isBlank()) {
            PacketDistributor.sendToServer(new PortableActionPayload(device, Action.SAY, state.room(),
                    state.draft().strip(), Optional.empty(), 0L));
            state.sent();
            state.askSoon();
        }
    }

    /* The rooms the player is shown, and the people on the service to talk to alone. */
    private static void rooms(final AppPage page, final MessengerStatePayload view, final String me,
                              final float top) {
        final MobileCanvas c = page.c();
        final IAppKit kit = page.kit();
        float y = kit.section(c, top + 2, MobileTexts.say(AppTexts.ROOMS));
        for (final String room : view.rooms()) {
            if (y + kit.rowPitch() > page.bottom()) {
                return;
            }
            y = kit.row(c, y, null, roomName(room, me), "", Tone.DIM,
                    MobileTap.run(() -> page.state().room(room)), false);
        }
        y = kit.section(c, y + 3, MobileTexts.say(AppTexts.PEOPLE));
        for (final String person : view.people()) {
            if (person.equalsIgnoreCase(me) || y + kit.rowPitch() > page.bottom()) {
                continue;
            }
            y = kit.row(c, y, null, person, "", Tone.GOOD, MobileTap.run(() ->
                    page.state().room(MessengerLog.privateRoom(me, person))), false);
        }
    }

    /* What was said in the room, the newest at the foot over the field it is typed in. */
    private void room(final AppPage page, final MessengerStatePayload view, final float top) {
        final MobileCanvas c = page.c();
        final IAppKit kit = page.kit();
        final MobileAppState state = page.state();
        final float w = c.width();
        final float fieldY = page.bottom() - 17;
        kit.field(c, 5, fieldY, w - 26, state.draft(), MobileTexts.say(AppTexts.MESSAGE),
                state.typing() == MobileAppState.Field.MESSAGE,
                MobileTap.run(() -> state.typeInto(MobileAppState.Field.MESSAGE)));
        c.disc(w - 11, fieldY + 6.5F, 6.5F, kit.ink(Tone.ACCENT));
        c.glyph(SEND, w - 12, fieldY + 4, kit.ink(Tone.SURFACE), 1);
        c.press(w - 18, fieldY, 18, 13, MobileTap.run(() -> send(page.device(), state)));
        final String me = me();
        float y = fieldY - 5;
        final List<MessengerStatePayload.Line> lines = view.room().equalsIgnoreCase(state.room()) ? view.lines()
                : List.of();
        for (int n = lines.size() - 1; n >= 0; n--) {
            final MessengerStatePayload.Line line = lines.get(n);
            final boolean mine = line.from().equalsIgnoreCase(me);
            final String said = line.nudge() ? MobileTexts.say(AppTexts.NUDGED.with(line.from()))
                    : mine || MessengerLog.isPrivate(state.room()) ? line.text()
                    : MobileTexts.say(AppTexts.SAID_BY.with(line.from(), line.text()));
            final List<String> wrapped = c.wrap(said, w * 0.7F, MobileCanvas.SMALL);
            float wide = 0;
            for (final String part : wrapped) {
                wide = Math.max(wide, c.width(part, MobileCanvas.SMALL));
            }
            final float h = 6 + LINE_H * wrapped.size();
            y -= h;
            if (y < top) {
                break;
            }
            final float x = mine ? w - wide - 15 : 5;
            c.rounded(x, y, wide + 10, h, 6, mine ? kit.ink(Tone.ACCENT) : kit.ink(Tone.SURFACE), null);
            for (int k = 0; k < wrapped.size(); k++) {
                c.text(wrapped.get(k), x + 5, y + 3 + k * LINE_H, mine ? kit.ink(Tone.SURFACE) : kit.ink(Tone.TEXT),
                        MobileCanvas.SMALL, Align.LEFT, false);
            }
            y -= 5;
        }
    }

    /* What a room is called: the lobby, or the other person a private one is with. */
    private static String roomName(final String room, final String me) {
        return MessengerLog.isPrivate(room) ? MessengerLog.otherIn(room, me) : MobileTexts.say(AppTexts.LOBBY);
    }

    private static String me() {
        return Minecraft.getInstance().player == null ? ""
                : Minecraft.getInstance().player.getGameProfile().getName();
    }
}
