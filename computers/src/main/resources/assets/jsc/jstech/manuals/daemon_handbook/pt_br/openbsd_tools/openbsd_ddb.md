---
section: openbsd_tools
order: 348
---
# Quando o sistema entra em pânico: o ddb

Quando o kernel do OpenBSD entra em pânico, ele para no seu depurador, o ddb, e espera no prompt ddb>. Ele nunca reinicia sozinho: você lê o que aconteceu, e então o reinicia.

Table: O que o ddb aceita

| Comando | O que mostra |
| --- | --- |
| `show panic` | Onde o kernel parou. |
| `trace` | Onde ele estava quando parou. |
| `ps` | Os processos que estavam rodando. |
| `boot reboot` | Reinicia o computador. |

> [!SEE] jsc:openbsd_overview
