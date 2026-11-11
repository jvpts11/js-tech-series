---
section: prompt_programs_and_network
order: 247
---
# Editores e Sigma no prompt

Alguns programas funcionam dentro da janela do Prompt de Comando em vez de abrir a própria. Dois editores tomam a janela enquanto você altera um arquivo, e as ferramentas do Sigma compilam e executam seus programas ali. Instale cada um antes ([](jsc:software_verbs)).

## vim e emacs

Digite vim ou emacs, um espaço e o nome de um arquivo. O editor ocupa a janela inteira e a devolve quando você sai. O arquivo não precisa existir: ele é criado quando você salva pela primeira vez.

Table: Salvar e sair

| Editor | Para salvar | Para sair |
| --- | --- | --- |
| `vim` | Pressione Esc, depois digite :w e pressione Enter. | Pressione Esc, depois digite :q e pressione Enter. :wq salva e sai de uma vez. |
| `emacs` | Pressione Ctrl+X, depois Ctrl+S. | Pressione Ctrl+X, depois Ctrl+C. |

O vim começa no modo normal, em que uma letra é um comando. Pressione i para digitar no arquivo, e Esc para parar de digitar. A Referência de Mesa do Linux lista todas as teclas dos dois editores ([](jsc:vim)).

```command
id: jsc:command/mc_dos/vim
```

```command
id: jsc:command/mc_dos/emacs
```

## Sigma

Sigma e Sigma# são as linguagens de programação destes computadores. O Livro do Sigma as ensina ([](jsc:sigma_two_languages)). As ferramentas delas são verbos do prompt, depois que você instala os pacotes.

Table: Os verbos do Sigma

| Verbo | Vem com o pacote | O que faz |
| --- | --- | --- |
| `scc` | `scc` | Compila um arquivo do Sigma, que termina em .sg, em uma listagem que termina em .asm. |
| `sgsc` | `sgsc` | Compila um arquivo do Sigma#, que termina em .sgs, em uma listagem. |
| `sigma` | `sigma` | Executa uma listagem com sigma run, compila uma para vários conjuntos de instruções com sigma build, interrompe uma com sigma stop e lista o que está rodando com sigma ps. |
| `sgpack` | `sigma` | Inicia um pacote, o compila, e o coloca no Mirror ou o retira de lá. |
| `sgdb` | `sgsc` | Depura uma listagem compilada com sgsc --debug, uma linha de cada vez. |

Nas edições do Frames, pckmgr install busca esses pacotes no Mirror. O MC-DOS os instala somente a partir da mídia deles. O compilador e o runtime do Sigma# precisam de um computador Legacy ou posterior, e no Frames precisam de Frames XP ou posterior, então o Frames 95 comporta só o compilador do Sigma ([](jsc:sigma_installing)).

1. Instale os pacotes de que precisa, como sgsc e sigma.
2. Escreva seu programa em um arquivo que termina em .sgs, como HELLO.SGS.
3. Digite sgsc HELLO.SGS para compilá-lo. O compilador grava a listagem ao lado.
4. Digite sigma run e o nome da listagem para executá-la.

```command
id: jsc:command/mc_dos/scc
```

```command
id: jsc:command/mc_dos/sgsc
```

```command
id: jsc:command/mc_dos/sigma
```

```command
id: jsc:command/mc_dos/sgpack
```

```command
id: jsc:command/mc_dos/sgdb
```

## Se algo der errado

> [!PROBLEM] O prompt diz que não conhece vim, emacs ou um verbo do Sigma.
> Instale o programa ou o pacote que o traz, depois digite de novo.

> [!PROBLEM] pckmgr diz que não foi possível resolver mirror://.
> Coloque o computador em uma rede cujo Mainframe rode o Mirror ([](jsc:mirror)).

> [!SEE] jsc:software_verbs, jsc:sigma_installing, jsc:sigma_compile
