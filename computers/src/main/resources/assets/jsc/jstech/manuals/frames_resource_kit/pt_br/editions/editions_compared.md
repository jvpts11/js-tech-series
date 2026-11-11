---
section: editions
order: 227
---
# As edições lado a lado

Frames é uma família de cinco edições. Uma edição é uma versão do Frames, com seu próprio instalador, sua própria área de trabalho e seus próprios programas integrados. As edições têm uma ordem, do Frames 95, a mais antiga, ao Frames 11, a mais nova.

Cada edição tem uma posição na família: o seu lugar na ordem da família, contado a partir de 1. Um programa pode pedir uma versão pela sua posição. Um programa que precisa de "Frames XP ou mais novo" roda no XP, no 7, no 10 e no 11, e recusa o Frames 95.

Cada edição precisa de um computador da sua própria era ou de uma mais nova. O Frames 95 e o XP precisam de Legacy, o Frames 7 precisa de Transition, o Frames 10 precisa de Standard e o Frames 11 precisa de Advanced. Cada edição também roda em todo computador de uma era mais nova. Uma edição mais nova não vai em um computador mais antigo, e o instalador avisa isso ([](jsc:the_systems)).

A mídia de instalação é o item que você coloca em uma unidade para instalar uma edição. O Frames 95 e o XP são instalados a partir de um disco compacto (CD), o Frames 7 a partir de um disco digital versátil (DVD), e o Frames 10 e o 11 a partir de um pen drive USB, que vai na [Estação de Dock](jsc:dock_station).

Table: As cinco edições

| Edição | Posição na família | Era mínima | Mídia de instalação |
| --- | --- | --- | --- |
| Frames 95 | 1 | Legacy | CD, na Unidade de CD |
| Frames XP | 2 | Legacy | CD, na Unidade de CD |
| Frames 7 | 3 | Transition | DVD, na Unidade de DVD |
| Frames 10 | 4 | Standard | Pen drive USB, na Estação de Dock |
| Frames 11 | 5 | Advanced | Pen drive USB, na Estação de Dock |

```table
{"caption": "O que cada edição ocupa", "heads": ["Edição", "Ocupação", "Memória (RAM) em uso", "Itens no disco da sua era"], "rows": [[{"words": "Frames 95"}, {"amount": 48, "unit": "MB"}, {"amount": 32, "unit": "MB"}, {"words": "3 itens em um disco Legacy"}], [{"words": "Frames XP"}, {"amount": 1536, "unit": "MB"}, {"amount": 128, "unit": "MB"}, {"words": "96 itens em um disco Legacy"}], [{"words": "Frames 7"}, {"amount": 16384, "unit": "MB"}, {"amount": 1024, "unit": "MB"}, {"words": "64 itens em um disco Transition"}], [{"words": "Frames 10"}, {"amount": 16384, "unit": "MB"}, {"amount": 3072, "unit": "MB"}, {"words": "64 itens em um disco Standard"}], [{"words": "Frames 11"}, {"amount": 20480, "unit": "MB"}, {"amount": 8192, "unit": "MB"}, {"words": "80 itens em um disco Advanced"}]]}
```

A ocupação é o espaço que um sistema ocupa em um disco depois de instalado. Um disco conta o seu espaço em itens, e um item vale 1 MB em um disco Vintage, 16 MB em um disco Legacy e 256 MB a partir da era Transition. Assim, o Frames 95 ocupa 3 itens de um disco Legacy.

A coluna Memória (RAM) em uso é a memória que cada sistema reserva para si antes que qualquer programa seja aberto.

> [!DEFINE] Era
> Uma geração de hardware, como Legacy ou Standard.

> [!DEFINE] Posição na família
> A posição de uma edição na ordem da sua família, contada a partir de 1.

> [!DEFINE] Ocupação
> O espaço que um sistema ocupa em um disco depois de instalado, contado nos itens do disco.

## Se algo der errado

> [!PROBLEM] O instalador diz "Frames 10 precisa de hardware da era Standard ou mais novo; esta máquina é da era Legacy."
> O computador é mais antigo que a edição. Use um computador da era que a mensagem indica, ou instale uma edição mais antiga ([](jsc:installing)).

> [!PROBLEM] O instalador diz que o disco de destino não tem espaço para a edição, e informa os megabytes de que ela precisa.
> Use um disco maior. Um disco Legacy comporta o Frames 95 em 3 itens, e o Frames 11 precisa de 80 itens de um disco Advanced.

> [!PROBLEM] O instalador diz "Nenhum disco está instalado para colocar Frames 11 nele."
> Instale um disco no computador primeiro ([](jsc:disks)).

> [!PROBLEM] O instalador diz "O sistema na mídia não é reconhecido por esta máquina."
> Cada mídia de instalação pertence a um sistema. Use a mídia feita para a edição que você quer ([](jsc:installing)).

> [!SEE] jsc:installing, jsc:eras, jsc:midsoft_systems
