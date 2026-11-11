---
section: installing_linux
order: 289
---
# As teclas, os discos e a escolha de área de trabalho dos instaladores

Os três instaladores compartilham um mesmo conjunto de teclas. As entradas acima dizem qual tecla cada passo precisa; esta entrada lista todas, com o que cada uma faz.

Table: Teclas dos instaladores Linux

| Tecla | O que faz |
| --- | --- |
| `UP and DOWN` | Move o cursor pela lista de discos, pela lista de áreas de trabalho e pelo hub do Fedora. |
| `ENTER` | Avança a partir da página. Na página de conclusão, reinicia. No hub do Fedora, abre a pergunta sob o cursor. |
| `1 to 9` | No hub do Fedora, abre a pergunta com esse número. |
| `Letters` | Na página de nome, digite o nome do computador, com até 256 letras. |
| `BACKSPACE` | Apaga a última letra do nome. |
| `E` | Na página de discos, pede para apagar o disco escolhido. |
| `Y` | No prompt de apagamento, apaga o disco. ENTER também apaga; qualquer outra tecla cancela. |
| `B` | No hub do Fedora, inicia a instalação. |
| `F3` | Sai da instalação, mas somente antes que algo seja gravado em um disco. |
| `ESC` | Fecha o monitor em que o instalador é mostrado. A instalação continua em execução. |

A página de discos lista os discos da máquina em quatro colunas: "Disco", "Tamanho", "Livre" e "Comporta". Um disco pode receber o sistema somente quando o seu espaço livre cobre o que o sistema ocupa. Um disco marcado para apagamento conta como vazio, então todo o seu tamanho conta.

Para apagar um disco, pressione E na página de discos. O prompt diz "Apagar o Disco N?", em que N é o número do disco. Ele diz que todos os arquivos do disco serão excluídos e que "Isso não pode ser desfeito." Abaixo, "Y = apagar" e "N = cancelar" mostram as teclas.

> [!WARNING] Apagar exclui todos os arquivos do disco, e isso não pode ser desfeito.

> [!DEFINE] Apagar
> Apagar todos os arquivos de um disco, para que o disco fique vazio antes que um sistema seja instalado nele. Um apagamento não pode ser desfeito.

A página de nome diz "Este nome identifica o computador no prompt e na rede." O nome se torna o nome de host e o nome mostrado no prompt.

A página de área de trabalho só aparece quando um Mirror responde. Sua lista traz as áreas de trabalho que esta máquina pode executar: um computador Legacy vê KDE Plasma e GNOME, e um computador Standard também vê Cinnamon. Escolher "Nenhum, somente o terminal" deixa o sistema no terminal dele.

A página de cópia mostra o progresso como "<n>% concluído" e "Cerca de <n> segundos restantes". Quando a cópia termina, o instalador diz "INSTALAÇÃO CONCLUÍDA" e pede para você "Retirar a mídia de instalação antes de reiniciar, ou a máquina inicializa o instalador de novo."

## Se algo der errado

> [!PROBLEM] A página de discos diz "Nenhum disco desta máquina tem espaço para ele."
> Adicione um disco maior à máquina, ou apague um disco que você não precisa mais com E na página de discos.

> [!PROBLEM] A página de discos diz "Sem espaço aqui. Apague este disco, ou escolha outro."
> Escolha um disco com espaço livre suficiente, ou pressione E para apagar o que você escolheu.

> [!SEE] jsc:install_ubuntu, jsc:install_debian, jsc:install_fedora
