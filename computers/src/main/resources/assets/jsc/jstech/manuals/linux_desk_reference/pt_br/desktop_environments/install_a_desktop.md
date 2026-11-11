---
section: desktop_environments
order: 263
icon: jsc:monitor
---
# Instalando uma área de trabalho

No Linux e no FreeBSD você tem duas formas de obter uma área de trabalho. Você pode escolher uma durante a instalação do sistema, ou adicioná-la depois pelo terminal. As duas formas consultam o Mirror, a prateleira de pacotes da rede ([](jsc:mirror)).

Um Mirror responde quando o seu computador alcança um Mainframe que o executa, por um cabo de dados. O Mirror é um serviço, então ele deixa de responder quando o cabo é cortado.

## Na hora da instalação

O instalador do Ubuntu, Debian, Fedora e FreeBSD tem uma página Área de trabalho. Gentoo, Arch Linux, UNIX System V e MC-DOS não têm nenhuma. A página Área de trabalho só aparece quando um Mirror responde.

1. Conecte o computador a uma rede cujo Mainframe execute o Mirror.
2. Inicie o instalador do Ubuntu, Debian, Fedora ou FreeBSD.
3. Na página Área de trabalho, escolha uma área de trabalho ou apenas o terminal.
4. Deixe o instalador terminar. A área de trabalho escolhida é instalada como uma etapa própria.
5. Reinicie o computador, para que a área de trabalho inicie.

## Depois da instalação

Quando o sistema já estiver em execução, você adiciona uma área de trabalho pelo terminal dele. Use a forma de instalação do gerenciador de pacotes do sistema, e informe o nome da área de trabalho ([](jsc:software_commands)).

Table: As formas de instalação

| Gerenciador de pacotes | Sistemas | Forma |
| --- | --- | --- |
| apt | Debian, Ubuntu | `apt install <name>` |
| dnf | Fedora | `dnf install <name>` |
| pacman | Arch Linux | `pacman -S <name>` |
| emerge | Gentoo | `emerge <name>` |
| pkg | FreeBSD | `pkg install <name>` |

O nome é o nome do comando da área de trabalho: kde-plasma, gnome, cinnamon ou cde. O id também funciona, como kde_plasma. Maiúsculas e minúsculas não fazem diferença.

O Gentoo compila uma área de trabalho a partir do código-fonte no terminal, e o terminal espera até a compilação terminar.

Quais áreas de trabalho o instalador oferece depende do sistema e da era do seu computador.

Em um computador Legacy, ele oferece KDE Plasma, GNOME e CDE, onde o sistema as executa. Um computador Standard também recebe o Cinnamon como opção. Um computador Vintage não recebe nenhuma.

O UNIX System V instala seus programas a partir da mídia de instalação, e não tem gerenciador de pacotes. Uma área de trabalho não é adicionada pelo Mirror no UNIX.

## Se algo der errado

> [!PROBLEM] A página Área de trabalho não aparece.
> Nenhum Mirror responde, ou o instalador não oferece área de trabalho. Conecte-se a uma rede cujo Mainframe execute o Mirror, ou adicione uma área de trabalho depois da instalação.

> [!PROBLEM] não foi possível resolver mirror:// - conecte este computador a uma rede cujo Mainframe execute o serviço Mirror
> Conecte o computador a uma rede cujo Mainframe execute o Mirror, e tente de novo.

> [!PROBLEM] O gerenciador de pacotes diz "unable to locate package" e o nome.
> O nome não é um pacote oferecido a este sistema. Confira o nome: kde-plasma, gnome, cinnamon ou cde.

> [!PROBLEM] cinnamon precisa de hardware Standard ou superior
> O computador é mais antigo que a área de trabalho. Use um computador dessa era ou de uma mais recente.

> [!PROBLEM] requisitos não atendidos (hardware ou espaço livre em disco)
> O disco tem menos espaço livre do que a área de trabalho ocupa, ou o hardware é insuficiente. Libere espaço no disco e tente de novo.

> [!PROBLEM] Este computador não consegue guardar programas instalados.
> O computador não tem disco para guardar programas. Coloque um.

> [!PROBLEM] A mensagem "Este computador ainda está sendo configurado" e o nome de uma área de trabalho.
> Uma instalação anterior ainda está em andamento. Espere até que ela termine.

> [!PROBLEM] Instalação cancelada. Nada foi instalado.
> A configuração parou, então a área de trabalho não foi adicionada. Instale de novo, como mostram os passos acima.

> [!PROBLEM] O gerenciador de pacotes diz "<name> já está na versão mais recente".
> A área de trabalho já está instalada. Reinicie o computador, porque ela inicia na próxima inicialização.

> [!SEE] jsc:desktops, jsc:the_systems, jsc:installing, jsc:mirror
