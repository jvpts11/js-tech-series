---
section: by_hand_basics
order: 299
icon: jsc:cd_rom
---
# Instalando manualmente

A maioria dos sistemas se instala sozinha. Você coloca a mídia deles em uma unidade, inicializa a partir dela, e o instalador faz o trabalho enquanto faz algumas perguntas. Arch Linux e Gentoo não funcionam assim. Você digita cada passo da instalação deles por conta própria, em ordem, e o sistema responde do jeito que a ferramenta real responderia.

A mídia de instalação é o disquete, o disco compacto (CD), o disco digital versátil (DVD) ou o pen drive USB a partir do qual um sistema é instalado. Para esses dois sistemas, é um CD. Quando você o inicializa, o CD roda o próprio shell, uma linha em que você digita comandos, e esse CD em execução é a mídia ao vivo.

> [!DEFINE] Mídia de instalação
> O disquete, o disco compacto (CD), o disco digital versátil (DVD) ou o pen drive USB a partir do qual um sistema é instalado.

> [!DEFINE] Mídia ao vivo
> Uma mídia de instalação que roda com um shell próprio. O shell vem da mídia, não do seu disco, e nada é instalado até que você digite os passos.

O shell responde com as palavras das ferramentas reais, e recusa o que uma máquina real recusaria naquele passo. Um erro parece um erro em um computador real, então você aprende os mesmos motivos que os sistemas reais dão.

Table: Os dois sistemas instalados manualmente

| Sistema | Shell na mídia | Como é construído |
| --- | --- | --- |
| Arch Linux | `zsh` | O sistema base é instalado a partir do repositório de pacotes da rede. |
| Gentoo | `bash` | O sistema base e o kernel são compilados a partir do código-fonte primeiro, o que leva tempo. |

> [!DEFINE] Arch Linux
> Um sistema Linux que você instala manualmente a partir da sua mídia ao vivo, pegando o sistema base do repositório de pacotes da rede.

> [!DEFINE] Gentoo
> Um sistema Linux que você instala manualmente a partir da sua mídia ao vivo, compilando o sistema base e o kernel a partir do código-fonte.

A instalação termina quando você digita reboot. O shell recusa o reinício até que o novo sistema tenha tudo o que precisa para iniciar, e lista o que falta. Quanto tempo os passos levam depende da sua máquina: uma máquina lenta leva mais tempo para compilar (Gentoo) e para descompactar (Arch Linux).

Antes de inicializar a mídia, confira o que você precisa ([](jsc:what_you_need)).

## Se algo der errado

> [!PROBLEM] Você espera que um instalador inicie sozinho, e um shell abre no lugar.
> Esse shell é a mídia ao vivo. Nada foi instalado ainda: leia o passo a passo e digite os passos dele, começando com help ([](jsc:the_walkthrough)).

> [!SEE] jsc:installing, jsc:the_systems
