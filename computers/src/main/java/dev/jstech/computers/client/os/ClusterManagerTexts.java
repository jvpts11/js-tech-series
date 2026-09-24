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
 * What the Cluster Manager says: its tabs, lists, tables, buttons and dialogs. The names of clusters, nodes, systems
 * and programs, and a rack position such as R1 U3, are data. Kept apart from the window so the language generator
 * can read it on a server too, where windows do not exist.
 */
@TextHolder
final class ClusterManagerTexts {

    // The tabs and the cluster list.
    static final TextKey SUPERCOMPUTERS_TAB = TextKey.of("jsc.cluster_manager.supercomputers_tab", "Supercomputers");
    static final TextKey DATACENTERS_TAB = TextKey.of("jsc.cluster_manager.datacenters_tab", "Datacenters");
    static final TextKey AI_TAB = TextKey.of("jsc.cluster_manager.ai_tab", "AI");
    static final TextKey REACHING = TextKey.of("jsc.cluster_manager.reaching", "Reaching the network ...");
    static final TextKey CARD_REQUIRED =
            TextKey.of("jsc.cluster_manager.card_required", "A Cluster Interface Card is required.");
    static final TextKey SUPERCOMPUTERS = TextKey.of("jsc.cluster_manager.supercomputers", "SUPERCOMPUTERS · %s");
    static final TextKey SECTIONS = TextKey.of("jsc.cluster_manager.sections", "SECTIONS · %s");
    static final TextKey AI_CLUSTERS = TextKey.of("jsc.cluster_manager.ai_clusters", "AI CLUSTERS · %s");
    static final TextKey NONE_YET = TextKey.of("jsc.cluster_manager.none_yet", "none yet");
    static final TextKey NONE_ON_NETWORK = TextKey.of("jsc.cluster_manager.none_on_network", "none on this network");
    static final TextKey NO_AI_CLUSTERS =
            TextKey.of("jsc.cluster_manager.no_ai_clusters", "No AI clusters on this network.");
    static final TextKey SELECT_ONE = TextKey.of("jsc.cluster_manager.select_one", "Select a cluster on the left.");

    // The selected cluster.
    static final TextKey ONLINE = TextKey.of("jsc.cluster_manager.online", "ONLINE");
    static final TextKey OFFLINE = TextKey.of("jsc.cluster_manager.offline", "OFFLINE");
    static final TextKey RENAME = TextKey.of("jsc.cluster_manager.rename", "RENAME");
    static final TextKey NODES_TAB = TextKey.of("jsc.cluster_manager.nodes_tab", "NODES");
    static final TextKey MAP_TAB = TextKey.of("jsc.cluster_manager.map_tab", "CLUSTER MAP");
    static final TextKey QUEUE_TAB = TextKey.of("jsc.cluster_manager.queue_tab", "QUEUE");
    static final TextKey SERVERS_TAB = TextKey.of("jsc.cluster_manager.servers_tab", "SERVERS");
    static final TextKey INVENTORY_TAB = TextKey.of("jsc.cluster_manager.inventory_tab", "INVENTORY");
    /* A job running on it: what it writes, how far it got, and the first node being written. */
    static final TextKey INSTALLING_JOB = TextKey.of("jsc.cluster_manager.installing_job", "Installing %s · %s of %s");
    static final TextKey CANCELLING = TextKey.of("jsc.cluster_manager.cancelling", "%s · cancelling");
    static final TextKey WITH_LANE = TextKey.of("jsc.cluster_manager.with_lane", "%s · %s %s%%");

    // The tables.
    static final TextKey RACK_UNIT_COLUMN = TextKey.of("jsc.cluster_manager.rack_unit_column", "RACK/U");
    static final TextKey NODE_COLUMN = TextKey.of("jsc.cluster_manager.node_column", "NODE");
    static final TextKey SYSTEM_COLUMN = TextKey.of("jsc.cluster_manager.system_column", "SYSTEM");
    static final TextKey STATUS_COLUMN = TextKey.of("jsc.cluster_manager.status_column", "STATUS");
    static final TextKey PHI_COLUMN = TextKey.of("jsc.cluster_manager.phi_column", "PHI");
    static final TextKey USED_COLUMN = TextKey.of("jsc.cluster_manager.used_column", "USED");
    static final TextKey SLOT_COLUMN = TextKey.of("jsc.cluster_manager.slot_column", "SLOT");
    static final TextKey CRAFTS_COLUMN = TextKey.of("jsc.cluster_manager.crafts_column", "CRAFTS");
    static final TextKey STATE_COLUMN = TextKey.of("jsc.cluster_manager.state_column", "STATE");
    static final TextKey OPERATION_COLUMN = TextKey.of("jsc.cluster_manager.operation_column", "OPERATION");
    static final TextKey BY_COLUMN = TextKey.of("jsc.cluster_manager.by_column", "BY");
    static final TextKey SLOTS_COLUMN = TextKey.of("jsc.cluster_manager.slots_column", "SLOTS");
    static final TextKey NO_NODES = TextKey.of("jsc.cluster_manager.no_nodes", "no nodes seated");
    static final TextKey PAST_THE_SLOTS =
            TextKey.of("jsc.cluster_manager.past_the_slots", "%s node(s) past the six slots · inert");
    static final TextKey NO_CRAFTS = TextKey.of("jsc.cluster_manager.no_crafts", "no crafts in this queue");
    static final TextKey INVENTORY_HINT = TextKey.of("jsc.cluster_manager.inventory_hint",
            "%s kinds · click to move out · drop a stack to deposit");
    static final TextKey SECTION_EMPTY = TextKey.of("jsc.cluster_manager.section_empty", "the section is empty");
    static final TextKey NO_NODE = TextKey.of("jsc.cluster_manager.no_node", "no node");
    static final TextKey CRAFT = TextKey.of("jsc.cluster_manager.craft", "CRAFT %s");
    static final TextKey WAITING = TextKey.of("jsc.cluster_manager.waiting", "WAITING");

    // What a node is doing.
    static final TextKey INCOMPLETE = TextKey.of("jsc.cluster_manager.state.incomplete", "INCOMPLETE");
    static final TextKey BAY_OFF = TextKey.of("jsc.cluster_manager.state.bay_off", "BAY OFF");
    static final TextKey INSTALLING = TextKey.of("jsc.cluster_manager.state.installing", "INSTALLING");
    static final TextKey NO_PHI = TextKey.of("jsc.cluster_manager.state.no_phi", "NO PHI");
    static final TextKey PHI_LOW = TextKey.of("jsc.cluster_manager.state.phi_low", "PHI LOW");
    static final TextKey INERT = TextKey.of("jsc.cluster_manager.state.inert", "INERT");
    static final TextKey NO_SYSTEM = TextKey.of("jsc.cluster_manager.state.no_system", "NO SYSTEM");
    /* A cluster slot's own reading of the node in it. */
    static final TextKey RATING_LOW = TextKey.of("jsc.cluster_manager.slot.rating_low", "RATING LOW");
    static final TextKey NO_PHI_CARD = TextKey.of("jsc.cluster_manager.slot.no_phi_card", "NO PHI CARD");
    static final TextKey SLOT_NO_NODE = TextKey.of("jsc.cluster_manager.slot.no_node", "NO NODE");

    // The buttons along the foot.
    static final TextKey SYSTEM_ALL = TextKey.of("jsc.cluster_manager.system_all", "SYSTEM ALL");
    static final TextKey PROGRAM_ALL = TextKey.of("jsc.cluster_manager.program_all", "PROGRAM ALL");
    static final TextKey ALL_ON = TextKey.of("jsc.cluster_manager.all_on", "ALL ON");
    static final TextKey ALL_OFF = TextKey.of("jsc.cluster_manager.all_off", "ALL OFF");
    static final TextKey CANCEL_JOB = TextKey.of("jsc.cluster_manager.cancel_job", "CANCEL JOB");
    static final TextKey BALANCE = TextKey.of("jsc.cluster_manager.balance", "BALANCE: %s");

    // The node dialog.
    static final TextKey NODE_TITLE = TextKey.of("jsc.cluster_manager.node_title", "NODE · %s  R%s U%s");
    static final TextKey NO_SYSTEM_INSTALLED =
            TextKey.of("jsc.cluster_manager.no_system_installed", "No system installed");
    static final TextKey NO_PROGRAMS = TextKey.of("jsc.cluster_manager.no_programs", "No programs");
    static final TextKey CLUSTER_SLOT = TextKey.of("jsc.cluster_manager.cluster_slot", "Cluster slot %s");
    static final TextKey BAY_ON_LINE = TextKey.of("jsc.cluster_manager.bay_on", "Bay on");
    static final TextKey BAY_OFF_LINE = TextKey.of("jsc.cluster_manager.bay_off", "Bay off");
    static final TextKey POWER_ON = TextKey.of("jsc.cluster_manager.power_on", "POWER ON");
    static final TextKey POWER_OFF = TextKey.of("jsc.cluster_manager.power_off", "POWER OFF");
    static final TextKey SYSTEM = TextKey.of("jsc.cluster_manager.system", "SYSTEM");
    static final TextKey PROGRAM = TextKey.of("jsc.cluster_manager.program", "PROGRAM");
    static final TextKey CLOSE = TextKey.of("jsc.cluster_manager.close", "CLOSE");

    // The move-out dialog.
    static final TextKey MOVE_OUT_TITLE = TextKey.of("jsc.cluster_manager.move_out_title", "MOVE OUT · %s");
    static final TextKey QUANTITY = TextKey.of("jsc.cluster_manager.quantity", "QUANTITY  (%s available)");
    static final TextKey MAX = TextKey.of("jsc.cluster_manager.max", "MAX");
    static final TextKey NO_DESTINATION = TextKey.of("jsc.cluster_manager.no_destination", "no destination");
    static final TextKey TO = TextKey.of("jsc.cluster_manager.to", "TO %s");
    static final TextKey MOVE = TextKey.of("jsc.cluster_manager.move", "MOVE");
    static final TextKey CANCEL = TextKey.of("jsc.cluster_manager.cancel", "CANCEL");

    // The rename dialog.
    static final TextKey RENAME_TITLE = TextKey.of("jsc.cluster_manager.rename_title", "RENAME CLUSTER");
    static final TextKey RENAME_HINT =
            TextKey.of("jsc.cluster_manager.rename_hint", "Type a name; empty goes back to the default.");
    static final TextKey APPLY = TextKey.of("jsc.cluster_manager.apply", "APPLY");

    // Where a node sits: its rack, then its unit in that rack.
    static final TextKey RACK_UNIT = TextKey.of("jsc.cluster_manager.rack_unit", "R%s U%s");

    private ClusterManagerTexts() {
    }
}
