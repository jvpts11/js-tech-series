---
section: operations
order: 134
icon: jsc:mainframe
---
# Os oito estados de uma Operação

Toda Operação está em um dos oito estados a qualquer momento. A aba Processos do [Gerenciador de Rede](jsc:network_manager) mostra cada um deles, com as palavras da tabela abaixo.

Três estados indicam que a Operação ainda está em andamento: na fila, em execução e em espera. Os outros cinco são finais. Depois disso a Operação está concluída, e ela fica guardada no log ([](jsc:cancelling_and_the_log)).

> [!DEFINE] log
> A lista do Mainframe com as últimas 32 Operações que terminaram, da mais recente para a mais antiga.

> [!DEFINE] retenção
> Uma reserva de itens. Outras Operações que precisam desses itens esperam até a reserva acabar.

Table: Os oito estados

| Estado | O Gerenciador de Rede diz | O que significa |
| --- | --- | --- |
| `PENDING` | na fila | Pronta, e esperando uma fila livre. |
| `PROCESSING` | em execução | Ocupa uma fila e move itens. |
| `WAITING` | em espera | Bloqueada pela retenção de outra Operação. Não ocupa fila enquanto espera. |
| `COMPLETED` | concluída | Moveu tudo o que foi pedido. |
| `COMPLETED_PARTIAL` | parcial | Moveu parte, menos do que foi pedido. |
| `FAILED` | falhou | Não moveu nada, e não foi concluída. |
| `RESOURCE_LOCKED` | bloqueada | Desistiu de esperar uma retenção, depois que o tempo de espera acabou. |
| `DISCARDED` | descartada | Interrompida antes de terminar, de propósito ou pela máquina. |

A lista colore cada linha pelo estado: em andamento ou concluída, retida ou interrompida. Uma linha também pode mostrar uma frase com o motivo, como as abaixo.

Table: As linhas de motivo

| A linha | O que significa |
| --- | --- |
| `delivered 40 of 64` | Moveu parte do que foi pedido. Os dois números são o que foi movido e o que foi pedido. |
| `timed out waiting on a locked resource` | Uma retenção sobre os itens durou tempo demais. |
| `failed` | Nada foi movido, e a Operação não foi concluída. |
| `the network was not holding enough` | Um SELECT pediu mais do que a rede tem. |
| `the network had no room left for the` | Um INSERT não encontrou espaço livre no armazenamento da rede. |

## Se algo der errado

> [!PROBLEM] Uma Operação fica na fila.
> Todas as filas estão ocupadas. Um Mainframe com mais placas de vídeo tem mais filas ([](jsc:expansion_cards)), e um processador mais rápido termina cada uma mais cedo ([](jsc:mainframes)).

> [!PROBLEM] Ela termina bloqueada.
> Os itens ficaram retidos por mais tempo do que o tempo de espera, que é de 60 segundos por padrão. Espere, ou faça UNLOCK do item ([](jsc:lock_unlock)).

> [!PROBLEM] Ela termina parcial.
> A rede tinha menos do que você pediu. Armazene mais do item ([](jsc:storage_servers)), ou peça menos.

> [!PROBLEM] Ela termina com falha.
> A rede não tinha nenhuma parte do item, ou um passo foi recusado. Confira o nome do item e se ele está armazenado.

> [!SEE] jsc:states_and_queues, jsc:cancelling_and_the_log
