---
section: openbsd_tools
order: 347
---
# pf and doas

pf is OpenBSD's firewall. It always runs, its rules are in /etc/pf.conf, and pfctl -si says how long it has run and what its state table holds. What its rules let through and what they stop comes with the network's security.

```conf /etc/pf.conf
# See pf.conf(5) and /etc/examples/pf.conf

set skip on lo

block return    # block stateless traffic
pass            # establish keep-state
```

```command
id: jsc:command/openbsd/pfctl
```

doas runs a command as another user, root unless -u names one. Its one rule, permit persist :wheel in /etc/doas.conf, lets the wheel group do it, and root is in the wheel group. Until there are accounts you are root, so doas runs what you give it.

doas -C /etc/doas.conf and a command say whether the rules permit it, without running it.

```command
id: jsc:command/openbsd/doas
```

> [!SEE] jsc:openbsd_daemons
