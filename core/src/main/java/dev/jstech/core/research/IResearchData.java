/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.research;

/** An item that is data of a kind, which a research computer takes and spends as its research goes. */
public interface IResearchData {

    /** The kind of data it is. */
    DataKind dataKind();
}
