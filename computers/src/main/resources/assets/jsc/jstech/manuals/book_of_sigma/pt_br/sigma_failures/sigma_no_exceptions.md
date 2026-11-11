---
section: sigma_failures
order: 425
---
# Sem exceções: formas Try e paradas

Muitas linguagens informam uma etapa que falhou com uma exceção, que outra parte do programa pode capturar e tratar. O Sigma não tem exceções, e nem o Sigma# antes da versão 3: eles recusam try, catch, throw e finally. Esta página trata de escrever sem elas. Quando você lê um número de um texto, use antes uma forma Try, para que o programa não pare. [Exceções](jsc:sigma_exceptions) mostra as que o Sigma# 3 tem.

> [!DEFINE] Exceção
> Um sinal de que uma etapa falhou, que um programa pode capturar e tratar em outro lugar.

Em vez disso, um programa pode perguntar antes com uma forma Try, ou deixar que o problema o pare. Uma forma Try é uma chamada da biblioteca que diz se deu certo, e entrega o seu resultado por out. As classes Convert e File do próprio Sigma oferecem menos chamadas, e as formas Try não estão entre elas. A [página de números](jsc:numbers_and_time) e a [página de arquivos](jsc:files_and_file_handles) mostram o que o Sigma oferece.

> [!DEFINE] Forma Try
> Uma chamada da biblioteca que devolve true ou false para sucesso, e escreve o seu resultado por out.

Table: As formas Try

| Chamada | Devolve |
| --- | --- |
| `Convert.TryInt(text, out int)` | Um bool: se o texto é um número inteiro, e o número por out |
| `Convert.TryLong(text, out long)` | Um bool: se o texto é um long, e o valor por out |
| `Convert.TryDouble(text, out double)` | Um bool: se o texto é um número decimal, e o valor por out |
| `Convert.TryBool(text, out bool)` | Um bool: se o texto é true ou false, e o valor por out |
| `File.TryRead(path, out string)` | Um bool: se o arquivo pôde ser lido, e o seu texto por out |
| `Map.TryGet(key, out value)` | Um bool: se a chave está lá, e o seu valor por out |

Uma parada é diferente. O programa para na hora, escreve uma mensagem no seu console e não continua. Sem exceções, nada captura uma parada; num programa Sigma# 3 que as usa, a maioria das paradas é lançada como uma exceção que ele pode capturar. A lista completa das mensagens está na [página de paradas do runtime](jsc:sigma_runtime_stops).

Table: Paradas que a própria linguagem causa

| A mensagem começa com | Causada por |
| --- | --- |
| divisão por zero | Uma divisão com divisor zero. |
| calculou o resto de uma divisão por zero | Um resto com divisor zero. |
| não há nada aqui para acessar | Um nome que não guarda nada é usado como se guardasse um objeto. |
| não há posição 4 em um array de 4 | Um índice fora do array. |
| isto foi descartado e não pode ser usado | Um objeto usado de novo depois do seu dispose. |
| sem memória | O programa pede mais memória do que pode usar. |
| tentativa de ler ou escrever memória protegida | Um ponteiro é lido ou escrito fora de todo bloco de memória crua. No Sigma, o sistema diz isso com as suas próprias palavras, como Falha de segmentação. |

Uma falha é diferente de novo. A própria máquina falhou em uma instrução, e só aquele programa para. A mensagem diz que o runtime não conseguiu executar a etapa.

> [!DEFINE] Falha
> Uma falha da própria máquina em uma instrução, que para só aquele programa.

```sgs Reader.sgs
using System.*;
using System.Utils.*;
using System.IO.*;

namespace Parse;

class Reader
{
    static void Main()
    {
        string text = "42";
        int n;
        if (Convert.TryInt(text, out n))
        {
            Console.PrintLine("" + n);
        }
        else
        {
            Console.PrintLine("not a number");
        }
    }
}
```

## Se algo der errado

> [!PROBLEM] O programa para e diz que não há nada aqui para acessar.
> Verifique se o nome guarda um objeto antes de usá-lo, por exemplo criando o objeto com new antes.

> [!PROBLEM] O programa para e diz que isto foi descartado e não pode ser usado.
> Não use um objeto depois de descartá-lo. Veja [Liberando objetos com dispose](jsc:sigma_dispose).

> [!PROBLEM] O programa para e diz “divisão por zero”.
> Verifique se o divisor não é zero antes de dividir.

> [!PROBLEM] O compilador recusa try.
> O Sigma, e o Sigma# antes da versão 3, não têm try: verifique o valor antes da etapa, use uma forma Try, ou prenda o projeto Sigma# à versão 3.

> [!SEE] jsc:sigma_runtime_stops, jsc:sigma_dispose, jsc:sigma_exceptions
