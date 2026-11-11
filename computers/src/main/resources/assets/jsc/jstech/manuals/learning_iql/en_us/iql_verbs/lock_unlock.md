---
section: iql_verbs
order: 371
---
# LOCK and UNLOCK

LOCK holds items where they are. Another Operation that asks for the same items then waits, instead of taking them. UNLOCK lets them go again. Use LOCK to keep stock in place while you need it.

```iql hold.iql
LOCK 64 iron_ingot
UNLOCK iron_ingot
```

With no quantity, or with ALL, LOCK holds every free item of that kind. UNLOCK uses only the item, so a quantity you type after UNLOCK has no effect.

LOCK and UNLOCK work with no engine. PRIORITY is accepted on them, but it changes nothing. The Task Manager shows what is locked ([](jsc:system_monitor_and_task_manager)).

Table: The replies of LOCK and UNLOCK

| Reply | What it means |
| --- | --- |
| `LOCK held <quantity> <item> (concurrent operations will wait)` | The items are held, and other Operations wait for them. |
| `<item> is already locked` | You already locked this item by hand. |
| `nothing to lock: the network holds no free <item>` | Every copy is in use, or none is on the network. |
| `UNLOCK released <quantity> <item>` | The hold is gone. |
| `<item> is not locked` | There was no hold to release. |

## If something goes wrong

> [!PROBLEM] <item> is already locked
> The item stays held. Type UNLOCK with its name when you want it free.

> [!PROBLEM] nothing to lock: the network holds no free <item>
> Wait until the network has free stock, then lock it again.

> [!PROBLEM] <item> is not locked
> Check the item's name. UNLOCK only releases a hold that you put by hand.

> [!SEE] jsc:operation_states, jsc:states_and_queues
