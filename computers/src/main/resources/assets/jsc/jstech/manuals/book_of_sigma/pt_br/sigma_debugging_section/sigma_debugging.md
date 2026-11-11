---
section: sigma_debugging_section
order: 485
---
# Depurando um programa

Um depurador executa o seu programa e o para onde você pedir: num ponto de parada, que é uma linha do seu código-fonte, ou uma linha de cada vez. Enquanto ele está parado, você vê onde ele está, as chamadas em que ele está e o que as suas variáveis guardam. Você pode depurar no Virtual Studio, no Virtual Studio Code e no terminal com o sgdb.

O depurador lê linhas que só uma compilação feita para ele carrega. Compile com sgsc --debug no prompt, com a configuração Debug no Virtual Studio, ou com Iniciar Depuração no Virtual Studio Code. Uma compilação assim também define DEBUG, então o código dentro de #if DEBUG é compilado.

Table: Depurando nos estúdios

| Tecla | O que faz |
| --- | --- |
| `F9` | Põe ou tira um ponto de parada na linha do cursor. Um clique na margem faz o mesmo. |
| `F5` | Compila para o depurador e começa a depurar, ou segue até o próximo ponto de parada. |
| `Ctrl+F5` | Executa o programa sem o depurador. |
| `F10` | Contornar: vai à próxima linha, executando uma chamada nela por inteiro. |
| `F11` | Intervir: vai à próxima linha, entrando numa chamada. |
| `Shift+F11` | Sair: segue até a chamada em que está retornar. |
| `Shift+F5` | Parar Depuração: encerra o programa na hora. |

O Virtual Studio mostra as variáveis em Locais e Inspeção, as chamadas em Pilha de Chamadas, e o que o programa imprimiu em Saída. As suas Ferramentas de Diagnóstico mostram os créditos que o programa gastou, divididos entre as chamadas que ele fez e os próprios passos. O Virtual Studio Code mostra o mesmo na sua vista Executar e Depurar e no seu Console de Depuração. A barra de status fica laranja enquanto você depura.

Table: Os comandos do sgdb

| Comando | O que faz |
| --- | --- |
| `break 11` | Para o programa na linha 11 do arquivo em que ele começa; um arquivo nomeado antes da linha, com dois-pontos entre os dois, o para naquele arquivo. |
| `run` | Inicia o programa, ou o inicia de novo. |
| `continue` | Segue até o próximo ponto de parada. |
| `step` | Vai à próxima linha, entrando numa chamada. |
| `next` | Vai à próxima linha, passando por cima de uma chamada. |
| `finish` | Segue até a chamada retornar. |
| `print logs` | Mostra o que a variável logs guarda, e o seu tipo. |
| `info locals` | Mostra as variáveis da chamada em que ele parou. |
| `backtrace` | Mostra as chamadas em que o programa está. |
| `info credits` | Mostra os créditos que o programa gastou. |
| `quit` | Encerra o programa e sai do sgdb. |

Cada passo gasta o que executar gasta: o depurador não acrescenta custo, e um programa parado não gasta nada. Um programa iniciado por um depurador pertence a ele, então termina quando você para de depurar ou sai do sgdb, e não é salvo com o mundo.

> [!DEFINE] Ponto de parada
> Uma linha do código-fonte onde o depurador para o programa antes de executá-la.

> [!DEFINE] Pilha de chamadas
> As chamadas em que um programa está, a que ele está executando primeiro.

## Se algo der errado

> [!PROBLEM] O programa passa direto por um ponto de parada.
> Compile-o para o depurador, com sgsc --debug ou com a configuração Debug: uma compilação sem as linhas do depurador não tem onde parar.

> [!PROBLEM] O sgdb diz que o programa não está executando.
> Digite run para iniciá-lo primeiro.

> [!SEE] jsc:sigma_compile, jsc:call_costs, jsc:sigma_directives
