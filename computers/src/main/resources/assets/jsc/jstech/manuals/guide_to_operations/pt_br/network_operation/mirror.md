---
section: network_operation
order: 105
icon: jsc:mainframe
---
# O Mirror

O Mirror é uma prateleira de pacotes para a rede. Um Mainframe que roda o Mirror guarda até 64 pacotes, e todo computador da rede pode instalar a partir dessa prateleira.

O Mirror é um serviço do Mainframe. Ele precisa de 64 megabytes (MB) de disco e 32 MB de memória de acesso aleatório (RAM) no Mainframe, e só serve pacotes enquanto o Mainframe estiver ligado.

1. Ligue o Mainframe.
2. Digite mirror install no terminal do Mainframe.
3. Conecte os outros computadores à rede deste Mainframe.
4. Instale um pacote do Mirror com o gerenciador de pacotes do seu sistema.

Table: Os comandos do Mirror

| Comando | O que faz |
| --- | --- |
| `mirror install` | Instala o Mirror neste Mainframe. |
| `mirror status` | Mostra se o Mirror não está instalado, se está servindo, ou se está instalado com o Mainframe desligado. |

Você publica um pacote seu com o comando sgpack. Compile-o primeiro com sgpack build, depois publique com sgpack publish. Publicar um nome que já está na prateleira substitui esse pacote, e é assim que você lança uma correção. Retire um pacote com sgpack unpublish.

> [!DEFINE] O Mirror
> A prateleira de pacotes que um Mainframe serve à sua rede.

## Se algo der errado

> [!PROBLEM] não foi possível resolver mirror://
> Este computador não está na rede de um Mainframe que roda o Mirror. Conecte-o a essa rede, ou instale o Mirror no Mainframe com mirror install.

> [!PROBLEM] a rede não tem um Mainframe em execução para hospedar o Mirror
> Ligue o Mainframe e tente de novo.

> [!PROBLEM] o Mirror está cheio (64 pacotes)
> Tire um pacote da prateleira com sgpack unpublish.

> [!PROBLEM] isto não é um package
> Faça a compilação do pacote com sgpack build primeiro.
