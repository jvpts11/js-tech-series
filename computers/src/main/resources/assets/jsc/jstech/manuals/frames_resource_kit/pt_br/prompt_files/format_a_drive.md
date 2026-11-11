---
section: prompt_files
order: 237
---
# Formatando uma unidade

Formatar apaga tudo em uma unidade e a prepara de novo para guardar arquivos. Use em um disco novo, ou em um disco cujos arquivos você não precisa mais.

> [!DEFINE] Formatar
> Apagar todos os arquivos de uma unidade e prepará-la de novo.

> [!DEFINE] Volume
> O disco como a máquina o vê, com os arquivos que estão nele.

> [!WARNING] Formatar apaga todos os arquivos da unidade, e não há como desfazer. Confira a letra da unidade antes de executar a linha.

1. Digite format, um espaço, a letra da unidade e dois-pontos, como D:.
2. Leia o aviso. Ele nomeia a unidade que será apagada.
3. Digite a mesma linha com /y no final, como format D: /y, para iniciar a formatação.

format pede /y antes de fazer qualquer coisa. Sem ele, o format imprime o aviso em vermelho e depois a linha esmaecida que mostra a linha a digitar. O apagamento em si precisa do /y.

```session
{"lines": [{"prompt": "C:\\> ", "text": "format D:", "typed": true}, {"text": "WARNING: ALL DATA ON DRIVE D: WILL BE LOST!"}, {"text": "Run 'format D: /y' to proceed."}], "look": "jsc:guide/terminal/dos", "title": "MC-DOS"}
```

Table: As mensagens do format

| Mensagem | O que significa |
| --- | --- |
| usage: format <drive>: [/y] | Você digitou format sem unidade. |
| format: unidade inválida: NAME | A primeira palavra não é uma letra de unidade. |
| format: unidade D: não está pronta | Não há disco nessa unidade. |
| format: unidade D: não encontrada | Nenhuma unidade com essa letra está ligada ao computador. |
| format: não é possível formatar a unidade C: - o sistema em execução está nela | O disco do sistema não pode ser formatado a partir do próprio prompt. |
| Formatando unidade D: ... concluído | A unidade está formatada. A linha seguinte diz: Todos os dados do volume foram apagados. |

```command
id: jsc:command/mc_dos/format
```

## Se algo der errado

> [!PROBLEM] format diz que não pode formatar a unidade C:.
> C: guarda o sistema em execução. Formate uma segunda unidade ([](jsc:drives)).

> [!PROBLEM] format diz que a unidade não está pronta.
> Ligue um disco a essa unidade ([](jsc:drives)) e tente de novo.

> [!PROBLEM] A unidade errada foi apagada.
> Confira a letra da unidade e o aviso antes de acrescentar /y.

> [!SEE] jsc:disks, jsc:drives
