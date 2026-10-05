/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.guide;

import java.util.Objects;

/**
 * A section of a mod's chapter, such as "Hardware": a run of entries under one title, numbered in the manual
 * ("3.2") and listed in its contents.
 *
 * @param id       the section's id, {@code namespace:path}, the namespace being its mod's
 * @param order    where it sits among its chapter's sections, smallest first
 * @param titleKey its title's sentence
 * @param icon     the item drawn beside it on the chapter's opening page, or empty
 */
public record GuideSection(String id, int order, String titleKey, String icon) {

    public GuideSection {
        Objects.requireNonNull(id, "id");
        Objects.requireNonNull(titleKey, "titleKey");
        icon = icon == null ? "" : icon;
    }

    /** The chapter the section belongs to: the namespace of its id. */
    public String chapter() {
        return GuideIds.namespace(this.id);
    }
}
