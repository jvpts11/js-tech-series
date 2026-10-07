# Security

A network **without** security lets everyone do everything, and that is how it is played until someone sets security up:
no base breaks because security exists. Today the only refusal is a computer refusing other machines' programs, with
`config remote off`; everything else on this page is still to build.

## To build

### The directory

Security is **software**, as in real life: a **directory** service (the equivalent of Active Directory or LDAP)
installed on a rack server or on the Mainframe. It keeps the network's users, groups and permissions. The server that
runs it is the network's **Security Control Computer**: there is no block of its own, it is a role. There can be more
than one, for redundancy.

What is managed in it (in an administration program, and on the command line):

- users and groups, with permissions inherited through groups;
- permissions (ACLs) per computer, per item, per item tag and per Operation type (GRANT and REVOKE,
  [Operations](operations.md));
- ID Cards: issuing them, and revoking them (the blacklist);
- the Network Manager's permissions: seeing other players' Operations, seeing every machine's hardware, cancelling other
  players' Operations;
- who may run a DROP;
- who may unlock items by hand;
- permissions per group across the WAN Gateway Computer's federation ([The network](network.md));
- who sees which alerts on the portable devices ([Wireless](wireless.md));
- the **audit log**: who did what, with filters by player, machine, period and Operation type (the network's event log
  is another one, [Operations](operations.md)).

**Where it is checked:** when the dispatch admits each Operation, from cached rules, O(1) per Operation and nothing per
tick. GRANT, REVOKE and AUTH are security's Operations; a network without a directory allows everything.

### Who is who

Permissions belong to **players**: one account per player (their id), with groups. With FTB Teams installed, a group can
follow a team. The base roles are templates the administrator adjusts:

| Role | Typical permissions |
| --- | --- |
| Admin | everything: every Operation, security, the whole Network Manager |
| Engineer | SELECT, INSERT, CRAFT, UPDATE; no DROP, no DELETE of valuable items |
| Operator | SELECT, INSERT; no CRAFT by hand, no UPDATE |
| Guest | only SELECT of items marked as public |
| Public | a player with no account: no Operation by default (configurable) |

**The systems' accounts:** every operating system gets real accounts and passwords (the `passwd` of every Linux and
Unix, Frames' accounts, the users page in Settings). With a directory on the network, a computer's login is the
network's, as in a domain ([Operating systems](operating-systems.md)).

### ID Cards

ID Cards serve **physical** access (doors, terminals) and prove who you are on a machine that isn't yours. They come per
era: a magnetic card (Legacy), a smart card (Transition), NFC (Standard) and biometric (Advanced).

- The **ID Card Encoder** and the **ID Card Reader** are devices, each on a device port of a computer
  ([Peripherals](peripherals.md)).
- The Encoder writes on a blank card its holder, its groups, an optional expiry and further restrictions.
- The Reader fires an AUTH when a player clicks it with the card: **OK** carries out the action (the door opens, by
  redstone, like the Redstone Interface; the terminal accepts); **DENIED** blocks; **EXPIRED** refuses a card past its
  date; **BLACKLISTED** refuses a revoked card, whatever it says. Every refusal goes into the audit log.

Physical access is the cards'; permissions on the network are the players'.

### Hardware security modules

HSMs (hardware security modules) are the specialised hardware of the server that runs the directory: cards that go in
it, like real HSMs, which are PCIe cards in the servers of banks, certificate authorities and governments. If someone
tries to open the chip, it destroys its keys.

The network's **security level** is the directory server's base level plus the bonus of each HSM installed, at most 10,
however many modules there are. It is what an attack has to beat.

| Module | Era | Bonus |
| --- | --- | --- |
| Basic HSM | Standard | +1 |
| Advanced HSM | Advanced | +2 |
| Military HSM | Advanced | +4 |

The bonuses are estimates. The modules already exist as art (texture and model), with no registration.

### Offensive security

Hacking and its kinds, viruses (CPU_DRAIN, NETWORK_SNIFFER, INDEX_CORRUPTOR, RANSOMWARE, BACKDOOR), the antivirus, the
IDS and the firewall come in a second phase, after the base above. They are designed then, with the HSMs per era and
today's capacity figures (the Threadkiller 7995WX does 43,776 items per tick). Viruses and hacking are among what can
corrupt storage ([Storage](storage.md)), and they come with their triggers for the index (INDEX_CORRUPTOR, and a hack
that corrupts the index). A server setting turns hacking off on servers without PvP.
