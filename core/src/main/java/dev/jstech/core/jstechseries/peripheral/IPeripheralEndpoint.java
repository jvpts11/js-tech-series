/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.jstechseries.peripheral;

import dev.jstech.core.cable.IPlugged;
import java.util.Optional;

/**
 * Contract for a BlockEntity that is the LINKED endpoint of a peripheral connection, the "client" side of the link.
 * A peripheral cable's plug enters it by its kind of port, a video plug at a screen.
 */
public interface IPeripheralEndpoint extends IPlugged {

    PeripheralCableType cableType();

    Optional<Long> linkedOwner();

    void onOwnerLinked(long ownerPos);

    void onOwnerUnlinked();

    /** The kind of port the peripheral takes on its owner; a device port unless it is a screen or a speaker. */
    default PortKind portKind() {
        return PortKind.DEVICE;
    }

    /** The plug a peripheral cable ends in at it is its kind of port's. */
    @Override
    default String plugKind() {
        return portKind().serializedName();
    }
}
