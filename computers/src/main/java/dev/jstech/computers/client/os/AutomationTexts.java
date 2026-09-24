/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Computers.
 */
package dev.jstech.computers.client.os;

import dev.jstech.core.text.TextHolder;
import dev.jstech.core.text.TextKey;

/**
 * What the Automation Manager says: its job list, the new-job form and the engine's line. Kept apart from the window
 * so the language generator can read it on a server too, where windows do not exist.
 */
@TextHolder
final class AutomationTexts {

    // The engine and the list.
    static final TextKey CONTACTING = TextKey.of("jsc.automation.contacting", "Contacting Mainframe...");
    static final TextKey ONLINE = TextKey.of("jsc.automation.online", "%s online");
    static final TextKey NO_ENGINE =
            TextKey.of("jsc.automation.install_engine", "No engine - install the Automation Engine on the Mainframe");
    static final TextKey JOBS = TextKey.of("jsc.automation.jobs", "%s jobs");
    static final TextKey JOB_COLUMN = TextKey.of("jsc.automation.job_column", "JOB");
    static final TextKey TYPE_COLUMN = TextKey.of("jsc.automation.type_column", "TYPE");
    static final TextKey TRIGGER_COLUMN = TextKey.of("jsc.automation.trigger_column", "TRIGGER");
    static final TextKey ACT_COLUMN = TextKey.of("jsc.automation.act_column", "ACT");
    static final TextKey NO_JOBS = TextKey.of("jsc.automation.no_jobs", "No jobs yet - create one below.");

    // The new-job form.
    static final TextKey NEW_JOB = TextKey.of("jsc.automation.new_job", "NEW JOB");
    static final TextKey KEEP_STOCK = TextKey.of("jsc.automation.form.keep_stock", "Keep Stock");
    static final TextKey BATCH_CRAFT = TextKey.of("jsc.automation.form.batch_craft", "Batch Craft");
    static final TextKey MOVE = TextKey.of("jsc.automation.form.move", "Move");
    static final TextKey IQL = TextKey.of("jsc.automation.form.iql", "IQL");
    static final TextKey NAME = TextKey.of("jsc.automation.form.name", "Name");
    static final TextKey ITEM_OR_ALL = TextKey.of("jsc.automation.form.item_or_all", "Item (blank=all)");
    static final TextKey ITEM_ID = TextKey.of("jsc.automation.form.item_id", "Item id");
    static final TextKey KEEP_AT_LEAST = TextKey.of("jsc.automation.form.keep_at_least", "Keep at least");
    static final TextKey AMOUNT = TextKey.of("jsc.automation.form.amount", "Amount");
    static final TextKey EVERY = TextKey.of("jsc.automation.form.every", "Every (30s)");
    static final TextKey FROM = TextKey.of("jsc.automation.form.from", "From");
    static final TextKey TO = TextKey.of("jsc.automation.form.to", "To");
    static final TextKey SCRIPT = TextKey.of("jsc.automation.form.script", "Script (on Mainframe disk):");
    static final TextKey NO_SCRIPTS =
            TextKey.of("jsc.automation.form.no_scripts", "no .iql files - save one in the NMS");
    static final TextKey CREATE = TextKey.of("jsc.automation.form.create", "Create job");

    private AutomationTexts() {
    }
}
