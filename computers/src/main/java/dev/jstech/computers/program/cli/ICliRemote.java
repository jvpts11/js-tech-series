/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Computers.
 */
package dev.jstech.computers.program.cli;

import java.util.List;

/**
 * What a command reaches of the other machines of a computer's network: which of them a remote shell could reach, and
 * opening and closing one.
 *
 * <p>Every member answers as a computer that cannot open a remote shell would.
 */
public interface ICliRemote {

    /** Every machine on this network a remote shell could reach, by host name. */
    default List<ICliComputer.RemoteHost> reachableHosts() {
        return List.of();
    }

    /**
     * Opens a remote shell on {@code hostname}: from here on the session's commands run on that
     * machine until it is closed. Same network means access, and authentication arrives with the
     * security module.
     */
    default ICliComputer.OpResult sshConnect(final String hostname) {
        return ICliComputer.OpResult.fail("ssh: not supported on this computer");
    }

    /** Closes the remote shell and returns to the local one; fails when there is no session. */
    default ICliComputer.OpResult sshDisconnect() {
        return ICliComputer.OpResult.fail("exit: not connected");
    }

    /** The host name of the machine this session is connected to, or {@code ""} when local. */
    default String sshSession() {
        return "";
    }
}
