---
section: racks
order: 55
---
# Tipos de servidor

Cada servidor é um chassi do seu próprio tipo. O chassi define sua altura, suas baias de unidade e quantos processadores e slots de expansão ele tem. Ao escolher um servidor, esta tabela mostra o que você recebe. Todos os servidores e caixas de servidor vêm das abas criativas de J's Computers.

Toda era tem três: um servidor 1U, um Servidor de Armazenamento 2U, com o maior número de baias de disco, e um Servidor de Computação 2U, com o maior número de processadores. O de uma era mais nova tem mais daquilo para que foi feito. Um servidor ainda só usa tantos processadores quantos soquetes a sua placa tiver.

```table
source: jsc:servers
caption: Servidores
heads: Servidor | Altura | Baias de unidade | Processadores | Slots de expansão | Era
```

Um servidor cabe num rack da sua própria era ou de uma mais nova. Um Servidor Vintage cabe num Rack de Servidores Standard, mas um servidor Standard nunca cabe num rack Vintage. Os Nós de Supercomputador vão num Rack de Supercomputador, e o Nó de Supercomputador Advanced vai num Rack de Supercomputador Advanced.

Os slots de expansão de um servidor recebem as placas dele, entre elas as placas de armazenamento: uma controladora RAID, que junta as unidades num único volume, e um cache de leitura, que responde às leituras delas mais rápido ([](jsc:raid_arrays)).

Cada unidade de rack tem cinco slots frontais. Uma máquina preenche em ordem os slots das suas fileiras com as suas baias de unidade, e o resto fica bloqueado. Quando um slot está bloqueado, a tela diz por quê, com uma de duas mensagens fixas.

## Se algo der errado

> [!PROBLEM] Um slot está bloqueado: "Bloqueado - nenhuma unidade nesta fileira fornece cabeamento para estes slots".
> Monte uma unidade nessa linha que faça o cabeamento desses slots, ou escolha um chassi que caiba na linha.

> [!PROBLEM] Um slot está bloqueado: "Bloqueado - o chassi não fornece cabeamento para este slot".
> Use um chassi com mais baias de unidade.

> [!SEE] jsc:server_racks, jsc:rack_drive_bays
