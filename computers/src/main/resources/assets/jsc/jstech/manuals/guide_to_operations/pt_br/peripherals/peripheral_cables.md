---
section: peripherals
order: 68
icon: jsc:peripheral_cable
covers_family: jsc:peripheral_cables
---
# Cabos periféricos e hubs

Um cabo periférico liga um computador aos seus dispositivos: monitores, alto-falantes, unidades, impressoras e Redstone Interfaces. Ele não é um cabo de dados e não carrega rede. Um dispositivo colocado encostado no seu computador não precisa de cabo nenhum.

```scene
{"balloons": [{"number": 1, "part": {"x": 9, "y": 0, "z": 11}, "text": "O computador"}, {"detail": "Ele liga o computador aos seus dispositivos e não transporta rede.", "number": 2, "part": {"x": 10, "y": 0, "z": 11}, "text": "O Cabo Periférico"}, {"detail": "Ligue os alto-falantes ao cabo próprio do computador, antes do Hub: eles não passam por um Hub.", "number": 3, "part": {"x": 11, "y": 0, "z": 12}, "text": "Um alto-falante"}, {"detail": "Ele ocupa uma porta do computador e oferece mais portas próprias.", "number": 4, "part": {"x": 13, "y": 0, "z": 11}, "text": "O Hub"}, {"number": 5, "part": {"x": 13, "y": 0, "z": 12}, "text": "Uma Unidade de Disquete"}, {"detail": "Pressione o botão de ejeção ao lado de uma unidade para colocar a bandeja dela para fora.", "number": 6, "part": {"x": 14, "y": 0, "z": 12}, "text": "Uma Unidade de CD"}, {"number": 7, "part": {"x": 15, "y": 0, "z": 12}, "text": "A Impressora"}], "blocks": [{"state": "jsc:personal_computer[facing=south]", "x": 9, "y": 0, "z": 11}, {"data": "{Wires:[{cable:\"jsc:peripheral_cable\"}],Links:[I;0,0,0,4144,0,0,0,0,0]}", "state": "jscore:cable", "x": 10, "y": 0, "z": 11}, {"data": "{Wires:[{cable:\"jsc:peripheral_cable\"}],Links:[I;0,0,0,2104,0,0,0,0,0]}", "state": "jscore:cable", "x": 11, "y": 0, "z": 11}, {"data": "{Wires:[{cable:\"jsc:peripheral_cable\"}],Links:[I;0,0,0,10296,0,0,0,0,0]}", "state": "jscore:cable", "x": 12, "y": 0, "z": 11}, {"data": "{LinkedOwner:2473901207552L}", "state": "jsc:standard_hub", "x": 13, "y": 0, "z": 11}, {"data": "{Wires:[{cable:\"jsc:peripheral_cable\"}],Links:[I;0,0,0,6200,0,0,0,0,0]}", "state": "jscore:cable", "x": 14, "y": 0, "z": 11}, {"data": "{Wires:[{cable:\"jsc:peripheral_cable\"}],Links:[I;0,0,0,2072,0,0,0,0,0]}", "state": "jscore:cable", "x": 15, "y": 0, "z": 11}, {"data": "{LinkedOwner:2473901207552L,Powered:1b}", "state": "jsc:speaker[facing=south,lit=true]", "x": 11, "y": 0, "z": 12}, {"data": "{LinkedOwner:2473901207552L,Powered:1b}", "state": "jsc:speaker[facing=south,lit=true]", "x": 12, "y": 0, "z": 12}, {"data": "{LinkedOwner:2473901207552L,MediaSlot:{Size:1,Items:[{Slot:0,id:\"jsc:floppy_disk\",count:1}]}}", "state": "jsc:floppy_drive[facing=south,loaded=true]", "x": 13, "y": 0, "z": 12}, {"data": "{LinkedOwner:2473901207552L,TrayOpen:1b,MediaSlot:{Size:1,Items:[{Slot:0,id:\"jsc:cd_rom\",count:1}]}}", "state": "jsc:cd_drive[facing=south,loaded=true]", "x": 14, "y": 0, "z": 12}, {"data": "{LinkedOwner:2473901207552L}", "state": "jsc:printer[facing=south]", "x": 15, "y": 0, "z": 12}], "caption": "Um Computador Pessoal com seus dispositivos em um Cabo Periférico: dois alto-falantes, um Hub, uma Unidade de Disquete, uma Unidade de CD e uma Impressora. Arraste a cena para ver a parte de trás.", "height": 130, "view": {"zoom": 1.35}}
```

Cada era tem o seu próprio cabo periférico, e cada um alcança um número fixo de cabos a partir do seu computador. Um dispositivo mais longe que isso não é reconhecido. Uma porta aceita o cabo da sua própria era e de todas as eras anteriores, nunca de uma mais nova.

> [!DEFINE] Porta
> Um soquete em um computador, em um hub ou em um aparelho. Cada soquete tem um tipo: dispositivo, vídeo ou áudio.

```table
source: jsc:peripheral_reach
caption: Até onde cada cabo alcança, em cabos
heads: Era | Cabo | Alcance
```

Cada tipo de porta aceita o seu próprio tipo de dispositivo. Uma unidade, uma impressora, uma Redstone Interface ou um hub ocupam uma porta de dispositivo. Um monitor ocupa uma saída de vídeo de uma placa de vídeo. Um alto-falante ocupa uma saída de áudio, e cada saída de áudio alimenta dois alto-falantes. O número de portas depende da era da placa-mãe e da placa de vídeo.

Table: Portas de dispositivo por era da placa

| Era da placa | Portas de dispositivo |
| --- | --- |
| Vintage | 2 portas |
| Legacy | 4 portas |
| Transition | 6 portas |
| Standard | 8 portas |
| Advanced | 10 portas |

Table: Saídas de vídeo por era da placa de vídeo

| Era da placa de vídeo | Saídas de vídeo |
| --- | --- |
| Vintage | 1 saída |
| Legacy | 2 saídas |
| Transition | 2 saídas |
| Standard | 4 saídas |
| Advanced | 4 saídas |

Uma placa-mãe de servidor acrescenta uma saída de console própria. Um processador com gráficos integrados acrescenta uma saída à placa-mãe. As saídas de áudio vêm da placa de som, e da própria placa-mãe a partir da era Transition ([](jsc:sound_cards)).

Quando a placa-mãe fica sem portas de dispositivo, um hub ocupa uma porta e oferece portas próprias. Um hub é um dispositivo, e cada dispositivo ligado a ele ocupa uma das portas do hub.

> [!DEFINE] Hub
> Um aparelho que ocupa uma porta e oferece mais portas do mesmo tipo, para que mais dispositivos se conectem a um computador.

Table: Hubs e as portas que oferecem

| Hub | Portas que oferece |
| --- | --- |
| Hub Vintage | 2 portas |
| Hub Legacy | 4 portas |
| Hub Transition | 4 portas |
| Hub Standard | 7 portas |
| Hub Advanced | 7 portas |

Um cabo depois de um hub alcança tanto quanto o cabo antes dele, e um hub pode ficar pendurado em outro hub. Só os links de dispositivos passam por um hub. Telas e alto-falantes não passam, então conecte-os ao computador. Se você quebrar um hub, os dispositivos atrás dele perdem a conexão.

O Gerenciador de Dispositivos, a ferramenta do Frames que lista o hardware, mostra o que está em cada porta ([](jsc:device_manager)).

## Se algo der errado

> [!PROBLEM] O dispositivo não é reconhecido.
> Confira se o cabo está dentro do alcance da sua era, e se ele não é mais novo que a porta.

> [!PROBLEM] Um monitor conectado por um hub fica apagado.
> Telas e alto-falantes não passam por um hub. Conecte-os ao computador.

> [!SEE] jsc:monitors, jsc:drives, jsc:device_manager
