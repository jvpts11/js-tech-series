---
section: starting_a_computer
order: 7
icon: jsc:mainframe
---
# Iniciando pela rede

Da era Legacy em diante, o firmware pode iniciar o instalador de um sistema pela rede, a partir do Mirror do Mainframe da rede ([](jsc:mirror)). Você não precisa de mídia em nenhuma unidade: o instalador vem pelo cabo, e a instalação corre como a partir de uma mídia.

Table: Inicialização pela rede em cada era

| Era | Onde você encontra | O que abre |
| --- | --- | --- |
| Vintage | Em lugar nenhum | Uma placa Vintage não tem inicialização pela rede. |
| Legacy e Transition | Rede (PXE), na página Boot do Setup | O agente da placa de rede, e depois o menu do Mirror no azul da BIOS. |
| Standard e Advanced | HTTP Boot, no Gerenciador de Inicialização | A lista do Mirror na própria página do firmware, com INSTALAR e VOLTAR. |

O menu lista só os sistemas que esta máquina roda, pela era e pelo processador dela, cada um com a sua casa. Um sistema escrito por um jogador mostra o autor e a palavra comunidade. Inicializar pelo disco local é sempre a última entrada.

> [!DEFINE] PXE
> Preboot Execution Environment: o jeito de uma BIOS iniciar um programa que ela recebe da rede.

> [!DEFINE] Comunidade
> A marca de um sistema ou de um desktop escrito por um jogador, ao lado do autor.

## Se algo der errado

> [!PROBLEM] Aparece "Nenhum Mirror respondeu na rede".
> Nenhum Mainframe da rede desta máquina serve um Mirror. Conecte a máquina à rede e ligue o Mirror no Mainframe ([](jsc:mirror)).

> [!PROBLEM] Um sistema não está na lista.
> Ele não roda nesta máquina. Confira a era e o processador do sistema com os da máquina.

> [!SEE] jsc:boot_menu, jsc:player_systems, jsc:mirror
