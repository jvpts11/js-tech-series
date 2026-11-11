---
section: openbsd_tools
order: 348
---
# When the system panics: ddb

When OpenBSD's kernel panics, it stops in its debugger, ddb, and waits at the ddb> prompt. It never starts over by itself: you read what happened, and then you start it over.

Table: What ddb takes

| Command | What it shows |
| --- | --- |
| `show panic` | What the kernel stopped on. |
| `trace` | Where it was when it stopped. |
| `ps` | The processes that were running. |
| `boot reboot` | Starts the computer over. |

> [!SEE] jsc:openbsd_overview
