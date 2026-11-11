---
section: by_hand_basics
order: 301
icon: jsc:motherboard_atx_standard_lga1150
---
# BIOS ou UEFI

O firmware vem em dois tipos, e a era da sua placa-mãe decide qual você tem. O tipo mais antigo é o sistema básico de entrada e saída (BIOS). O tipo mais novo é a Interface Unificada e Extensível de Firmware (UEFI). A instalação precisa dessa diferença, porque cada tipo inicia um disco do seu próprio jeito.

> [!DEFINE] BIOS
> O tipo mais antigo de firmware, em modo texto ou com um painel azul. Ele inicia um disco a partir da primeira parte dele.

> [!DEFINE] Partição
> Uma parte de um disco que o sistema trata como um disco próprio. Um disco pode ter várias dessas partes.

> [!DEFINE] UEFI
> O tipo mais novo de firmware. Ele lê uma pequena partição do disco, formatada como FAT (File Allocation Table), para encontrar o programa que inicia o sistema.

Table: O firmware de cada era

| Era | Firmware | O que a instalação precisa |
| --- | --- | --- |
| Vintage | Text BIOS | O disco inteiro: o programa de inicialização fica no próprio disco. |
| Legacy e Transition | Blue BIOS | O disco inteiro: o programa de inicialização fica no próprio disco. |
| Standard, Advanced e posteriores | UEFI | Uma pequena partição FAT para o programa de inicialização, criada nos passos de disco. |

Em um computador Vintage, Legacy ou Transition, o tutorial é mais curto. As primeiras linhas dele avisam: "No firmware antigo, a raiz é o disco inteiro, /dev/sda, e os passos que dizem isso abaixo são omitidos."

Em um computador Standard ou posterior, crie a partição primeiro. O tutorial marca os passos para o firmware moderno, e você os digita em ordem: uma tabela nova, uma partição de 512 MB, com o tipo definido como EFI System (tipo 1), e depois a partição formatada como FAT. O capítulo [Os discos](jsc:preparing_disks) mostra cada passo.

Os dois tipos usam as mesmas teclas enquanto o computador inicia. [[DEL]] entra no Setup, as configurações do firmware, e [[F12]] abre o Menu de Inicialização ([](jsc:firmware)).

## Se algo der errado

> [!PROBLEM] A etapa do bootloader recusa a instalação em um computador Standard ou posterior.
> O programa de inicialização precisa da partição FAT, montada onde o passo indica. Crie a partição primeiro, como mostram os passos modernos do tutorial ([](jsc:the_walkthrough)).

> [!SEE] jsc:firmware, jsc:the_systems
