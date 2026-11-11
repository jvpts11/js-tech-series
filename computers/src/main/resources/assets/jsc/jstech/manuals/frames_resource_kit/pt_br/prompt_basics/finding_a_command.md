---
section: prompt_basics
order: 235
---
# Encontrando um comando

Cada comando tem uma página própria, e a página é o mesmo texto que help e a opção /? imprimem. Você pode listar os comandos que a máquina tem, ou pedir a página de um comando que já conhece. Digite as primeiras letras de um nome e pressione [[Tab]] para completá-lo.

Cada comando tem um escopo. Um comando funciona apenas nos sistemas que ele nomeia, e só em uma máquina com as partes de que precisa, como um disco para os comandos de arquivo ou uma rede para o iql. Quando uma parte falta, o comando não aparece em help, e digitá-lo dá uma mensagem.

> [!DEFINE] Escopo
> Os sistemas e as partes de uma máquina de que um comando precisa antes que a máquina o ofereça.

## Listando os comandos

Table: Formas de ver os comandos

| Forma | O que mostra |
| --- | --- |
| `help` | Os comandos que esta máquina tem, uma linha cada. MC-DOS e MC-NET os mostram em tela cheia; o Frames os imprime. |
| `help NAME` | A página de um comando. |
| `listcmd` | Todos os comandos e todos os programas desta máquina, em uma só lista. |

listcmd lista tudo o que esta máquina pode executar: os comandos que ela oferece e todos os programas instalados nela, em uma só lista. A lista tem um cabeçalho COMMANDS, com uma linha por comando, e depois um cabeçalho PROGRAMS, com o nome e o id de cada programa. Ele fica desligado, a menos que as configurações do servidor o liguem.

```command
id: jsc:command/mc_dos/listcmd
```

No Linux e nos outros sistemas Unix, man abre a página de um comando Unix. Esses comandos são explicados nos livros Unix, em [encontrando ajuda](jsc:man_pages).

## Se algo der errado

> [!PROBLEM] listcmd exibe: comando não encontrado.
> O servidor está com listcmd desligado. Digite help para listar os comandos.

> [!PROBLEM] Um comando está faltando em help.
> Esta máquina não tem algo de que o comando precisa, como um disco, uma rede ou uma porta para periféricos. Ou o comando pertence a um pacote que você ainda não instalou.

> [!PROBLEM] O shell exibe: comando não encontrado: NAME
> A palavra não é um comando nesta máquina, e nenhum programa com esse nome está instalado. Confira a grafia, ou digite help para listar os comandos.

> [!PROBLEM] A página diz que funciona depois que um pacote é instalado.
> Instale esse pacote primeiro ([](jsc:software_verbs)). Depois disso, o comando estará na máquina.

> [!SEE] jsc:help_on_a_computer, jsc:the_systems
