---
section: prompt_system
order: 243
---
# Printing from the prompt

print sends text files to the printer in the background, so the prompt is free while the files print. You need a printer linked to the computer ([](jsc:printers)).

The first time you use print, it says "Resident part of PRINT installed". Each file that you name joins the queue. The queue lists the file being printed, then the files waiting in the queue. When the queue is empty, it says "PRINT queue is empty".

```command
id: jsc:command/mc_dos/print
```

## If something goes wrong

> [!PROBLEM] print says Errors on list device indicate that it may be off-line.
> The printer is not linked, or it is off. Check the printer, then print again ([](jsc:printers)).

> [!SEE] jsc:printers
