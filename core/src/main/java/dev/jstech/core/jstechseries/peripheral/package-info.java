/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */

/**
 * Peripheral links for any mod: the short point-to-point cables between a machine and its own devices (its screens,
 * speakers, drives and sensors), and between a controller and the machine it runs, each device linked to one owner by
 * following its cable. A link is no part of any network and carries no network's identity.
 *
 * <p>An owner, as a computer is, implements {@link IPeripheralOwner}, or {@link IPeripheralOwnerSupport} to keep
 * its links in a {@link PeripheralPorts} and say only how many ports of each {@link PortKind} its hardware gives
 * it: a video output for a screen, an audio output for a speaker, a device port for the rest. A device implements
 * {@link IPeripheralEndpoint}, says the kind of port it takes, and keeps a {@link PeripheralLink} as one of its
 * block entity's fields, saved and sent to the players who see it, which once a tick finds an owner along a cable
 * of its {@link PeripheralCableType}, or drops a link that no longer holds. A link that cannot be made says why
 * ({@link ILinkResult}): another owner has it, the owner has no free port of its kind, there is no path, the path is
 * longer than the cable's range, or a cable of another system lies on the way. An {@link IPeripheralHub} takes one
 * port of its owner and offers several, the cable after it reaching as far again; a computer's line, with a generation
 * of cable for each era, is {@link PeripheralLine}; and {@link PeripheralReconciler} frees the ports of devices that
 * went away while their owner was not loaded.
 *
 * <p>How a link is made, from zero, is in {@code core/docs/NETWORKS.md}. Examples that compile are J's Computers'
 * monitor ({@code computers/src/main/java/dev/jstech/computers/blockentity/MonitorBlockEntity.java}) and the
 * computers that own it
 * ({@code computers/src/main/java/dev/jstech/computers/blockentity/AbstractComputerBlockEntity.java}).
 */
@ApiStatus.Experimental
package dev.jstech.core.jstechseries.peripheral;

import org.jetbrains.annotations.ApiStatus;
