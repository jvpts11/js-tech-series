---
section: peripherals
order: 78
---
# Desativando um dispositivo

Você pode desativar um dispositivo, para que o seu computador pare de usá-lo. O dispositivo mantém a sua porta, e ativá-lo de novo o traz de volta. Você desativa um dispositivo quando quer que o computador pare de lê-lo ou de escrever nele.

> [!DEFINE] Desativar
> Para impedir que um computador use um dispositivo. O dispositivo mantém a sua porta até ser ativado de novo.

## No Gerenciador de Dispositivos

No Frames, o Gerenciador de Dispositivos ([](jsc:device_manager)) lista os dispositivos por tipo, por conexão ou por porta.

1. Abra o Gerenciador de Dispositivos.
2. Selecione o dispositivo.
3. Pressione Desativar, ou Ativar se o dispositivo já estiver desativado.

A linha de status diz que o dispositivo está desativado, e que o computador para de ler e escrever nele. Se um dispositivo estiver faltando na lista, procure-o em outra visão, porque a opção Show hidden devices não faz nada.

## No MC-DOS e no MC-NET

O MC-DOS e o MC-NET não têm Gerenciador de Dispositivos. O programa deles de diagnóstico da Midsoft, o MSD ([](jsc:dos_msd)), ocupa o terminal inteiro. Pressione L para as portas LPT, ou C para as portas COM. LPT e COM são os nomes antigos das portas paralela e serial.

Table: A lista de portas LPT e COM

| Coluna | O que mostra |
| --- | --- |
| Porta | O nome da porta. |
| Conectado | O que está conectado na porta. |
| Status | Ligado, ou Desligado (desativado). |

Na lista, pressione E para ativar o dispositivo na porta que você escolheu, ou D para desativá-lo. Enter guarda a lista, e F3 sai do MSD.

## No Common Desktop Environment (CDE) e no KDE

No CDE, a janela de Informações da Estação de Trabalho tem um botão Dispositivos... ([](jsc:cde_workstation_info)). Na caixa de diálogo Dispositivos dela, selecione um dispositivo, depois pressione Desativar ou Ativar.

No KDE, a página Dispositivos por porta do Centro de Informações tem um botão para o dispositivo selecionado, que o desativa ou o ativa ([](jsc:kde_plasma)).

Um dispositivo desativado fica em silêncio. Um monitor mostra "Sem sinal - o computador desativou este monitor", e uma Redstone Interface não lê nem emite nada. O computador também para de ler uma unidade, ou os discos da Estação de Dock.

## Se algo der errado

> [!PROBLEM] Um dispositivo fica apagado, ou não é visto, depois que você o desativou.
> Ative-o de novo: Ativar no Gerenciador de Dispositivos ou nas Informações da Estação de Trabalho, ou E na lista do MSD.

> [!PROBLEM] O Gerenciador de Dispositivos não lista um dispositivo.
> Procure-o em outra visão da lista, porque a opção Show hidden devices não faz nada.

> [!SEE] jsc:device_manager, jsc:dos_msd, jsc:cde_workstation_info, jsc:kde_plasma
