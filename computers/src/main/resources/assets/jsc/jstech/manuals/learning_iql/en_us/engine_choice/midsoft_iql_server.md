---
section: engine_choice
order: 385
icon: jsc:mainframe
---
# Midsoft IQL Server

The Midsoft IQL Server is the engine every Mainframe starts with. It speaks IQL, the language of the network, and it is the only one of the three engines that keeps views and procedures. You save a query under a name as a view, or as a procedure that you run by name with EXEC ([](jsc:iql_views), [](jsc:iql_procedures)).

```table
{"caption": "The Midsoft IQL Server", "rows": [[{"words": "Dialect"}, {"words": "IQL"}], [{"words": "Extras"}, {"words": "procedures and views"}], [{"words": "Memory"}, {"amount": 24, "unit": "MB"}], [{"words": "Ages"}, {"words": "Vintage to Advanced"}]]}
```

The version depends on the age of the Mainframe it is installed on. The Services tab shows the version of the installed engine.

Table: Versions by the age of the Mainframe

| Age of the Mainframe | Midsoft IQL Server version |
| --- | --- |
| Vintage | 4.2 |
| Legacy | 2000 |
| Transition | 2008 |
| Standard | 2012 |
| Advanced | 2022 |

Versions compare by their numbers, so version 4.2 is older than version 2000. Your Mainframe can only run the version made for its own age.

On a Legacy or newer Mainframe, the IQL Server Management Studio (ISMS) works with the Midsoft ([](jsc:studio_overview)). The ISMS needs version 2000 or newer. A Vintage Mainframe has no studio, so you work with the Midsoft from the prompt.

The Midsoft has no EXPLAIN, KEEP or WATCH. Those words belong to NextgreIQL and Prophet YourIQL, and the Midsoft refuses them with a syntax message.

The Midsoft does not keep jobs either. A job needs the Automation Engine installed on the Mainframe ([](jsc:automation_engine)).

## If something goes wrong

> [!PROBLEM] You type CREATE JOB, and the network says jobs are kept by the Automation Engine.
> Install the Automation Engine on the Mainframe first.

> [!PROBLEM] You type EXPLAIN, KEEP or WATCH, and the answer starts with "syntax:".
> Those words belong to NextgreIQL or Prophet YourIQL. Use the engine that offers them.

> [!SEE] jsc:iql_procedures, jsc:engine_services
