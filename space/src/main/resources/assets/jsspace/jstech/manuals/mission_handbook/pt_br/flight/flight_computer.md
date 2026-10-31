---
section: flight
order: 1
icon: jsspace:flight_computer
covers: [jsspace:flight_computer]
---
# Computador de Voo

## O que é

O computador embutido em toda [cápsula](jsspace:capsule) e na cabine de todo [módulo lunar](jsspace:lander): um mostrador plano com teclas no alto e dos dois lados. Ele planeja um voo e depois o pilota.

## Para que serve

Ir de um mundo a outro: a transferência, o pouso, a volta e o destino de cada estágio gasto.

## Como conseguir

No menu criativo, aba J's Space.

## Como usar

Use-o com a mão vazia para abrir a tela e clique numa tecla ou nas palavras ao lado dela. As teclas do alto abrem as páginas; SIS e COM ainda não abrem nada.

Table: As páginas

| Página | As teclas |
| --- | --- |
| TRAN | Esquerda: o mundo de destino; JAN - e JAN +, a janela de partida. Direita: DV gasta mais delta-v para uma viagem mais curta e TV gasta menos; ASSIST passa por um mundo no caminho, gastando menos e demorando mais; AERO freia no ar na chegada em vez de queimar; EXEC marca o plano. |
| POUS | Clique no mapa para marcar um ponto. Esquerda: ZOOM +, ZOOM -, MARCA, LIMPAR. Direita: PERIGO mostra o chão íngreme demais para pousar, GRADE, e FIXAR, que fixa o pouso na marca. GUIA acende enquanto uma plataforma de guiagem guia o foguete. |
| EST | Esquerda: o estágio. Direita: QQR, ZONA, PLAT, PLAT + e RAIO +, para onde ele vai depois de gasto. |

1. No chão, defina para onde vai cada estágio gasto em EST, a transferência em TRAN com EXEC e o pouso em POUS com FIXAR.
2. Lance pelo [painel de lançamento](jsspace:launch_panel). O computador solta cada estágio esgotado e, passado o alto do céu, o foguete fica parado na órbita baixa do mundo.
3. Quando a janela chega, ele queima e parte; entre o Overworld e a Lua não há janela. Se todos a bordo dormirem, a viagem passa de uma vez.
4. Na órbita do outro mundo, ele [desce](jsspace:coming_down).
5. Depois do pouso, marque um novo plano com EXEC: ele decola quando a janela chegar.

> [!SEE] jsspace:landing_platform, jsspace:rocket_wreckage

## O que pode dar errado

> [!CAUTION]
> Escolher outro mundo em TRAN apaga o plano, a marca e o pouso.

> [!PROBLEM] TRAN mostra SEM TRANSFERÊNCIA DAQUI.
> Ele só planeja no chão de um mundo. Marque o plano antes de decolar.

> [!PROBLEM] TRAN mostra FALTA DV.
> Os estágios que restam não pagam a queima e a captura, e o foguete não parte. Gaste menos com TV, ligue AERO ou ASSIST, ou espere uma janela em vez de sair agora.

> [!PROBLEM] EST mostra SEM PLANO DE FOGUETE.
> O computador recebe o plano do foguete quando o foguete sai do hangar.
