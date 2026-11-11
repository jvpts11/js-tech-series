---
section: racks
order: 54
icon: jsc:server_rack
covers_family: jsc:server_racks
---
# Racks de servidores e servidores

Seus itens precisam morar em algum lugar. Em J's Computers eles moram nos discos dos servidores, e os servidores moram em racks.

> [!DEFINE] Servidor
> Um computador feito para caber num Rack de Servidores.

Um Rack de Servidores oferece oito unidades de rack de espaço. A altura de uma máquina é contada nessas unidades: a maioria dos servidores ocupa uma, os maiores ocupam duas.

> [!DEFINE] Rack de Servidores
> Um armário, com dois blocos de largura, três de altura e dois de profundidade, que abriga máquinas em fileiras.

> [!DEFINE] Unidade de rack (U)
> A altura que uma máquina ocupa num rack, contada em unidades. Cada armário tem oito.

```scene
{"balloons": [{"detail": "Clique na frente do rack com um servidor na mão para montá-lo.", "number": 1, "part": {"box": [6, 9, 14, 16, 14, 16], "x": 15, "y": 2, "z": 16}, "text": "Servidor"}, {"detail": "Ele permite que um monitor alcance todas as máquinas do rack.", "number": 2, "part": {"box": [6, 4, 14, 16, 8, 16], "x": 15, "y": 2, "z": 16}, "text": "KVM Switch"}, {"detail": "Ele ocupa duas unidades de rack, e suas dez baias de disco comportam os discos.", "number": 3, "part": {"box": [6, 4, 14, 16, 13, 16], "x": 15, "y": 1, "z": 16}, "text": "Servidor de Armazenamento"}, {"detail": "Esta, a Standard, acrescenta 1.500 W à capacidade de dissipação de calor do rack.", "number": 4, "part": {"box": [6, 8, 14, 16, 13, 16], "x": 15, "y": 0, "z": 16}, "text": "Unidade de Refrigeração"}, {"number": 5, "part": {"box": [6, 3, 14, 16, 7, 16], "x": 15, "y": 0, "z": 16}, "text": "Nobreak de Rack"}], "blocks": [{"data": "{Units:[B;1b,10b,0b,4b,0b,1b,12b,11b],DisplayEra:3,BayPowerOff:0,ServicePanelOff:0b}", "state": "jsc:server_rack[facing=south]", "x": 16, "y": 0, "z": 16}, {"state": "jsc:server_rack_part[facing=south,top=false,front=false,compute=false]", "x": 16, "y": 0, "z": 15}, {"state": "jsc:server_rack_part[facing=south,top=false,front=true,compute=false]", "x": 15, "y": 0, "z": 16}, {"state": "jsc:server_rack_part[facing=south,top=false,front=false,compute=false]", "x": 15, "y": 0, "z": 15}, {"state": "jsc:server_rack_part[facing=south,top=false,front=true,compute=false]", "x": 16, "y": 1, "z": 16}, {"state": "jsc:server_rack_part[facing=south,top=false,front=false,compute=false]", "x": 16, "y": 1, "z": 15}, {"state": "jsc:server_rack_part[facing=south,top=false,front=true,compute=false]", "x": 15, "y": 1, "z": 16}, {"state": "jsc:server_rack_part[facing=south,top=false,front=false,compute=false]", "x": 15, "y": 1, "z": 15}, {"state": "jsc:server_rack_part[facing=south,top=true,front=false,compute=false]", "x": 16, "y": 2, "z": 16}, {"state": "jsc:server_rack_part[facing=south,top=true,front=false,compute=false]", "x": 16, "y": 2, "z": 15}, {"state": "jsc:server_rack_part[facing=south,top=true,front=false,compute=false]", "x": 15, "y": 2, "z": 16}, {"state": "jsc:server_rack_part[facing=south,top=true,front=false,compute=false]", "x": 15, "y": 2, "z": 15}], "caption": "Um Rack de Servidores com suas unidades de rack preenchidas: um servidor, um KVM Switch, um Servidor de Armazenamento, outro servidor, uma Unidade de Refrigeração e um Nobreak de Rack. Arraste para girá-lo.", "height": 130}
```

O Rack de Supercomputador comporta apenas nós de supercomputador, e existe nas versões Standard e Advanced ([](jsc:supercomputers)).

> [!DEFINE] Supercomputador
> Vários computadores que funcionam como uma única máquina grande, ligados por cabo de computação de alta capacidade.

Um rack recebe servidores da sua própria era ou de eras anteriores. Cada servidor tem baias de unidade, que aparecem na frente do rack. Os [tipos de servidor](jsc:server_types) listam quantas um servidor tem.

Para montar um servidor à mão, segure-o e clique na frente do armário. Ele entra na fileira sob a mira, contada a partir do topo. Se mirar em outro lugar, ele ocupa a primeira fileira que couber, de cima para baixo. Uma unidade de rack é montada do mesmo jeito.

Clique no armário com a mão vazia para abrir a tela do rack. Num Rack de Supercomputador, clique agachado para tirar o painel de identidade visual.

Cada unidade de rack tem um interruptor de energia para a sua baia, e um rack novo liga todas as baias. O interruptor de uma baia é o botão de energia da máquina que está nela. A máquina ainda executa o seu autoteste e para no menu de inicialização.

As unidades nas baias são o armazenamento. Um item de servidor não traz discos próprios: as suas unidades são os discos que você coloca nas baias. Ligue a porta traseira do rack ao cabo de espinha dorsal, que o rack aceita até a sua era ([](jsc:data_cables)). Da era Standard em diante, o backbone é de fibra, e o rack só o aceita por uma Interface de Fibra Óptica montada numa das suas fileiras ([](jsc:rack_equipment)).

## Se algo der errado

> [!PROBLEM] A fibra não se liga ao rack.
> Monte uma Interface de Fibra Óptica no rack: a Standard para o Cabo de Fibra Óptica, a Advanced também para o Cabo de Fibra OM5 ([](jsc:rack_equipment)).

> [!PROBLEM] Um servidor não entra.
> Ela é de uma era mais nova que o rack, ou as linhas de que precisa estão ocupadas ou são pequenas demais para ela. Use um rack da sua era ou de uma mais nova, ou libere essas linhas.

> [!PROBLEM] A mensagem indica outro tipo de rack, como "Este chassi pertence a um Rack de Servidores".
> Coloque-o no rack que a mensagem indica.

> [!PROBLEM] A mensagem diz "Este chassi é de uma era posterior à deste rack".
> Coloque-o num rack da sua própria era ou de uma posterior.

> [!SEE] jsc:rack_drive_bays, jsc:data_cables
