---
section: autocrafting
order: 145
icon: jsc:crafting_interface
covers_family: jsc:crafting_interfaces
---
# Interfaces de Fabricação e a linha de máquina

Uma linha de máquina é como a rede roda uma máquina. Você a monta no Cabo de Fabricação, e ela tem três partes: a Interface de Fabricação, os Roteadores de Entrada de Fabricação e o Barramento de Recepção de Fabricação.

> [!DEFINE] Roteador de Entrada de Fabricação
> Uma peça de cabo que fica no cabo de uma interface, encostada em uma face de entrada de uma máquina, e leva as entradas que essa face aceita.

A Interface de Fabricação fica no cabo, encostada na máquina. Ela guarda os padrões dessa máquina, e alimenta a máquina diretamente quando está encostada nela. Ela também pode alimentar a máquina por um cabo próprio, com cor diferente do principal.

Table: Arquivos de padrão que uma interface guarda

| Era | Arquivos de padrão que ela guarda |
| --- | --- |
| Vintage | 3 |
| Legacy | 6 |
| Transition | 8 |
| Standard | 9 |
| Advanced | 12 |

Para uma máquina que recebe entradas em mais de uma face, um Roteador de Entrada de Fabricação fica encostado em cada face de entrada, no cabo próprio da interface ([](jsc:input_routers_and_receiving_buses)). O Barramento de Recepção de Fabricação fica encostado na saída da máquina: ele recolhe o que sai e o credita à fabricação que alimentou a máquina.

1. Passe Cabo de Fabricação do Computador de Fabricação até a máquina.
2. Coloque uma Interface de Fabricação no cabo, encostada na máquina.
3. Para uma máquina com várias faces de entrada, coloque um Roteador de Entrada de Fabricação encostado em cada uma.
4. Coloque um Barramento de Recepção de Fabricação encostado na saída da máquina.
5. Carregue o padrão da máquina na interface, pelo Gerenciador de Fabricação ([](jsc:crafting_manager)).

Uma interface trabalha uma receita por vez, ou várias tarefas ao mesmo tempo. O modo dela decide qual.

Table: Os modos de uma interface

| Modo | O que faz |
| --- | --- |
| EXCLUSIVA | Uma receita por vez nesta máquina, como os seus roteadores precisam; tarefas do mesmo padrão rodam juntas. |
| NÃO EXCLUSIVA | Várias tarefas ao mesmo tempo, com receitas diferentes também, desde que a máquina aceite. |

MÁX. JOBS define quantas tarefas a interface roda ao mesmo tempo, de 0 a 64 tarefas. Auto (0) faz a interface aceitar quantas tarefas a máquina aceitar. Uma interface pausada não aceita nova tarefa. Você também dá um nome à interface, com até 32 caracteres.

Uma interface é comandada quando um Computador de Fabricação comanda o cabo em que ela está. Uma placa comanda as interfaces do seu cabo na ordem do cabo.

Interfaces além do que as placas conseguem comandar esperam, sem nenhum computador comandá-las, até que outra placa seja colocada. A tela mostra "Comandada por um Computador de Fabricação" ou "Comandada por nenhum Computador de Fabricação".

Table: O log de tarefas de uma interface

| Linha no log | O que significa |
| --- | --- |
| Nenhuma tarefa ainda. | A interface não rodou nenhuma tarefa. |
| 3 de 8 feitos | Quantas peças da saída de uma tarefa a máquina já fabricou, de quantas ela precisa fabricar; os números mostrados são um exemplo. |
| a máquina não deu nenhuma das saídas esperadas | A tarefa terminou sem nenhuma das saídas que o padrão lista. |
| saídas inesperadas, enviadas para a rede | A máquina fez saídas que o padrão não lista, e elas foram para a rede. |
| esvaziando: a receita muda | A interface espera as saídas ainda devidas antes de uma nova receita entrar. |

## Se algo der errado

> [!PROBLEM] Uma máquina nunca recebe itens.
> Coloque a interface em um cabo que um Computador de Fabricação comanda, ligue o computador, ou dê a ele outra placa.

> [!PROBLEM] A saída nunca volta.
> Coloque um Barramento de Recepção de Fabricação encostado na saída da máquina.

> [!SEE] jsc:input_routers_and_receiving_buses, jsc:crafting_manager, jsc:patterns, jsc:data_cables, jsc:crafting_cards
