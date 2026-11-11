---
section: autocrafting
order: 151
---
# Quando uma fabricação dá errado

Uma fabricação pode falhar antes de começar, ou enquanto roda. A rede aponta a etapa que deu errado, e as mensagens abaixo dizem o que cada uma significa e como corrigi-la.

Table: Antes de uma fabricação começar

| O que você vê | O que significa | A solução |
| --- | --- | --- |
| Nenhuma fabricação começa. | Uma fabricação só começa enquanto o Mainframe estiver rodando e tiver um sistema operacional. | Ligue o Mainframe e instale o sistema operacional dele. |
| O texto "nada na rede sabe fabricar", seguido do item. | Nenhum padrão na rede fabrica o item. | Adicione uma bancada, máquina ou padrão de várias etapas para ele ([](jsc:patterns)). |
| O texto "nenhum computador de fabricação estava livre para fabricar", seguido do item. | Nenhum Computador de Fabricação está ligado com um Cartão de Fabricação, ou todos estão ocupados. | Ligue um Computador de Fabricação, dê a ele um Cartão de Fabricação, ou espere até que um fique livre. |
| A receita não aparece no plano. | A receita está na ROM de um Computador de Fabricação que não está rodando. | Ligue esse Computador de Fabricação. |

## Enquanto uma fabricação roda

Table: Enquanto uma fabricação roda

| O que você vê | O que significa | A solução |
| --- | --- | --- |
| O texto "a máquina fabricando", seguido do item, e "parou de receber qualquer coisa". | Uma tarefa de máquina desistiu depois do tempo limite, enquanto nada se movia na interface dela. | Verifique a energia da máquina e a saída dela. Um Barramento de Recepção de Fabricação precisa estar voltado para a saída. |
| O texto "nenhuma Interface de Fabricação tem a receita de", seguido do item. | Nenhuma interface tem o padrão da máquina, e ele não foi colocado a tempo. | Coloque o padrão da máquina em uma Interface de Fabricação que um Computador de Fabricação comande. |
| O texto "os ingredientes para", seguido do item, e "acabaram". | A fabricação na bancada ficou sem ingredientes no armazenamento. | Armazene mais desse ingrediente. |
| O texto "o padrão não tem estágios". | Um padrão de várias etapas não tem nenhuma etapa. | Adicione etapas ao pipeline ([](jsc:patterns)). |
| O texto "etapa", seguido de um número, "de", uma contagem, e "não pôde ser iniciada". | A máquina ou a interface daquela etapa não está pronta. | Verifique a máquina daquela etapa e a interface dela, como para uma máquina que parou. |
| O texto "etapa", seguido de um número, "de", uma contagem, e "falhou". | A etapa atingiu o tempo limite. | Verifique a energia e o espaço da máquina da etapa, e se a saída dela está voltada para um Barramento de Recepção de Fabricação. |

## Como uma fabricação termina

Uma fabricação que produz parte do que foi pedido é parcial, e a parte produzida é mantida. Uma fabricação que não produz nada falhou. Se uma etapa falha, a fabricação inteira falha: as etapas em bancada devolvem os itens que pegaram, e as etapas em máquina mantêm o que colocaram nas máquinas.

Os oito estados de uma operação estão listados em [](jsc:operation_states).

Table: Como uma fabricação termina

| O resultado | O que significa | A solução |
| --- | --- | --- |
| O texto "entregue", com duas quantidades. | Uma fabricação parcial: parte da quantidade foi produzida. | Peça o restante depois que os itens que faltam estiverem no armazenamento. |
| O texto "tempo esgotado esperando por um recurso bloqueado". | Outra operação estava com o que esta fabricação precisava. | Peça de novo depois que essa outra operação terminar. |
| O texto "falhou". | A fabricação não produziu nada. | Leia a etapa que falhou acima e corrija-a primeiro. |

## Tempos limite

Uma tarefa de máquina espera pelas saídas dela, e o relógio dela só conta enquanto nada se move na interface. Quando o tempo limite passa, a tarefa desiste. O tempo limite de um padrão de máquina é 200 ticks por padrão, o que equivale a 10 segundos ([](jsc:patterns)).

Saídas que chegam atrasadas ainda são creditadas por um curto período depois que uma tarefa termina. Depois disso, elas vão para a rede como inesperadas.

Uma interface espera até que as saídas devidas estejam dentro antes que uma nova receita entre. O log dela mostra esvaziando até lá ([](jsc:crafting_interfaces)).

## As interfaces e os cabos

Table: Interfaces e cabos

| O que você vê | O que significa | A solução |
| --- | --- | --- |
| A tela diz "Comandada por nenhum Computador de Fabricação". | A interface não está em um cabo que um Computador de Fabricação comande, ou os cartões comandam menos interfaces do que o cabo alcança. | Coloque-a em um cabo que um Computador de Fabricação comande, ligue o computador, ou dê ao computador outro cartão. |
| A tela diz "nenhum roteador do cabo aceita: este padrão não pode correr". | Nenhum roteador no cabo da interface aceita uma das entradas. | Coloque um Roteador de Entrada de Fabricação nesse cabo, com um filtro que aceite a entrada ([](jsc:input_routers_and_receiving_buses)). |

As outras mensagens dos roteadores e dos barramentos estão listadas em [](jsc:input_routers_and_receiving_buses).

## Limites do planejador

O planejador para em 16 níveis de receitas, e corta uma receita que depende de si mesma ([](jsc:craft_planner)).

## Se algo der errado

> [!PROBLEM] Uma fabricação nem começa.
> Verifique se o Mainframe está rodando com um sistema operacional, e se um Computador de Fabricação está ligado com um cartão.

> [!PROBLEM] Uma fabricação produz menos do que você pediu.
> A fabricação terminou como parcial. Peça o restante depois que os itens que faltam estiverem no armazenamento.

> [!SEE] jsc:asking_for_a_craft, jsc:crafting_interfaces, jsc:input_routers_and_receiving_buses, jsc:craft_planner, jsc:operation_states, jsc:network_manager
