---
section: assembly
order: 16
icon: jsc:internal_cd_drive
covers_family: jsc:case_front
---
# A frente do gabinete

A frente de um Computador Pessoal, de um Computador de Fabricação ou de um Computador de Gerenciamento de Cluster tem peças que você pode clicar. Ela tem um botão de energia, baias para unidades e, a partir da Transition, portas USB. Clique em uma dessas peças e ela faz o seu trabalho. Clique em qualquer outro ponto do gabinete e a tela de montagem se abre, ou, com um clique agachado, a tampa lateral sai ou volta ao lugar.

```scene
{"balloons": [{"detail": "Clique nele para ligar ou desligar o computador.", "number": 1, "part": {"box": [12, 9, 15, 14, 12, 16], "x": 15, "y": 0, "z": 15}, "text": "Botão de energia"}, {"detail": "A primeira acende enquanto o computador funciona. A segunda pisca enquanto ele acessa o disco.", "number": 2, "part": {"box": [9, 10, 15, 12, 12, 16], "x": 15, "y": 0, "z": 15}, "text": "Luzes de energia e de disco"}, {"detail": "Seu botão de ejeção abre e fecha a bandeja.", "number": 3, "part": {"box": [3, 13, 14, 13, 15, 16], "x": 15, "y": 0, "z": 15}, "text": "A Unidade de DVD Interna"}, {"detail": "Clique com um disco na mão para colocá-lo sobre a bandeja aberta. Agache-se e clique para retirá-lo.", "number": 4, "part": {"box": [4, 12, 14, 12, 13, 16], "x": 15, "y": 0, "z": 15}, "text": "A bandeja e seu disco"}, {"detail": "Agache-se e clique na porta para puxar o pen drive.", "number": 5, "part": {"box": [1, 11, 15, 5, 13, 16], "x": 15, "y": 0, "z": 15}, "text": "Um pen drive USB"}, {"detail": "Clique nela com um pen drive na mão para conectar o pen drive.", "number": 6, "part": {"box": [1, 9, 15, 5, 10, 16], "x": 15, "y": 0, "z": 15}, "text": "Uma porta USB livre"}], "blocks": [{"data": "{Hardware:{Size:15,Items:[{Slot:0,id:\"jsc:motherboard_atx_standard_lga1150\",count:1},{Slot:1,id:\"jsc:cpu_integra_centro_c5_4690k\",count:1},{Slot:2,id:\"jsc:ram_ddr3_8192\",count:1},{Slot:3,id:\"jsc:ram_ddr3_8192\",count:1},{Slot:6,id:\"jsc:gpu_vertex_gtx_970\",count:1},{Slot:10,id:\"jsc:psu_650g\",count:1},{Slot:11,id:\"jsc:disk_hdd_4t\",count:1},{Slot:12,id:\"jsc:disk_nvme_500g\",count:1},{Slot:13,id:\"jsc:internal_dvd_drive\",count:1}]},Front:{Size:4,Items:[{Slot:0,id:\"jsc:dvd_rom\",count:1},{Slot:2,id:\"jsc:usb_flash_drive\",count:1}]},Bay0TrayOpen:1b,Lamps:3}", "state": "jsc:personal_computer[facing=south]", "x": 15, "y": 0, "z": 15}], "caption": "A frente de um Computador Pessoal Standard: o botão de energia e suas duas luzes, uma unidade de DVD com a bandeja para fora e um pen drive em uma porta USB. Arraste para ver o gabinete de lado.", "height": 130, "view": {"pitch": 15.0, "yaw": -20.0, "zoom": 1.4}}
```

Mire em uma peça saliente, como uma bandeja aberta ou um pen drive em uma porta, e o seu clique vai para essa peça. O botão de energia, no alto da frente, liga e desliga o computador, qualquer que seja o item que você esteja segurando. Ele faz o mesmo que POWER na tela de montagem.

> [!DEFINE] Peça saliente
> Uma peça que sai para fora da frente e recebe cliques próprios.

Table: A frente do gabinete de cada era

| Era | Baias de 5,25 polegadas | Portas USB na frente |
| --- | --- | --- |
| Vintage | 1 baia | 0 portas USB |
| Legacy | 2 baias | 0 portas USB |
| Transition | 2 baias | 2 portas USB |
| Standard | 1 baia | 2 portas USB |
| Advanced, Exa e Singularity | 0 baias | 2 portas USB |

> [!DEFINE] Baia
> Um encaixe na frente do gabinete para uma unidade de 5,25 polegadas.

## O que vai em uma baia

Quatro peças cabem em uma baia, uma por baia, e qualquer uma delas cabe em qualquer baia que o gabinete tenha. Você a coloca na tela de montagem.

Table: As peças de uma baia

| Peça | Era | O que aceita |
| --- | --- | --- |
| Baia Hot-Swap de Disco | Vintage | Discos, pela frente |
| Unidade de CD Interna | Legacy | CDs |
| Unidade de DVD Interna | Transition | DVDs e CDs |
| Unidade de Blu-ray Interna | Advanced | Discos Blu-ray, DVDs e CDs |

Uma unidade interna funciona como a unidade do mesmo tipo fora do gabinete ([as unidades](jsc:drives)). O botão de ejetar na sua frente abre ou fecha a bandeja. Com a bandeja aberta, clique com um disco na mão para depositá-lo na bandeja. Clique agachado na bandeja aberta para retirar o disco de novo.

Uma unidade lê apenas os discos do seu tipo. Uma Unidade de CD lê CDs, uma Unidade de DVD lê DVDs e CDs, e uma Unidade de Blu-ray lê os três.

> [!DEFINE] Bandeja
> A gaveta de uma unidade óptica, que leva o disco.

## A Baia Hot-Swap de Disco

A Baia Hot-Swap de Disco aceita um disco pela frente. Clique com um disco na mão para deslizá-lo para dentro, e clique com a mão vazia para tirá-lo. Funciona com o computador ligado ou desligado. Aceita um disco da própria era do computador, ou de uma era anterior.

> [!DEFINE] Troca a quente
> Uma peça que você pode puxar e encaixar de novo enquanto o computador está ligado.

O disco na baia é um dos discos do próprio computador. Ele recebe uma letra de unidade, um sistema pode ser instalado nele, e o computador pode inicializar a partir dele. Ele soma à capacidade do computador e à energia que ele precisa. Não ocupa nenhum dos slots de disco da placa-mãe, então uma placa-mãe cheia ainda aceita mais um disco aqui.

## Portas USB

Clique em uma porta enquanto segura um pen drive USB para conectá-lo. Clique agachado em uma porta para puxar o pen drive dela. Um pen drive funciona como uma unidade própria, e um sistema pode ser instalado a partir dele ([Instalando um sistema](jsc:installing)).

## Se algo der errado

> [!PROBLEM] O disco não entra, e o jogo diz "A bandeja está fechada - aperte o botão de ejetar para abri-la."
> Pressione o botão de ejetar na frente da unidade para abrir a bandeja.

> [!PROBLEM] A unidade não consegue ler esse disco.
> O jogo diz o nome da unidade, como em "Esta unidade de CD não consegue ler este disco." Uma Unidade de CD lê CDs, uma Unidade de DVD lê DVDs e CDs, e uma Unidade de Blu-ray lê os três. Use uma unidade do tipo certo.

> [!PROBLEM] A bandeja não sai, e o jogo diz "A unidade está lendo - aguarde até terminar."
> A unidade está lendo um disco. Espere terminar.

> [!PROBLEM] A unidade já tem um disco.
> Pressione o botão de ejetar para abrir a bandeja, depois clique na bandeja aberta enquanto estiver agachado para tirar o disco.

> [!PROBLEM] A porta já tem um pen drive.
> Clique agachado na porta para puxar o pen drive dela.

> [!PROBLEM] A baia já tem um disco.
> Clique na baia com a mão vazia para tirar o disco.

> [!PROBLEM] A baia Hot-Swap recusa um disco, e a mensagem diz que este computador não aceita esse disco.
> O disco é mais novo que a era do computador. Use um disco da era do computador ou de uma era anterior.

> [!SEE] jsc:assembly_screen, jsc:drives, jsc:disks
