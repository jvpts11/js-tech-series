---
section: saving
order: 483
---
# Programas e o save do mundo

Seus programas são guardados quando o mundo é salvo. O computador grava os programas em execução nos próprios dados de save, que é a parte do save do mundo que pertence a esse computador. Os discos e as configurações dele também são salvos ali.

Cada programa é gravado por inteiro, para que possa continuar. Quando o mundo volta, um programa continua de onde parou. Ele não recomeça do início.

Table: O que um save guarda de cada programa

| Parte | O que significa |
| --- | --- |
| Memória | Cada objeto que o programa possui, e as ligações entre eles. |
| Threads | Cada thread, e o que ela estava fazendo. |
| Console | As linhas do console, e as linhas digitadas antecipadamente. |
| Chamadas em espera | As chamadas que esperam para executar no programa. |
| Janelas | As janelas que o programa tem abertas. |
| Listagem | A listagem do programa, identificada pela sua soma de verificação. |
| Pai | O programa que o iniciou, se outro programa o fez. |

O próximo número livre também é salvo, para que nenhum número seja dado duas vezes.

## Desligando o computador

Desligar um computador é diferente. Seus programas são parados, cada um com sua despedida, e não são guardados para quando ele for ligado de novo. Quando você o ligar outra vez, inicie os programas de novo.

## Lendo o save de volta

Quando o save é lido de volta, o computador o verifica. Se o save veio de outra versão do mod, ou se uma listagem mudou desde que o programa começou, o computador deixa de fora todos os programas desse computador.

Um programa cuja linguagem não está mais instalada é deixado de fora sozinho. O mesmo vale para um programa cuja linguagem não aceita o que ele escreveu. Os outros programas não são afetados.

Um programa iniciado a partir de outro computador mantém sua ligação com o programa que o iniciou. Depois de um recarregamento, esse pai continua sendo avisado quando o filho termina.

> [!DEFINE] Soma de verificação
> Um número calculado a partir de um texto. O mesmo texto sempre dá o mesmo número, e um texto alterado dá outro.

## Se algo der errado

> [!PROBLEM] O prompt diz "os programas que estavam em execução não puderam ser restaurados a partir do save".
> O save veio de outra versão do mod, ou uma listagem mudou desde que o programa começou. Inicie os programas de novo.

> [!PROBLEM] Um programa escrito em uma linguagem que não está mais instalada desaparece depois de um recarregamento.
> A linguagem dele não está mais instalada, então o programa foi deixado de fora. Instale essa linguagem.

> [!PROBLEM] Um programa some depois que o computador foi desligado.
> Ele foi parado, não mantido. Inicie-o de novo.

> [!SEE] jsc:sigma_run, jsc:running_programs
