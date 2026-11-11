---
section: get_programs
order: 79
icon: jsc:cd_rom
---
# Programas

Um programa é um software que faz um trabalho para você, como desenhar uma imagem, rodar um jogo ou manter uma lista de arquivos. Todo sistema vem com alguns programas, e você pode instalar outros. Cada programa tem um nome, roda em alguns tipos de sistema e precisa de algum espaço em disco e de memória.

> [!DEFINE] Programa
> Software que um sistema executa para você, em uma janela ou em um prompt.

Há duas formas de instalar um programa. Você pode instalar a partir de uma mídia de instalação: o disco ou o pen drive em que o programa vem, como um CD (compact disc), um DVD (digital versatile disc) ou um pen drive USB (universal serial bus). Ou pode instalar [o Mirror](jsc:mirror), pelo nome, sem nenhuma mídia.

A partir da mídia, você coloca a mídia de instalação do programa em uma unidade vinculada do computador e depois inicia a configuração dele.

1. Coloque a mídia de instalação do programa em uma unidade vinculada do computador.
2. Em um prompt, digite install e o nome do programa, depois pressione [[Enter]].

Em uma área de trabalho Frames, você também pode abrir Este Computador, onde cada unidade vinculada mostra o programa com um botão Instalar ao lado. O computador responde com uma linha que começa com Configurando, seguida do nome do programa, depois da palavra from e da mídia, como o disquete ou o CD.

> [!DEFINE] Área de trabalho
> A face gráfica de um sistema: suas janelas, ícones, menus e os programas que você inicia por ele.

Table: Mídia de instalação por era

| Era do programa | Mídia | Unidade que você usa |
| --- | --- | --- |
| Vintage | Disquete | Unidade de Disquete |
| Legacy | CD | Unidade de CD |
| Transition | DVD | Unidade de DVD |
| Standard | Pen drive USB para um serviço que roda em segundo plano, DVD para o resto | Estação de Dock para o pen drive, Unidade de DVD para o disco |
| Advanced | Pen drive USB para um serviço que roda em segundo plano, Blu-ray para o resto | Estação de Dock para o pen drive, Unidade de Blu-ray para o disco |

Os sistemas vêm nas mesmas mídias, de acordo com a era: um sistema Vintage em disquete, um sistema Legacy em CD, um sistema Transition em DVD, e um sistema Standard ou Advanced em pen drive USB. Leia mais sobre as unidades na página própria delas ([](jsc:drives)).

Pelo Mirror você instala pelo nome, e o computador precisa de uma rede cujo Mainframe rode o Mirror. O Mirror é o serviço de pacotes desse Mainframe: ele guarda os pacotes da rede, e cada computador instala a partir dele com o seu gerenciador de pacotes.

> [!DEFINE] Pacote
> Um programa como o repositório da rede o guarda, pronto para instalar pelo nome.

> [!DEFINE] Gerenciador de pacotes
> O programa que busca um pacote e registra o que está instalado no computador, como apt, dnf, pacman, emerge, pkg ou pckmgr.

Table: Gerenciadores de pacotes por sistema

| Sistema | Gerenciador de pacotes |
| --- | --- |
| Ubuntu e Debian | `apt` |
| Fedora | `dnf` |
| Arch Linux | `pacman` |
| Gentoo | `emerge` |
| FreeBSD | `pkg` |
| Frames 95 a 11 | `pckmgr` |
| MC-DOS, MC-NET e UNIX System V | Nenhum: estes sistemas instalam programas somente a partir de mídia. |

O Gentoo compila cada programa a partir do código-fonte. Quando o computador tem espaço para a compilação, a instalação roda como um script na frente do terminal, e a resposta vem depois.

O comando store lista todos os programas que não vêm pré-instalados, uma linha cada, com o comando de instalação ao lado. Você também pode digitar available. A lista não depende do seu computador: um programa da lista ainda pode recusar a instalação aqui, e a instalação diz o motivo. Quando não sobra nada, store responde "nada mais para instalar".

O comando update atualiza os programas instalados para a versão mais recente. Ele nunca instala um programa novo, e responde "Todos os pacotes estão atualizados." quando não há nada a fazer.

No Frames, o comando pckmgr também tem os verbos remove, search e list. O verbo list com --available mostra todos os programas que o Mirror oferece.

Alguns programas são serviços que rodam em um Mainframe. Quando você instala um deles ali, o serviço é ligado, e o Mainframe guarda um registro da versão dele. Alguns programas também precisam de um motor IQL na rede ([](jsc:engines)).

Você pode publicar seus próprios pacotes no Mirror, para outros jogadores instalarem, com a ferramenta de pacotes do Sigma ([](jsc:sigma_publish_mirror)).

Os programas dos outros capítulos têm páginas próprias: o Gerenciador de Fabricação ([](jsc:crafting_manager)), o Pattern Studio ([](jsc:pattern_studio)) e a Workshop ([](jsc:workshop)).

Os programas de área de trabalho vêm nas próximas páginas: a janela Rede, Este Computador, Arquivos, o Editor, Configurações, o Monitor do Sistema, o Gerenciador de Tarefas, uma Calculadora, Paint, Exceed e os jogos. Os livros da série, um para cada família de sistemas, aprofundam a instalação e o uso ([](jsc:library)).

## Atualizações

Quando o Mirror tem uma versão mais nova de um programa que você instalou, as Configurações o marcam na página Programas, em todo desktop. Embaixo do nome do programa você lê atualização disponível, depois a versão nova e a que você tem, e o botão Atualizar instala a versão nova. No alto, a página diz quantas atualizações o Mirror tem. Um pacote que outro jogador publicou de novo com o mesmo nome também é uma atualização.

Cada gerenciador de pacotes abaixo também lista as atualizações, com o seu próprio comando, e não instala nada enquanto as lista. No Frames e no UNIX System V, você vê as atualizações só nas Configurações. No OpenBSD, o syspatch -c lista os patches do próprio sistema ([](jsc:openbsd_patches)).

Table: O comando que lista as atualizações

| Gerenciador | Comando |
| --- | --- |
| `apt` | `apt list --upgradable` |
| `dnf` | `dnf check-update` |
| `pacman` | `pacman -Qu` |
| `emerge` | `emerge -pvuD @world` |
| `pkg` | `pkg version -vRL=` |
| `pkg_add` | `pkg_add -un` |
| `keg` | `keg outdated` |

## Se algo der errado

> [!PROBLEM] A instalação diz que o programa precisa do disco de instalação dele em uma unidade vinculada.
> Coloque o disco desse programa em uma unidade vinculada do mesmo computador e instale de novo.

> [!PROBLEM] A instalação diz que o programa precisa de uma era mais recente de hardware.
> O hardware do computador é mais antigo do que o programa permite. Instale-o em um computador mais novo.

> [!PROBLEM] A instalação diz que o programa só instala em um tipo de computador, como o Mainframe.
> Instale-o no tipo de computador que a mensagem indica.

> [!PROBLEM] A instalação diz que os requisitos não são atendidos (hardware ou espaço livre no disco).
> O computador não tem um processador ou uma placa de vídeo que o programa precisa, ou tem pouco espaço livre no disco. Dê a ele o que falta e instale de novo.

> [!PROBLEM] A instalação diz que não foi possível resolver mirror://.
> Conecte o computador a uma rede cujo Mainframe rode o Mirror ([](jsc:mirror)).

> [!PROBLEM] A instalação diz que este sistema instala programas a partir de mídia de instalação, não de um gerenciador de pacotes.
> Coloque a mídia de instalação do programa em uma unidade vinculada e instale a partir dela.

> [!PROBLEM] A instalação diz que não foi possível localizar o pacote, seguido de um nome.
> O Mirror não oferece esse nome para o seu sistema. Confira o nome na lista da loja e instale de novo.

> [!PROBLEM] A instalação diz que este computador ainda está configurando um programa.
> Espere essa configuração terminar antes de iniciar outra instalação.

> [!PROBLEM] A instalação diz que não foi possível gravar a instalação no disco.
> Não há espaço no disco, ou não há disco. Libere espaço, ou coloque um disco no computador, e instale de novo.

> [!PROBLEM] A instalação diz que um programa já está na versão mais recente.
> Não é preciso fazer nada: o programa está instalado e atualizado.

> [!SEE] jsc:installing, jsc:the_systems, jsc:drives, jsc:mirror
