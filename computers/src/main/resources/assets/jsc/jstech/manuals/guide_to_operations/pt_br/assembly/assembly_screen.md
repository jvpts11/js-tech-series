---
section: assembly
order: 15
icon: jsc:personal_computer
shows: [jsc:motherboard_atx_standard_lga1150, jsc:cpu_integra_centro_c5_4690k, jsc:ram_ddr3_8192, jsc:gpu_vertex_gtx_970, jsc:psu_650g]
---
# A tela de montagem

Um computador aqui é uma caixa com peças dentro. Clique com o botão direito na caixa para abrir a tela de montagem. Ela mostra o interior do computador: um slot para cada peça, e as opções e lâmpadas que o fazem funcionar. Clique agachado no lado da caixa, e não em uma das peças da frente, para tirar o painel lateral esquerdo ou colocá-lo de volta.

```scene
{"balloons": [{"detail": "Todas as outras peças ficam sobre ela, e é ela que decide quais peças cabem.", "number": 1, "part": {"box": [1, 5, 0, 13, 15, 3], "x": 15, "y": 0, "z": 15}, "text": "A placa-mãe"}, {"detail": "O processador fica sob o cooler, no soquete que a placa indica.", "number": 2, "part": {"box": [4, 10, 2, 9, 15, 7], "x": 15, "y": 0, "z": 15}, "text": "O processador e seu cooler"}, {"detail": "Dois módulos estão encaixados em seus slots.", "number": 3, "part": {"box": [9, 9, 1, 13, 15, 4], "x": 15, "y": 0, "z": 15}, "text": "A memória"}, {"number": 4, "part": {"box": [1, 7, 1, 11, 10, 5], "x": 15, "y": 0, "z": 15}, "text": "A placa de vídeo"}, {"number": 5, "part": {"box": [1, 0, 1, 13, 4, 9], "x": 15, "y": 0, "z": 15}, "text": "A fonte de alimentação"}, {"number": 6, "part": {"box": [9, 0, 6, 14, 2, 11], "x": 15, "y": 0, "z": 15}, "text": "O disco rígido"}, {"detail": "Ela fica na própria placa, não em uma baia.", "number": 7, "part": {"box": [8, 6, 1, 12, 8, 3], "x": 15, "y": 0, "z": 15}, "text": "A unidade M.2"}, {"detail": "Ela fica atrás da frente do gabinete.", "number": 8, "part": {"box": [7, 12, 3, 16, 15, 13], "x": 15, "y": 0, "z": 15}, "text": "A unidade de DVD na baia"}], "blocks": [{"data": "{Hardware:{Size:15,Items:[{Slot:0,id:\"jsc:motherboard_atx_standard_lga1150\",count:1},{Slot:1,id:\"jsc:cpu_integra_centro_c5_4690k\",count:1},{Slot:2,id:\"jsc:ram_ddr3_8192\",count:1},{Slot:3,id:\"jsc:ram_ddr3_8192\",count:1},{Slot:6,id:\"jsc:gpu_vertex_gtx_970\",count:1},{Slot:10,id:\"jsc:psu_650g\",count:1},{Slot:11,id:\"jsc:disk_hdd_4t\",count:1},{Slot:12,id:\"jsc:disk_nvme_500g\",count:1},{Slot:13,id:\"jsc:internal_dvd_drive\",count:1}]},SidePanelOff:1b}", "state": "jsc:personal_computer[facing=east]", "x": 15, "y": 0, "z": 15}], "caption": "Um Computador Pessoal com a tampa lateral removida e todas as peças em seus lugares. Arraste para girar o gabinete.", "height": 130, "view": {"pitch": 20.0, "yaw": -20.0, "zoom": 1.2}}
```

Um computador só roda quando as suas peças formam uma compilação que se mantém. Você lê o estado da compilação no cabeçalho da janela, e a janela mostra cada peça no seu próprio slot.

A dica do botão POWER informa quais peças um computador precisa. Sem uma placa de vídeo, o monitor fica apagado, a menos que o processador tenha gráficos integrados.

> [!DEFINE] Montagem
> O conjunto de peças nos slots da tela de montagem. É válido quando não tem problemas.

> [!DEFINE] Placa-mãe
> A placa-mãe, onde todas as outras peças se encaixam. Ela decide quais peças o computador aceita.

> [!DEFINE] Processador (CPU)
> A peça que faz o processamento. CPU significa unidade central de processamento.

> [!DEFINE] Memória (RAM)
> Onde o computador guarda o que está usando agora. RAM significa memória de acesso aleatório.

> [!DEFINE] Placa de vídeo (GPU)
> A peça que desenha a imagem para os monitores. GPU significa unidade de processamento gráfico.

> [!DEFINE] Fonte de alimentação (PSU)
> A peça que fornece energia às outras peças. PSU significa unidade de fonte de alimentação.

> [!DEFINE] Soquete
> O lugar na placa onde fica um processador. Um processador só cabe no soquete que a placa indica.

> [!DEFINE] Buffer de RAM
> Quantos itens um computador consegue segurar enquanto os move: o buffer total dos seus módulos de memória.

## Lendo a janela

O título no cabeçalho diz qual computador você está olhando.

Table: Título no cabeçalho

| Título | Computador |
| --- | --- |
| `PC` | Um Computador Pessoal. |
| `CC` | Um Computador de Fabricação. |
| `CMC` | Um Computador de Gerenciamento de Cluster (CMC). |
| `MAINFRAME` | Um Mainframe. |
| `SERVER` | Um servidor em um Rack de Servidores. |
| `SUPERCOMPUTER NODE` | Um nó de supercomputador em um Rack de Servidores. |

A palavra no canto superior direito diz o estado do computador.

Table: A palavra de estado

| Palavra | Cor | O que significa |
| --- | --- | --- |
| `OFFLINE` | Vermelho | A compilação não é válida: não há compilação, ou ela tem problemas. |
| `READY` | Âmbar | A compilação é válida, e o computador está desligado. |
| `ONLINE` | Verde | A compilação é válida, e o computador está ligado e rodando. |
| `NO FIRMWARE` | Âmbar | O computador está ligado, mas a placa dele não tem firmware: as ventoinhas giram e nada mais ([](jsc:motherboards)). |

Três lâmpadas mostram o que o computador está fazendo.

Table: As lâmpadas

| Lâmpada | Acesa quando |
| --- | --- |
| `PWR` | Verde enquanto o computador roda. Vermelha enquanto não roda. |
| `DISK` | Âmbar enquanto o computador trabalha no seu disco. |
| `NET` | Na cor de destaque enquanto o computador está conectado a uma rede. |

Uma torre tem indicadores que mostram os seus números. Passe o mouse sobre um deles para ler a dica.

Table: Os indicadores de uma torre

| Indicador | O que mostra |
| --- | --- |
| `CAPACITY` | Os itens por tick (it/t) que o computador processa, acesos enquanto ele funciona. |
| `RAM BUFFER` | O buffer da sua memória, em itens (it). |
| `NETWORK` | LINKED na cor de destaque quando há uma rede, caso contrário um "--" discreto; a dica conta os servidores ligados. |

## As opções e o botão de energia

O botão grande à esquerda do painel frontal de uma torre é o POWER. Ele liga e desliga o computador. Uma torre tem duas opções.

Table: As opções de uma torre

| Opção | O que faz |
| --- | --- |
| `AUTO START` | Mantém o computador ligado enquanto a sua montagem estiver válida (veja abaixo). |
| `SIDE PANEL` | Acesa enquanto o painel lateral está retirado, para você ver as peças. |

Ligue o INÍCIO AUTO enquanto a compilação for válida, e o computador liga na hora. Ele continua ligado enquanto a compilação se mantiver. Enquanto o INÍCIO AUTO estiver ligado, o POWER fica travado, e a sua dica diz: "O início automático o mantém ligado: desligue o início automático para desligá-lo aqui".

Se a compilação deixar de se manter, o computador apaga. Quando a compilação voltar a se manter com o INÍCIO AUTO ligado, o computador volta a ligar sozinho.

Se você ligar a energia com uma compilação inválida, a opção liga, mas o computador não roda. A placa-mãe então faz um autoteste e emite um bipe de falha.

Enquanto a compilação não for válida, passar o mouse sobre POWER mostra três linhas: "Ainda não é um computador", depois "Precisa de uma placa-mãe, uma CPU, RAM, um disco e uma fonte", depois "Depois aperte para ligar". A linha do meio é um lembrete geral, então não diz qual peça está errada.

Os indicadores e o botão POWER só mostram a dica quando o mouse para sobre eles e o cursor não segura nada.

A tela de um Mainframe tem INÍCIO AUTO e FAILOVER. Leia sobre failover em [Failover](jsc:failover) e sobre o Mainframe em [Mainframes](jsc:mainframes).

## Dando nome ao computador

O campo de nome no cabeçalho dá nome ao computador. Ele aceita até 32 caracteres e salva cada mudança enquanto você digita. Um campo vazio mostra uma dica que convida você a dar nome ao computador.

## Levando um computador com você

Quebre um computador e ele volta para você inteiro: um item só, com cada peça no seu lugar, e o nome dele. A tooltip dele lista as peças que tem dentro e o que ainda falta para ele rodar. Coloque-o de novo e ele fica como estava. Só as mídias nas unidades da frente, os discos e os disquetes, saem sozinhas.

## Para fazer um computador rodar

1. Clique com o botão direito na caixa para abrir a tela de montagem.
2. Coloque as peças nos seus slots, começando pela placa-mãe.
3. Leia a palavra no cabeçalho: READY significa que a compilação é válida e o computador está desligado.
4. Aperte POWER, ou ligue o INÍCIO AUTO para manter o computador ligado.

## Se algo der errado

> [!PROBLEM] O botão POWER não faz nada.
> O INÍCIO AUTO está ligado, e ele trava o botão. Desligue o INÍCIO AUTO primeiro.

> [!PROBLEM] A opção está ligada, mas o computador continua desligado.
> A compilação não é válida. Passe o mouse sobre POWER: as três linhas dizem quais peças o computador precisa. Em um servidor de rack, a lista CHECKS indica cada problema ([Quando a compilação é recusada](jsc:build_refusals)).

> [!PROBLEM] Uma peça fica na janela e não faz nada.
> A placa-mãe não oferece esse slot. Use uma placa-mãe com mais slots ([](jsc:motherboards)).

> [!SEE] jsc:first_computer, jsc:case_front, jsc:build_refusals
