---
section: systems_and_eras
order: 10
icon: jsc:vintage_personal_computer
shows: [jsc:vintage_personal_computer, jsc:legacy_personal_computer, jsc:transition_personal_computer, jsc:personal_computer, jsc:advanced_personal_computer]
---
# As eras

Computadores e peças do J's Computers pertencem a uma era, uma geração de hardware. As eras vão de Vintage a Advanced, e cada uma tem seu próprio tamanho de palavra, conjunto de instruções, firmware e mídias.

O tamanho da palavra é a quantidade de dados que um processador trata de uma vez. Computadores mais antigos tinham palavras menores, então o espaço que um item ocupa em um disco depende da era ([](jsc:items_as_data)).

O espaço de um disco é medido em megabytes (MB). O espaço por item é quantos megabytes um item ocupa em um disco daquela era.

```table
{"caption": "As eras", "heads": ["Era", "Tamanho da palavra", "Conjunto de instruções", "Espaço por item"], "rows": [[{"words": "Vintage"}, {"words": "16 bits"}, {"literal": "IA-16"}, {"amount": 1, "unit": "MB"}], [{"words": "Legacy"}, {"words": "32 bits"}, {"literal": "x86"}, {"amount": 16, "unit": "MB"}], [{"words": "Transition"}, {"words": "64 bits"}, {"literal": "x86-64"}, {"amount": 256, "unit": "MB"}], [{"words": "Standard"}, {"words": "64 bits"}, {"literal": "x86-64"}, {"amount": 256, "unit": "MB"}], [{"words": "Advanced"}, {"words": "64 bits"}, {"literal": "x86-64"}, {"amount": 256, "unit": "MB"}]]}
```

Table: O firmware e a mídia de cada era

| Era | Firmware | Mídia do sistema | Mídia de programas |
| --- | --- | --- | --- |
| Vintage | BIOS de texto | Disquete, em uma Unidade de Disquete | Disquete, em uma Unidade de Disquete |
| Legacy | Blue BIOS | CD-ROM, em uma Unidade de CD | CD-ROM, em uma Unidade de CD |
| Transition | Blue BIOS | DVD-ROM, em uma Unidade de DVD | DVD-ROM, em uma Unidade de DVD |
| Standard | UEFI | Pen Drive, em uma Estação de Dock ou na porta USB de um gabinete | DVD-ROM para programas, Pen Drive para serviços em segundo plano |
| Advanced | UEFI | Pen Drive, em uma Estação de Dock ou na porta USB de um gabinete | Blu-ray para programas, Pen Drive para serviços em segundo plano |

As peças não se misturam livremente entre as eras, então confira a era de cada peça antes de colocá-la. Um Computador Pessoal aceita só uma placa-mãe da sua própria era. Um processador, um módulo de memória e uma placa de som também precisam ter a era da placa. Uma placa de vídeo só é verificada pelo barramento em que se encaixa.

Uma era mais nova é mais rápida: as peças têm mais núcleos, clocks mais altos, mais memória e discos mais rápidos. A inicialização dela também termina mais cedo ([](jsc:turning_on)).

Cada era tem sua própria aba no criativo, de J's Computers - Vintage até J's Computers - Advanced.

> [!DEFINE] Conjunto de instruções (ISA)
> A linguagem que um processador entende. Um programa feito para uma delas roda nele e em todos os mais novos, nunca em um mais antigo: o x86-64 roda programas x86 e IA-16.

## Se algo der errado

> [!PROBLEM] Uma peça não encaixa no slot de uma placa-mãe de outra era.
> O processador, o módulo de memória ou a placa de som devem ser da era da sua placa-mãe ([](jsc:motherboards)).

> [!SEE] jsc:items_as_data, jsc:motherboards
