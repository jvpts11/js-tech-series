---
section: gentoo_install
order: 318
icon: jsc:usb_flash_drive
---
# Como o Gentoo é instalado

O Gentoo não tem um instalador que faça o trabalho por você. Sua mídia de instalação oferece um shell e um passo a passo, e você digita cada passo sozinho. Cada ferramenta que você executa é a verdadeira. Os passos gerais de todos os sistemas estão em [Instalando um sistema](jsc:installing).

O sistema que você obtém é construído na sua máquina a partir do código-fonte. O kernel e todo programa que você adicionar são compilados primeiro a partir do seu código-fonte, então a instalação leva bastante tempo. Você decide quantas compilações rodam ao mesmo tempo.

> [!DEFINE] compilação
> Transformar o código-fonte, o texto escrito por um programador, em um programa que o computador pode executar.

> [!DEFINE] kernel
> O núcleo do sistema operacional, o programa que controla o hardware e executa os outros programas.

O Gentoo é um sistema Linux da era Legacy, e ocupa 4.096 MB de disco depois de instalado. Seu gerenciador de pacotes é o Portage, que instala programas compilando-os.

> [!DEFINE] Portage
> O gerenciador de pacotes do Gentoo. Ele lê as receitas de compilação e compila cada programa a partir do seu código-fonte.

O passo a passo é um arquivo na mídia, /root/install.txt. Leia-o a qualquer momento: digite less /root/install.txt e pressione [[Enter]]. O arquivo está sempre em inglês.

Passos longos mantêm o terminal ocupado até terminarem. Pressione [[Ctrl]]+[[C]] para interromper um passo que demora demais ([](jsc:medium_shell)).

O passo a passo marca com um asterisco os passos sem os quais um sistema não pode inicializar. A configuração install_by_hand.gentoo_every_step decide quais passos são pedidos a você, e vem desligada por padrão.

Table: Quais passos são pedidos a você

| Configuração | O que é pedido a você |
| --- | --- |
| `false` | Apenas os passos marcados com um asterisco (o padrão). |
| `true` | Todos os passos do manual, inclusive os opcionais. |

Todo download e todo pacote vem do [Mirror](jsc:mirror). Um [Mainframe](jsc:mainframes) na mesma rede precisa rodar o Mirror, ou os passos de busca falham.

O novo sistema é construído em /mnt/gentoo, no seu disco. A mídia ao vivo roda na memória, e esquece todo arquivo escrito fora de /mnt/gentoo quando reinicia.

## Se algo der errado

> [!PROBLEM] Um arquivo que você escreveu some depois de um reinício.
> Só /mnt/gentoo fica no seu disco. Escreva o novo sistema ali, e não em /root.

> [!PROBLEM] Os passos param, e você não consegue saber o que fazer em seguida.
> Cada recusa indica o passo que está faltando. Faça esse passo e tente de novo.

> [!SEE] jsc:installing, jsc:the_systems
