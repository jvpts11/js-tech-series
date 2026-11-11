---
section: starting_a_computer
order: 8
icon: jsc:floppy_disk
---
# Sistemas que os jogadores escrevem

Um jogador pode escrever um sistema operacional próprio em Sigma#, e um desktop próprio para acompanhá-lo. A linguagem e cada chamada estão n'[O Livro do Sigma](jsc:system_os_library). Esta página é sobre você instalar e compartilhar um.

1. Escreva o sistema numa pasta, com o desktop e os serviços dele ao lado.
2. Digite sigma build --os e o nome da pasta, com uma mídia virgem numa unidade: o instalador é gravado nela.
3. Digite sigma build --desktop e o nome do desktop para fazer o pacote dele.
4. Digite sgpack --publish e o nome da pasta para colocar os dois no Mirror da rede.

O sistema de um jogador se instala como qualquer outro: pela mídia dele, através do firmware, ou pela rede ([](jsc:network_boot)). O gerenciador de inicialização o lista pelo nome. O desktop vem junto na primeira vez que o sistema o inicia, e os gerenciadores de pacotes dos sistemas do mod instalam o desktop pelo nome a partir do Mirror.

Um sistema mostra a própria página enquanto inicia, e a própria página quando cai: a mensagem no topo, o erro com as linhas dele, o sistema, o autor, o processo e a memória, e no rodapé quando ele reinicia.

## Se algo der errado

> [!PROBLEM] A máquina cai na página própria do sistema.
> Leia o erro e a linha dele na página, corrija o sistema, compile de novo e instale de novo. Reinicie a máquina pelo botão de energia quando a página pedir.

> [!PROBLEM] A compilação diz que nenhuma mídia virgem pode ser gravada.
> Coloque um disquete, um CD ou um pendrive virgem da era do sistema numa unidade. O sistema fica guardado mesmo assim, e o sgpack --publish ainda pode colocá-lo no Mirror.

> [!SEE] jsc:network_boot, jsc:system_os_library, jsc:the_systems
