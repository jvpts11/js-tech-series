---
section: cryptocurrency
order: 118
icon: jsc:asic_forge_s1
---
# Teracoin num portátil e num programa

Mais dois lugares guardam Teracoin além da Crypto Wallet de um computador. Um portátil também roda a Crypto Wallet, a partir da Transition ([](jsc:portables)), com uma carteira guardada no próprio aparelho em vez de num arquivo: você vê o saldo e o endereço dela, envia a partir dela e lê os movimentos mais recentes. O aparelho chega à cadeia pela rede a que está ligado; sem uma, a carteira dele espera.

Os seus programas chegam ao Teracoin pelo System.Teracoin, uma parte da biblioteca do Sigma# 3 ([](jsc:teracoin_library)). Um programa lê o endereço e o saldo do arquivo de uma carteira e envia a partir dele, lê a cadeia, ajusta o minerador da sua máquina como o comando miner faz, e lista os Mining Pools da rede. Um programa que lê o arquivo de uma carteira pode gastar dela, como você pode, então guarde o arquivo de uma carteira onde só os seus próprios programas rodam.

## Se algo der errado

> [!PROBLEM] A carteira do portátil não mostra nada de novo.
> O aparelho não está ligado a nenhuma rede, e a carteira dele espera por uma. Ligue-o a uma rede ([](jsc:portables)).

> [!PROBLEM] O envio de um programa dá não.
> A carteira não tem tanto. Faça o programa ler o Wallet.Balance primeiro ([](jsc:teracoin_library)).

> [!SEE] jsc:teracoin, jsc:portables, jsc:teracoin_library
