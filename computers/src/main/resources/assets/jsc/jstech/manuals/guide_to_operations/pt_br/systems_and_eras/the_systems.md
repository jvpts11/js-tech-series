---
section: systems_and_eras
order: 11
icon: jsc:floppy_disk
---
# Sistemas operacionais

O sistema operacional é aquilo em que um computador inicia e no que todo programa roda. Você escolhe um sistema ao instalá-lo ([](jsc:installing)).

Cada sistema precisa de hardware da sua era ou de uma mais nova, ocupa espaço no disco em que é instalado e ocupa memória enquanto roda.

```table
{"caption": "Os sistemas", "heads": ["Sistema", "Era mais antiga", "Espaço no disco", "Memória durante a execução"], "rows": [[{"words": "MC-DOS"}, {"words": "Vintage"}, {"amount": 4, "unit": "MB"}, {"amount": 1, "unit": "MB"}], [{"words": "MC-NET"}, {"words": "Vintage"}, {"amount": 8, "unit": "MB"}, {"amount": 2, "unit": "MB"}], [{"words": "UNIX System V"}, {"words": "Vintage"}, {"amount": 10, "unit": "MB"}, {"amount": 2, "unit": "MB"}], [{"words": "Frames 95"}, {"words": "Legacy"}, {"amount": 48, "unit": "MB"}, {"amount": 32, "unit": "MB"}], [{"words": "Frames XP"}, {"words": "Legacy"}, {"amount": 1536, "unit": "MB"}, {"amount": 128, "unit": "MB"}], [{"words": "Frames 7"}, {"words": "Transition"}, {"amount": 16384, "unit": "MB"}, {"amount": 1024, "unit": "MB"}], [{"words": "Frames 10"}, {"words": "Standard"}, {"amount": 16384, "unit": "MB"}, {"amount": 3072, "unit": "MB"}], [{"words": "Frames 11"}, {"words": "Advanced"}, {"amount": 20480, "unit": "MB"}, {"amount": 8192, "unit": "MB"}], [{"words": "Ubuntu"}, {"words": "Legacy"}, {"amount": 8192, "unit": "MB"}, {"amount": 48, "unit": "MB"}], [{"words": "Debian"}, {"words": "Legacy"}, {"amount": 4096, "unit": "MB"}, {"amount": 24, "unit": "MB"}], [{"words": "Fedora"}, {"words": "Legacy"}, {"amount": 8192, "unit": "MB"}, {"amount": 48, "unit": "MB"}], [{"words": "Arch Linux"}, {"words": "Legacy"}, {"amount": 2048, "unit": "MB"}, {"amount": 12, "unit": "MB"}], [{"words": "Gentoo"}, {"words": "Legacy"}, {"amount": 4096, "unit": "MB"}, {"amount": 12, "unit": "MB"}], [{"words": "FreeBSD"}, {"words": "Legacy"}, {"amount": 2048, "unit": "MB"}, {"amount": 16, "unit": "MB"}], [{"words": "OpenBSD"}, {"words": "Legacy"}, {"amount": 1024, "unit": "MB"}, {"amount": 12, "unit": "MB"}], [{"words": "Linux Mint"}, {"words": "Standard"}, {"amount": 12288, "unit": "MB"}, {"amount": 64, "unit": "MB"}]]}
```

O MC-DOS é um prompt de comando. O UNIX System V é um shell, sem área de trabalho própria.

Cada edição do Frames roda na sua própria era e em todas as mais novas. O Frames 10 precisa de hardware Standard, e o Frames 11 precisa de Advanced.

Os programas que você abre ocupam memória além da do sistema. O que um programa ocupa cresce com a era do computador: 8 vezes mais num computador Transition do que num Legacy, 32 vezes num Standard e 128 vezes num Advanced ([](jsc:frames_memory)).

O Arch Linux e o Gentoo são instalados à mão, passo a passo. Os outros sistemas têm um instalador guiado ([](jsc:installing)).

Table: O livro de ajuda de cada sistema

| Sistema | Livro de ajuda |
| --- | --- |
| MC-DOS e as edições do Frames | O Kit de Recursos do Frames |
| Ubuntu, Debian, Fedora e Linux Mint | A Referência de Mesa do Linux |
| Arch Linux e Gentoo | Linux à Mão |
| FreeBSD, OpenBSD e UNIX System V | O Manual do Daemon |
| MC-NET | Este Guia de Operações |

Cada livro está na biblioteca ([](jsc:library)). A Ajuda de um computador abre primeiro no livro do seu sistema ([](jsc:help_on_a_computer)).

## Se algo der errado

> [!PROBLEM] O instalador recusa o sistema.
> O hardware é mais antigo que a primeira era do sistema, ou o disco é pequeno demais para ele ([](jsc:installing)).

> [!SEE] jsc:installing, jsc:eras
