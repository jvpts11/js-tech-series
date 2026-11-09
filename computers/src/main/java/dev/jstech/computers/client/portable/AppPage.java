/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Computers.
 */
package dev.jstech.computers.client.portable;

import dev.jstech.computers.operation.payload.portable.PortableActionPayload;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import net.neoforged.neoforge.network.PacketDistributor;
import org.jetbrains.annotations.Nullable;

/**
 * An app's page as it is drawn this frame: the canvas, the frame it shows, its family's kit, where the system it runs
 * on is, and the room between the bar and the foot. An app draws in it and says what its soft keys do; the keys are
 * drawn last, over the page, as Frames Pocket's Menu rises over it.
 */
public final class AppPage {

    private final MobileCanvas canvas;
    private final MobileFrame frame;
    private final IAppKit kit;
    private final MobileSession session;
    private final float top;
    private final float bottom;
    @Nullable
    private IAppKit.Entry left;
    private List<IAppKit.Entry> menu = List.of();
    private int chosen = -1;

    AppPage(final MobileCanvas canvas, final MobileFrame frame, final IAppKit kit, final MobileSession session,
            final float top) {
        this.canvas = canvas;
        this.frame = frame;
        this.kit = kit;
        this.session = session;
        this.top = top;
        this.bottom = kit.bottom(canvas);
    }

    public MobileCanvas c() {
        return canvas;
    }

    public MobileFrame frame() {
        return frame;
    }

    public IAppKit kit() {
        return kit;
    }

    public MobileAppState state() {
        return session.apps();
    }

    public UUID device() {
        return session.device();
    }

    /** Where the page starts, under the status bar. */
    public float top() {
        return top;
    }

    /** Where the page ends, above its foot. */
    public float bottom() {
        return bottom;
    }

    public float width() {
        return canvas.width();
    }

    /** Asks the server for {@code action}, and to tell the app again soon. */
    public void send(final PortableActionPayload action) {
        PacketDistributor.sendToServer(action);
        state().askSoon();
    }

    /** What the left soft key does, and the Menu with {@code at} of it marked, on a look that has them. */
    public void keys(@Nullable final IAppKit.Entry soft, final List<IAppKit.Entry> entries, final int at) {
        left = soft;
        menu = entries;
        chosen = at;
    }

    /** Draws the foot of the page: the soft keys and the Menu, each entry closing the Menu as it is chosen. */
    void finish() {
        final List<IAppKit.Entry> closing = new ArrayList<>();
        for (final IAppKit.Entry entry : menu) {
            closing.add(new IAppKit.Entry(entry.label(), MobileTap.run(() -> {
                state().closeMenu();
                session.tap(entry.tap(), PacketDistributor::sendToServer);
            })));
        }
        kit.keys(canvas, left, closing, chosen, state().menuOpen(), MobileTap.run(state()::toggleMenu));
    }
}
