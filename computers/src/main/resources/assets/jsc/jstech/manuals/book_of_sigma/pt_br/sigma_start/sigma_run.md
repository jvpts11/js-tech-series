---
section: sigma_start
order: 409
---
# Executando programas

Uma listagem está pronta para ser executada, e o comando sigma a executa. O runtime inicia o programa, o mantém na sua lista de programas em execução e permite pará-lo de novo.

Table: O comando sigma

| Comando | O que faz |
| --- | --- |
| `sigma run <file> [--heap <n>M] [arguments]` | Inicia um programa. A palavra start faz o mesmo. |
| `sigma build <source> [--isa <isa>[,<isa>...]]` | Compila um programa para um conjunto de instruções, ou para vários de uma vez ([](jsc:sigma_compile)). |
| `sigma stop <id>` | Para um programa em execução. A palavra kill faz o mesmo. |
| `sigma ps` | Lista os programas em execução. A palavra list faz o mesmo, e o mesmo faz sigma sozinho. |

## Executando um programa

1. Compile o arquivo-fonte, ou tenha uma listagem pronta.
2. Digite sigma run hello.asm e pressione [[Enter]].
3. Digite sigma ps para ver o programa na lista.
4. Digite sigma stop 3 com o id da lista para pará-lo.

Table: Os arquivos que sigma run aceita

| Extensão do arquivo | O que acontece |
| --- | --- |
| `.asm` | Uma listagem. O runtime a inicia. |
| `.sgs or .sg` | Um arquivo-fonte. O runtime o compila primeiro e depois o executa. |
| Qualquer outra extensão | Recusado: "hello.txt não é um programa Σ#". |

Ao executar um arquivo-fonte, o runtime o compila primeiro. Ele usa a versão mais nova da linguagem e o conjunto de instruções padrão da sua linguagem. As configurações de um projeto não se aplicam a essa compilação.

Antes de uma listagem começar, o runtime faz três verificações. Ele verifica se a listagem cabe no conjunto de instruções do computador, ou se o sistema do computador traz um tradutor para ela, que a roda mais devagar. Verifica a versão de montagem da listagem, que é recusada quando é antiga ou nova demais. E verifica se a memória do programa cabe na memória livre do computador.

Cada programa em execução recebe memória própria, chamada heap. O heap é medido em megabytes, e você o define com --heap.

> [!DEFINE] heap
> A memória que um programa em execução recebe, medida em megabytes.

```table
{"caption": "O heap", "heads": ["Configuração", "Valor"], "rows": [[{"words": "Padrão"}, {"amount": 1, "unit": "MB"}], [{"words": "Maior possível"}, {"words": "64 MB. Um pedido maior é reduzido para 64 MB, sem mensagem."}], [{"words": "Como escrever"}, {"words": "--heap 16M, --heap 16MB e --heap 16 significam todos 16 megabytes."}]]}
```

Um valor que não é um número conta como zero, e zero resulta no padrão. Um --heap que seja a última palavra não é uma configuração: ele é passado ao programa como argumento.

Tudo o que vem depois do arquivo, exceto --heap e o seu número, é passado ao programa. Um programa Sigma lê essas palavras com Program.Args ([](jsc:programs_and_processes)). Um programa Sigma# pode recebê-las como um array de strings em Main, se o seu projeto for da versão 2.

Table: O que sigma ps mostra

| Coluna | O que mostra |
| --- | --- |
| id | O número que sigma stop recebe. |
| name | O nome do programa, cortado em 20 caracteres, com um ~ quando for cortado. |
| state | executando, em espera (aguarda algo fora dele), concluído, interrompido (parou por causa de um erro) ou entrada (aguarda o teclado). |
| memory | A memória que o programa ocupa, dentro do heap que recebeu, em KB (ou em bytes quando for menos de 1.024). |

Sem programa em execução, a lista informa isso. Esta é a resposta:

```session
{"lines": [{"prompt": "player@pc:~$ ", "text": "sigma ps", "typed": true}, {"text": "no Σ# programs are running"}], "look": "jsc:guide/terminal/bash", "title": "Ubuntu"}
```

Parar um programa responde parado com o seu id, como "parado 3". Um id que não está em execução responde "sigma: nada está em execução como 3".

A página completa do comando sigma vem a seguir.

```command
id: jsc:command/linux/sigma
```

## Se algo der errado

> [!PROBLEM] Aparece "hello.asm: 16 MB não cabem em 8 MB de memória livre".
> Diminua o valor de --heap, pare outro programa com sigma stop ou libere memória no computador.

> [!PROBLEM] Aparece "compilado para x86; esta máquina é IA-16".
> Compile com scc, ou execute a listagem em um computador Legacy ou posterior.

> [!PROBLEM] Uma mensagem indica a versão de montagem da listagem e a versão que este runtime lê.
> Compile o arquivo-fonte de novo, com o compilador instalado.

> [!PROBLEM] Aparece "hello.txt não é um programa Σ#".
> Execute um arquivo que termina em .asm, .sgs ou .sg.

> [!PROBLEM] Aparece "hello.sgs não compila".
> Compile o arquivo-fonte com sgsc ou scc, e leia os erros que ele indicar.

> [!SEE] jsc:sigma_running, jsc:sigma_compile
