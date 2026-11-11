---
section: network_remote
order: 109
icon: jsc:personal_computer
---
# Acesso remoto: ssh e Controle Remoto

Estar na mesma rede é o que permite alcançar outro computador. O comando de shell seguro (ssh) abre um shell em outro computador da sua rede, a partir do terminal em que você está.

1. Digite ssh sozinho para ver as máquinas que você pode alcançar.
2. Digite ssh e o nome do host da máquina que você quer.
3. Digite exit para sair. Fechar a janela também a encerra.

Uma máquina responde pelo seu nome de host, pelo nome que o dono lhe deu, pelo início do seu ID do nó ou pelo nome do seu sistema, desde que esse nome identifique exatamente uma máquina. Você pode alcançar o Mainframe, os Computadores Pessoais da rede e os servidores nos racks. Você não pode alcançar o computador em que está sentado.

O acesso não precisa de senha: estar na mesma rede é o que o concede.

Table: Mensagens do ssh

| Mensagem | O que significa |
| --- | --- |
| Conectado a <machine>. Digite exit para voltar. | A sessão está aberta no outro computador. |
| <name>: host não encontrado nesta rede | Nenhuma máquina da sua rede tem esse nome; digite apenas ssh para ver os nomes. |
| <name> corresponde a <n> máquinas (<names>) - use o nome do host ou o ID do nó | Use o nome do host ou o início do ID do nó. |
| conectar ao host <name>: máquina está desligada | A máquina está desligada. Ligue-a. |

Os comandos que você executa em outro computador usam o orçamento próprio desse computador, e levam o nome dele. Um computador pode recusar programas de outros: a configuração remote dele decide isso.

Table: A configuração remote

| Comando | O que faz |
| --- | --- |
| `config remote on` | Outros computadores podem executar programas aqui. Este é o padrão. |
| `config remote off` | Outros computadores não podem executar programas aqui. |

O Controle Remoto é o jeito de apontar e clicar para entrar nas outras máquinas da rede ([](jsc:remote_control)).

> [!DEFINE] Controle Remoto
> O programa de área de trabalho que assume o controle de outra máquina da rede e a mostra na sua tela.

## Se algo der errado

> [!PROBLEM] host não encontrado nesta rede
> Confira o nome com ssh sozinho, ou na aba Dispositivos do Gerenciador de Rede. A máquina precisa estar na mesma rede.

> [!PROBLEM] máquina está desligada
> Ligue a máquina.

> [!PROBLEM] não pode executar programas
> O outro computador está com config remote off. Ative-o lá com config remote on.
