---
section: clusters
order: 61
icon: jsc:cluster_management_computer
covers_family: jsc:cluster_management_computers
---
# Computadores de Gerenciamento de Cluster

Quando você tem um rack, anda até cada servidor. Quando tem dez, não vai querer. O Computador de Gerenciamento de Cluster instala sistemas e programas nas máquinas dos seus racks, e liga e desliga as baias delas, tudo de uma mesa só.

> [!DEFINE] Cluster
> Um grupo de máquinas que um computador gerencia como um conjunto só.

```scene
{"balloons": [{"detail": "Ele instala sistemas e programas nas máquinas dos racks e alterna as baias delas.", "number": 1, "part": {"x": 18, "y": 0, "z": 15}, "text": "Computador de Gerenciamento de Cluster"}, {"number": 2, "part": {"x": 16, "y": 1, "z": 16}, "text": "Um Rack de Servidores que ele gerencia"}, {"number": 3, "part": {"x": 13, "y": 1, "z": 16}, "text": "Um Rack de Servidores que ele gerencia"}, {"detail": "Ele liga o computador à parte de trás de cada rack.", "number": 4, "part": {"x": 14, "y": 2, "z": 14}, "text": "Cabo backbone"}], "blocks": [{"data": "{Units:[B;1b,10b,0b,4b,0b,1b,12b,11b],DisplayEra:3,BayPowerOff:0,ServicePanelOff:0b}", "state": "jsc:server_rack[facing=south]", "x": 16, "y": 0, "z": 16}, {"state": "jsc:server_rack_part[facing=south,top=false,front=false,compute=false]", "x": 16, "y": 0, "z": 15}, {"state": "jsc:server_rack_part[facing=south,top=false,front=true,compute=false]", "x": 15, "y": 0, "z": 16}, {"state": "jsc:server_rack_part[facing=south,top=false,front=false,compute=false]", "x": 15, "y": 0, "z": 15}, {"state": "jsc:server_rack_part[facing=south,top=false,front=true,compute=false]", "x": 16, "y": 1, "z": 16}, {"state": "jsc:server_rack_part[facing=south,top=false,front=false,compute=false]", "x": 16, "y": 1, "z": 15}, {"state": "jsc:server_rack_part[facing=south,top=false,front=true,compute=false]", "x": 15, "y": 1, "z": 16}, {"state": "jsc:server_rack_part[facing=south,top=false,front=false,compute=false]", "x": 15, "y": 1, "z": 15}, {"state": "jsc:server_rack_part[facing=south,top=true,front=false,compute=false]", "x": 16, "y": 2, "z": 16}, {"state": "jsc:server_rack_part[facing=south,top=true,front=false,compute=false]", "x": 16, "y": 2, "z": 15}, {"state": "jsc:server_rack_part[facing=south,top=true,front=false,compute=false]", "x": 15, "y": 2, "z": 16}, {"state": "jsc:server_rack_part[facing=south,top=true,front=false,compute=false]", "x": 15, "y": 2, "z": 15}, {"data": "{Units:[B;1b,1b,1b,1b,1b,1b,12b,11b],DisplayEra:3,BayPowerOff:0,ServicePanelOff:0b}", "state": "jsc:server_rack[facing=south]", "x": 13, "y": 0, "z": 16}, {"state": "jsc:server_rack_part[facing=south,top=false,front=false,compute=false]", "x": 13, "y": 0, "z": 15}, {"state": "jsc:server_rack_part[facing=south,top=false,front=true,compute=false]", "x": 12, "y": 0, "z": 16}, {"state": "jsc:server_rack_part[facing=south,top=false,front=false,compute=false]", "x": 12, "y": 0, "z": 15}, {"state": "jsc:server_rack_part[facing=south,top=false,front=true,compute=false]", "x": 13, "y": 1, "z": 16}, {"state": "jsc:server_rack_part[facing=south,top=false,front=false,compute=false]", "x": 13, "y": 1, "z": 15}, {"state": "jsc:server_rack_part[facing=south,top=false,front=true,compute=false]", "x": 12, "y": 1, "z": 16}, {"state": "jsc:server_rack_part[facing=south,top=false,front=false,compute=false]", "x": 12, "y": 1, "z": 15}, {"state": "jsc:server_rack_part[facing=south,top=true,front=false,compute=false]", "x": 13, "y": 2, "z": 16}, {"state": "jsc:server_rack_part[facing=south,top=true,front=false,compute=false]", "x": 13, "y": 2, "z": 15}, {"state": "jsc:server_rack_part[facing=south,top=true,front=false,compute=false]", "x": 12, "y": 2, "z": 16}, {"state": "jsc:server_rack_part[facing=south,top=true,front=false,compute=false]", "x": 12, "y": 2, "z": 15}, {"state": "jsc:cluster_management_computer[facing=south,optical=false]", "x": 18, "y": 0, "z": 15}, {"data": "{Wires:[{cable:\"jsc:cx4_cable\"}],Links:[I;0,2088,0,0,0,0,0,0,0]}", "state": "jscore:cable", "x": 12, "y": 2, "z": 14}, {"data": "{Wires:[{cable:\"jsc:cx4_cable\"}],Links:[I;0,2104,0,0,0,0,0,0,0]}", "state": "jscore:cable", "x": 13, "y": 2, "z": 14}, {"data": "{Wires:[{cable:\"jsc:cx4_cable\"}],Links:[I;0,48,0,0,0,0,0,0,0]}", "state": "jscore:cable", "x": 14, "y": 2, "z": 14}, {"data": "{Wires:[{cable:\"jsc:cx4_cable\"}],Links:[I;0,2104,0,0,0,0,0,0,0]}", "state": "jscore:cable", "x": 15, "y": 2, "z": 14}, {"data": "{Wires:[{cable:\"jsc:cx4_cable\"}],Links:[I;0,2104,0,0,0,0,0,0,0]}", "state": "jscore:cable", "x": 16, "y": 2, "z": 14}, {"data": "{Wires:[{cable:\"jsc:cx4_cable\"}],Links:[I;0,48,0,0,0,0,0,0,0]}", "state": "jscore:cable", "x": 17, "y": 2, "z": 14}, {"data": "{Wires:[{cable:\"jsc:cx4_cable\"}],Links:[I;0,17,0,0,0,0,0,0,0]}", "state": "jscore:cable", "x": 18, "y": 2, "z": 14}, {"data": "{Wires:[{cable:\"jsc:cx4_cable\"}],Links:[I;0,3,0,0,0,0,0,0,0]}", "state": "jscore:cable", "x": 18, "y": 1, "z": 14}, {"data": "{Wires:[{cable:\"jsc:cx4_cable\"}],Links:[I;0,2058,0,0,0,0,0,0,0]}", "state": "jscore:cable", "x": 18, "y": 0, "z": 14}], "caption": "Um Computador de Gerenciamento de Cluster, cabeado aos dois Racks de Servidores que ele gerencia. Arraste para seguir o cabo atrás dos racks.", "height": 130}
```

Monte-o como montaria um Computador Pessoal ([](jsc:personal_computers)), e dê a ele uma Placa de Interface de Cluster. Sem uma, ele é um computador comum ([](jsc:expansion_cards)). Com uma, ele é o gerenciador do seu cluster: cada placa alcança um conjunto diferente de máquinas, e instala em um número fixo delas ao mesmo tempo.

> [!DEFINE] Datacenter
> Os Racks de Servidores que um Roteador de Servidores gerencia, agrupados em seções atrás das suas faces.

Table: Placas de Interface de Cluster

| Placa | Era | Alcança | Nós ao mesmo tempo |
| --- | --- | --- | --- |
| Placa de Console Serial | Vintage | Racks de Servidores | 1 |
| NIC de Gerenciamento | Legacy | Racks de Servidores e Racks de Supercomputadores | 2 |
| Adaptador de Host Fabric | Standard | Todo tipo de rack | 4 |
| Fabric DPU | Advanced | Todo tipo de rack | 8 |

A placa decide o que o gerenciador alcança, não a era do computador. A Placa de Console Serial da era Vintage já alcança Racks de Servidores, e todas as placas os alcançam.

Um job instala em vários nós ao mesmo tempo, conforme a placa permite. Cada nó leva seu próprio tempo de instalação: 280 ticks a 2 GHz, que são 14 segundos. Um processador mais rápido leva menos tempo, mas nunca menos de 60 ticks, que são 3 segundos, nem mais de 1.200 ticks, que são 60 segundos.

LIGAR TUDO e DESLIGAR TUDO ligam ou desligam todas as baias do cluster de uma vez. Cada nó também tem sua própria baia, que mostra "Compartimento desligado" quando está desligada.

Um sistema ou um programa vem de um disco em um leitor que está vinculado ao gerenciador por cabo ([](jsc:drives)). O gerenciador procura um disco de sistema e um disco de programa nesses leitores vinculados.

Table: Mensagens do Gerenciador de Cluster

| Mensagem | O que significa | Correção |
| --- | --- | --- |
| compartimento desligado | O compartimento do nó está desligado. | Ligue a baia dele, ou use LIGAR TUDO. |
| já instalado | O nó já tem esse sistema ou programa. | Nada a fazer. |
| sem sistema | Um programa precisa de um sistema no nó primeiro. | Instale um sistema no nó primeiro ([](jsc:installing)). |
| antigo demais para o programa | A era do nó é mais antiga do que o programa permite. | Use um nó de uma era mais nova. |
| não atende aos requisitos do programa | O nó não tem o que o programa pede. | Use um nó que atenda a eles. |
| esse sistema se instala manualmente pelo próprio shell | O gerenciador não consegue instalar esse sistema. | Instale-o nesse nó manualmente ([](jsc:installing)). |
| esse disco não traz nada para instalar | O disco não tem sistema nem programa. | Coloque um disco que tenha um. |
| nenhum disco de sistema em um leitor vinculado | Nenhum leitor vinculado tem um disco de sistema. | Coloque um disco de sistema em um leitor vinculado por cabo. |
| nenhum disco de programa em um leitor vinculado | Nenhum leitor vinculado tem um disco de programa. | Coloque um disco de programa em um leitor vinculado por cabo. |
| um job já está em execução | Só um job roda por vez. | Espere terminar, ou cancele-o. |
| esta placa não alcança esse cluster | O alcance da placa fica aquém desse cluster. | Use uma placa da tabela acima que o alcance. |
| nenhuma placa de interface de cluster | O computador não tem placa. | Coloque uma Placa de Interface de Cluster no computador. |
| mudou durante a gravação | O nó mudou durante o job. | Rode o job de novo. |

## Se algo der errado

> [!PROBLEM] O Gerenciador de Cluster não alcança nenhuma máquina.
> O computador não tem Placa de Interface de Cluster, ou a placa não alcança as máquinas. A tabela acima diz o que cada placa alcança.

> [!PROBLEM] O gerenciador diz "É necessária uma Placa de Interface de Cluster."
> Coloque uma Placa de Interface de Cluster no computador.

> [!SEE] jsc:expansion_cards, jsc:datacenters
