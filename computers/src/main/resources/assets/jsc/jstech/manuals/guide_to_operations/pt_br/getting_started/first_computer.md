---
section: getting_started
order: 2
icon: jsc:personal_computer
shows: [jsc:personal_computer, jsc:motherboard_atx_standard_lga1150, jsc:cpu_integra_centro_c5_4690k, jsc:ram_ddr3_8192, jsc:gpu_vertex_gtx_970, jsc:psu_650g, jsc:disk_ssd_1t, jsc:monitor, jsc:dock_station]
---
# Montando seu primeiro computador

Montar um computador é mais fácil do que parece. Você coloca um gabinete, põe as peças nele uma por uma e depois adiciona um monitor.

Pegue um Computador Pessoal na aba criativa J's Computers - Standard e coloque-o. Usar o computador abre a janela dele, que é o interior do gabinete, com um lugar para cada peça. Um gabinete sozinho ainda não faz nada.

## As peças

1. Coloque uma placa-mãe. Ela é a placa sobre a qual as outras peças ficam, e o gabinete aceita apenas uma placa da sua própria era ([](jsc:motherboards)).
2. Coloque um processador. O soquete dele precisa combinar com o soquete da placa ([](jsc:processors)).
3. Coloque memória. A placa aceita alguns tipos de memória e não outros ([](jsc:memory)).
4. Coloque uma placa de vídeo. Sem ela o monitor não tem imagem, a menos que o processador tenha gráficos integrados ([](jsc:graphics_cards)).
5. Coloque um disco. O sistema é instalado nele, e seus arquivos ficam guardados nele ([](jsc:disks)).
6. Coloque uma fonte. Ela fornece energia às peças, medida em watts (W) ([](jsc:power_supplies)).

Um computador sem disco ainda liga, e depois informa que nenhum sistema pode ser iniciado ([](jsc:no_bootable_device)). Um computador sem placa-mãe ou sem fonte não tem compilação alguma, então permanece desligado.

Uma peça que não encaixa não entra no seu slot. Um processador ou um módulo de memória precisa combinar com o soquete, a era e a geração de memória da placa. Uma placa de vídeo precisa caber no barramento da placa.

Uma fonte de potência fixa recusa a compilação quando as peças consomem mais watts do que ela fornece. O interruptor liga, mas o computador não roda.

Em um Computador Pessoal, a janela só diz que ainda não é um computador e o que ele ainda precisa. A tela de montagem de um servidor lista cada problema na dica dele.

## O monitor

Um computador sem tela não serve de muita coisa. Coloque um Monitor ([](jsc:monitors)) bem junto ao gabinete, e ele fica ligado ao computador. Estenda um [Cabo Periférico](jsc:peripheral_cables) quando o monitor precisar ficar mais longe.

```scene
{"balloons": [{"number": 1, "part": {"x": 14, "y": 0, "z": 15}, "text": "O Computador Pessoal"}, {"detail": "Pressione-o para ligar o computador.", "number": 2, "part": {"box": [12, 9, 15, 14, 12, 16], "x": 14, "y": 0, "z": 15}, "text": "Botão de energia"}, {"number": 3, "part": {"x": 16, "y": 0, "z": 15}, "text": "O Monitor"}, {"detail": "Ele liga o monitor ao computador quando o monitor não pode ficar encostado no gabinete.", "number": 4, "part": {"x": 15, "y": 0, "z": 15}, "text": "O Cabo Periférico"}], "blocks": [{"data": "{Hardware:{Size:15,Items:[{Slot:0,id:\"jsc:motherboard_atx_standard_lga1150\",count:1},{Slot:1,id:\"jsc:cpu_integra_centro_c5_4690k\",count:1},{Slot:2,id:\"jsc:ram_ddr3_8192\",count:1},{Slot:3,id:\"jsc:ram_ddr3_8192\",count:1},{Slot:6,id:\"jsc:gpu_vertex_gtx_970\",count:1},{Slot:10,id:\"jsc:psu_650g\",count:1},{Slot:11,id:\"jsc:disk_ssd_1t\",count:1}]},ManualOn:1b,Lamps:1}", "state": "jsc:personal_computer[facing=south]", "x": 14, "y": 0, "z": 15}, {"data": "{Lit:1b,SwitchedOn:1b}", "state": "jsc:monitor[facing=north,button=true,lit=true,on=true]", "x": 16, "y": 0, "z": 15}, {"data": "{Wires:[{cable:\"jsc:peripheral_cable\"}],Links:[I;0,0,0,4116,0,0,0,0,0]}", "state": "jscore:cable", "x": 15, "y": 0, "z": 15}, {"data": "{Wires:[{cable:\"jsc:peripheral_cable\"}],Links:[I;0,0,0,40,0,0,0,0,0]}", "state": "jscore:cable", "x": 15, "y": 0, "z": 14}, {"data": "{Wires:[{cable:\"jsc:peripheral_cable\"}],Links:[I;0,0,0,2072,0,0,0,0,0]}", "state": "jscore:cable", "x": 16, "y": 0, "z": 14}], "caption": "Um Computador Pessoal com seu Monitor, ligado por um Cabo Periférico. Arraste para ver a parte de trás.", "height": 120}
```

A Estação de Dock ([](jsc:dock_station)) lê pen drives USB, e é ali que vai uma mídia de instalação USB. Ligue-a ao computador como qualquer outra unidade.

> [!DEFINE] Mídia de instalação
> O disquete, CD, DVD ou pen drive USB a partir do qual um sistema é instalado. O instalador é o programa que está nele.

O sistema e os programas aparecem no monitor, não na janela. A frente do gabinete tem o botão de energia ([](jsc:case_front)).

## Ligando o computador

Pressione o botão de energia na frente do gabinete ([](jsc:case_front)). O monitor mostra a inicialização, e se nenhum sistema estiver em nenhum disco, o firmware espera por você ([](jsc:no_bootable_device)).

Quando o computador estiver rodando, a barra de energia do monitor oferece os botões Desligar o PC e Reiniciar o PC.

1. Coloque a mídia de instalação do Frames 10 na Estação de Dock.
2. Ligue o computador e observe o monitor ([](jsc:turning_on)).
3. Inicie o instalador a partir da mídia e siga-o ([](jsc:installing)).

## Se algo der errado

> [!PROBLEM] A dica da janela diz "Ainda não é um computador".
> Uma compilação precisa de uma placa-mãe, um processador, memória e uma fonte. A dica também cita um disco: coloque um também, para que o computador tenha um sistema para iniciar.

> [!PROBLEM] O interruptor liga, mas o computador não roda.
> A compilação é recusada. Confira as peças entre si: o soquete, a era, a geração de memória e a energia ([](jsc:build_refusals)).

> [!PROBLEM] O monitor diz "Nenhum computador encontrado ao alcance por um Cabo Periférico".
> Coloque o monitor bem junto ao gabinete, ou estenda um Cabo Periférico entre eles ([](jsc:peripheral_cables)).

> [!PROBLEM] O monitor diz "O computador não tem saída de vídeo: instale uma placa de vídeo".
> Coloque uma placa de vídeo no gabinete, ou use um processador com gráficos integrados ([](jsc:graphics_cards)).

> [!PROBLEM] O monitor diz "Sem sinal - o computador está desligado".
> Pressione o botão de energia na frente do gabinete ([](jsc:case_front)).

> [!PROBLEM] O computador apita, e o monitor diz que as peças dele não formam um computador.
> Falta uma peça ou uma delas não serve. Abra o computador: a lista TESTES dele diz o que corrigir ([](jsc:build_refusals)).

> [!PROBLEM] Um processador ou um módulo de memória não encaixa no seu slot.
> Confira a placa: o soquete, a era e a geração de memória precisam combinar ([](jsc:motherboards)).

> [!PROBLEM] O firmware informa que nenhum sistema pode ser iniciado.
> Ainda não há sistema em nenhum disco. Instale um ([](jsc:installing)).

> [!SEE] jsc:turning_on, jsc:installing
