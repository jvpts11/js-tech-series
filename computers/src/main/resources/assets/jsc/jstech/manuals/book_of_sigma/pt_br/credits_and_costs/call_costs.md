---
section: credits_and_costs
order: 481
---
# Quanto custa cada chamada

Um programa alcança o computador, a rede e outros computadores por meio de chamadas. Cada chamada que você faz custa a sua instrução, mais um preço adicional. O preço depende de quanto trabalho a chamada faz, então uma chamada que lê um arquivo inteiro custa mais do que uma que lê um nome.

O preço tem três partes. A parte fixa é sempre paga. Uma linha é uma entrada de uma lista que a chamada devolve, como um nome de arquivo, e cada linha soma a segunda parte. Um bloco tem 4.096 bytes, ou 4 quilobytes (KB), de memória que a chamada lê ou escreve. Cada bloco soma a terceira parte, e uma parte de um bloco conta como um bloco inteiro.

Uma chamada que toca apenas no próprio programa é grátis, então só a sua instrução é paga. Imprimir no console e ler linhas digitadas são grátis, assim como as chamadas que consultam o nome e os argumentos do próprio programa.

## Os níveis de preço

Cada chamada tem preço em um destes níveis. Os mesmos níveis valem em todos os computadores.

Table: Os níveis de preço

| Nível | Preço em instruções | Para que serve |
| --- | --- | --- |
| Relance | 5 | uma olhada em algo que o computador já conhece sobre si mesmo |
| Relance na rede | 10 | uma olhada em algo que a rede já conhece |
| Reunir | 30 | reunir o que o computador tem: os seus discos, os seus programas e o que ele executa |
| Ler | 50 | leitura de um disco ou da rede |
| Ler, por 4 KB | 1 | somado à leitura para cada bloco lido |
| Escrever | 100 | escrita, que não pode ser desfeita |
| Escrever, por 4 KB | 2 | somado à escrita para cada bloco escrito |
| Desenhar | 50 | mudança do que uma janela mostra, que o computador então desenha de novo |
| Desenhar, por KB | 10 | somado a cada KB de um valor dado a um componente de janela |
| Enviar | 200 | pedir à rede que faça trabalho para a rede inteira |
| Iniciar uma thread | 49 | iniciar uma thread, além da chamada |
| Chamada através de um Gateway | 100 | chamar um computador de outro mod através de um Gateway |
| Cada argumento extra através de um Gateway | 5 | para cada coisa entregue através de um Gateway |
| Envio através de um Gateway | 20 | dizer algo a um computador através de um Gateway, ou ligá-lo ou desligá-lo |

As chamadas de arquivo usam os mesmos níveis. Ler um arquivo custa o preço de leitura, mais o preço de cada bloco que ele lê.

## Chamadas para programas e processos

Table: Chamadas de programa e de processo

| Chamada | Preço em instruções |
| --- | --- |
| `Program.SetName(name)` | grátis |
| `Program.Name, Program.Args, Program.Current` | grátis |
| `Program.Exit(code)` | grátis |
| `Program.OnMessage(handler)` | grátis |
| `Program.Start(path)` | 200, e as formas mais longas, com argumentos e uma prioridade, custam o mesmo |
| `Program.Shell(line)` | 200, mais 1 por linha |
| `Process.Id, Process.Name, Process.Host` | grátis |
| `Process.Running` | 5 |
| `Process.ExitCode` | 5 |
| `Process.Wait()` | grátis, e assim também a forma com ticks |
| `Process.Kill()` | 10 |
| `Process.Output()` | 50, mais 1 por linha |
| `Process.Send(number, text)` | 10 |
| `Thread.Start(action)` | 49 |
| `Thread.Sleep, Thread.Yield, Thread.Join, Thread.Stop` | grátis |
| `Thread.Id, Thread.Current` | grátis |

## Chamadas para o computador e a rede

Table: Chamadas de computador, de rede e remotas

| Chamada | Preço em instruções |
| --- | --- |
| `Computer.Name, Cpu, Os, RamMb, FreeRamMb, Online` | 5 |
| `Computer.Disks, Programs, Processes` | 30 |
| `Network.Online, Network.Current, Network.Computer(name)` | 10 |
| `Network.Capacity, Network.Used, Network.Total(name)` | 50 |
| `Network.Computers, Network.Find(name), Network.Servers` | 50, mais 1 por linha |
| `RemoteComputer.Start(path)` | 200 |
| `RemoteComputer.Shell(line)` | 200 |
| `RemoteComputer.Send(number, text)` | 10 |
| `RemoteComputer.Processes()` | 50, mais 1 por linha |
| `Gateway.Computers` | 30, mais 1 por linha |
| `Gateway.Send(number, text)` | 20 |
| `Gateway.OnMessage(handler)` | grátis |

## Se algo der errado

> [!PROBLEM] Um programa fica lento quando lê uma lista longa.
> Uma chamada que devolve uma lista custa mais conforme a lista cresce, uma linha por vez. Peça menos, ou faça a chamada uma vez e mantenha a sua resposta.

> [!PROBLEM] Uma chamada que lê um arquivo grande custa mais do que o esperado.
> Ler um arquivo custa 50 instruções, mais 1 para cada bloco de 4 KB. Um arquivo de 40 KB são dez blocos, então lê-lo com File.Read custa 60 instruções.

> [!SEE] jsc:credits_per_tick, jsc:what_operations_are
