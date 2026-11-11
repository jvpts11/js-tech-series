---
section: iql_verbs
order: 372
---
# ANALYZE, VACUUM and REINDEX

The network keeps an index: its record of what its disks hold. ANALYZE, VACUUM and REINDEX look after that index. Each one may take one word after it, such as an object's name, and the network then ignores that word.

> [!DEFINE] index
> The network's record of what its disks hold. The maintenance verbs keep it up to date.

You run maintenance on the Mainframe. Typed on any other computer, it is refused. It needs no engine.

Table: The three maintenance verbs

| Verb | What it does, and its reply |
| --- | --- |
| `ANALYZE` | Reconciles the index. The reply is "ANALYZE complete - <n> types reconciled", with the number of item types it holds. |
| `VACUUM` | Frees the ghost entries. The reply is "VACUUM freed <n> ghost entries", with the number freed. |
| `REINDEX` | Rebuilds the index from the disks. The reply is "REINDEX started - rebuilding the catalog from disks", and the new index replaces the old one a short time later. |

## If something goes wrong

> [!PROBLEM] maintenance runs on the Mainframe only
> Type the command at the Mainframe itself.

> [!PROBLEM] the network has no running Mainframe
> Put a Mainframe on the network and let it run ([](jsc:mainframes)).

> [!PROBLEM] unexpected token: <word>
> A maintenance verb takes one word at most. Remove the extra word.

> [!SEE] jsc:mainframes, jsc:iql_limits
