---
section: by_hand_basics
order: 300
---
# O que você precisa

Quatro coisas precisam estar prontas antes de você começar. Cada uma tem uma recusa própria, então a máquina informa qual está faltando, e a tabela mostra de onde vem cada uma.

> [!DEFINE] Firmware
> O programa na placa-mãe que inicia o computador, antes que qualquer sistema rode.

> [!DEFINE] Era
> Uma geração de hardware, como Vintage, Legacy ou Standard. Um computador tem a era da sua placa-mãe.

> [!DEFINE] Mainframe
> O computador que executa uma rede, que é todo computador e dispositivo ligado a ele por cabo de dados. Ele hospeda os serviços da rede, como o Mirror.

Primeiro, o computador precisa ser de era Legacy ou mais recente. A era é a da placa-mãe do computador, não a das peças que você põe nele. Um computador Vintage não consegue instalar nenhum dos dois sistemas.

Table: O que você precisa

| Item | De onde vem | Se estiver faltando |
| --- | --- | --- |
| Uma placa-mãe de era Legacy ou mais recente | A placa-mãe do computador. | O firmware recusa: "Arch Linux precisa de hardware de era Legacy ou mais recente; esta máquina é de era Vintage." |
| Um disco em um compartimento de disco | Os compartimentos de disco do computador, chamados sda, sdb e assim por diante, na ordem dos compartimentos. | `fdisk: cannot open /dev/sdb: No such file or directory` |
| O CD do Arch Linux ou do Gentoo em uma Unidade de CD conectada ao computador | O CD está na aba criativa Legacy, J's Computers - Legacy. | O firmware diz: "Nenhuma mídia de instalação está em uma unidade conectada a esta máquina." |
| O Mirror, rodando no Mainframe da rede | Instalado na rede com o comando mirror install. | Os passos de pacote falham, porque o Mirror não responde. |

> [!DEFINE] Mirror
> O repositório de pacotes da rede, um serviço no Mainframe. Todo gerenciador de pacotes da rede instala a partir dele.

Os passos de pacote buscam no Mirror, então precisam dele. Ele só responde enquanto o Mainframe estiver em execução ([](jsc:mainframes)). Digite mirror sozinho para ver como ele está:

Table: Como está o Mirror

| O que diz | O que significa |
| --- | --- |
| `Mirror: not installed` | Instale-o na rede com o comando mirror install. |
| `Mirror: installed (Mainframe off)` | Está instalado, mas o Mainframe não está em execução, então nada responde. |
| `Mirror: serving` | O Mirror responde, e os passos de pacote conseguem buscar. |

Por último, a máquina precisa iniciar a partir da mídia. Na lista do firmware, escolha o CD. A máquina reinicia, roda o autoteste de novo, e então o shell da mídia aparece ([](jsc:firmware_for_install)).

## Se algo der errado

> [!PROBLEM] A lista do firmware mostra "era Legacy ou mais recente" ao lado do meio.
> Esta máquina é mais antiga do que o meio exige. Coloque no computador uma placa-mãe de era Legacy ou mais recente.

> [!PROBLEM] O firmware diz que não há mídia de instalação em uma unidade vinculada.
> Coloque o CD em uma Unidade de CD e conecte essa unidade ao computador ([](jsc:drives)).

> [!PROBLEM] O shell diz que o disco não existe.
> Coloque um disco em um compartimento de disco. Depois digite lsblk para listar os discos que o shell consegue ver ([](jsc:disks)).

> [!PROBLEM] Os passos de pacote falham, e o Mirror não responde.
> Digite mirror para ver como ele está. Instale-o com mirror install se ele não estiver instalado, e mantenha o Mainframe em execução.

> [!SEE] jsc:disks, jsc:drives, jsc:mainframes
