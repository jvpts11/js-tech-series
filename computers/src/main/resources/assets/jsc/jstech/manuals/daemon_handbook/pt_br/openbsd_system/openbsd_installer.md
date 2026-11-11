---
section: openbsd_system
order: 343
---
# Instalando o OpenBSD, pergunta por pergunta

O instalador do OpenBSD faz as suas perguntas no terminal, uma embaixo da outra, e você responde cada uma digitando e apertando Enter. A resposta entre colchetes é a que o Enter escolhe quando você não digita nada.

Ponha o meio do OpenBSD num drive ligado ao computador e inicie o computador por ele. A primeira pergunta é (I)nstall, (U)pgrade, (A)utoinstall or (S)hell?: digite i.

Table: As perguntas que ele faz

| Pergunta | O que a sua resposta faz |
| --- | --- |
| `System hostname?` | Dá nome ao computador. |
| `Start sshd(8) by default?` | Deixa outros computadores alcançarem este, ou não. |
| `Do you expect to run the X Window System?` | Diz se uma área de trabalho vai rodar aqui. |
| `...started by xenodm(1)?` | Inicia a área de trabalho com o computador. Não, o padrão, o deixa no terminal. |
| `Which disk is the root disk?` | Escolhe o disco onde o sistema vai: wd0 num disco IDE, sd0 num SATA. |
| `Use (W)hole disk or (E)dit the MBR?` | w usa o disco inteiro; e põe o OpenBSD no espaço livre ao lado do que o disco guarda. |
| `Location of sets?` | O meio, ou http para buscá-los no Mirror da rede. |
| `Set name(s)?` | Quais sets instalar; done inicia a cópia. |

A rede, a senha e o fuso horário já vêm respondidos: o computador ainda não tem conta a proteger, e o mundo tem um relógio só.

## Os sets

O sistema vem em cinco sets. Digite -man\* para deixar o manual de fora, e o nome de um set para trazê-lo de volta; all traz todos de volta.

Table: Os sets

| Set | O que guarda | Deixado de fora |
| --- | --- | --- |
| `bsd` | O kernel. | Sempre instalado. |
| `bsd.rd` | O kernel que conserta um sistema. | Poupa o seu espaço. |
| `base75.tgz` | O sistema base. | Sempre instalado. |
| `comp75.tgz` | O compilador e as suas ferramentas. | Poupa o seu espaço, e o make vai junto. |
| `man75.tgz` | As páginas do manual. | Poupa o seu espaço, e o man e o apropos vão junto. |

Quando a cópia termina, responda Exit to (S)hell, (H)alt or (R)eboot? com r, e tire o meio.

## Se algo der errado

> [!PROBLEM] O instalador diz que o disco é pequeno demais.
> O OpenBSD precisa de 1.024 MB. Escolha um disco maior, ou responda w para usar o disco inteiro.

> [!PROBLEM] O instalador diz que não é uma escolha válida.
> Digite o nome do disco como a pergunta o lista, como wd0.

> [!SEE] jsc:installing, jsc:openbsd_overview
