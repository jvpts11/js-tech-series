---
section: sigma_start
order: 408
---
# Compilando com sgsc e scc

Compilar transforma o seu arquivo-fonte em uma listagem que um computador consegue executar. Você compila pela linha de comando com o compilador da sua linguagem: sgsc para o Sigma#, e scc para o Sigma.

Table: Os dois compiladores

| Comando | Linguagem | Conjunto de instruções padrão |
| --- | --- | --- |
| `sgsc` | Sigma# | `x86` |
| `scc` | Sigma | `IA-16` |

Os dois comandos aceitam as mesmas opções. Digite o comando sem arquivo, e ele mostra a sua linha de uso.

```session
{"lines": [{"prompt": "player@pc:~$ ", "text": "sgsc", "typed": true}, {"text": "usage: sgsc <file.sgs> [more.sgs ...] [-o <out.asm>] [--arch <isa>] [--lang <version>]"}], "look": "jsc:guide/terminal/bash", "title": "Ubuntu"}
```

Table: As opções de sgsc e scc

| Opção | O que faz |
| --- | --- |
| `-o <file>` | Nomeia a listagem que ele escreve; sem ele, a listagem usa o nome do primeiro arquivo-fonte com .asm no lugar da extensão. |
| `--arch <isa>` | Compila para um conjunto de instruções: IA-16, x86, x86-64, 68k, PowerPC ou ARM64, em qualquer combinação de maiúsculas e minúsculas; sem ele, vale o padrão da linguagem. |
| `--lang <version>` | Limita a compilação a uma versão da linguagem, 1 ou 2; sem ela, usa-se a versão mais nova que o compilador instalado conhece. |

A página completa de cada compilador vem a seguir.

```command
id: jsc:command/linux/sgsc
```

```command
id: jsc:command/linux/scc
```

Uma compilação bem-sucedida exibe primeiro o nome e a versão do compilador, depois o que ele gravou. Aqui ele compila um arquivo chamado hello.sgs:

```session
{"lines": [{"prompt": "player@pc:~$ ", "text": "sgsc hello.sgs", "typed": true}, {"text": "Σ# Compiler 2.0"}, {"text": "sgsc: wrote hello.asm"}, {"text": "run it with: sigma run hello.asm"}], "look": "jsc:guide/terminal/bash", "title": "Ubuntu"}
```

Compilar não executa o programa. A última linha diz como executá-lo, com o comando sigma ([](jsc:sigma_run)).

Table: Como lê um erro

| Parte | O que significa |
| --- | --- |
| hello.sgs | O arquivo-fonte que tem o erro. |
| (3,5) | A linha e a coluna onde está o problema. |
| erro CODE: mensagem | O tipo de mensagem, um código que identifica o problema, e as suas palavras. |

Um aviso tem a mesma aparência, mas diz aviso em vez de erro, e aparece em texto esmaecido. Quando a compilação falha, o compilador exibe por último o número de erros, como "sgsc: 1 erro, nada foi escrito". Uma compilação com falha não gera nenhuma listagem.

## Compilando para vários conjuntos de instruções

Um programa roda só na linha de processadores para a qual foi compilado. As máquinas da Quince têm linhas próprias (68k, PowerPC e ARM64), então um programa para elas e para as outras é compilado com sigma build, o comando do próprio runtime. Com vários conjuntos de instruções depois de --isa, ele faz um build universal: uma listagem para cada, num arquivo só, e cada máquina roda a listagem do seu próprio processador. O arquivo fica do tamanho de todas juntas.

Table: O comando sigma build

| Comando | O que faz |
| --- | --- |
| `sigma build <source> [-o <out.asm>] [--isa <isa>[,<isa>...]] [--lang <version>]` | Compila um arquivo-fonte para os conjuntos de instruções nomeados, ou para o padrão da sua linguagem sem --isa. A terminação da fonte diz a linguagem. |

```session
{"lines": [{"prompt": "player@pc:~$ ", "text": "sigma build hello.sgs --isa powerpc,x86-64", "typed": true}, {"text": "sigma: wrote hello.asm"}, {"text": "a universal build for PowerPC, x86-64, which runs on each"}, {"text": "run it with: sigma run hello.asm"}], "look": "jsc:guide/terminal/bash", "title": "Ubuntu"}
```

## Se algo der errado

> [!PROBLEM] Aparece "sgsc: 1 erro, nada foi escrito", ou a mesma mensagem com mais erros.
> Leia cada erro listado acima, corrija a linha que ele aponta e compile novamente.

> [!PROBLEM] O compilador exibe o nome do comando e um erro de leitura quando um arquivo-fonte está faltando.
> Confira o nome do arquivo e se ele está na pasta que você indicou.

> [!PROBLEM] Aparece a linha de uso, com as opções listadas.
> Forneça pelo menos um arquivo-fonte.

> [!PROBLEM] Aparece "IA-16 executa apenas o Σ; escreva-o em Σ e compile-o com scc".
> Compile o programa Sigma com scc, ou escolha x86 ou x86-64 com --arch.

> [!PROBLEM] Aparece "nenhum conjunto de instruções se chama", seguido dos nomes que existem.
> Use um dos nomes IA-16, x86, x86-64, 68k, PowerPC ou ARM64.

> [!PROBLEM] Aparece "'3' não é uma versão do Σ#; há de 1 a 2".
> Use 1 ou 2 depois de --lang.

> [!PROBLEM] Aparece "este compilador conhece o Σ# 1 no máximo; o Σ# 2 precisa de um mais novo".
> Atualize o pacote do compilador, ou use uma versão menor depois de --lang.

> [!SEE] jsc:sigma_errors, jsc:sigma_run
