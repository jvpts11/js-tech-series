/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Computers.
 */
package dev.jstech.computers.client.portable;

import dev.jstech.computers.operation.payload.portable.PortableActionPayload;
import dev.jstech.computers.portable.MobileApp;
import net.neoforged.neoforge.network.PacketDistributor;
import org.jetbrains.annotations.Nullable;

/**
 * The portables' apps on the player's game (computers/docs/design/wireless.md): which view draws each, drawing the
 * open one in its family's look, asking the server again while it is open, and the keys typed into it.
 */
public final class MobileApps {

    private static final NetworkAppView NETWORK = new NetworkAppView();
    private static final AlertsAppView ALERTS = new AlertsAppView();
    private static final MessengerAppView MESSENGER = new MessengerAppView();
    private static final CameraAppView CAMERA = new CameraAppView();
    private static final GalleryAppView GALLERY = new GalleryAppView();
    private static final MapsAppView MAPS = new MapsAppView();
    private static final ProspectingAppView PROSPECTING = new ProspectingAppView();
    private static final WalletAppView WALLET = new WalletAppView();

    private MobileApps() {
    }

    /** The view of {@code app}; every app has one. */
    @Nullable
    public static IMobileAppView of(final MobileApp app) {
        return switch (app) {
            case NETWORK -> NETWORK;
            case ALERTS -> ALERTS;
            case MESSENGER -> MESSENGER;
            case CAMERA -> CAMERA;
            case GALLERY -> GALLERY;
            case MAPS -> MAPS;
            case PROSPECTING -> PROSPECTING;
            case CRYPTO_WALLET -> WALLET;
        };
    }

    /**
     * Draws the app open in {@code frame} with {@code kit}, its page from {@code top}; false when it has no view and
     * the look draws its own shell.
     */
    public static boolean paint(final MobileCanvas c, final MobileFrame frame, final IAppKit kit, final float top) {
        final MobileSession session = frame.session();
        final IMobileAppView view = frame.app() == null ? null : of(frame.app());
        if (session == null || view == null) {
            return false;
        }
        final AppPage page = new AppPage(c, frame, kit, session, top);
        view.paint(page);
        page.finish();
        return true;
    }

    /** Asks the server again for what the open app shows, when it is due, at the game time {@code now}. */
    public static void tick(final MobileSession session, final long now) {
        if (session.page() != MobilePage.APP || session.app() == null) {
            return;
        }
        final IMobileAppView view = of(session.app());
        if (view != null && session.apps().dueToAsk(now, view.askEvery())) {
            final PortableActionPayload ask = view.ask(session.device(), session.apps());
            if (ask != null) {
                PacketDistributor.sendToServer(ask);
            }
        }
    }

    /** Types {@code letter} into the open app's field; false when none takes it. */
    public static boolean typed(final MobileSession session, final char letter) {
        return session.page() == MobilePage.APP && session.apps().type(letter);
    }

    /** The open app's Enter: sends what was typed, where a field sends; false when none does. */
    public static boolean entered(final MobileSession session) {
        if (session.page() != MobilePage.APP || session.app() != MobileApp.MESSENGER) {
            return false;
        }
        MESSENGER.send(session.device(), session.apps());
        return true;
    }
}
