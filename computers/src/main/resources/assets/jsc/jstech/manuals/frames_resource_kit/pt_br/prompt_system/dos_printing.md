---
section: prompt_system
order: 243
---
# Imprimindo pelo prompt

print envia arquivos de texto para a impressora em segundo plano, então o prompt fica livre enquanto os arquivos são impressos. Você precisa de uma impressora vinculada ao computador ([](jsc:printers)).

Na primeira vez que você usar o print, aparece "Parte residente do PRINT instalada". Cada arquivo que você nomear entra na fila. A fila lista o arquivo sendo impresso e, depois, os arquivos que esperam na fila. Quando a fila estiver vazia, o PRINT diz "A fila do PRINT está vazia".

```command
id: jsc:command/mc_dos/print
```

## Se algo der errado

> [!PROBLEM] print diz Erros no dispositivo de lista indicam que ele pode estar desconectado.
> A impressora não está vinculada, ou está desligada. Verifique a impressora e depois imprima de novo ([](jsc:printers)).

> [!SEE] jsc:printers
