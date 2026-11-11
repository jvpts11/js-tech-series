---
section: messages_and_sharing
order: 484
---
# Programas conversando entre si

Programas conversam entre si com mensagens. Uma mensagem é uma linha de texto que um programa envia para o número de outro programa. O outro programa pode rodar no mesmo computador, em outro computador da rede, ou ser um computador [ComputerCraft](jsc:computercraft_side) acessado por meio de um [Gateway de Rede](jsc:network_gateway).

## Em um computador

Um programa envia uma mensagem com Process.Send, informando o número do outro programa e o texto. Para receber mensagens, um programa define um manipulador com Program.OnMessage: os passos que rodam para cada mensagem. O manipulador recebe o número do remetente (From), o texto (Text) e o tick em que ela chegou (Tick).

Table: Mensagens

| Chamada | Custo em instruções | O que faz |
| --- | --- | --- |
| `Process.Send(number, text)` | 10 | Envia o texto ao programa com esse número neste computador. |
| `Program.OnMessage(handler)` | grátis | Executa o manipulador para cada mensagem que chega. |
| `Gateway.Send(number, text)` | 20 | Envia o texto ao computador ComputerCraft com esse número. |
| `Gateway.OnMessage(handler)` | grátis | Executa o manipulador para cada mensagem que chega por um Gateway de Rede. |

Se o programa de destino não tiver manipulador, ele recebe a mensagem e a descarta, então Process.Send continua retornando true. Process.Send retorna false quando nenhum programa em execução tem esse número.

Uma mensagem é recusada quando o programa de destino terminou ou parou, ou quando suas chamadas em espera estão cheias: 256 chamadas, ou 64 KB de texto. Nesse caso, Process.Send retorna false.

Uma mensagem para um Gateway de Rede chega a todo programa do computador que esteja escutando por ela. Um computador onde nenhum programa escuta descarta a mensagem.

## Outros computadores da rede

Network.Computers lista os computadores da rede. Network.Computer(name) retorna um deles pelo nome. Cada um tem um nome de host, um nome, um tipo, um sistema operacional e um estado online.

Um programa em outro computador é acessado pelas chamadas remotas abaixo. Antes que qualquer uma delas funcione, o outro computador precisa estar na rede e ligado. Ele também precisa aceitar programas de outros computadores: a opção remota define isso, e ela vem ativada por padrão. Defina-a como on ou off. Um Start remoto também precisa de um computador que execute programas.

Table: Chamadas a outro computador

| Chamada | Custo em instruções | O que faz |
| --- | --- | --- |
| `RemoteComputer.Start(path)` | 200 | Inicia um programa lá, do mesmo jeito que Program.Start faz aqui. |
| `RemoteComputer.Shell(line)` | 200 | Executa uma linha no prompt desse computador. |
| `RemoteComputer.Send(number, text)` | 10 | Envia o texto ao programa com esse número ali. |
| `RemoteComputer.Processes()` | 50, mais 1 por linha | Lista os programas em execução ali. |

Um programa iniciado em outro computador registra quem o chamou como seu pai. Quando ele termina, quem chamou é avisado. Quem chamou lê sua saída e seu código de saída com Process.Output e Process.ExitCode, enquanto esse programa ainda estiver ali para ser lido.

O valor Process de um programa remoto carrega o nome de host do outro computador em Process.Host, e as chamadas nesse valor vão para esse computador. Se o host for desconhecido ou estiver desligado, Process.Running retorna false e Process.Kill não faz nada.

## Pastas compartilhadas

Um computador compartilha uma pasta com a rede pelo seu prompt, com o comando config. O compartilhamento usa o próprio nome da pasta. Ele é somente leitura, a menos que você adicione write.

Table: Comandos de compartilhamento

| Comando | O que faz |
| --- | --- |
| `config share <folder> [read\|write]` | Abre a pasta para a rede, que só pode lê-la, a menos que write seja informado. |
| `config unshare <name>` | Fecha o compartilhamento com esse nome. |

O prompt responde com o endereço do compartilhamento, como \\\\host\\name, e diz se ele é somente leitura ou de leitura e gravação. Um caminho em outro computador começa com duas barras invertidas, depois o nome do host, depois o compartilhamento e o resto do caminho: \\\\host\\share\\folder\\file. O outro computador responde pelos próprios discos.

Uma cópia pode usar uma pasta compartilhada em qualquer das pontas. O arquivo é lido onde está e gravado onde deve ir. Os compartilhamentos de um computador também são tratados em [Pastas compartilhadas](jsc:shared_folders).

## Se algo der errado

> [!PROBLEM] Process.Send retorna false.
> O programa terminou ou parou, ou suas chamadas em espera estão cheias. Tente de novo mais tarde, ou envie menos texto.

> [!PROBLEM] O prompt diz que um computador está desligado.
> Ligue esse computador e tente de novo.

> [!PROBLEM] O prompt diz que um computador não aceita programas de outros computadores.
> A opção remota dele está desativada. Ative essa opção nesse computador. Ela vem ativada por padrão.

> [!PROBLEM] O prompt diz que um computador não pode executar programas.
> Esse computador não executa programas, então nenhum programa inicia ali. Inicie o programa em um computador que execute programas.

> [!PROBLEM] O prompt diz que nenhum computador com esse nome está nesta rede.
> O nome está errado, ou o computador não está nesta rede. Network.Computers lista os computadores dela.

> [!PROBLEM] O prompt diz que um nome de host corresponde a mais de uma máquina.
> Use o nome de host de uma máquina, ou o ID do nó dela, que é a sua [identidade de rede](jsc:network_identity).

> [!PROBLEM] O prompt diz que uma pasta está compartilhada somente para leitura.
> A pasta foi compartilhada sem gravação. Seu proprietário executa config unshare nela e depois config share com write.

> [!PROBLEM] O prompt diz que este computador já compartilha 16 pastas.
> Deixe de compartilhar uma pasta com config unshare para abrir espaço para outro compartilhamento.

> [!PROBLEM] O prompt diz que outra pasta já está compartilhada com esse nome.
> Um compartilhamento usa o nome da pasta, então só uma pasta pode tê-lo. Cancele primeiro o compartilhamento da outra pasta.

> [!PROBLEM] O prompt diz que um host não foi encontrado nesta rede.
> Confira o nome de host. Network.Computers lista os computadores da rede.

> [!PROBLEM] O prompt diz que a máquina de um caminho está desligada.
> Ligue esse computador.

> [!PROBLEM] O prompt diz que não existe esse compartilhamento nesse host.
> Esse computador não compartilha nenhuma pasta com esse nome. Peça ao proprietário o nome do compartilhamento.

> [!PROBLEM] O prompt diz que um compartilhamento precisa ter um nome.
> Escreva o caminho na forma host, depois o compartilhamento, depois o resto do caminho: \\\\host\\share\\folder.

> [!SEE] jsc:network_gateway, jsc:shared_folders, jsc:remote_access
