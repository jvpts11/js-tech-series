---
section: sigma_old_style
order: 427
---
# printf e os nomes antigos

O Sigma mantém um conjunto de nomes curtos de um estilo mais antigo de programação, como printf, strlen e puts. Você os chama sem tipo na frente, e eles funcionam no Sigma e no Sigma#. O primeiro que você encontra é printf, que imprime um texto com valores dentro dele.

> [!DEFINE] String de formato
> O texto que printf imprime, com lacunas onde os valores vão.

> [!DEFINE] Lacuna
> Um lugar em uma string de formato, marcado com um sinal de porcentagem e uma letra, onde um valor é escrito.

printf recebe uma string de formato e depois um valor para cada lacuna. Ele imprime o texto no console e não devolve nada. A string de formato precisa ser escrita na própria chamada, entre aspas, para que o compilador possa conferir as lacunas com os valores.

```sg Old.sg
using Standard.*;

namespace Old;

class Tool
{
    static void Main()
    {
        int n = 42;
        printf("n = %d, text = %s", n, "ok");
    }
}
```

Este programa imprime n = 42, text = ok. A primeira lacuna recebe o número inteiro n, e a segunda recebe o texto ok.

Table: As lacunas de uma string de formato

| Lacuna | Espera | Versão |
| --- | --- | --- |
| `%d, %i` | Um número inteiro, um int ou um long | 1 |
| `%u, %x, %X, %o` | Um número inteiro | 2 |
| `%f` | Um número | 1 |
| `%e, %E` | Um número | 2 |
| `%s` | Texto | 1 |
| `%c` | Um char | 1 |
| `%%` | Nada: escreve um sinal de porcentagem | 1 |
| `l in front, as in %ld` | Nada: é aceito e não significa nada | 1 |
| `ll, h, hh in front` | Nada: aceito, e não significa nada | 2 |
| `Flags, width, precision` | Indicadores como - e 0, e uma largura ou precisão de até 999 | 2 |

O número de lacunas precisa ser igual ao número de valores. Cada lacuna também espera um tipo de valor: %d espera um número inteiro, %s espera texto, e %c espera um char. O compilador confere tudo isso antes de o programa rodar.

sprintf recebe o mesmo formato e os mesmos valores, e devolve o texto em vez de imprimi-lo. scanf lê um valor do console para uma variável. Seu formato tem exatamente uma lacuna e mais nada, e a variável é escrita com out, como em scanf("%d", out n). Ele devolve 1 quando leu um valor, e 0 quando o que foi digitado não era um. Todo nome antigo, exceto printf, precisa da versão 2 do projeto.

Cada nome antigo é uma chamada da biblioteca, e dá o mesmo valor que o caminho longo. strlen(text) dá o mesmo que text.Length. Um método ou uma variável sua com o mesmo nome vence o nome antigo.

Table: Alguns nomes antigos e o seu caminho longo

| Nome antigo | Caminho longo | Versão |
| --- | --- | --- |
| `puts(text)` | `Console.PrintLine(text)` | 2 |
| `putchar(char)` | `Console.Print(char)` | 2 |
| `exit(int)` | `Program.Exit(int)` | 2 |
| `rand()` | `Random.Next(32768)` | 2 |
| `strlen(text)` | `text.Length` | 2 |
| `strcmp(a, b)` | `string.Compare(a, b)` | 2 |
| `atoi(text)` | `Convert.ToInt(text, 0)` | 2 |
| `toupper(char)` | `char.ToUpper(char)` | 2 |
| `fopen(path, mode)` | `File.Open(path, mode)` | 2 |

A página [nomes antigos na biblioteca](jsc:standard_functions) lista todos os nomes antigos e a chamada que cada um faz.

## Se algo der errado

> [!PROBLEM] O compilador diz que a string de formato de printf precisa ser escrita no lugar onde é usada, entre aspas, para que as lacunas possam ser lidas.
> Escreva a string de formato diretamente na chamada, entre aspas duplas.

> [!PROBLEM] O compilador diz printf: a string de formato tem 2 lacunas e a chamada dá 1 valor.
> Dê um valor para cada lacuna, na mesma ordem.

> [!PROBLEM] O compilador diz printf: '%d' recebe um número inteiro, e isto é texto.
> Use %s para texto, ou dê um número inteiro à lacuna.

> [!PROBLEM] O compilador diz que '%q' não é uma lacuna que printf tenha.
> Use uma das lacunas da tabela acima.

> [!PROBLEM] O compilador diz que 'strlen' precisa da versão 2; este projeto é da versão 1.
> Use um projeto em uso na versão 2, ou use printf, que existe em todas as versões.

> [!SEE] jsc:sigma_strings_chars, jsc:sigma_library
