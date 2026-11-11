---
section: midsoft_installs
order: 197
icon: jsc:personal_computer
---
# Qual Frames vai em qual máquina

A Midsoft fez cinco edições do Frames. Cada edição é feita para uma era de computador, a geração a que uma máquina pertence ([](jsc:eras)). Um sistema roda na era para a qual foi feito e em todas as eras mais novas, mas nunca em uma máquina mais antiga.

Escolha a edição de acordo com a máquina que você tem. As edições também têm uma classificação, de 1 para o Frames 95 a 5 para o Frames 11. Os programas usam a classificação quando pedem uma edição do Frames, como XP ou mais nova. O instalador não a usa.

A mídia de instalação é o disco compacto (CD), o disco digital versátil (DVD) ou o pen drive Universal Serial Bus (USB) de onde um sistema é instalado. Você lê uma mídia em uma unidade conectada ao computador: um CD vai em uma Unidade de CD, um DVD em uma Unidade de DVD, e um pen drive USB em uma Estação de Dock ([](jsc:dock_station)).

Table: As cinco edições e suas máquinas

| Edição | Era da máquina | Mídia de instalação |
| --- | --- | --- |
| Frames 95 | Legacy | CD |
| Frames XP | Legacy | CD |
| Frames 7 | Transition | DVD |
| Frames 10 | Standard | Pen drive USB |
| Frames 11 | Advanced | Pen drive USB |

RAM própria é a memória de acesso aleatório (RAM) que o sistema em execução reserva para si antes de qualquer programa abrir. Ocupação é o espaço que o sistema ocupa no disco. Ambas são contadas em megabytes (MB), e a tabela abaixo as informa para cada edição ([](jsc:frames_memory)).

```table
{"caption": "RAM própria e ocupação de cada edição", "heads": ["Edição", "RAM própria (MB)", "Ocupação (MB)"], "rows": [[{"words": "Frames 95"}, {"amount": 32, "unit": "MB"}, {"amount": 48, "unit": "MB"}], [{"words": "Frames XP"}, {"amount": 128, "unit": "MB"}, {"amount": 1536, "unit": "MB"}], [{"words": "Frames 7"}, {"amount": 1024, "unit": "MB"}, {"amount": 16384, "unit": "MB"}], [{"words": "Frames 10"}, {"amount": 3072, "unit": "MB"}, {"amount": 16384, "unit": "MB"}], [{"words": "Frames 11"}, {"amount": 8192, "unit": "MB"}, {"amount": 20480, "unit": "MB"}]]}
```

Um disco mede seu espaço em itens, e um item ocupa 1 MB em um disco Vintage, 16 MB em um disco Legacy e 256 MB em um disco da era Transition ou mais nova. Por isso o mesmo sistema ocupa menos itens em um disco mais novo. A ocupação conta itens inteiros, arredondados para cima ([](jsc:disks)).

Table: Itens que cada edição ocupa em um disco

| Edição | Em um disco Legacy | Em um disco Transition, Standard ou Advanced |
| --- | --- | --- |
| Frames 95 | 3 itens | 1 item |
| Frames XP | 96 itens | 6 itens |
| Frames 7 | 1.024 itens | 64 itens |
| Frames 10 | 1.024 itens | 64 itens |
| Frames 11 | 1.280 itens | 80 itens |

Um disco Legacy de 4 gigabytes (GB), como o Vaultis Link IDE-HDD 4G, tem 4.096 MB. Isso dá espaço para o Frames XP, que precisa de 1.536 MB, mas não para o Frames 7, que precisa de 16.384 MB. Os discos que você pode comprar estão listados em [Discos](jsc:disks).

A era também define o firmware, o programa que inicia a máquina. O Frames 95, o XP e o 7 iniciam pelo sistema básico de entrada e saída (BIOS) azul, e o Frames 10 e o 11 pela Interface Unificada de Firmware Extensível (UEFI) ([](jsc:firmware)).

A memória de uma máquina vem de seus módulos de memória. O menor módulo da era Legacy é o Stratix Layer SDRAM-128, com 128 MB. Uma máquina Legacy com esse módulo roda o Frames XP sem RAM apertada, o que a página de memória explica ([](jsc:frames_memory)).

> [!DEFINE] Edição
> Uma versão do Frames, com seu próprio instalador e sua própria área de trabalho.

> [!DEFINE] Mídia de instalação
> O disco ou pen drive de onde um sistema é instalado, como um CD ou um pen drive USB.

> [!DEFINE] Firmware
> O programa na placa-mãe que liga o computador e testa seu hardware antes de passar o controle para um sistema.

> [!DEFINE] RAM própria
> A memória que o sistema em execução reserva para si antes de qualquer programa abrir, em megabytes.

> [!DEFINE] Ocupação
> O espaço que um sistema ocupa no disco, em megabytes. O disco conta esse espaço em itens.

## Se algo der errado

> [!PROBLEM] A tela diz: "O Frames 10 precisa de hardware da era Standard ou mais novo; esta máquina é da era Legacy."
> Escolha a edição da era desta máquina, ou use uma máquina mais nova ([](jsc:eras)).

> [!PROBLEM] A tela diz: "Nenhum disco está instalado para colocar Frames 7 nele."
> Instale um disco na máquina primeiro, depois inicie o instalador de novo ([](jsc:disks)).

> [!PROBLEM] A tela diz: "O disco de destino não tem espaço para", seguido do nome do sistema e, entre parênteses, dos megabytes de que ele precisa.
> Escolha um disco maior, ou outro disco com espaço ([](jsc:disks)).

> [!SEE] jsc:frames_setup_95_xp, jsc:frames_setup_7_10_11, jsc:eras
