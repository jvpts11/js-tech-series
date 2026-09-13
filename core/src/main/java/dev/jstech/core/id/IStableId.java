/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.id;

/**
 * An enum constant with a number of its own, which saves, packets and menu data carry instead of the constant's
 * position in the declaration. Constants can then be reordered, and new ones added anywhere, without a number already
 * written somewhere coming back as a different constant.
 *
 * <p>Look constants up with a {@link StableIds} table built once per enum.
 */
public interface IStableId {

    /** The constant's number: unique within its enum, between 0 and {@link StableIds#MAX_ID}, and never reused. */
    int id();
}
