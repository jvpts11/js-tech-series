/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Computers.
 */
package dev.jstech.computers.datacenter;

import dev.jstech.computers.eventlog.EventLevel;
import dev.jstech.core.text.Text;

/**
 * One alert standing on a datacenter section: a server's disks nearly full, a rack over its thermal budget, the
 * router at its rack budget, or the latency over twice the base.
 *
 * @param key   what tells it apart from the section's other alerts, and from itself a second later
 * @param level how much it matters
 * @param what  what it says
 */
public record SectionAlert(String key, EventLevel level, Text what) {
}
