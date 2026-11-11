---
section: linux_packages
order: 293
---
# O Mirror e os pacotes nele

O Mirror é o servidor de pacotes da rede. Ele guarda os programas e as áreas de trabalho que apt e dnf buscam, e roda como serviço no Mainframe da rede ([](jsc:mainframes)). Um computador Linux alcança o Mirror por um cabo de dados até esse Mainframe. O Guia de Operações tem uma página própria sobre o Mirror ([](jsc:mirror)).

## Colocando o Mirror no Mainframe

1. Fique no console do Mainframe.
2. Digite mirror install e depois pressione ENTER.
3. Leia a resposta: "Mirror instalado no Mainframe e servindo pacotes".

Digite mirror status a qualquer momento para ver se o Mirror está rodando. Se aparecer "não foi possível resolver mirror://", verifique isso primeiro.

Table: A linha de status do Mirror

| Linha de status | O que significa |
| --- | --- |
| `Mirror: not installed` | O Mirror ainda não está neste Mainframe. |
| `Mirror: serving` | O Mirror roda e responde aos computadores da rede. |
| `Mirror: installed (Mainframe off)` | O Mirror está instalado, mas o Mainframe está desligado. |

Quando o Mainframe está servindo, a inicialização imprime "Pacote mirror encontrado em" e o nome do Mainframe.

## O que há para instalar

A tabela lista os programas que apt e dnf oferecem no Linux. Para cada um, ela dá o disco que ocupa, a memória que mantém enquanto roda e a era mais antiga que consegue rodá-lo. O nome da primeira coluna é o que você digita depois de install. Programas que vêm com todo sistema, como o programa Arquivos e as Configurações, não estão na tabela.

Table: Programas que o Mirror oferece ao Linux

| Comando | Programa | Disco e RAM | Era mínima |
| --- | --- | --- | --- |
| `kde-plasma` | Área de trabalho KDE Plasma | 256 MB de disco, 224 MB de RAM | Legacy |
| `gnome` | Área de trabalho GNOME | 192 MB de disco, 256 MB de RAM | Legacy |
| `cinnamon` | Área de trabalho Cinnamon | 160 MB de disco, 160 MB de RAM | Standard |
| `vim` | Editor Vim | 8 MB de disco, 4 MB de RAM | Vintage |
| `emacs` | Editor Emacs | 24 MB de disco, 12 MB de RAM | Vintage |
| `screenfetch` | screenfetch, a identidade do sistema com o seu logo | 4 MB de disco, 1 MB de RAM | Vintage |
| `sgsc` | Compilador Sigma# | 8 MB de disco, 16 MB de RAM | Legacy |
| `scc` | Compilador Sigma | 4 MB de disco, 2 MB de RAM | Vintage |
| `sigma` | Runtime Sigma, um serviço em segundo plano | 12 MB de disco, 24 MB de RAM | Legacy |
| `mirror` | Serviço Mirror, apenas no Mainframe | 64 MB de disco, 32 MB de RAM | Vintage |
| `mines` | Campo Minado | 16 MB de disco, 1 MB de RAM | Vintage |
| `solitaire` | Paciência | 16 MB de disco, 1 MB de RAM | Vintage |
| `snake` | Snake | 8 MB de disco, 1 MB de RAM | Vintage |
| `ark` | 67ark, o arquivador | 24 MB de disco, 16 MB de RAM | Vintage |
| `paint` | Paint | 48 MB de disco, 32 MB de RAM | Vintage |
| `soundfoundry` | Soundfoundry, para música | 4 MB de disco, 16 MB de RAM | Legacy |
| `exceed` | Exceed, a planilha de células | 64 MB de disco, 64 MB de RAM | Vintage |
| `messenger` | Midsoft Messenger | 48 MB de disco, 32 MB de RAM | Vintage |
| `knot` | Knot, a ferramenta de repositório | 48 MB de disco, 32 MB de RAM | Vintage |
| `insights` | Análise de Armazenamento | 64 MB de disco, 64 MB de RAM | Vintage |
| `planner` | Planejador de Fabricação | 64 MB de disco, 64 MB de RAM | Vintage |
| `autoeng` | Motor de Automação, um serviço | 32 MB de disco, 64 MB de RAM | Vintage |
| `msgsvc` | Serviço de Mensagens, um serviço | 48 MB de disco, 8 MB de RAM | Vintage |
| `iqlengine` | Motor IQL, apenas no Mainframe | 32 MB de disco, 24 MB de RAM | Vintage |
| `nextgreiql` | Motor NextgreIQL, apenas no Mainframe | 32 MB de disco, 48 MB de RAM | Legacy |
| `youriql` | Motor Prophet YourIQL, apenas no Mainframe | 32 MB de disco, 32 MB de RAM | Legacy |

Os números são os do próprio jogo, não requisitos exatos. Um programa só é instalado em um computador que tenha o disco e o hardware para ele. O tempo de instalação é o tamanho do programa dividido por 8 MB por segundo, multiplicado pelo fator da era: 1 na Vintage, 2 na Legacy, 3 na Transition, 4 na Standard, 8 na Advanced, 16 na Exa e 32 na Singularity. O tempo fica entre 3 e 90 segundos.

Remover um programa leva um quinto do tempo que a instalação leva. Programas que os jogadores publicam no Mirror são instalados da mesma forma. Um programa do jogador escrito em Sigma# precisa primeiro do Sigma Runtime: apt install sigma. A publicação é descrita em [](jsc:sigma_publish_mirror).

## Se algo der errado

> [!PROBLEM] E: não foi possível resolver mirror://: conecte este computador a uma rede cujo Mainframe roda o serviço Mirror
> Conecte o cabo de dados do computador ao Mainframe da rede. Se o Mirror ainda não estiver instalado, digite mirror install no console do Mainframe.

> [!PROBLEM] o Mirror está cheio (64 pacotes)
> O Mirror guarda no máximo 64 pacotes que os jogadores publicam, então um precisa sair antes que outro possa entrar ([](jsc:sigma_unpublish)).

> [!PROBLEM] este computador não pode armazenar programas instalados
> Este computador não tem onde guardar programas instalados. Instale o programa em um computador com disco.

> [!SEE] jsc:apt, jsc:dnf, jsc:mirror, jsc:mainframes
