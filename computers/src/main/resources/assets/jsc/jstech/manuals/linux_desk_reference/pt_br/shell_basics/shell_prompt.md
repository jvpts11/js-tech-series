---
section: shell_basics
order: 248
---
# O que é um shell, e o seu prompt

Um shell é o programa que lê a linha que você digita, executa o comando nomeado nessa linha e mostra o que o comando responde. Você digita no terminal, a tela de texto de um computador, no prompt: a linha curta de texto que fica esperando por você.

Cada computador tem um prompt, e você digita nele pela própria tela do computador. Linux, FreeBSD e UNIX System V compartilham um conjunto de comandos e uma forma de ler uma linha. Um comando que um desses sistemas não tem não está na lista deles, então não pode ser executado ali.

Você entra como player nos três sistemas, e o prompt mostra esse nome primeiro. A parte depois do arroba é o nome do próprio computador, que você altera com o comando config ([](jsc:system_commands)).

## O prompt em cada sistema

Table: Prompt por sistema

| Sistema | Shell | Prompt |
| --- | --- | --- |
| Ubuntu, Debian e Fedora | bash | `player@<name>:<folder>$` |
| Gentoo | bash | `player@<name>:<folder>$` |
| Arch Linux | zsh | `player@<name> <folder> %` |
| FreeBSD | sh | `root@<name>:<folder> #` |
| UNIX System V | sh | `player@<name>:<folder> $` |

A parte da pasta mostra onde você está. O sinal ~ representa a sua pasta pessoal, a pasta onde seus arquivos ficam: /home/player no Linux e no FreeBSD, e /usr/player no UNIX System V.

No FreeBSD, o prompt mostra root e termina com #, porque uma máquina FreeBSD nova abre como root. Esses sistemas não têm sudo nem o comando su, então você não troca de conta com um comando.

> [!DEFINE] Shell
> O programa que lê as linhas que você digita e executa os comandos nelas.

> [!DEFINE] Prompt
> O texto antes do cursor, que mostra quem você é, em qual computador e em qual pasta.

> [!DEFINE] Root
> A conta todo-poderosa de um sistema. No FreeBSD, o prompt mostra o nome dessa conta e termina com o sinal #.

> [!DEFINE] Terminal
> A tela de texto de um computador, com teclado, onde você digita comandos e lê as respostas.

> [!DEFINE] Comando
> Um programa que você chama pelo nome, com as palavras depois do nome que dizem sobre o que ele trabalha.

## Se algo der errado

> [!PROBLEM] Não consigo encontrar o comando de que preciso.
> Digite help no Linux ou no UNIX System V para listar os comandos; o FreeBSD não tem comando help. O comando apropos com uma palavra encontra comandos pelo que fazem ([](jsc:whatis_and_apropos)).

> [!PROBLEM] O shell diz que o nome não foi encontrado e sugere apropos ou man intro.
> apropos com uma palavra encontra o comando que você quer. man intro lê uma entrada do manual somente no FreeBSD.

> [!PROBLEM] Um comando que o manual lista diz que não foi encontrado.
> O comando pertence a um pacote que você não instalou. O comando which diz se ele está aqui e o que o colocou aqui ([](jsc:software_commands)).

> [!SEE] jsc:file_tree, jsc:help_command
