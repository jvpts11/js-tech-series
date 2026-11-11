---
section: buses_and_eras
order: 124
icon: jsc:import_bus
covers_family: jsc:buses
---
# Barramentos

Barramentos são peças finas que você coloca na face de um cabo de dados, encostados num baú ou numa máquina, para que a rede os alcance sem que você precise fazer nada.

```scene
{"balloons": [{"detail": "Ele pega itens do baú acima dele e os coloca na rede.", "number": 1, "part": {"box": [2, 11, 2, 14, 16, 14], "x": 1, "y": 0, "z": 1}, "text": "O Barramento de Importação"}, {"detail": "Ele pega itens da rede e os coloca na fornalha acima dele.", "number": 2, "part": {"box": [2, 11, 2, 14, 16, 14], "x": 5, "y": 0, "z": 1}, "text": "O Barramento de Exportação"}, {"number": 3, "part": {"x": 1, "y": 1, "z": 1}, "text": "O baú"}, {"number": 4, "part": {"x": 5, "y": 1, "z": 1}, "text": "A fornalha"}, {"detail": "Ele leva o trabalho dos barramentos até a rede.", "number": 5, "part": {"x": 3, "y": 0, "z": 1}, "text": "O cabo de dados"}], "blocks": [{"data": "{Wires:[{cable:\"jsc:gigabit_cable\"}],Links:[I;32,0,0,0,0,0,0,0,0],Parts:[{Face:1b,Type:\"jsc:import_bus\",Data:{}}]}", "state": "jscore:cable", "x": 1, "y": 0, "z": 1}, {"data": "{Wires:[{cable:\"jsc:gigabit_cable\"}],Links:[I;48,0,0,0,0,0,0,0,0]}", "state": "jscore:cable", "x": 2, "y": 0, "z": 1}, {"data": "{Wires:[{cable:\"jsc:gigabit_cable\"}],Links:[I;48,0,0,0,0,0,0,0,0]}", "state": "jscore:cable", "x": 3, "y": 0, "z": 1}, {"data": "{Wires:[{cable:\"jsc:gigabit_cable\"}],Links:[I;48,0,0,0,0,0,0,0,0]}", "state": "jscore:cable", "x": 4, "y": 0, "z": 1}, {"data": "{Wires:[{cable:\"jsc:gigabit_cable\"}],Links:[I;16,0,0,0,0,0,0,0,0],Parts:[{Face:1b,Type:\"jsc:export_bus\",Data:{}}]}", "state": "jscore:cable", "x": 5, "y": 0, "z": 1}, {"state": "minecraft:chest[facing=south]", "x": 1, "y": 1, "z": 1}, {"state": "minecraft:furnace[facing=south,lit=false]", "x": 5, "y": 1, "z": 1}], "caption": "Um Barramento de Importação em uma face do cabo puxa itens de um baú, e um Barramento de Exportação em outra face abastece uma fornalha. O cabo entre eles liga os dois à rede. Arraste para ver os barramentos de lado.", "height": 120, "view": {"pitch": 30.0, "zoom": 1.35}}
```

> [!DEFINE] Barramento
> Uma peça fina em uma face de um cabo de dados que move itens entre a rede e o bloco que ela toca.

Todo movimento de um barramento é uma Operação ([](jsc:what_operations_are)) que o Mainframe da rede executa. Por isso, nenhum barramento se move enquanto a rede não tiver um Mainframe ([](jsc:mainframes)).

Há três tipos de barramento. Cada um alcança um só bloco, o baú, o tanque ou a máquina para o qual está voltado.

Table: Os tipos de barramento

| Tipo | O que faz |
| --- | --- |
| Barramento de Importação | Pega itens do bloco para o qual está voltado e os põe na rede. |
| Barramento de Exportação | Pega itens da rede e os põe no bloco para o qual está voltado. |
| Barramento de Armazenamento Externo | Não move nada por si: a rede usa o inventário para o qual está voltado como armazenamento próprio. |

> [!DEFINE] Barramento de Importação
> Um barramento que pega itens do bloco para o qual está voltado e os põe na rede.

> [!DEFINE] Barramento de Exportação
> Um barramento que pega itens da rede e os põe no bloco para o qual está voltado.

Barramentos também movem fluidos ([](jsc:fluids)). O Barramento de Armazenamento Externo tem a sua própria página ([](jsc:external_storage_bus)).

## Colocando um barramento em um cabo

1. Segure o barramento na mão.
2. Clique com o botão direito no cabo de dados na face onde o barramento deve ficar.
3. Clique com o botão direito no barramento colocado para abrir a sua janela ([](jsc:bus_window)).

Se você apontar para o baú ou a máquina, o barramento procura primeiro um cabo atrás dessa face e depois qualquer cabo ao lado dela.

Um barramento só se instala em um cabo de acesso ou de backbone da sua própria era ou de uma anterior ([](jsc:data_cables), [](jsc:eras)). Em qualquer outro cabo, ele não encaixa. Uma face que já tem uma peça, ou que um fio atravessa, não aceita barramento, e nada acontece.

## Quando um barramento se move

Um barramento só se move quando todos os requisitos desta tabela valem. Confira-os na janela do barramento quando um barramento fica parado.

Table: O que um barramento precisa antes de se mover

| Requisito | O que significa |
| --- | --- |
| O cabo está em uma rede | Um cabo sem rede não move nada, e a luz da janela diz OFFLINE. |
| A rede tem um Mainframe | Sem um Mainframe, nenhum barramento se move. |
| O barramento está em ON | Um barramento em OFF fica parado até você o colocar em ON. |
| O seu modo permite | Um barramento em ON DEMAND só se move enquanto o bloco do seu cabo tiver um sinal de redstone. |
| As suas condições valem | Uma condição que não vale faz o barramento esperar. |

> [!DEFINE] Condição
> Uma regra que um barramento verifica antes de se mover, como uma contagem de estoque ou uma hora do dia.

Um barramento que moveu algo nos últimos 60 ticks, ou seja, 3 segundos, mostra um modelo ocupado no seu cabo, e esse modelo pisca.

## Se algo der errado

> [!PROBLEM] A mensagem "Barramentos de armazenamento se instalam em cabos de acesso e de backbone da sua era ou de uma anterior" aparece na barra de ação.
> Use um cabo de acesso ou de backbone da era do barramento ou de uma era anterior ([](jsc:data_cables)).

> [!PROBLEM] Clicar com o botão direito no cabo não faz nada, e nenhuma mensagem aparece.
> A face já tem uma peça, ou um fio a atravessa. Escolha outra face do cabo.

> [!PROBLEM] O barramento não move nada.
> Confira os requisitos na tabela acima. A aba ACTIVITY mostra as retenções que o barramento teve, com os seus motivos ([](jsc:bus_activity)).

> [!SEE] jsc:bus_filters_and_tags, jsc:buses_by_era
