---
section: storage
order: 118
icon: jsc:storage_server
---
# Servidores de armazenamento

O espaço da rede para itens fica nos discos, e os discos ficam nos servidores. Um servidor vive num Rack de Servidores, um armário com posições chamadas unidades de rack (U), e cada unidade de rack é uma posição de altura. Um servidor de armazenamento é o servidor feito para discos: tem 2 unidades de rack de altura e mais baias de disco do que qualquer outro servidor da sua era. Toda era tem um, das cinco baias do Vintage às dez do Standard e do Advanced.

> [!DEFINE] Servidor de armazenamento
> Um servidor feito para discos, com o maior número de baias de disco em 2 unidades de rack (U). Os discos dele dão à rede espaço para itens.

> [!DEFINE] Unidade de rack (U)
> Uma posição de altura num Rack de Servidores. Um servidor ocupa uma ou mais delas.

Cada disco entra numa baia de disco na frente do servidor. As baias são de troca a quente: você pode retirar um disco e colocar outro enquanto o servidor funciona.

As baias, os processadores e os slots de cada servidor estão em [](jsc:server_types).

RAID (redundant array of independent disks) junta os discos de um servidor num único disco para a rede. Você coloca uma placa controladora RAID, a Vaultis Array da era, num slot de expansão do servidor, e escolhe o modo na página STORAGE do Setup do servidor ([](jsc:setup_utility)). Os modos e os números deles estão em [](jsc:raid_arrays).

```scene
{"balloons": [{"detail": "Clique com o botão direito na frente dele para abrir as baias.", "number": 1, "part": {"x": 16, "y": 0, "z": 16}, "text": "O Rack de Servidores"}, {"detail": "O servidor de armazenamento Standard tem 10 delas.", "number": 2, "part": {"box": [0, 4, 14, 10, 14, 16], "x": 16, "y": 2, "z": 16}, "text": "As baias de disco"}, {"detail": "Retire um e coloque outro enquanto o servidor estiver em execução.", "number": 3, "part": {"box": [6, 4, 14, 16, 14, 16], "x": 15, "y": 2, "z": 16}, "text": "Os discos"}], "blocks": [{"data": "{Servers:{Size:8,Items:[{Slot:0,id:\"jsc:storage_server\",count:1,components:{\"jsc:server_hardware\":[{slot:13,item:{id:\"jsc:vaultis_array_pcie3\",count:1,components:{\"jsc:raid_mode\":\"raid5\",\"jsc:raid_members\":10}}}]}}]},FrontSlots:{Size:40,Items:[{Slot:0,id:\"jsc:disk_hdd_4t\",count:1},{Slot:1,id:\"jsc:disk_hdd_4t\",count:1},{Slot:2,id:\"jsc:disk_hdd_4t\",count:1},{Slot:3,id:\"jsc:disk_hdd_4t\",count:1},{Slot:4,id:\"jsc:disk_hdd_4t\",count:1},{Slot:5,id:\"jsc:disk_hdd_4t\",count:1},{Slot:6,id:\"jsc:disk_hdd_4t\",count:1},{Slot:7,id:\"jsc:disk_hdd_4t\",count:1},{Slot:8,id:\"jsc:disk_hdd_4t\",count:1},{Slot:9,id:\"jsc:disk_hdd_4t\",count:1}]},BayPowerOff:0,ServicePanelOff:0b,ActiveChannel:0,DisplayEra:3,Units:[B;4b,0b,0b,0b,0b,0b,0b,0b]}", "state": "jsc:server_rack[facing=south]", "x": 16, "y": 0, "z": 16}, {"state": "jsc:server_rack_part[facing=south,top=false,front=false]", "x": 16, "y": 0, "z": 15}, {"state": "jsc:server_rack_part[facing=south,top=false,front=true]", "x": 15, "y": 0, "z": 16}, {"state": "jsc:server_rack_part[facing=south,top=false,front=false]", "x": 15, "y": 0, "z": 15}, {"state": "jsc:server_rack_part[facing=south,top=false,front=true]", "x": 16, "y": 1, "z": 16}, {"state": "jsc:server_rack_part[facing=south,top=false,front=false]", "x": 16, "y": 1, "z": 15}, {"state": "jsc:server_rack_part[facing=south,top=false,front=true]", "x": 15, "y": 1, "z": 16}, {"state": "jsc:server_rack_part[facing=south,top=false,front=false]", "x": 15, "y": 1, "z": 15}, {"state": "jsc:server_rack_part[facing=south,top=true,front=false]", "x": 16, "y": 2, "z": 16}, {"state": "jsc:server_rack_part[facing=south,top=true,front=false]", "x": 16, "y": 2, "z": 15}, {"state": "jsc:server_rack_part[facing=south,top=true,front=false]", "x": 15, "y": 2, "z": 16}, {"state": "jsc:server_rack_part[facing=south,top=true,front=false]", "x": 15, "y": 2, "z": 15}], "caption": "Um Rack de Servidores com um servidor de armazenamento nas unidades de rack superiores: dez discos nas baias de disco, unidos como um array RAID 5 pela placa Vaultis Array dentro do servidor. Arraste para ver o rack de lado.", "height": 130}
```

A rede conta o espaço de cada disco em cada servidor de um rack. Um servidor sem discos não acrescenta nada ao espaço da rede.

## Se algo der errado

> [!PROBLEM] Um slot mostra "Bloqueado - o chassi não fornece cabeamento para este slot".
> Esse slot não aceita disco. Use outro slot.

> [!PROBLEM] Um slot mostra "Bloqueado - nenhuma unidade nesta fileira fornece cabeamento para estes slots".
> Use um slot que uma unidade desta linha ligue por cabo.

> [!SEE] jsc:server_racks, jsc:raid_arrays, jsc:local_storage
