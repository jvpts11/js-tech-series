---
section: engine_choice
order: 389
icon: jsc:mainframe
---
# Prophet YourIQL: observadores, esquecer e SHOW STATES

Um observador executa uma ação quando uma condição se torna verdadeira. Você escreve a condição como um item, uma comparação e um número. Depois de DO você escreve a ação, como escreveria qualquer instrução.

1. Digite WATCH, o item, a comparação e o número.
2. Digite DO, depois a ação.

```iql watch_redstone.iql
WATCH redstone < 500 DO CRAFT redstone TO 1000
```

Table: As comparações

| Comparação | Significa |
| --- | --- |
| `<` | abaixo |
| `<=` | no máximo |
| `>` | acima |
| `>=` | no mínimo |

O primeiro observador que você define é o número 1, e os seguintes sobem a partir dele. A resposta diz observador 1 definido, e depois escreve o observador por inteiro, para que você possa conferir.

Um observador dispara uma vez, quando sua condição se torna verdadeira. Ele é armado de novo só depois que a condição deixa de ser verdadeira.

## O que um observador faz

Table: As ações de um observador

| Ação | O que acontece |
| --- | --- |
| `CRAFT item TO n` | Fabrica até n do item, contando o que a rede tem, lote por lote. |
| Qualquer outra instrução | Roda como instrução, e sua resposta é mostrada, até 64 linhas. |

## Esquecer

Table: Esquecer um item ou um observador

| Instrução | O que acontece |
| --- | --- |
| `FORGET item` | O item deixa de ser mantido. |
| `FORGET WATCH n` | O observador com esse número é solto. |

FORGET aceita o nome que você escreveu para o item. Esquecer um item também apaga o registro dele de Operações recentes.

## SHOW STATES

SHOW STATES lista o que o motor mantém e observa. Digite em duas palavras, em qualquer caixa. A resposta conta os estados e os observadores, e depois mostra uma tabela.

Table: A tabela de SHOW STATES

| Coluna | O que mostra |
| --- | --- |
| estado | O estado como você escreveu, ou a instrução WATCH de um observador. |
| status | A palavra de status da tabela abaixo. |
| em uso | O que a rede tem do item. |
| a caminho | As Operações a caminho do item. |
| último | A última coisa que o Motor fez por ele; fica vazia em uma linha de observador. |

## As palavras de status

Table: As palavras de status

| Status | O que significa |
| --- | --- |
| Novo | Ainda não verificado. |
| Estoque | Dentro da sua faixa, sem nada a caminho. |
| Trabalhando | Há trabalho a caminho para trazê-lo para a faixa ou mantê-lo ali. |
| Não é possível manter | Abaixo da sua faixa, e nada pôde ser posto em andamento. |
| Acima | Acima da sua faixa; nada é jogado fora para baixá-lo. |
| Armado | Um observador esperando sua condição. |
| Disparado | Um observador cuja condição se tornou verdadeira. |

Estoque é o bom estado. Não é possível manter e Acima precisam da sua atenção. Os outros esperam, ou estão em andamento.

O motor não reage a observadores enquanto está parado, então nenhum observador dispara nesse período.

## Se algo der errado

> [!PROBLEM] A resposta é "sintaxe: WATCH item < n DO instrução (a comparação uma de <, <=, >, >=)".
> Escreva WATCH, o item, uma comparação, um número, DO e a ação.

> [!PROBLEM] A resposta diz que o observador disparou, e foi recusado, com um motivo depois dos dois pontos.
> Leia o motivo, corrija a instrução, solte o observador com FORGET WATCH e defina-o de novo.

> [!PROBLEM] A resposta diz que o observador disparou sem nada a fabricar.
> A rede já tem o alvo, então nada está errado.

> [!PROBLEM] FORGET diz que o item não é mantido.
> Confira o nome do item em SHOW STATES.

> [!PROBLEM] FORGET WATCH diz que nenhum observador tem esse número.
> Confira o número em SHOW STATES.

> [!SEE] jsc:prophet_youriql, jsc:prophet_console, jsc:engine_problems
