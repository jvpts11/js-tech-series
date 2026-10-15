/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.signal;

import java.util.UUID;
import net.minecraft.core.GlobalPos;
import org.jetbrains.annotations.Nullable;

/**
 * A frequency heard at a place, as a signal probe shows it: whose it is, where it comes from, how well it arrives and
 * the value it carries; a clash carries none. A frequency of the listener's team not heard there has no bars.
 *
 * @param frequency the frequency
 * @param owner     whose its transmitter is; none for nobody's
 * @param from      where it is heard best from: its transmitter, or a relay that sends it again; its transmitter
 *                  when it is not heard
 * @param origin    where its transmitter is, in this dimension or another
 * @param relayed   whether it is heard from a relay
 * @param bars      how well it arrives, 0 (not heard there) to {@link RadioReach#MOST_BARS}
 * @param value     the value it carries, 0 to 15; 0 in a clash
 * @param clash     whether two transmitters clash on it
 */
public record Heard(Frequency frequency, @Nullable UUID owner, GlobalPos from, GlobalPos origin, boolean relayed,
                    int bars, int value, boolean clash) {
}
