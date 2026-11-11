---
section: sharing_knot
order: 500
---
# KnotHub

O KnotHub é o serviço que guarda o repositório: cada arquivo que você enviou com o Knot e cada revisão de cada um. Ele é instalado em um Servidor, um dos computadores de um Rack de Servidores ([](jsc:server_racks)). Ele não roda no Mainframe nem em uma área de trabalho.

Cada Servidor guarda o seu próprio repositório, e o código-fonte é salvo com aquele item Servidor. Se você tirar o Servidor do rack, o histórico dele vai junto. O Knot lê o repositório do primeiro KnotHub em execução que encontrar na rede, na ordem em que a rede lista os seus Servidores.

Um KnotHub em um Servidor desligado não responde. A janela então não mostra revisões, e a barra de status mostra "sem serviço".

> [!WARNING] Desinstalar o KnotHub apaga todo o código-fonte dele: cada arquivo e cada revisão. A numeração recomeça na revisão 1.

## Se algo der errado

> [!PROBLEM] A janela diz "Nenhum KnotHub nesta rede", e não mostra revisões.
> Coloque em um Rack de Servidores um Servidor com o KnotHub instalado, e ligue o Servidor ([](jsc:server_racks)).

> [!PROBLEM] A barra de status mostra "sem serviço".
> Vale a mesma solução: um KnotHub em execução precisa responder na rede.

> [!PROBLEM] O Push ou o Pull diz "Sem Disco do Sistema".
> Coloque um disco do sistema no computador ([](jsc:knot)).

> [!PROBLEM] O Pull diz "Nenhuma revisão desse tipo".
> A revisão foi descartada, ou o KnotHub foi reiniciado. Escolha outra revisão no histórico.

> [!SEE] jsc:knot, jsc:server_racks
