---
section: library_reach
order: 469
---
# Teracoin: carteiras, a cadeia, o minerador e os pools

O seu programa pode chegar ao Teracoin: o saldo de qualquer endereço, as carteiras nos arquivos do seu computador, a cadeia, o próprio minerador do seu computador e os Mining Pools da rede dele. Você faz isso pelo Wallet, Chain, Miner e Pool, tipos embutidos do System.Teracoin. Eles vieram com o Sigma# 3.

As quantias são centésimos-milionésimos inteiros de um TRC, como a cadeia as guarda: um TRC são 100000000 deles, e o seu programa nunca arredonda uma moeda para fora.

Table: Membros do Wallet e do Chain

| Membro | O que faz |
| --- | --- |
| `Wallet.Balance(address)` | O que o endereço guarda. |
| `Wallet.Address(path)` | O endereço da carteira nesse arquivo, ou texto vazio quando o arquivo não guarda nenhuma. |
| `Wallet.Send(path, to, amount, label)` | Envia a quantia da carteira nesse arquivo para o endereço, com a etiqueta, e dá sim; não quando a carteira não tem tanto. |
| `Chain.Height` | Quantos blocos a cadeia tem. |
| `Chain.Difficulty` | Quão difícil é achar o próximo bloco. |
| `Chain.Reward` | O que achar o próximo bloco paga. |

Table: Membros do Miner

| Membro | O que faz |
| --- | --- |
| `Miner.Installed` | Sim quando o minerador está instalado. |
| `Miner.Status` | O que ele está fazendo: off, alone, pool, no_address, no_pool, waiting_for_owner ou nothing_hashes. |
| `Miner.Hashrate` | Quantos hashes por segundo ele faz. |
| `Miner.Pool, Miner.Address` | O pool para o qual minera, vazio quando minera sozinho, e o endereço que ele paga. |
| `Miner.Background` | Sim enquanto ele roda em segundo plano. |
| `Miner.SetAddress(address), Miner.UsePool(name), Miner.MineAlone()` | Define o endereço que ele paga, o pool para o qual minera (vazio para o primeiro da rede), ou minerar sozinho. |
| `Miner.Start(), Miner.Stop()` | Põe-no para rodar em segundo plano, e para-o. O Start dá não quando ele não tem endereço para pagar. |

Table: Membros do Pool

| Membro | O que faz |
| --- | --- |
| `Pool.Names()` | Os nomes dos Mining Pools da rede. |
| `Pool.Of(name)` | O pool com esse nome, o primeiro da rede para texto vazio, ou null quando não há nenhum. |
| `Pool.Name, Pool.Host, Pool.FeePermille, Pool.Hashrate, Pool.Members, Pool.Found` | O nome dele, o servidor onde roda, a taxa em milésimos, quantos hashes por segundo os mineradores dele fazem, quantos mineradores tem, e quantos blocos achou. |

Uma carteira é o arquivo dela: quem lê o arquivo pode gastar dela, como no programa Wallet. Um arquivo que não guarda carteira nenhuma, um endereço escrito errado, uma quantia de nada ou uma etiqueta de mais de uma linha para o seu programa com uma ArgumentException. O que você envia sai da carteira na hora e chega à outra com o próximo bloco.

O Miner lê e define o minerador do seu computador como o comando miner faz, e o que você define é guardado com o computador.

```sgs Rent.sgs
string wallet = "Wallets/main.wallet";
if (Wallet.Balance(Wallet.Address(wallet)) > 500000000) {
    Wallet.Send(wallet, "trc1qexampleexampleexampleexample", 100000000, "Rent");
}
```

> [!DEFINE] Centésimo-milionésimo
> A menor quantia de Teracoin: um TRC dividido em 100000000 partes, do jeito que a cadeia conta cada quantia que você tem.

## Se algo der errado

> [!PROBLEM] Miner.Start dá não.
> O seu minerador não tem um endereço para pagar. Chame o Miner.SetAddress primeiro.

> [!PROBLEM] Wallet.Send dá não.
> A carteira não tem tanto. Leia o Wallet.Balance primeiro.

> [!SEE] jsc:teracoin, jsc:mining, jsc:mining_pools
