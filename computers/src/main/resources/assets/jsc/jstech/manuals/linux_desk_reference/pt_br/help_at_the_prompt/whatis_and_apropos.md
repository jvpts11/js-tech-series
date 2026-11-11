---
section: help_at_the_prompt
order: 282
icon: jsc:guide_to_operations
---
# whatis e apropos

whatis dá a linha única que diz para que serve um comando. É a resposta mais rápida, e diz se a página completa vale a pena ser lida.

apropos serve para quando você conhece a tarefa, não o nome. Ele pesquisa o nome e a descrição de uma linha de cada comando que este computador tem, e imprime os comandos que combinam.

As descrições estão em inglês, qualquer que seja o idioma que o jogo mostra para você. Pesquise com palavras em inglês, como network ou disk.

## Como usá-los

1. Digite whatis e o nome de um comando, como whatis ls.
2. Digite apropos e uma palavra, como apropos network, para encontrar comandos pela tarefa que fazem.
3. Digite man e o nome de um comando que você quer ler por inteiro.

Table: O que whatis imprime

| Parte da linha | O que informa |
| --- | --- |
| O nome, como ls | O comando a que esta linha se refere. |
| `(1)` | A seção. A página de cada comando fica na seção 1. |
| Um hífen e a descrição | Para que serve o comando, em poucas palavras. |

Tudo o que você digita depois de apropos é a pesquisa. apropos network card procura a frase inteira, network card, e não duas palavras. Somente os comandos que este computador tem são pesquisados.

```command
id: jsc:command/linux/whatis
```

```command
id: jsc:command/linux/apropos
```

> [!DEFINE] descrição de uma linha
> A descrição curta que um comando dá de si mesmo em uma linha, e que whatis e apropos leem.

## Se algo der errado

> [!PROBLEM] A resposta termina com "nada apropriado." depois da palavra que você digitou.
> Nenhum nome ou descrição contém essa palavra. Tente uma palavra mais curta, ou uma palavra em inglês de uma descrição, como disk.

> [!PROBLEM] A resposta é "whatis o quê?".
> Você digitou whatis sem nome. Digite whatis e o nome de um comando, como whatis ls.

> [!PROBLEM] A resposta é "apropos o quê?".
> Você digitou apropos sem palavra. Digite apropos e a palavra a pesquisar, como apropos network.

> [!PROBLEM] apropos não encontra nada para uma palavra em outro idioma.
> A pesquisa lê somente as descrições em inglês. Digite a palavra em inglês.

> [!SEE] jsc:man_pages, jsc:help_command
