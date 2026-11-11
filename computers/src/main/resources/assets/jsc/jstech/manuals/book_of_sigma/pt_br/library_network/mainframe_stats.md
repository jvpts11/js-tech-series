---
section: library_network
order: 447
---
# Mainframe: o que o orquestrador já fez

O Mainframe mantém a contagem do trabalho que a sua rede fez: quantos de cada tipo ele executou, quanto tempo cada um esperou e rodou, e quanto cada um moveu.

Se você quer saber se um Mainframe roda na rede, pergunte a Mainframe.Online. Os outros membros precisam de uma rede, e são apenas do Sigma#.

Table: Membros do Mainframe

| Membro | O que faz |
| --- | --- |
| `Mainframe.Online` | Sim, quando um Mainframe roda na rede. |
| `Mainframe.PeakToday` | O maior número de Operações rodando ao mesmo tempo no último dia. Zero quando não há Mainframe. |
| `Mainframe.Stats(kind)` | O trabalho de um tipo, como um WorkStat. |
| `Mainframe.Work()` | O trabalho de todos os tipos que a rede fez, como uma lista de WorkStat. |

Um tipo de trabalho que a rede não fez aparece como zeros. Stats lê um tipo; Work lista os tipos que ela já fez.

## O que um WorkStat guarda

Table: Campos de WorkStat

| Campo | O que guarda |
| --- | --- |
| `WorkStat.Type` | O tipo de trabalho, em letras minúsculas. |
| `WorkStat.Count` | Quantos desse tipo a rede fez. |
| `WorkStat.AverageWait` | A espera média de cada um, como número inteiro. |
| `WorkStat.AverageRun` | A duração média de execução de cada um, como número inteiro. |
| `WorkStat.ShortfallPercent` | Um número inteiro de porcentagem. |
| `WorkStat.Moved` | Quanto do item foi movido, como um número inteiro longo. |

## Se algo der errado

> [!PROBLEM] O programa para e diz que este computador não está em uma rede.
> Verifique primeiro Network.Online.

> [!PROBLEM] Mainframe.PeakToday é 0.
> Não há Mainframe na rede. Verifique Mainframe.Online.

> [!PROBLEM] Stats mostra zeros para um tipo de trabalho.
> A rede ainda não fez esse tipo de trabalho. Use Mainframe.Work para ver os tipos que ela já fez.

> [!SEE] jsc:mainframes, jsc:operations_catalogue, jsc:states_and_queues
