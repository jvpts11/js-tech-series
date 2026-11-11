---
section: the_editors
order: 277
---
# vi

vi é o editor que vem com o FreeBSD e o UNIX System V. É o mesmo motor do [Vim](jsc:vim): as mesmas teclas, os mesmos modos e os mesmos comandos de dois-pontos.

A máquina escolhe qual vi você recebe, e você não pode mudar isso. O FreeBSD recebe o nvi, a sua própria versão do vi. O UNIX System V recebe o original, com as suas próprias palavras.

> [!DEFINE] nvi
> A versão do vi própria do FreeBSD.

As palavras diferem das do Vim. O vi não tem linha de nome própria: a linha de baixo mostra só as mensagens abaixo, então não há nome de arquivo nem texto -- INSERT --. Ele não mostra cor.

Table: As palavras de cada voz

| Quando | FreeBSD (nvi) | UNIX System V |
| --- | --- | --- |
| Você abre um arquivo que existe | `hello.txt: unmodified: line 1` | `"hello.txt" 3 lines, 42 characters` |
| Você abre um arquivo que ainda não existe | `hello.txt: new file: line 1` | `"hello.txt" [New file]` |
| Você grava o arquivo com :w | `hello.txt: 3 lines, 42 characters` | `"hello.txt" 3 lines, 42 characters` |

Os números contam as linhas e os caracteres do texto. Neste exemplo, o arquivo tem 3 linhas e 42 caracteres.

As teclas são as mesmas do Vim, então a tabela de teclas do modo normal na entrada do Vim funciona aqui também. Desfazer e a colagem de uma linha dizem as mesmas palavras do Vim, nas duas vozes.

```command
id: jsc:command/freebsd/vi
```

## Se algo der errado

> [!PROBLEM] Você digita vi e o prompt diz "comando não encontrado: vi".
> O vi vem só com o FreeBSD e o UNIX System V. Em outros sistemas, use o [vim](jsc:vim).

> [!PROBLEM] A linha de baixo diz E37: sem escrita desde a última alteração (adicione ! para ignorar).
> Digite :w para salvar, e depois :q. Ou digite :q! para sair sem salvar.

> [!SEE] jsc:vim, jsc:the_systems
