---
section: starting_a_computer
order: 4
icon: jsc:motherboard_atx_standard_lga1150
---
# Firmware: a BIOS e a UEFI

O firmware fica na placa-mãe, não em nenhum disco, então ele existe antes de qualquer sistema. É o primeiro programa a rodar quando o computador inicia: ele verifica o hardware ([](jsc:turning_on)), encontra os discos e unidades e entrega o computador a um sistema. Se nenhum sistema for encontrado, ele espera na tela por você.

O tipo depende da era da placa-mãe. Placas mais antigas têm uma BIOS, e placas mais novas têm uma UEFI. Ambas são firmware, e cada era tem a sua aparência:

Table: O firmware de cada era

| Era | Firmware na tela |
| --- | --- |
| Vintage | BIOS em modo texto, texto simples na tela. |
| Legacy e Transition | BIOS azul. |
| Standard e Advanced | UEFI. |

Uma placa também pode vir sem firmware: a tooltip dela diz isso. Um computador montado nela liga e gira as ventoinhas, e nada mais: nem autoteste, nem imagem, e o monitor diz que a placa-mãe não tem firmware. Uma Pré-Instaladora grava o firmware da era da placa na placa, ou no computador em que ela está ([](jsc:computer_making_machines)).

A ordem de inicialização é a lista de discos na ordem em que o firmware os tenta. Você muda a ordem na página Ordem de Inicialização do Setup ([](jsc:setup_utility)).

> [!DEFINE] Ordem de inicialização
> A ordem em que o firmware tenta os discos quando o computador inicia. O primeiro disco com um sistema nele é iniciado.

O Menu de Inicialização ([](jsc:boot_menu)) é a mesma escolha, mas para uma única inicialização. Um disco escolhido ali não é salvo, então a próxima inicialização segue a ordem de inicialização de novo.

Um disco com dois sistemas pode mostrar um menu antes de iniciar. Veja [Dois sistemas em um computador](jsc:two_systems).

O Setup também tem uma página Hardware, que lista o que o firmware encontrou na máquina. Em um servidor com um controlador de armazenamento, há uma página Storage para o array dos seus discos ([](jsc:raid_arrays)).

> [!DEFINE] BIOS
> Basic Input/Output System: o tipo mais antigo de firmware. As placas Vintage, Legacy e Transition têm uma BIOS.

> [!DEFINE] UEFI
> Unified Extensible Firmware Interface: o tipo mais novo de firmware. As placas Standard e Advanced têm uma UEFI.

## Se algo der errado

> [!PROBLEM] A tela informa que nada pode ser iniciado, com palavras que dependem da era.
> Nenhum disco tem um sistema ainda, e nenhuma mídia de instalação está em uma unidade. Coloque uma e inicie por ela ([](jsc:installing)). As palavras exatas dependem da era ([](jsc:no_bootable_device)).

> [!PROBLEM] Um disco não está na lista.
> O disco não está conectado ao computador, ou a mídia está no tipo errado de unidade. Um disquete vai em uma Unidade de Disquete, e um CD em uma Unidade de CD ([](jsc:drives)).

> [!PROBLEM] O computador inicia pelo disco errado.
> Mude a ordem de inicialização na página Ordem de Inicialização do Setup, ou escolha o disco no Menu de Inicialização ([](jsc:setup_utility)).
