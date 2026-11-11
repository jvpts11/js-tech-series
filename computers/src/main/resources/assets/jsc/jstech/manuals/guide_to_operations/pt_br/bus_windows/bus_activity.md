---
section: bus_windows
order: 130
---
# O que um barramento fez

A aba Atividade é o registro de um barramento. Cada linha é um movimento que o barramento fez, ou uma vez em que ele esperou, com o motivo. O barramento guarda as últimas 16 linhas e mostra 8 por vez, então você rola a tela para ver as demais.

Cada movimento é uma Operação ([](jsc:what_operations_are)). O Terminal e o Gerenciador de Rede listam o mesmo log, então você pode ler esses movimentos no Gerenciador de Rede ([](jsc:network_manager)).

Uma linha mostra a hora, o item com a quantidade depois de um x, um status à direita e o motivo abaixo do status.

Table: Status

| Status | O que significa |
| --- | --- |
| `COMPLETED` | O movimento inteiro foi concluído. |
| `COMPLETED_PARTIAL` | Parte do movimento foi concluída. O resto fica no barramento e é enviado de novo. |
| `WAITING` | O barramento segurou, porque o baú manteve a quantidade KEEP dele ou uma condição não se cumpriu. |
| `RESOURCE_LOCKED` | Sem espaço: a rede está cheia para um Barramento de Importação, ou o baú está cheio para um Barramento de Exportação. |

Table: Motivo

| Motivo | O que significa |
| --- | --- |
| para dentro da rede | Um Barramento de Importação moveu itens para dentro da rede. |
| para dentro do baú | Um Barramento de Exportação moveu itens para dentro do baú. |
| o baú mantém 16 | O baú já tem a quantidade KEEP dele, então o Barramento de Exportação espera. |
| a rede está cheia | Um Barramento de Importação não encontrou espaço na rede. |
| o baú está cheio | Um Barramento de Exportação encontrou o baú cheio. |
| uma condição o segura | Uma condição não se cumpriu, então o barramento esperou. |

Uma retenção que se repete atualiza a hora da linha mais nova, em vez de acrescentar uma linha. Quando a rede aceita só parte de um movimento, o resto fica no barramento, e o barramento o envia quando há espaço.

## Se algo der errado

> [!PROBLEM] O registro diz "Nada movido ainda."
> O barramento ainda não moveu nenhum item. Confira o que o barramento precisa antes de mover ([](jsc:buses)), e o que o filtro dele deixa passar ([](jsc:bus_filters_and_tags)).

> [!PROBLEM] O status diz RESOURCE_LOCKED.
> A rede ou o baú não tem espaço. Libere espaço no baú, ou adicione armazenamento à rede ([](jsc:storage)).

> [!PROBLEM] O motivo diz "uma condição o segura".
> Uma condição não se cumpriu. Leia as condições do barramento em CONFIGURE ([](jsc:bus_conditions)).

> [!SEE] jsc:bus_window, jsc:bus_conditions
