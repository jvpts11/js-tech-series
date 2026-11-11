---
section: shell_software
order: 259
---
# Instalando e removendo software

Há duas formas de adicionar software a um sistema Unix, e este verbete diz qual é qual. A loja instala um programa a partir de um disco de instalação em uma unidade ligada ao computador. Um gerenciador de pacotes instala e remove pacotes do Mirror.

Um gerenciador de pacotes funciona só no sistema que o usa. Remover software nesses sistemas é trabalho do gerenciador de pacotes: não há comando de desinstalação aqui, porque uninstall é um comando do DOS. Se você não tem certeza de como um comando chegou aqui, digite which, e ele diz se o sistema ou um pacote o colocou ali.

Table: Gerenciadores de pacotes

| Sistema | Gerenciador | Seus verbos |
| --- | --- | --- |
| Ubuntu e Debian | `apt` | install, remove, search, list, update e upgrade |
| Fedora | `dnf` | Os mesmos verbos do apt |
| Arch Linux | `pacman` | \-S para instalar, -R para remover, -Ss para pesquisar, -Q para listar, -Syu para atualizar |
| Gentoo | `emerge` | Um nome para instalar, --unmerge para remover, --search, --sync e --update @world |
| FreeBSD | `pkg` | install, delete, search, info, update e upgrade |
| UNIX System V | `none` | Nenhum gerenciador de pacotes neste sistema |

Para os verbos de apt e dnf, leia o Linux Desk Reference ([](jsc:apt)). Para pkg, leia o Daemon Handbook ([](jsc:pkg)). A instalação ou remoção de um pacote precisa do Mirror, o serviço de rede que mantém os pacotes ([](jsc:mirror)).

## Instalando pela loja

1. Digite store para listar os programas que você pode instalar neste computador.
2. Coloque o disco de instalação do programa em uma unidade ligada ao computador.
3. Digite install e o nome do programa, como screenfetch.
4. Digite programs para ver os programas instalados, e confira se o seu está lá.

```command
id: jsc:command/linux/install
```

```command
id: jsc:command/linux/store
```

```command
id: jsc:command/linux/programs
```

O programa screenfetch mostra um resumo curto do sistema, com rótulos como OS, Kernel, Uptime, Packages e Shell. Você o instala primeiro, como qualquer programa da loja.

> [!DEFINE] screenfetch
> Um programa que mostra o logo do seu sistema ao lado de um resumo da máquina: o sistema, o kernel, o tempo de atividade, os pacotes e o shell.

```command
id: jsc:command/linux/screenfetch
```

## Se algo der errado

> [!PROBLEM] O comando install diz que o programa não existe.
> O comando store lista os programas que você pode instalar.

> [!PROBLEM] O comando install diz que o programa precisa do disco de instalação em uma unidade vinculada.
> Coloque o disco em uma unidade ligada ao computador, e depois execute install de novo.

> [!PROBLEM] Um comando de pacote diz que não foi possível resolver mirror.
> Conecte o computador a uma rede cujo [Mainframe](jsc:mainframes) execute o Mirror.

> [!PROBLEM] O shell diz screenfetch: não encontrado.
> Execute install screenfetch primeiro, depois digite screenfetch de novo.

> [!SEE] jsc:apt, jsc:dnf, jsc:arch_pacman_in_system, jsc:pkg
