/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.cable;

/**
 * A device whose port a wire's plug enters by its kind, as on a real machine: a screen's video port, a loudspeaker's
 * audio port. A cable that has a plug of that kind ({@link CableType#plug(String)}) ends in it there, and in its own
 * plug at any other device.
 */
public interface IPlugged {

    /** The name of the kind of port a plug enters on it: {@code video}, {@code audio}, {@code device}. */
    String plugKind();
}
