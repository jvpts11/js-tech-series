---
section: social_and_media
order: 89
icon: jsc:personal_computer
---
# Messenger

O Messenger permite que as pessoas de uma rede conversem entre si. Dois programas fazem ele funcionar. Na área de trabalho, o programa se chama Midsoft Messenger, e é a janela onde você conversa. O Serviço de Mensagens guarda as conversas, e roda em um servidor em um rack.

> [!DEFINE] Midsoft Messenger
> A janela de área de trabalho do Messenger, onde você lê e digita as suas mensagens.

> [!DEFINE] Serviço de Mensagens
> O serviço de um servidor em rack que guarda as conversas da rede para cada Midsoft Messenger que há nela.

O Serviço de Mensagens só se instala em um servidor em um rack da rede ([](jsc:server_racks)). Um Computador Pessoal ou um Mainframe não consegue hospedá-lo.

## A janela

Table: Partes do Messenger

| Parte | O que faz |
| --- | --- |
| Lista | As pessoas na rede, sob On the network; Everybody é a única sala que todos leem. |
| Conversa | O que foi dito, à direita. |
| Campo de escrita | Onde você digita uma mensagem. |
| Enviar | Envia a mensagem. |
| Cutucar | Envia um cutucão aos outros. |

Pressione [[Enter]] para enviar uma mensagem. Uma mensagem tem no máximo 200 caracteres. A janela pede ao servidor novidades a cada 2 segundos. Quando você cutuca, os outros computadores mostram uma linha dizendo que um cutucão foi enviado, com o nome do remetente.

## Limites e custo

O serviço guarda as últimas 500 mensagens. Quando há mais, as mais antigas são descartadas do fim. O histórico fica no servidor, não em nenhum dos dois computadores, então a conversa está lá quando você se senta em qualquer computador da rede. Ele vai junto com o servidor se o servidor for retirado.

O serviço custa 8 MB de memória antes que alguém se conecte, e mais 2 MB para cada pessoa conectada. Cada mensagem ocupa o seu próprio texto e um pouco de sobrecarga. A barra no pé da lista mostra quanto pesa o serviço.

## Apagar conversas

Quem está no servidor que roda o Serviço de Mensagens apaga as conversas que ele guarda. Lá, a lista mostra um botão Conversas guardadas, cuja janela lista cada conversa com quem participou, quantas linhas tem, o seu tamanho e quando se falou nela pela última vez, e apaga uma ou todas. Uma conversa apagada sai de todos os Messengers na hora, sem aviso a ninguém.

O prompt do servidor faz o mesmo com msgsvc: msgsvc list numera as conversas, msgsvc delete 2 apaga a segunda, msgsvc delete --all apaga todas, e msgsvc status diz o que o serviço guarda e quanto pesa ([](jsc:prompt_mainframe_verbs)).

## Se algo der errado

> [!PROBLEM] A janela diz Nenhum Serviço de Mensagens nesta rede.
> Instale o Serviço de Mensagens em um servidor em um rack desta rede, e deixe-o rodando.

> [!SEE] jsc:server_racks, jsc:server_router, jsc:mainframes
