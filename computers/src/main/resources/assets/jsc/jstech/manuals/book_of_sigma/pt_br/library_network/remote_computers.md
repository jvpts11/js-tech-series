---
section: library_network
order: 448
---
# Outros computadores da rede

Um programa pode iniciar um programa em outro computador da mesma rede e perguntar a esse computador o que ele executa. O outro computador é um RemoteComputer, e você obtém um a partir de Network.Computer ou Network.Computers.

O computador precisa estar na mesma rede. Um nome de host que corresponda a várias máquinas é recusado, então use o ID do nó ou o nome de host completo.

> [!DEFINE] Nome de host
> O nome pelo qual uma máquina responde na rede.

> [!DEFINE] ID do nó
> O identificador próprio da máquina. Use-o no lugar de um nome de host que corresponde a várias máquinas.

Table: Campos de RemoteComputer

| Campo | O que guarda |
| --- | --- |
| `RemoteComputer.Host` | O nome do host. |
| `RemoteComputer.Name` | O nome do computador. |
| `RemoteComputer.Type` | Mainframe, Computer, Computador de Fabricação, Computador Pessoal ou Computador de Gerenciamento de Cluster. |
| `RemoteComputer.Os` | O sistema que o computador executa. |
| `RemoteComputer.Online` | Sim quando o computador está ligado. |

Table: Chamadas em um RemoteComputer

| Chamada | O que faz |
| --- | --- |
| `RemoteComputer.Start(path)` | Inicia um programa nesse computador, como faz Program.Start, e devolve um Process; Start também aceita (path, args) ou (path, args, priority). |
| `RemoteComputer.Shell(line)` | Executa uma linha no prompt desse computador e devolve as linhas que ele imprimiu. |
| `RemoteComputer.Send(id, text)` | Envia uma mensagem a um processo nesse computador. Devolve false quando não consegue. |
| `RemoteComputer.Processes()` | Os processos desse computador, como uma lista de Process. |

Table: Recusas de outro computador

| Mensagem | O que significa |
| --- | --- |
| O computador não pode executar programas | Essa máquina não executa programas. |
| O computador não aceita programas de outros computadores | Essa máquina recusa programas enviados por outras. |
| O computador está desligado | Essa máquina está desligada. |
| Nenhum computador assim nesta rede | Nenhuma máquina com esse nome está nesta rede. |

## Se algo der errado

> [!PROBLEM] O programa para e diz que o computador está desligado.
> Ligue o computador e chame-o de novo.

> [!PROBLEM] O programa para e diz que o computador não pode executar programas.
> Escolha outro computador da rede para o programa.

> [!PROBLEM] O programa para e diz que o computador não aceita programas de outros computadores.
> Inicie o programa em outro computador, ou execute-o nesse computador você mesmo.

> [!PROBLEM] Network.Computer devolve null.
> Nenhum computador com esse nome está na rede. Veja os nomes em Network.Computers.

> [!PROBLEM] O nome do host corresponde a várias máquinas.
> Use o ID do nó ou o nome completo do host.

> [!SEE] jsc:remote_access, jsc:what_a_network_is
