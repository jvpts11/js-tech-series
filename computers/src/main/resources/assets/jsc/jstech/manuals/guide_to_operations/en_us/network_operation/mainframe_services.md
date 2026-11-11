---
section: network_operation
order: 107
icon: jsc:mainframe
---
# Services on the Mainframe

The Mainframe carries three kinds of software: an engine that decides the network's work, the Automation Engine that runs saved jobs, and the Mirror ([](jsc:mirror)). A new Mainframe ships with the Midsoft IQL Server installed and running, in the version of its age.

The services of a Mainframe are on or off. They serve only while the Mainframe is powered, and they are removed when its disk is formatted.

Table: The engines of the Mainframe

| Engine | What it does |
| --- | --- |
| Midsoft IQL Server | Plans the network's work and answers it in IQL. The ISMS is its studio. ([](jsc:engines)) |
| NextgreIQL | Shows how it plans the network's work, and lets you take part in the planning. |
| Prophet YourIQL | You tell it which state the network is to keep, and it works out how to get there and stay there. |

Only one engine plans the network's work at a time. An engine that you install becomes the active one when the network has none. Activating an installed engine makes it the one that plans, and the work the previous engine planned carries on, since an Operation belongs to the network once it is made.

Replacing an engine takes a while, and the network has no engine until the replacement is over. The replacement goes through these steps, in order:

1. Stop taking new plans.
2. Stop the old engine.
3. Start the new engine.
4. Discover the storage and the machines.
5. Build the indexes.
6. Publish the capabilities.
7. Wait until the screen says Ready.

The Automation Engine keeps the network's jobs and fires them on time. You write and supervise those jobs in the Automation Manager ([](jsc:automation_engine)).

## If something goes wrong

> [!PROBLEM] The Services tab says No Network Operations Engine is running.
> Install an engine, or start the installed one.

## If something goes wrong

> [!PROBLEM] A replacement will not start.
> Another replacement is under way, the engine is not installed, the Mainframe is off, or the engine is already the running one.
