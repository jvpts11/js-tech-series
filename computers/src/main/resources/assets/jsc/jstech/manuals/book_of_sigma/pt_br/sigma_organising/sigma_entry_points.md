---
section: sigma_organising
order: 422
---
# Onde um programa começa: Main e programas residentes

Todo programa tem exatamente um ponto de início. Quando você executa um programa, a máquina executa esse ponto primeiro. Há dois tipos de início. Um programa de console executa uma vez um método chamado Main e termina. Um programa residente continua ativo depois de iniciar e executa código a cada tick do jogo até ser parado.

> [!DEFINE] Ponto de entrada
> O ponto onde um programa começa a executar.

> [!DEFINE] Programa de console
> Um programa que executa o seu método Main uma vez, e termina quando Main retorna.

> [!DEFINE] Programa residente
> Um programa que inicia e depois fica ativo, executando os seus métodos a cada tick do jogo até ser parado.

Table: Os dois tipos de programa

| Tipo | Começa com | Termina quando |
| --- | --- | --- |
| Programa de console | `static void Main()` | Main retorna. |
| Programa residente | Uma classe que herda de Script ou, no Sigma#, uma classe que implementa IScript. | Ele chama Program.Exit, ou o programa é removido. |

Uma classe que herda de Script e implementa IScript é um programa residente, porque escrever a interface é um ato deliberado. Um arquivo sem ponto de início é recusado, e também é recusado um arquivo com mais de um.

Table: As formas de Main

| Forma | Versão | Significado |
| --- | --- | --- |
| `static void Main()` | 1 | Executa uma vez, não recebe nada e não devolve nada. |
| `static int Main()` | 2 | Devolve um código de saída, um número inteiro, quando termina. |
| `static void Main(string[] args)` | 2 | Recebe os argumentos com que o programa foi iniciado. |
| `static int Main(string[] args)` | 2 | Recebe os argumentos e devolve um código de saída. |

Main precisa ser static, na classe que o contém. Um projeto está preso à versão que indica, então uma forma de Main que uma versão posterior acrescenta é recusada em um projeto anterior.

Um programa residente tem três métodos que a máquina chama. OnInit executa uma vez, quando o programa começa. OnTick executa uma vez por tick do jogo, e um novo tick espera até que o último OnTick termine, então um OnTick lento nunca se acumula. OnDestroy executa quando o programa é parado, se a máquina ainda tiver tempo para executá-lo. No Sigma#, escreva public void em cada um deles dentro de uma classe IScript. No Sigma, escreva public override void nos que você usa, em uma classe que herda de Script.

Um programa de console em Sigma tem esta forma, com uma linha que imprime algo quando executa.

```sg Greeter.sg
using Standard.*;

namespace Hello;

class Greeter
{
    static void Main()
    {
        printf("Hello");
    }
}
```

Um programa residente em Sigma herda de Script, e sobrescreve somente os métodos de que precisa. A cada tick do jogo, a máquina chama OnTick, como explica a [página de créditos](jsc:credits_per_tick).

```sg Light.sg
using Standard.*;

namespace Lamp;

class Light : Script
{
    public override void OnTick()
    {
        // runs every game tick
    }
}
```

## Se algo der errado

> [!PROBLEM] O compilador diz que o programa precisa de exatamente um ponto de início, e este não tem nenhum.
> Adicione uma classe com um método static chamado Main, ou uma classe que herde de Script, ou que implemente IScript no Sigma#.

> [!PROBLEM] O compilador diz que 'Main' precisa da versão 2; este projeto é da versão 1.
> Escreva o Main que a versão do seu projeto tem, ou altere a versão que o projeto indica.

> [!SEE] jsc:sigma_namespaces_using, jsc:sigma_inheritance
