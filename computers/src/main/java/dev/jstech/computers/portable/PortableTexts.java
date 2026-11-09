/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Computers.
 */
package dev.jstech.computers.portable;

import dev.jstech.core.text.TextHolder;
import dev.jstech.core.text.TextKey;

/** The words the portable devices are described with, in their tooltips and their systems. */
@TextHolder
public final class PortableTexts {

    public static final TextKey KIND_PDA = TextKey.of("jsc.portable.kind.pda", "PDA");
    public static final TextKey KIND_SMARTPHONE = TextKey.of("jsc.portable.kind.smartphone", "smartphone");
    public static final TextKey KIND_TABLET = TextKey.of("jsc.portable.kind.tablet", "tablet");
    public static final TextKey FRAMES_POCKET = TextKey.of("jsc.portable.system.frames_pocket", "Frames Pocket");
    public static final TextKey QOS = TextKey.of("jsc.portable.system.qos", "qOS");
    public static final TextKey DROIDIX = TextKey.of("jsc.portable.system.droidix", "Droidix");
    public static final TextKey FRAMES_MARKETPLACE = TextKey.of("jsc.portable.store.frames_pocket",
            "Frames Marketplace");
    public static final TextKey QUINCE_STORE = TextKey.of("jsc.portable.store.qos", "Quince Store");
    public static final TextKey DROIDIX_STORE = TextKey.of("jsc.portable.store.droidix", "Droidix Store");
    public static final TextKey MEGABYTES = TextKey.of("jsc.portable.megabytes", "%s MB");
    public static final TextKey GIGABYTES = TextKey.of("jsc.portable.gigabytes", "%s GB");
    public static final TextKey CLOCK_MHZ = TextKey.of("jsc.portable.clock_mhz", "%s MHz");
    public static final TextKey CLOCK_CORES = TextKey.of("jsc.portable.clock_cores", "%s x %s GHz");
    public static final TextKey APP_NETWORK = TextKey.of("jsc.portable.app.network", "Network");
    public static final TextKey APP_ALERTS = TextKey.of("jsc.portable.app.alerts", "Alerts");
    public static final TextKey APP_CRYPTO_WALLET = TextKey.of("jsc.portable.app.crypto_wallet", "Crypto Wallet");
    public static final TextKey APP_MESSENGER = TextKey.of("jsc.portable.app.messenger", "Messenger");
    public static final TextKey APP_CAMERA = TextKey.of("jsc.portable.app.camera", "Camera");
    public static final TextKey APP_GALLERY = TextKey.of("jsc.portable.app.gallery", "Gallery");
    public static final TextKey APP_MAPS = TextKey.of("jsc.portable.app.maps", "Maps");
    public static final TextKey APP_PROSPECTING = TextKey.of("jsc.portable.app.prospecting", "Prospecting");
    public static final TextKey LABEL_WALLET = TextKey.of("jsc.portable.app.wallet_label", "Wallet");
    public static final TextKey LABEL_PROSPECT = TextKey.of("jsc.portable.app.prospect_label", "Prospect");
    /** What a portable is, in its tooltip: its era, its kind and its system. */
    public static final TextKey WHAT_IT_IS = TextKey.of("jsc.portable.tooltip.what", "%s · %s · %s");
    /** Its hardware, in its tooltip: its chip, its memory and its storage. */
    public static final TextKey HARDWARE = TextKey.of("jsc.portable.tooltip.hardware", "%s · %s memory · %s storage");
    /** Its hardware with a memory card beside its storage. */
    public static final TextKey HARDWARE_WITH_CARD = TextKey.of("jsc.portable.tooltip.hardware_card",
            "%s · %s memory · %s storage + %s card");
    public static final TextKey BATTERY = TextKey.of("jsc.portable.tooltip.battery", "Battery %s%%");
    /** Its link, while an access point reaches it: the Wi-Fi it goes at and how fast. */
    public static final TextKey LINKED = TextKey.of("jsc.portable.tooltip.linked", "Wi-Fi %s · %s items per tick");
    public static final TextKey NO_ACCESS_POINT = TextKey.of("jsc.portable.tooltip.no_access_point",
            "No access point in reach");
    public static final TextKey SWITCHED_OFF = TextKey.of("jsc.portable.tooltip.switched_off",
            "Switched off: its battery is flat");
    /** The qPad Pro's chip, Quince's own, named in its settings before its cores. */
    public static final TextKey CHIP_Q1 = TextKey.of("jsc.portable.chip.q1", "Quince Q1");
    /** A chip with a name of its own: the name, then its cores. */
    public static final TextKey CHIP_NAMED = TextKey.of("jsc.portable.chip.named", "%s, %s");
    /** The network's store as a place to build from, reached through a portable the player carries. */
    public static final TextKey BUILD_SOURCE = TextKey.of("jsc.portable.build_source", "Network storage");

    private PortableTexts() {
    }
}
