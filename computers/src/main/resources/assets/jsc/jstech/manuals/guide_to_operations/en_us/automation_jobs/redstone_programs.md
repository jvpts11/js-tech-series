---
section: automation_jobs
order: 154
icon: jsc:standard_redstone_interface
---
# Redstone in programs

A program can read a redstone signal, or make one, through a Redstone Interface ([](jsc:redstone_interfaces)). You place the interface, cable it to your computer, and let a program read it or set it.

Name the interface in its window first. A program finds the interface by that name. The name takes up to 32 characters, and it must be unique on its computer, whatever the capitals. An empty name answers to "Redstone Interface".

If you name a second interface with a name the first one has, the window refuses it with "Another interface already has this name".

## From IQL

The SET REDSTONE statement makes an interface read or emit. The IQL book explains it in full ([](jsc:set_redstone)).

Table: SET REDSTONE statements

| Statement | What it does |
| --- | --- |
| `SET REDSTONE 'Gate' IN` | Makes the interface named Gate read. |
| `SET REDSTONE 'Gate' OUT 15` | Makes the interface named Gate emit a strength of 15. |
| `SET REDSTONE 'Main Gate' IN` | The same, for a name with a space. Quotes go round the name. |

The strength runs from 0 to 15. A statement typed at a prompt marks the setting as made by IQL, and the window then shows "Set by IQL".

## From Sigma

A Sigma program reaches an interface through redstone("Gate"). The Sigma book explains its library ([](jsc:redstone_library)).

Table: Sigma calls on an interface

| Call | What it does |
| --- | --- |
| `redstone("Gate")` | Finds the interface named Gate on the computer the program runs on. |
| `.In()` | Makes the interface read. |
| `.Out(15)` | Makes the interface emit 15, from 0 to 15. |
| `.Level()` | Returns the strength it reads or emits, or 0 while it has no power. |

```sgs Lamp.sgs
redstone("Gate").Out(15);
```

Each call marks the setting with the name of the program that made it, so the window shows that program.

## Limits

A program reaches only the interfaces on its own computer. An interface on another computer is not found, even when both are on the same network.

A SET REDSTONE statement in a job runs each time the job fires, so a job can open a door on a timer ([](jsc:automation_engine)).

## If something goes wrong

> [!PROBLEM] IQL says "no Redstone Interface named Gate is linked to this machine".
> Check the spelling of the name in the interface's window. Check that its cable reaches this computer, and that the interface is not disabled.

> [!PROBLEM] Sigma stops the program and says the interface is not linked to this machine.
> Check the name, and check the cable and the disabled setting, as above.

> [!PROBLEM] IQL says "SET REDSTONE needs IN, or OUT and a strength from 0 to 15".
> Write IN, or OUT followed by a number from 0 to 15.

> [!PROBLEM] IQL says the strength is not a redstone strength, which runs from 0 to 15.
> Use a number from 0 to 15.

> [!PROBLEM] Sigma stops the program, because the strength is not from 0 to 15.
> Pass a number from 0 to 15 to Out.

> [!PROBLEM] The window says "Off, no power".
> The interface is not live. Check its peripheral cable, and check that the interface is not disabled.

> [!PROBLEM] The window refuses a name with "Another interface already has this name".
> Give this interface another name.

> [!SEE] jsc:redstone_interfaces, jsc:set_redstone, jsc:redstone_library, jsc:peripheral_cables
