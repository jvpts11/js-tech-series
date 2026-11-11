---
section: choosing_a_distribution
order: 285
---
# O que é uma distribuição e qual escolher

Um sistema Linux existe em muitas versões, e cada versão é chamada de distribuição. Uma distribuição reúne o kernel Linux, os programas que executam nele, seu próprio instalador e seu próprio gerenciador de pacotes. Aqui você pode instalar três delas: Ubuntu, Debian e Fedora.

Cada um dos três é um sistema Legacy: ele exige hardware Legacy ou mais recente. Os três inicializam o mesmo kernel, chamado 6.8-jsc, e diferem no disco que ocupam, na memória de acesso aleatório (RAM) que mantêm enquanto executam e no gerenciador de pacotes que usam. Os números abaixo são do próprio jogo, não os requisitos exatos de uma máquina real.

```table
{"caption": "As três distribuições", "heads": ["Distribuição", "Disco que ocupa", "RAM durante a execução", "Gerenciador de pacotes"], "rows": [[{"words": "Ubuntu"}, {"amount": 8192, "unit": "MB"}, {"amount": 48, "unit": "MB"}, {"literal": "apt"}], [{"words": "Debian"}, {"amount": 4096, "unit": "MB"}, {"amount": 24, "unit": "MB"}, {"literal": "apt"}], [{"words": "Fedora"}, {"amount": 8192, "unit": "MB"}, {"amount": 48, "unit": "MB"}, {"literal": "dnf"}]]}
```

O número do disco é o espaço que o sistema ocupa para si, antes de qualquer programa ou área de trabalho. Um disco com menos espaço livre do que isso não consegue receber o sistema: veja [](jsc:installing_linux_keys).

Uma distribuição inicia no seu console de texto, a tela com um prompt. Uma área de trabalho, a tela gráfica com janelas e menus, é um pacote que você adiciona depois. As três áreas de trabalho estão na tabela abaixo.

```table
{"caption": "Áreas de trabalho que você pode adicionar", "heads": ["Área de trabalho", "Comando", "Disco para instalar", "RAM durante a execução"], "rows": [[{"words": "KDE Plasma"}, {"literal": "kde-plasma"}, {"amount": 256, "unit": "MB"}, {"amount": 224, "unit": "MB"}], [{"words": "GNOME"}, {"literal": "gnome"}, {"amount": 192, "unit": "MB"}, {"amount": 256, "unit": "MB"}], [{"words": "Cinnamon"}, {"literal": "cinnamon"}, {"amount": 160, "unit": "MB"}, {"amount": 160, "unit": "MB"}]]}
```

KDE Plasma e GNOME exigem hardware Legacy ou mais recente. O Cinnamon exige hardware Standard ou mais recente, então um computador Legacy não pode recebê-lo. Os instaladores oferecem uma área de trabalho só quando um Mirror responde, como explica [](jsc:mirror_and_packages).

A tabela traz os números para comparar. O Debian ocupa o menor disco e mantém a menor memória, enquanto Ubuntu e Fedora ocupam mais. O gerenciador de pacotes é a outra diferença: apt no Ubuntu e no Debian, dnf no Fedora.

> [!DEFINE] Distribuição
> Um sistema Linux, empacotado com seu próprio instalador, seu próprio gerenciador de pacotes e seus próprios tamanhos.

> [!DEFINE] Gerenciador de pacotes
> O comando que busca e instala programas de uma distribuição, a partir do Mirror.

> [!DEFINE] Mirror
> O servidor de pacotes no Mainframe da rede. Os instaladores e os gerenciadores de pacotes buscam nele.

> [!DEFINE] Kernel
> O núcleo do sistema, a parte que executa os programas e se comunica com a máquina. Aqui ele se chama 6.8-jsc.

## Se algo der errado

> [!PROBLEM] O instalador não mostra nenhuma página de área de trabalho.
> Nenhum Mirror responde, pois o computador não está em uma rede cujo Mainframe executa o Mirror. Conecte-o e depois instale ([](jsc:mirror_and_packages)).

> [!PROBLEM] O Cinnamon não aparece na lista de áreas de trabalho.
> O computador é uma máquina Legacy. Escolha KDE Plasma ou GNOME, ou use um computador Standard.

> [!SEE] jsc:installing_linux_keys, jsc:install_ubuntu, jsc:apt, jsc:dnf
