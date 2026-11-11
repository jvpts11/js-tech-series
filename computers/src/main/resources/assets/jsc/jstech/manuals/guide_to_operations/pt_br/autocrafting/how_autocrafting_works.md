---
section: autocrafting
order: 140
icon: jsc:crafting_cable
covers: [jsc:crafting_cable]
---
# Como funciona a autofabricação

Você pede 512 pistões. A rede calcula cada passo a partir do que tem: quantas tábuas, quanto pedregulho, ferro e redstone ela precisa, e o que deve fabricar primeiro. Ela também decide em quais máquinas e em que ordem. Depois executa as receitas da mesa de trabalho e as máquinas, e guarda os pistões no armazenamento. Isso é a autofabricação.

Para isso, a rede precisa conhecer as receitas. Uma receita que ela conhece é um padrão, e há três tipos: um padrão de bancada, uma grade 3x3 como a da mesa de trabalho, com seu resultado; um padrão de máquina, o que entra em uma máquina e o que sai dela; e um padrão de várias etapas, passos dos dois tipos em ordem.

> [!DEFINE] Autofabricação
> A rede fabricando o que lhe falta, executando as receitas que conhece com o que possui.

> [!DEFINE] Padrão
> Uma receita que a rede conhece: uma grade de bancada, as entradas e saídas de uma máquina, ou várias delas em ordem.

As peças são um Computador de Fabricação, que executa as fabricações ([](jsc:crafting_computers)), o Cabo de Fabricação que o liga às máquinas, e, nesse cabo, as peças que abastecem cada máquina. Um Cartão de Fabricação no computador é o que permite que ele fabrique qualquer coisa ([](jsc:crafting_cards)). Dois programas que você mesmo instala escrevem os padrões e os carregam: o Pattern Studio e o Gerenciador de Fabricação ([](jsc:pattern_studio), [](jsc:crafting_manager)).

```scene
{"balloons": [{"detail": "Ele executa as fabricações e guarda o Cartão de Fabricação.", "number": 1, "part": {"x": 0, "y": 0, "z": 1}, "text": "O Computador de Fabricação"}, {"number": 2, "part": {"x": 2, "y": 0, "z": 1}, "text": "O Cabo de Fabricação"}, {"detail": "Ela guarda os padrões da máquina e a abastece.", "number": 3, "part": {"box": [6, 1, 1, 16, 15, 15], "x": 3, "y": 0, "z": 1}, "text": "A Interface de Fabricação"}, {"detail": "Coloque um em cada face de entrada de uma máquina que tenha várias.", "number": 4, "part": {"box": [1, 1, 6, 15, 15, 16], "x": 4, "y": 0, "z": 0}, "text": "O Roteador de Entrada de Fabricação"}, {"detail": "Coloque-o contra a saída da máquina.", "number": 5, "part": {"box": [1, 1, 0, 15, 15, 10], "x": 4, "y": 0, "z": 2}, "text": "O Barramento de Recepção de Fabricação"}, {"number": 6, "part": {"x": 4, "y": 0, "z": 1}, "text": "A máquina"}], "blocks": [{"data": "{Hardware:{Size:15,Items:[{Slot:0,id:\"jsc:motherboard_atx_standard_1155\",count:1},{Slot:1,id:\"jsc:cpu_integra_celer_g530\",count:1},{Slot:2,id:\"jsc:ram_ddr3_8192\",count:1},{Slot:3,id:\"jsc:ram_ddr3_8192\",count:1},{Slot:6,id:\"jsc:crafting_card_pcie3\",count:1},{Slot:10,id:\"jsc:psu_850g\",count:1},{Slot:11,id:\"jsc:disk_ssd_500g\",count:1}]}}", "state": "jsc:crafting_computer[facing=south]", "x": 0, "y": 0, "z": 1}, {"data": "{Wires:[{cable:\"jsc:crafting_cable\"}],Links:[I;0,0,0,0,0,4144,0,0,0]}", "state": "jscore:cable", "x": 1, "y": 0, "z": 1}, {"data": "{Wires:[{cable:\"jsc:crafting_cable\"}],Links:[I;0,0,0,0,0,48,0,0,0]}", "state": "jscore:cable", "x": 2, "y": 0, "z": 1}, {"data": "{Wires:[{cable:\"jsc:crafting_cable\"}],Links:[I;0,0,0,0,0,28,0,0,0],Parts:[{Face:5b,Type:\"jsc:crafting_interface\",Data:{}}]}", "state": "jscore:cable", "x": 3, "y": 0, "z": 1}, {"data": "{Wires:[{cable:\"jsc:crafting_cable\"}],Links:[I;0,0,0,0,0,40,0,0,0]}", "state": "jscore:cable", "x": 3, "y": 0, "z": 0}, {"data": "{Wires:[{cable:\"jsc:crafting_cable\"}],Links:[I;0,0,0,0,0,36,0,0,0]}", "state": "jscore:cable", "x": 3, "y": 0, "z": 2}, {"data": "{Wires:[{cable:\"jsc:crafting_cable\"}],Links:[I;0,0,0,0,0,16,0,0,0],Parts:[{Face:3b,Type:\"jsc:crafting_input_router\",Data:{}}]}", "state": "jscore:cable", "x": 4, "y": 0, "z": 0}, {"data": "{Wires:[{cable:\"jsc:crafting_cable\"}],Links:[I;0,0,0,0,0,16,0,0,0],Parts:[{Face:2b,Type:\"jsc:receiving_bus\",Data:{}}]}", "state": "jscore:cable", "x": 4, "y": 0, "z": 2}, {"state": "minecraft:furnace[facing=south]", "x": 4, "y": 0, "z": 1}], "caption": "A linha inteira: um Computador de Fabricação, o Cabo de Fabricação e uma fornalha com as peças que a abastecem e recolhem o que ela produz. Arraste para ver a parte de trás.", "height": 130, "view": {"zoom": 1.35}}
```

> [!DEFINE] Cabo de Fabricação
> O cabo que liga um Computador de Fabricação às máquinas que ele abastece, e transporta as peças de fabricação.

> [!DEFINE] Cartão de Fabricação
> Um cartão que vai em um slot de expansão de um Computador de Fabricação, e define quantas interfaces ele controla e quantas receitas ele mantém.

> [!DEFINE] Interface de Fabricação
> Uma peça de cabo que fica encostada em uma máquina, guarda os padrões da máquina e a abastece ([](jsc:crafting_interfaces)).

> [!DEFINE] Barramento de Recepção de Fabricação
> Uma peça de cabo que fica encostada na saída de uma máquina e recolhe o que a máquina produz em uma fabricação.

Os padrões são escritos no Pattern Studio e levados ao Computador de Fabricação em mídias ([](jsc:pattern_encoders)). Uma fabricação então passa pelos passos abaixo.

Table: Os passos de uma fabricação

| Passo | O que acontece |
| --- | --- |
| O pedido | O Mainframe só inicia uma fabricação enquanto estiver ligado e tiver um sistema operacional. |
| O plano | A rede calcula o plano, e a fabricação espera até que o plano esteja pronto. |
| O computador | O plano vai para um Computador de Fabricação que esteja ligado e tenha um Cartão de Fabricação. |
| Um de cada vez | Um Computador de Fabricação executa uma fabricação de cada vez, a menos que a rede tenha um Supercomputador. |
| Os passos | Os passos de bancada rodam no Computador de Fabricação, a partir das receitas dos seus cartões. |
| As máquinas | Os passos de máquina rodam nas Interfaces de Fabricação que o computador controla. |
| A saída | O que uma máquina produz volta para a rede pelo Barramento de Recepção de Fabricação ligado à sua interface. |

A rede precisa de duas coisas antes que qualquer parte funcione: um Mainframe ligado com sistema operacional, e as receitas guardadas em um cartão ou em uma interface. Uma máquina trabalha com a rede quando oferece acesso a itens, fluidos e substâncias químicas, seja qual for o mod de origem.

## Se algo der errado

> [!PROBLEM] O plano diz "nada na rede sabe como fabricar", e depois o nome do item.
> Não há nenhum padrão para esse item na rede. Adicione um, e carregue-o em um cartão ou em uma interface ([](jsc:patterns)).

> [!SEE] jsc:crafting_computers, jsc:crafting_interfaces, jsc:pattern_encoders, jsc:asking_for_a_craft
