---
section: linux_packages
order: 291
---
# apt, verbo por verbo

O apt é o gerenciador de pacotes do Ubuntu e do Debian. Você digita apt, um verbo como install, depois o nome daquilo com que ele trabalha, e ele busca no Mirror. No Fedora essa palavra não é um comando: use dnf ([](jsc:dnf)).

Palavras que começam com hífen são ignoradas, então apt install -y vim instala o vim.

Table: Os verbos do apt

| Comando | O que faz |
| --- | --- |
| `apt install <package>` | Procura o pacote no Mirror e o instala. |
| `apt remove <package>` | Remove um pacote instalado. purge e erase fazem o mesmo. |
| `apt search [term]` | Lista os pacotes do Mirror cujo nome ou texto corresponde ao termo. Os instalados são marcados. |
| `apt list` | Lista os pacotes instalados, com seus nomes e descrições. |
| `apt update` | Lê as listas de pacotes do Mirror. Não instala nada. |
| `apt upgrade` | Atualiza os pacotes instalados para as versões do Mirror; full-upgrade e dist-upgrade fazem o mesmo. |
| `apt list --upgradable` | Lista os pacotes instalados de que o Mirror tem uma versão mais nova, cada um com a versão que você tem. Não instala nada. |

```command
id: jsc:command/linux/apt
```

## Instalando um programa

1. Digite apt search e uma palavra do nome do programa, depois pressione ENTER.
2. Encontre o nome do comando na primeira coluna da lista.
3. Digite apt install e o nome do comando, depois pressione ENTER.
4. Aguarde até a barra de progresso da instalação encher.

Uma instalação imprime estas linhas, nesta ordem, antes da sua barra de progresso.

Table: O que o apt imprime em uma instalação

| Linha | O que significa |
| --- | --- |
| `Reading package lists... Done` | As listas de pacotes são lidas. |
| `Building dependency tree... Done` | Os pacotes de que o programa precisa são calculados. |
| `The following NEW packages will be installed:` | O nome do programa aparece na linha seguinte. |
| `Need to get <MB> MB of archives.` | O tamanho do download. |
| `Get:1 <mirror address> stable/main <package> <version> [<MB> MB]` | O download do Mirror. |

Uma remoção imprime as mesmas duas primeiras linhas. Depois imprime "Os pacotes a seguir serão REMOVIDOS:" e "Depois desta operação, <MB> de espaço em disco será liberado."

## Se algo der errado

> [!PROBLEM] comando não encontrado: apt
> Este é o Fedora, que usa dnf. Digite dnf no lugar ([](jsc:dnf)).

> [!PROBLEM] E: não foi possível resolver mirror:// - conecte este computador a uma rede cujo Mainframe execute o serviço Mirror
> O computador não está em uma rede cujo Mainframe execute o Mirror. Conecte-o, como explica [](jsc:mirror_and_packages).

> [!PROBLEM] E: não foi possível localizar o pacote <name>
> O nome está errado, ou o programa não é oferecido para Linux. Encontre o nome certo com apt search.

> [!PROBLEM] E: <name>: requisitos não atendidos (hardware ou espaço livre em disco)
> A janela informa quanto de disco está faltando. Libere espaço em disco, ou use um disco maior.

> [!PROBLEM] E: cinnamon precisa de hardware Standard ou superior
> O computador é uma máquina Legacy. Use um computador Standard, ou instale KDE Plasma ou GNOME.

> [!PROBLEM] <name> já está na versão mais recente
> Nada a fazer. Para atualizar todos os pacotes instalados, use apt upgrade.

> [!PROBLEM] Este computador ainda está configurando <name>.
> Um computador executa uma instalação por vez. Aguarde a primeira terminar.

> [!PROBLEM] <name> não está instalado.
> Uma remoção não encontrou nada para remover. Confira a lista de instalados com apt list.

> [!SEE] jsc:dnf, jsc:mirror_and_packages, jsc:what_is_missing
