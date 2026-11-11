---
section: shell_software
order: 261
---
# Sigma no prompt

Sigma e Sigma# são as linguagens de programação destes computadores. Suas ferramentas são comandos no prompt, depois que você instala os pacotes delas. O Livro do Sigma ensina as próprias linguagens ([](jsc:sigma_two_languages)).

Table: Os comandos do Sigma

| Comando | Vem com o pacote | O que faz |
| --- | --- | --- |
| `scc` | `scc` | Compila um arquivo Sigma, que termina em .sg, em uma listagem que termina em .asm. |
| `sgsc` | `sgsc` | Compila um arquivo Sigma#, que termina em .sgs, em uma listagem. |
| `sigma` | `sigma` | Roda uma listagem com sigma run, compila uma para vários conjuntos de instruções com sigma build, para uma com sigma stop, e lista o que está rodando com sigma ps. |
| `sgpack` | `sigma` | Inicia um pacote, o compila, e o coloca no Mirror ou o tira de lá. |
| `sgdb` | `sgsc` | Depura uma listagem compilada com sgsc --debug, uma linha de cada vez. |

Você instala um pacote pelo nome dele, com o gerenciador de pacotes do seu sistema ([](jsc:software_commands)). Os pacotes vêm do Mirror, então seu computador precisa estar em uma rede cujo Mainframe rode o Mirror.

1. Instale os pacotes de que você precisa, como sgsc e sigma.
2. Escreva seu programa em um arquivo que termine em .sgs, como hello.sgs.
3. Digite sgsc hello.sgs para compilá-lo. O compilador escreve hello.asm ao lado dele.
4. Digite sigma run hello.asm para rodar a listagem.

```command
id: jsc:command/linux/scc
```

```command
id: jsc:command/linux/sgsc
```

```command
id: jsc:command/linux/sigma
```

```command
id: jsc:command/linux/sgpack
```

```command
id: jsc:command/linux/sgdb
```

## Se algo der errado

> [!PROBLEM] O prompt diz que não conhece sgsc, scc, sigma ou sgpack.
> Instale o pacote que traz o comando, como mostra a tabela, depois digite o comando de novo.

> [!PROBLEM] O gerenciador de pacotes diz que não conseguiu resolver mirror://.
> Coloque o computador em uma rede cujo Mainframe rode o Mirror ([](jsc:mirror)).

> [!SEE] jsc:sigma_installing, jsc:sigma_compile, jsc:sigma_run
