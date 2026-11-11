---
section: iql_recipe_list
order: 401
icon: jsc:import_bus
---
# Armazenar itens por tipo

Às vezes uma rede deve receber apenas alguns tipos de item, como minério de ferro e carvão e nada mais. Você define isso com um Barramento de Importação, que traz itens para a rede a partir do inventário para o qual ele está voltado ([](jsc:buses)).

> [!DEFINE] Barramento de Importação
> Uma peça em um cabo de dados que move itens do inventário para o qual ela está voltada para dentro da rede.

Um barramento move apenas os itens do seu filtro. Você define o filtro com uma linha SET BUS ([](jsc:set_bus)). Escreva o nome do barramento entre aspas simples quando ele tiver um espaço.

> [!DEFINE] Filtro
> A lista de itens que um barramento pode mover.

Table: O que a receita precisa

| Necessidade | O que é preciso |
| --- | --- |
| Era | Legacy e as seguintes para um filtro; Advanced para tags |
| Programa | Nenhum: você digita a linha no prompt |
| Motor | O motor do Mainframe em execução; todo Mainframe tem o Midsoft IQL Server |

1. Coloque um Barramento de Importação em um cabo de dados, voltado para o inventário de onde ele pega itens.
2. Dê um nome ao barramento, como explica a página de barramentos ([](jsc:buses)).
3. Digite iql SET BUS 'Ore in' FILTER ONLY iron_ore, coal no prompt.
4. Abra a janela do barramento para conferi-lo ([](jsc:bus_window)).

Table: Linhas para um barramento chamado Ore in

| Linha | O que o barramento faz |
| --- | --- |
| `SET BUS 'Ore in' FILTER ONLY iron_ore, coal` | Move apenas minério de ferro e carvão. |
| `SET BUS 'Ore in' FILTER ALL BUT dirt` | Move tudo, exceto terra. |
| `SET BUS 'Ore in' FILTER NONE` | Esvazia o filtro, então o barramento não move nada. |
| `SET BUS 'Ore in' FILTER TAG c:ores, c:logs` | Move os itens que têm estas tags. |
| `SET BUS 'Ore in' WHEN STOCK iron_ore < 512` | Espera enquanto a rede tiver 512 ou mais minérios de ferro. |

Barramentos Advanced podem filtrar por tag. O prefixo c: marca as tags que muitos mods compartilham.

> [!DEFINE] Tag
> Um grupo de itens que compartilham um rótulo, como c:ores para minérios.

Table: O que cada era de Barramento de Importação pode definir

| Era | Filtro | Tags | Condições |
| --- | --- | --- | --- |
| Vintage | Não | Não | Não |
| Legacy | Sim | Não | Não |
| Transition | Sim | Não | Não |
| Standard | Sim | Não | Sim |
| Advanced | Sim | Sim | Sim |

Um filtro comporta 5 itens, e um barramento comporta 4 tags. As condições são as linhas WHEN: elas mantêm o barramento em espera até que a rede as atenda ([](jsc:bus_conditions)).

O filtro decide o que um barramento move, não para onde os itens vão. A rede decide qual servidor os guarda.

## Se algo der errado

> [!PROBLEM] A resposta diz "o barramento Ore in não pode ser ajustado assim: a era ou o tipo dele não tem esse ajuste".
> O barramento é um barramento Vintage, que não tem filtro, ou você pediu tags em um barramento abaixo de Advanced. Um filtro com mais de 5 itens também é recusado, então divida a lista.

> [!PROBLEM] A resposta diz "nenhum item ou fluido chamado steel_ingt".
> Confira a grafia do id do item, como iron_ore.

> [!PROBLEM] A resposta diz "nenhum barramento chamado Ore in nesta rede".
> Confira o nome do barramento e a grafia. O barramento precisa estar nesta rede.

> [!PROBLEM] O barramento não move nada.
> Confira se o barramento está ligado (SET BUS 'Ore in' ON), se o filtro dele cita itens que o baú contém, e se a condição WHEN dele está sendo atendida.

> [!SEE] jsc:buses, jsc:bus_filters_and_tags, jsc:bus_conditions, jsc:set_bus
