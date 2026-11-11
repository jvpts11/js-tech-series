---
section: clusters
order: 62
---
# Supercomputadores

Um supercomputador é todo Rack de Supercomputador ligado por um cabo de malha, encontrando a rede em uma única Interface HBW ([](jsc:hbw_interface)). O Standard e o Advanced têm uma interface própria.

Os cabos de malha são o Cabo InfiniBand (Transition), o Cabo de Alta Computação (Standard) e o Cabo OSFP (Advanced).

```scene
{"balloons": [{"detail": "Um computador completo de duas unidades de rack, com seu próprio coprocessador.", "number": 1, "part": {"box": [6, 4, 13, 16, 14, 16], "x": 13, "y": 2, "z": 16}, "text": "Nó de Supercomputador"}, {"number": 2, "part": {"x": 14, "y": 1, "z": 16}, "text": "Rack de Supercomputador"}, {"detail": "Ele liga cada rack do supercomputador à interface.", "number": 3, "part": {"x": 12, "y": 2, "z": 14}, "text": "Cabo de Alta Computação"}, {"detail": "O único ponto onde o supercomputador encontra a rede.", "number": 4, "part": {"x": 16, "y": 0, "z": 14}, "text": "Interface HBW"}, {"number": 5, "part": {"x": 17, "y": 0, "z": 14}, "text": "Cabo backbone"}, {"number": 6, "part": {"x": 18, "y": 0, "z": 14}, "text": "Um roteador da rede"}], "blocks": [{"data": "{Units:[B;6b,0b,6b,0b,6b,0b,6b,0b],DisplayEra:3,BayPowerOff:0,ServicePanelOff:1b}", "state": "jsc:supercomputer_rack[facing=south]", "x": 14, "y": 0, "z": 16}, {"state": "jsc:server_rack_part[facing=south,top=false,front=false,compute=true]", "x": 14, "y": 0, "z": 15}, {"state": "jsc:server_rack_part[facing=south,top=false,front=true,compute=true]", "x": 13, "y": 0, "z": 16}, {"state": "jsc:server_rack_part[facing=south,top=false,front=false,compute=true]", "x": 13, "y": 0, "z": 15}, {"state": "jsc:server_rack_part[facing=south,top=false,front=true,compute=true]", "x": 14, "y": 1, "z": 16}, {"state": "jsc:server_rack_part[facing=south,top=false,front=false,compute=true]", "x": 14, "y": 1, "z": 15}, {"state": "jsc:server_rack_part[facing=south,top=false,front=true,compute=true]", "x": 13, "y": 1, "z": 16}, {"state": "jsc:server_rack_part[facing=south,top=false,front=false,compute=true]", "x": 13, "y": 1, "z": 15}, {"state": "jsc:server_rack_part[facing=south,top=true,front=false,compute=true]", "x": 14, "y": 2, "z": 16}, {"state": "jsc:server_rack_part[facing=south,top=true,front=false,compute=true]", "x": 14, "y": 2, "z": 15}, {"state": "jsc:server_rack_part[facing=south,top=true,front=false,compute=true]", "x": 13, "y": 2, "z": 16}, {"state": "jsc:server_rack_part[facing=south,top=true,front=false,compute=true]", "x": 13, "y": 2, "z": 15}, {"data": "{Units:[B;6b,0b,6b,0b,6b,0b,6b,0b],DisplayEra:3,BayPowerOff:0,ServicePanelOff:1b}", "state": "jsc:supercomputer_rack[facing=south]", "x": 11, "y": 0, "z": 16}, {"state": "jsc:server_rack_part[facing=south,top=false,front=false,compute=true]", "x": 11, "y": 0, "z": 15}, {"state": "jsc:server_rack_part[facing=south,top=false,front=true,compute=true]", "x": 10, "y": 0, "z": 16}, {"state": "jsc:server_rack_part[facing=south,top=false,front=false,compute=true]", "x": 10, "y": 0, "z": 15}, {"state": "jsc:server_rack_part[facing=south,top=false,front=true,compute=true]", "x": 11, "y": 1, "z": 16}, {"state": "jsc:server_rack_part[facing=south,top=false,front=false,compute=true]", "x": 11, "y": 1, "z": 15}, {"state": "jsc:server_rack_part[facing=south,top=false,front=true,compute=true]", "x": 10, "y": 1, "z": 16}, {"state": "jsc:server_rack_part[facing=south,top=false,front=false,compute=true]", "x": 10, "y": 1, "z": 15}, {"state": "jsc:server_rack_part[facing=south,top=true,front=false,compute=true]", "x": 11, "y": 2, "z": 16}, {"state": "jsc:server_rack_part[facing=south,top=true,front=false,compute=true]", "x": 11, "y": 2, "z": 15}, {"state": "jsc:server_rack_part[facing=south,top=true,front=false,compute=true]", "x": 10, "y": 2, "z": 16}, {"state": "jsc:server_rack_part[facing=south,top=true,front=false,compute=true]", "x": 10, "y": 2, "z": 15}, {"data": "{Wires:[{cable:\"jsc:hpc_cable\"}],Links:[I;0,0,0,0,2088,0,0,0,0]}", "state": "jscore:cable", "x": 10, "y": 2, "z": 14}, {"data": "{Wires:[{cable:\"jsc:hpc_cable\"}],Links:[I;0,0,0,0,2104,0,0,0,0]}", "state": "jscore:cable", "x": 11, "y": 2, "z": 14}, {"data": "{Wires:[{cable:\"jsc:hpc_cable\"}],Links:[I;0,0,0,0,48,0,0,0,0]}", "state": "jscore:cable", "x": 12, "y": 2, "z": 14}, {"data": "{Wires:[{cable:\"jsc:hpc_cable\"}],Links:[I;0,0,0,0,2104,0,0,0,0]}", "state": "jscore:cable", "x": 13, "y": 2, "z": 14}, {"data": "{Wires:[{cable:\"jsc:hpc_cable\"}],Links:[I;0,0,0,0,2104,0,0,0,0]}", "state": "jscore:cable", "x": 14, "y": 2, "z": 14}, {"data": "{Wires:[{cable:\"jsc:hpc_cable\"}],Links:[I;0,0,0,0,17,0,0,0,0]}", "state": "jscore:cable", "x": 15, "y": 2, "z": 14}, {"data": "{Wires:[{cable:\"jsc:hpc_cable\"}],Links:[I;0,0,0,0,3,0,0,0,0]}", "state": "jscore:cable", "x": 15, "y": 1, "z": 14}, {"data": "{Wires:[{cable:\"jsc:hpc_cable\"}],Links:[I;0,0,0,0,8226,0,0,0,0]}", "state": "jscore:cable", "x": 15, "y": 0, "z": 14}, {"state": "jsc:hbw_interface", "x": 16, "y": 0, "z": 14}, {"data": "{Wires:[{cable:\"jsc:cx4_cable\"}],Links:[I;0,12336,0,0,0,0,0,0,0]}", "state": "jscore:cable", "x": 17, "y": 0, "z": 14}, {"state": "jsc:standard_router", "x": 18, "y": 0, "z": 14}], "caption": "Um supercomputador: dois Racks de Supercomputador com nós, ligados por Cabo de Alta Computação à Interface HBW, que encontra a rede. Arraste para ver os cabos atrás dos racks.", "height": 130, "view": {"zoom": 1.35}}
```

Cada Rack de Supercomputador guarda nós de supercomputador, que são computadores completos. Um nó só acrescenta fabricações quando tem uma placa Phi compatível com o seu slot, está montado e com a baia ligada. Os coprocessadores Integra Phi vêm em quatro modelos.

> [!DEFINE] Coprocessador
> Uma placa que adiciona processamento extra a um nó, como a série Integra Phi.

Table: Coprocessadores Integra Phi

| Coprocessador | Slots em que cabe | Núcleos | Consumo de energia |
| --- | --- | --- | --- |
| Integra Phi 5100 | 1 a 2 | 60 núcleos a 1,05 GHz | 225 W |
| Integra Phi 7120 | 1 a 3 | 61 núcleos a 1,24 GHz | 250 W |
| Integra Phi 7290 | 1 a 4 | 72 núcleos a 1,5 GHz | 270 W |
| Integra Phi 9000 | 1 a 6 | 96 núcleos a 1,8 GHz | 300 W |

A dica de ferramenta de um Phi dá os seus slots como "Slots de supercomputador 1-2" para o Integra Phi 5100. Os nós são contados nos racks da execução, de cima para baixo, e os seis primeiros nós são os slots nominais, numerados de 1 a 6.

Cada slot dá fabricações em paralelo: o slot 1 dá 8 fabricações, o slot 2 dá 16, dobrando até o slot 6, com 256 fabricações. Juntos, os seis slots dão até 504 fabricações em paralelo. Cada slot mostra o seu estado:

Table: O que um slot mostra

| Slot mostra | O que significa |
| --- | --- |
| SEM PLACA PHI | O nó não tem nenhuma placa Phi. |
| CLASSIFICAÇÃO BAIXA | A placa Phi só é nominal para os slots antes deste. |
| COMPARTIMENTO DESLIGADO | A baia do nó está desligada. |
| SEM NÓ | Nenhum nó está nesse slot. |

O supercomputador fica ONLINE quando a sua interface está em uma rede e pelo menos um slot executa fabricações. Um slot executa fabricações quando o seu nó tem uma placa Phi compatível, está montado e com a baia ligada. Uma operação pode ocupar vários slots, quantos estiverem livres, e o Mainframe mostra as fabricações em uso em relação ao total.

## Se algo der errado

> [!PROBLEM] O supercomputador não está ONLINE.
> Confira os slots: cada um precisa de um nó com uma placa Phi compatível, montado e com a baia ligada.

> [!PROBLEM] Um slot mostra CLASSIFICAÇÃO BAIXA.
> Use uma placa Phi cujos slots nominais cheguem até esse.

> [!PROBLEM] Um nó além do sexto não acrescenta nada.
> Só os seis primeiros nós contam, na ordem dos racks, de cima para baixo. Coloque primeiro os que você quer.

> [!SEE] jsc:server_racks, jsc:hbw_interface
