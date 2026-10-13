/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Computers.
 */
package dev.jstech.computers.api.planner;

import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import org.jetbrains.annotations.ApiStatus;

/**
 * A rule another mod adds to the planner of an engine that takes extensions: it looks at every plan the planner
 * weighs for a craft and may change what the plan costs, or set it aside. A player switches each rule on or off for
 * their own Mainframe; a new rule starts on.
 */
@ApiStatus.Experimental
public interface IPlannerRule {

    /** The rule's id, under which a Mainframe remembers whether it is on. */
    ResourceLocation id();

    /** The rule's name, as the list of rules shows it. */
    Component name();

    /** What the rule does, in a sentence. */
    Component description();

    /** Weighs one plan. Called for every plan the planner considers while the rule is on. */
    void weigh(PlanCandidate candidate);
}
