---
section: examples_intro
order: 501
---
# Executando os exemplos

Todos os exemplos deste capítulo são programas completos em Sigma#, a linguagem completa. Cada um é um único arquivo, e você pode digitá-lo, compilá-lo e executá-lo em um computador do mod.

Um exemplo começa com linhas using, que trazem os nomes da biblioteca para dentro do arquivo. A sua classe tem um método chamado Main, e o programa começa ali ([](jsc:sigma_entry_points)).

## Compilando e executando um exemplo

1. Salve o programa como um arquivo que termina em .sgs, como Buses.sgs.
2. Digite sgsc Buses.sgs no prompt e pressione [[Enter]].
3. Leia a resposta. Quando ela disser gravou Buses.asm, o programa está compilado.
4. Digite sigma run Buses.asm e pressione [[Enter]] para iniciar o programa.

O compilador grava o programa compilado ao lado do arquivo-fonte, com a terminação .asm. Após uma boa compilação, ele imprime gravou Buses.asm, e depois uma linha que começa com execute com: sigma run Buses.asm. O compilador é explicado em ([](jsc:sigma_compile)).

## As opções do compilador

Table: Opções do sgsc

| Opção | O que faz |
| --- | --- |
| `-o <out.asm>` | Indica o nome do arquivo compilado, no lugar do nome da fonte com .asm. |
| `--arch <isa>` | Escolhe o conjunto de instruções. Sem essa opção, é usado o jsc:x86 padrão. |
| `--lang <version>` | Escolhe a versão da linguagem em que o programa é verificado: 1 ou 2. |

A versão decide quais nomes um programa pode usar. Um programa que usa um nome da versão 2 precisa da versão 2. A tabela abaixo indica a versão que cada exemplo exige.

Table: Os exemplos

| Exemplo | Arquivo-fonte | Versão necessária |
| --- | --- | --- |
| Rede | `Network.sgs` | 1 |
| Barramentos | `Buses.sgs` | 2 |
| Fabricação | `Crafting.sgs` | 2 |
| Máquina | `Machine.sgs` | 1 |
| Redstone | `Redstone.sgs` | 2 |
| Som | `Sound.sgs` | 2 |
| Gateway | `Gateway.sgs` | 1 |
| Valores | `Values.sgs` | 1 |
| Passos | `Steps.sgs` | 1 |
| Programas | `Programs.sgs` | 1 |
| Nomes antigos | `OldNames.sgs` | 2 |
| Janelas | `Ui.sgs` | 1 |
| Widgets | `Widgets.sgs` | 2 |

> [!DEFINE] Versão da linguagem
> Uma etapa numerada do Sigma#. Uma etapa posterior acrescenta nomes, e um programa que usa um deles precisa dessa etapa.

## Se algo der errado

> [!PROBLEM] O compilador imprime um erro e informa que nada foi gravado.
> Corrija o primeiro erro impresso e compile o arquivo de novo. Um arquivo com erro não gera arquivo .asm.

> [!PROBLEM] A mensagem diz "'abc' não é uma versão de Σ#; existem as versões 1 a 2", porque o texto depois de --lang não é uma versão.
> Digite 1 ou 2 depois de --lang.

> [!PROBLEM] O compilador recusa um nome que a versão escolhida não tem, com o código S3057.
> Compile com a versão de que o nome precisa, ou tire o nome do programa.

> [!PROBLEM] O compilador informa que um nome está em um namespace e pede uma linha using, com o código S3040.
> Adicione a linha using que ele pede acima da linha namespace e compile de novo.

> [!SEE] jsc:sigma_compile, jsc:sigma_run, jsc:sigma_errors
