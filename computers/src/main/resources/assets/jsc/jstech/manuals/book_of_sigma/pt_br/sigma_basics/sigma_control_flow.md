---
section: sigma_basics
order: 415
---
# Instruções: if, laços, switch, break e return

Algumas instruções guardam outras instruções dentro delas. Elas decidem se o conteúdo é executado, ou quantas vezes ele é executado. A maioria testa uma condição: uma expressão que é verdadeira ou falsa.

Um bloco pode ficar em qualquer lugar onde caberia uma instrução. Uma variável declarada dentro de um bloco existe só até a chave de fechamento.

Table: Instruções de controle

| Instrução | Forma | Linguagem |
| --- | --- | --- |
| `if` | `if (condition) statement else statement` | Sigma e Sigma# |
| `while` | `while (condition) statement` | Sigma e Sigma# |
| `do` | `do statement while (condition);` | Sigma e Sigma# |
| `for` | `for (start; condition; step) statement` | Sigma e Sigma# |
| `foreach` | `foreach (Type name in array) statement` | Somente Sigma# |
| `switch` | `switch (value) with case and default labels` | Sigma e Sigma# |
| `lock` | `lock (object) statement` | Somente Sigma# |

A instrução if executa a sua instrução quando a condição é verdadeira. O else, que é opcional, executa a sua instrução quando a condição é falsa. Um laço repete uma instrução: while verifica a condição antes de cada execução, e do verifica depois de cada execução. O laço for mantém um contador. Por exemplo, for (int i = 0; i < 3; i++) executa três vezes, com i valendo 0, 1 e 2.

A instrução foreach executa a sua instrução uma vez para cada item de um array ou de uma lista, em ordem. Ela entrega cada item a uma variável à sua escolha. O Sigma recusa foreach e pede que você escreva um for sobre o Length do array.

A instrução switch compara um valor com cada rótulo de case e executa a seção cujo rótulo corresponde. Uma seção não para sozinha. Sem um break ou um return no fim dela, o programa continua para a seção seguinte. Coloque break no fim de cada seção, a não ser que você queira isso.

```sg Counting.sg
for (int i = 0; i < 3; i++)
{
    switch (i)
    {
        case 0:
            printf("zero");
            break;
        default:
            printf("%d", i);
            break;
    }
}
```

A instrução break sai do laço ou do switch mais próximo. A instrução continue pula para a próxima volta do laço mais próximo. A instrução return encerra um método. Um método que devolve um valor precisa devolver um em todas as saídas, para que o código não consiga chegar ao fim do método sem um return.

A instrução lock, só no Sigma#, recebe um objeto, e um número não pode ser bloqueado. Como os bloqueios são usados com threads é explicado em [Threads e bloqueios](jsc:threads_and_locks).

> [!DEFINE] Bloco
> Um grupo de instruções entre um par de chaves, que conta como uma única instrução.

> [!DEFINE] Laço
> Uma instrução que repete outra instrução, como while, do ou for.

> [!DEFINE] Condição
> Uma expressão que precisa dar um valor verdadeiro ou falso, como count > 0.

> [!DEFINE] Array
> Uma sequência fixa de valores de um mesmo tipo, numerados a partir de 0. Você escreve o tipo com colchetes, como int[].

> [!DEFINE] Lista
> Uma sequência de valores de um mesmo tipo que cresce conforme necessário, do tipo List do Sigma#. Seus itens também são numerados a partir de 0.

## Se algo der errado

> [!PROBLEM] O compilador diz que 'break' só tem sentido dentro de um laço ou de um switch.
> Mova o break para dentro de um laço ou de um switch, ou remova-o.

> [!PROBLEM] O compilador diz que 'continue' só tem sentido dentro de um laço.
> Mova o continue para dentro de um laço, ou remova-o.

> [!PROBLEM] O compilador diz que um método que devolve 'int' precisa retornar um valor.
> Termine o método com um return que dê um valor, como return total;

> [!PROBLEM] O compilador diz que um método que não devolve nada não pode retornar um valor.
> Remova o valor depois de return, ou mude o método para que ele devolva um valor de algum tipo, como int.

> [!SEE] jsc:sigma_methods_out_ref, jsc:sigma_dispose
