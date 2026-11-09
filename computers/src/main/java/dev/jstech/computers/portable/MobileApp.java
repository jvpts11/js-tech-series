/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Computers.
 */
package dev.jstech.computers.portable;

import dev.jstech.core.id.IStableName;
import dev.jstech.core.id.StableNames;
import dev.jstech.core.jstechseries.tier.Era;
import dev.jstech.core.jstechseries.tier.HardwareEra;
import dev.jstech.core.text.TextKey;
import java.util.EnumSet;
import java.util.Set;
import org.jetbrains.annotations.Nullable;

/**
 * The apps of the mobile systems (computers/docs/design/wireless.md), in the order a home screen shows them: the
 * mobile systems' own programs, made for the three families and served by the network's Mirror through each family's
 * store, everything in it free. The network app, the alerts and the apps of the camera and the GPS come from the
 * factory, the hardware ones only on the smartphones and the tablets that have it; the others are installed from the
 * store: the Crypto Wallet from the Transition on, the Messenger from the Standard, prospecting from the Standard on
 * the devices with a GPS.
 *
 * <p>Pure: no part of the game.
 */
public enum MobileApp implements IStableName {

    NETWORK("network", HardwareEra.LEGACY, false, true),
    ALERTS("alerts", HardwareEra.LEGACY, false, true),
    CRYPTO_WALLET("crypto_wallet", HardwareEra.TRANSITION, false, false),
    MESSENGER("messenger", HardwareEra.STANDARD, false, false),
    CAMERA("camera", HardwareEra.TRANSITION, true, true),
    GALLERY("gallery", HardwareEra.TRANSITION, true, true),
    MAPS("maps", HardwareEra.TRANSITION, true, true),
    PROSPECTING("prospecting", HardwareEra.STANDARD, true, false);

    private final String serializedName;
    private final Era firstEra;
    private final boolean needsCameraAndGps;
    private final boolean fromTheFactory;

    private static final StableNames<MobileApp> NAMES = StableNames.of(MobileApp.class);

    MobileApp(final String serializedName, final Era firstEra, final boolean needsCameraAndGps,
              final boolean fromTheFactory) {
        this.serializedName = serializedName;
        this.firstEra = firstEra;
        this.needsCameraAndGps = needsCameraAndGps;
        this.fromTheFactory = fromTheFactory;
    }

    /** The app of that serialized name, or null when none is. */
    @Nullable
    public static MobileApp byName(@Nullable final String name) {
        return NAMES.find(name);
    }

    /** The apps a device of {@code model} comes with. */
    public static Set<MobileApp> factory(final PortableModel model) {
        final Set<MobileApp> out = EnumSet.noneOf(MobileApp.class);
        for (final MobileApp app : values()) {
            if (app.fromTheFactory && app.runsOn(model)) {
                out.add(app);
            }
        }
        return out;
    }

    /** Whether a device of {@code model} can have it: of its era or after, with the hardware it needs. */
    public boolean runsOn(final PortableModel model) {
        return firstEra.isAtMost(model.era()) && (!needsCameraAndGps || model.kind().hasCameraAndGps());
    }

    /** Whether it comes with every device that can have it, rather than from the store. */
    public boolean fromTheFactory() {
        return fromTheFactory;
    }

    /** Its name, under its icon and in the store. */
    public TextKey word() {
        return switch (this) {
            case NETWORK -> PortableTexts.APP_NETWORK;
            case ALERTS -> PortableTexts.APP_ALERTS;
            case CRYPTO_WALLET -> PortableTexts.APP_CRYPTO_WALLET;
            case MESSENGER -> PortableTexts.APP_MESSENGER;
            case CAMERA -> PortableTexts.APP_CAMERA;
            case GALLERY -> PortableTexts.APP_GALLERY;
            case MAPS -> PortableTexts.APP_MAPS;
            case PROSPECTING -> PortableTexts.APP_PROSPECTING;
        };
    }

    /** Its shorter name, under its icon on a home screen with little room. */
    public TextKey label() {
        return switch (this) {
            case CRYPTO_WALLET -> PortableTexts.LABEL_WALLET;
            case PROSPECTING -> PortableTexts.LABEL_PROSPECT;
            default -> word();
        };
    }

    @Override
    public String serializedName() {
        return serializedName;
    }
}
