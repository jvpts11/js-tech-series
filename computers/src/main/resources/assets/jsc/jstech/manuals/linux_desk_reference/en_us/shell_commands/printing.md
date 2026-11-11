---
section: shell_commands
order: 257
---
# Printing from the terminal

You print from the terminal with two commands. The lp command sends files to a printer, and the lpstat command shows the print requests and the printers.

> [!DEFINE] Destination
> The printer that a print job goes to.

> [!DEFINE] Copies
> How many prints of each file one job makes.

Name the destination with -d, or leave it out to use the default one. The number of copies comes with -n.

```command
id: jsc:command/linux/lp
```

```command
id: jsc:command/linux/lpstat
```

## If something goes wrong

> [!PROBLEM] The lp command says there is no default destination.
> Name a printer with -d, and check its name with lpstat.

> [!PROBLEM] The lp command says the destination is non-existent.
> Type the name exactly as lpstat shows it.

> [!PROBLEM] The lp command says there are no files to print.
> Give at least one file name after the command.

> [!SEE] jsc:shell_commands, jsc:pipes_and_variables
