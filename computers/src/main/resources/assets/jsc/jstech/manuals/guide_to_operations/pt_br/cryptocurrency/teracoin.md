---
section: cryptocurrency
order: 114
icon: jsc:monitor
---
# Teracoin e a Carteira Cripto

Teracoin, TRC para abreviar, é a criptomoeda do mundo: uma cadeia de blocos para o servidor inteiro, de ninguém. Os mineradores acham os seus blocos e são pagos em TRC. Você guarda os seus numa carteira, envia para outros jogadores e os transforma em itens numa impressora 3D.

> [!DEFINE] Teracoin
> A criptomoeda do mundo, TRC. Um TRC se divide em oito casas decimais.

> [!DEFINE] Carteira
> Um arquivo no disco de um computador que guarda a chave de um endereço. Quem tiver o arquivo pode gastar o que o endereço tem.

> [!DEFINE] Carteira Cripto
> O programa da Blockwright que cria carteiras, mostra o saldo e o histórico delas, envia TRC e imprime com eles.

Instale a Carteira Cripto num computador da Transition ou posterior. Na primeira vez que você a abre, ela oferece criar uma carteira: dê um nome, e ela salva o arquivo da carteira na pasta Wallets dos documentos do computador. Com um pen drive no computador, marque a caixa para salvar uma cópia nele também.

As moedas vivem na cadeia, no endereço da carteira, não no arquivo dela. Copiar o arquivo copia o acesso, não as moedas: quem tiver uma cópia gasta o mesmo saldo. Perca todas as cópias e os TRC se vão para sempre, então guarde uma cópia em lugar seguro.

Table: As abas da carteira

| Aba | O que faz |
| --- | --- |
| Visão geral | O saldo, o que espera o próximo bloco e os movimentos recentes. |
| Enviar | Envia TRC para um endereço. Um envio não tem taxa: espera o próximo bloco, ou dez minutos quando nenhum bloco vem. |
| Receber | O endereço da carteira, para copiar, também desenhado como um quadrado de blocos para mostrar do outro lado de uma sala. |
| Transações | Cada movimento da carteira: recebido, enviado, pago por um pool e impresso. |
| Imprimir | Imprime itens nas impressoras 3D ligadas ao computador ([](jsc:printers_3d)). |
| Carteiras | As carteiras deste computador, para abrir uma ou criar outra. |

## Blocos, a dificuldade e a recompensa

Um bloco vem mais ou menos a cada dez minutos. A cada segundo, a chance de cada minerador achá-lo é o seu hashrate dividido pela dificuldade, que muda a cada dez blocos para manter esse ritmo. Um bloco paga 50 TRC, caindo pela metade a cada 2.100 blocos, e um bloco em vinte também traz um item raro, como um diamante, para o armazenamento da rede da máquina que o achou. As configurações do seu servidor podem dar outros números.

## Se algo der errado

> [!PROBLEM] A carteira diz que o arquivo dela não guarda mais a chave.
> O arquivo sumiu ou foi mudado. Abra outra carteira na aba Carteiras, ou copie o arquivo de volta de onde você o guardou.

> [!PROBLEM] Um envio diz que isso não é um endereço Teracoin.
> Copie o endereço de novo da aba Receber da outra carteira: ele começa com trc1q.

> [!PROBLEM] Um envio espera muito tempo.
> Ele espera o próximo bloco. Quando nenhum vem por dez minutos, ele passa sozinho.

> [!SEE] jsc:mining, jsc:mining_pools, jsc:printers_3d
