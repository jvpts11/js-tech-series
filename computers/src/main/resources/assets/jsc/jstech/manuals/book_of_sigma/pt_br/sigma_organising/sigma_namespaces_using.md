---
section: sigma_organising
order: 421
---
# Namespaces e using

Todo tipo de um programa pertence a um namespace, que é um nome que agrupa tipos. Um arquivo declara o seu namespace com uma linha antes dos seus tipos, como namespace Shop;. Todo tipo do arquivo então pertence a esse namespace, então dois programas podem ter cada um um tipo chamado Till sem conflito. Ao escrever um programa, as primeiras linhas dizem ao compilador de onde vêm os tipos que ele usa.

A biblioteca também é agrupada em namespaces. Um programa que quer um tipo da biblioteca escreve uma linha using no topo do arquivo, antes da linha namespace. using System.IO.\*; traz tudo o que está em System.IO, e using System.IO.Console; traz somente um tipo.

Table: Namespaces que você vai encontrar

| Namespace | Contém |
| --- | --- |
| `Standard` | A biblioteca do Sigma, que o Sigma# também tem: Console, File, Math, Script e mais. |
| `System` | Action, Func e IScript. |
| `System.IO` | Console e File. |
| `System.Utils` | Math e Random. |
| `System.Collections` | List e Map. |
| `System.Machine` | Outras partes da biblioteca. O [capítulo da biblioteca](jsc:sigma_library) as descreve. |

Uma linha using para um namespace inteiro é escrita com asterisco, como using System.IO.\*;. Uma linha que nomeia um só tipo não tem asterisco. Um programa escrito em Sigma escreve using Standard.\*; para acessar a biblioteca do Sigma.

Um tipo que o próprio programa declara vence um tipo da biblioteca com o mesmo nome. Um nome completo, como System.IO.Console.PrintLine("hi"), funciona no Sigma# sem linha using, e o Sigma recusa nomes completos.

Table: O início de um projeto novo

| Projeto | Linhas de cabeçalho |
| --- | --- |
| Programa de console do Sigma# | `using System.IO.*; then namespace, then the class` |
| Script do Sigma# | `using System.*; and using System.IO.*; then namespace, then the class` |
| Programa de console do Sigma | `using Standard.*; then namespace` |
| Script do Sigma | `using Standard.*; then namespace` |

```sgs Ping.sgs
using System.IO.*;

namespace Garage.Tools;

class Ping
{
    static void Main() { Console.PrintLine("up"); }
}
```

## Se algo der errado

> [!PROBLEM] O compilador diz que 'Console' está em System.IO; acrescente 'using System.IO.\*;' ou 'using System.IO.Console;' no topo do arquivo.
> Adicione essa linha using no topo do arquivo.

> [!PROBLEM] O compilador diz que 'System.IO' nomeia um namespace.
> Escreva 'using System.IO.\*;' para trazer tudo o que ela contém, ou nomeie um dos seus tipos.

> [!PROBLEM] O compilador diz que um using precisa vir antes do namespace e dos tipos.
> Mova a linha using para acima da linha namespace.

> [!PROBLEM] O compilador recusa using System no Sigma.
> Escreva using Standard.\*; no lugar, que contém tudo o que o Sigma tem.

> [!SEE] jsc:sigma_entry_points, jsc:sigma_library
