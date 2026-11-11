---
section: openbsd_software
order: 346
---
# O syspatch, os patches do próprio sistema

O OpenBSD fica na versão 7.5: o que muda são os seus patches, que o syspatch busca para você no Mirror da rede e instala. A versão tem dois, 001_ksh e 002_sshd.

Table: O que o syspatch faz

| Comando | O que faz |
| --- | --- |
| `syspatch -c` | Lista os patches ainda não instalados. |
| `syspatch` | Os instala, e liga o kernel de novo. |
| `syspatch -l` | Lista os patches instalados. |
| `syspatch -r` | Retira o último patch instalado. |

```command
id: jsc:command/openbsd/syspatch
```

## Se algo der errado

> [!PROBLEM] O syspatch diz que não há rota até o Mirror.
> O computador não está numa rede cujo Mainframe roda o Mirror ([](jsc:openbsd_packages)).

> [!SEE] jsc:openbsd_packages
