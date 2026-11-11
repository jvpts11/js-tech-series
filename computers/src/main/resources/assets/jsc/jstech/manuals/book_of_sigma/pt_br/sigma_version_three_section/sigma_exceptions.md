---
section: sigma_version_three_section
order: 435
---
# Exceções: try, catch, finally e throw

A partir da versão 3, o Sigma# tem exceções. throw interrompe a etapa em que está e lança uma exceção, um objeto do tipo Exception ou de uma classe que se apoia nele. O programa então procura um catch que a receba, primeiro no método que a lançou e depois em cada método que o chamou. O Sigma não tem exceções.

Um try protege um bloco. Cada catch depois dele nomeia um tipo de exceção e recebe as desse tipo, ou de um tipo que se apoia nele; o primeiro que serve é o que roda. Um finally roda seja qual for a forma como se sai do bloco: no seu fim, por um return, um break ou um salto, ou por uma exceção. Um throw escrito sozinho num catch relança a exceção que esse catch recebeu.

Table: As exceções que a linguagem tem

| Tipo | Lançada quando |
| --- | --- |
| `NullReferenceException` | Um nome que não guarda nada é usado como se guardasse um objeto. |
| `ObjectDisposedException` | Um objeto é usado depois do seu dispose. |
| `DivideByZeroException` | Um número inteiro é dividido por zero. |
| `IndexOutOfRangeException` | Um índice está fora do seu array. |
| `ArgumentException, ArgumentOutOfRangeException` | Uma chamada recebe um valor que não pode receber. |
| `InvalidOperationException` | Uma etapa que o programa não pode dar agora, como uma conversão para um tipo que o objeto não é. |
| `FormatException` | Um texto não é o número como o qual foi lido. |
| `IOException, FileNotFoundException` | Um arquivo não pode ser lido nem escrito, ou não está lá. |
| `NetworkException` | O computador não está numa rede. |
| `AccessViolationException` | Uma leitura ou uma escrita através de um ponteiro cai fora de todo bloco de memória crua. |

Toda exceção tem uma Message, o File e a Line em que foi lançada, um Trace das chamadas por que passou, e a exceção Inner com que foi criada. Uma parada da linguagem, como uma divisão por zero, é lançada como a sua exceção num programa que usa exceções; ficar sem memória e chamar fundo demais não lançam nada.

Uma exceção que nada captura para o seu programa, que escreve a mensagem e o rastro dela no console. Quando um programa é parado com sigma stop, o Gerenciador de Tarefas ou kill, os seus blocos finally rodam antes; kill -9 e uma máquina que perde energia não rodam nada. Um throw custa 50 instruções, e mais 5 por cada chamada de que sai.

```sgs Parse.sgs
using System.*;

namespace Tools;

class Parse
{
    static void Main()
    {
        try
        {
            int n = Convert.ToInt("seven");
            Console.PrintLine("" + n);
        }
        catch (FormatException failed)
        {
            Console.PrintLine("not a number: " + failed.Message);
        }
        finally
        {
            Console.PrintLine("done");
        }
    }
}
```

> [!DEFINE] Exceção
> Um objeto lançado quando uma etapa falha, que um catch em algum ponto das chamadas pode receber e tratar.

> [!DEFINE] Finally
> Um bloco que roda seja qual for a forma como se sai do bloco que ele segue.

## Se algo der errado

> [!PROBLEM] O compilador diz que um catch antes deste já recebe toda exceção desse tipo.
> Ponha primeiro o catch do tipo mais estreito, e por último o de Exception.

> [!PROBLEM] O compilador diz que nada sai de um finally a não ser o seu próprio fim.
> Tire o return, o break ou o salto do finally, e faça-o depois do try.

> [!PROBLEM] O programa para e diz que uma exceção foi lançada e nada a capturou.
> Capture-a onde o programa possa continuar, ou verifique o valor antes da etapa.

> [!SEE] jsc:sigma_no_exceptions, jsc:sigma_runtime_stops
