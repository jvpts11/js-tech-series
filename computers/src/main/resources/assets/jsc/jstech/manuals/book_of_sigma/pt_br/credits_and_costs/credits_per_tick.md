---
section: credits_and_costs
order: 480
---
# Quanto uma máquina roda a cada tick

As unidades centrais de processamento (CPU) de um computador dão trabalho aos seus programas. A cada tick, o computador transforma os seus processadores em créditos: o número de instruções que os programas dele podem gastar naquele tick. Mais núcleos e um clock mais rápido significam mais créditos.

O computador soma, para cada processador da sua compilação, o número de núcleos vezes a velocidade do clock em megahertz (MHz). Núcleos são as partes de um processador que trabalham ao mesmo tempo. Veja [Processadores](jsc:processors). Se os seus programas rodam devagar, confira primeiro os processadores.

Depois ele divide essa soma por oito, descartando qualquer fração. Um computador com um processador sempre recebe pelo menos 32 créditos. Um computador sem processador na compilação não recebe créditos, então os programas dele não rodam nada. Não há limite superior: um processador mais rápido recebe mais créditos.

Table: Créditos de alguns processadores

| Núcleos vezes MHz, somados | Créditos em um tick |
| --- | --- |
| 0, sem processador na compilação | 0 |
| 128 (1 núcleo a 128 MHz) | 32, o mínimo que um computador recebe |
| 256 (2 núcleos a 128 MHz) | 32 |
| 2.048 (4 núcleos a 512 MHz) | 256 |
| 12.000 (6 núcleos a 2.000 MHz) | 1.500 |

A dica de um processador também mostra uma capacidade em itens por tick (it/t). Esse número mede o trabalho da rede, não a velocidade de um programa. A velocidade de um programa são os créditos acima. Veja [Processadores](jsc:processors).

> [!DEFINE] Crédito
> O número de instruções que os processadores de um computador dão aos seus programas em um tick.

## Cada tick

A cada tick o computador verifica se roda. Um computador ligado, com uma compilação válida, roda os seus programas. Um computador desligado, ou cujas peças não formam mais uma compilação válida, para todos eles. Cada um recebe a sua despedida, dentro de um orçamento.

## Turnos dentro de um tick

Os créditos de um tick são divididos entre os programas em rodadas. Em cada rodada, um programa usa até 64 créditos, ou uma parte igual do que sobrou, quando isso for menos. Um programa que termina ou espera por algo deixa a sua vez para o resto do tick.

Todo programa tem uma prioridade. Só a prioridade baixa muda alguma coisa: um programa de prioridade baixa fica de fora de uma rodada a cada duas. Dê a prioridade como o terceiro argumento de Program.Start, depois do arquivo e dos argumentos.

Table: Prioridades

| Prioridade | O que faz |
| --- | --- |
| `low` | Fica de fora de uma rodada a cada duas do tick. |
| `medium` | A prioridade padrão, que roda em todas as rodadas. |
| `high` | Roda em todas as rodadas, como medium faz. |

## Trabalho para outros computadores

Um computador que faz trabalho para um computador [ComputerCraft](jsc:computercraft_side), por meio de um [Gateway de Rede](jsc:network_gateway), paga por esse trabalho com os próprios créditos. Os programas dele recebem menos créditos nos próximos ticks, até a dívida ser paga.

Uma dívida maior que os créditos de um tick passa para o tick seguinte. Um computador sem programas ainda paga as suas dívidas, um tick por vez.

## O orçamento de despedida

Um programa parado executa a sua despedida com um orçamento de 4.096 instruções por tick. Todos os programas parados naquele tick dividem o orçamento. Um programa que ultrapassa a sua parte para antes de conseguir executar o código de despedida.

Quando um computador é desligado, ou a sua compilação deixa de ser válida, o orçamento é dividido igualmente entre os programas parados.

## Se algo der errado

> [!PROBLEM] Os programas não rodam nada.
> O computador não tem processador na sua compilação, ou a compilação não é válida. Coloque um processador na compilação e deixe a compilação válida.

> [!PROBLEM] Um programa está lento em um computador antigo.
> Os processadores dele têm poucos núcleos ou um clock lento, então ele recebe menos créditos. Instale processadores com mais núcleos ou um clock mais rápido ([Processadores](jsc:processors)).

> [!PROBLEM] Alguns programas param sem a despedida.
> Muitos programas parados no mesmo tick dividem o orçamento. Pare menos programas de uma vez.

> [!SEE] jsc:call_costs, jsc:saved_with_world
