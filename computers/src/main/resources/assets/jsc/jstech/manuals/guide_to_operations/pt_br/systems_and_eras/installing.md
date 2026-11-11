---
section: systems_and_eras
order: 12
icon: jsc:usb_flash_drive
covers_family: jsc:installing
---
# Instalando um sistema

Um computador sem sistema é uma caixa que mostra o seu firmware. O sistema é o que você realmente usa: seus arquivos e seus programas. Para instalar um, você inicia o computador a partir da mídia de instalação do sistema e segue o instalador dele.

Cada era tem a sua própria mídia de instalação. Ela vai em uma unidade que a lê. A unidade precisa estar conectada ao computador, como qualquer outro dispositivo ([](jsc:drives)).

```table
{"caption": "As mídias de instalação de cada era", "rows": [[{"words": "Vintage"}, {"words": "Disquete, em uma Unidade de Disquete"}], [{"words": "Legacy"}, {"words": "CD-ROM, em uma Unidade de CD"}], [{"words": "Transition"}, {"words": "DVD-ROM, em uma Unidade de DVD"}], [{"words": "Standard e Advanced"}, {"words": "Pen Drive, em uma Estação de Dock ou na porta USB do gabinete"}]]}
```

Os programas vêm em mídias próprias. O Standard usa DVD-ROM para programas, o Advanced usa Blu-ray para eles, e os dois usam o Pen Drive para serviços em segundo plano.

1. Coloque a mídia de instalação em uma unidade conectada ao computador.
2. Ligue o computador e observe o monitor ([](jsc:turning_on)).
3. Escolha a mídia na página Boot do Setup ([](jsc:setup_utility)), ou no Menu de Inicialização ([](jsc:boot_menu)), e inicie-a.
4. Siga as páginas do instalador até o fim.

Nem todo sistema segura a sua mão. Arch Linux e Gentoo inicializam como uma mídia ao vivo, e você coloca o sistema deles no disco manualmente. O Gentoo também compila os seus programas a partir do código-fonte.

Uma configuração do servidor pode fazer os dois pedirem cada passo. No arquivo de configuração jscomputers-server, install_by_hand.arch_every_step e install_by_hand.gentoo_every_step estão desativadas por padrão. Com a configuração do Arch ativada, o Arch pede o guia inteiro, além do relógio de hardware e de um nome para a máquina. Com a configuração do Gentoo ativada, o Gentoo pede o manual inteiro.

Um sistema precisa de hardware da sua era ou de uma mais nova, então o Frames 10 não vai para um computador Legacy, e o instalador avisa isso ([](jsc:the_systems)).

## Se algo der errado

> [!PROBLEM] O disco não aceitaria o sistema.
> O disco não tem espaço para o sistema. Use um disco maior, ou um disco de uma era mais nova, que comporta mais ([](jsc:items_as_data)).

> [!PROBLEM] A instalação não pôde ser gravada no disco (sem espaço ou sem disco).
> O disco está cheio, ou não está conectado ao computador. Confira o disco e o espaço dele.

> [!PROBLEM] A mídia foi retirada antes que o sistema fosse gravado.
> Mantenha a mídia na unidade até que o instalador termine.

> [!PROBLEM] Não há instalador nessa mídia.
> A mídia não é uma mídia de instalação para um sistema. Use a mídia de instalação do sistema que você quer.

> [!PROBLEM] Nenhum sistema instalável em uma unidade conectada.
> Coloque a mídia de instalação em uma unidade conectada ao computador ([](jsc:drives)).

> [!PROBLEM] O instalador pede o disco de instalação dele em uma unidade vinculada.
> Coloque a mídia de instalação desse sistema em uma unidade conectada ao computador ([](jsc:drives)).

> [!PROBLEM] O instalador diz que o sistema precisa de outro hardware.
> Instale-o em um computador que tenha o hardware que o instalador indica ([](jsc:eras)).

> [!SEE] jsc:the_systems, jsc:drives, jsc:firmware
