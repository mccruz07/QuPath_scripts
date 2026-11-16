# Cellpose cpsam em tecido

[← README](../README.md) · [Script](../scripts/classificadores/celulas_cellpose_cpsam_tecido.groovy) · [Nomes antigos e novos](NOMES_DOS_SCRIPTS.md)

**Arquivo anterior:** `milena_simples.groovy`  
**Arquivo no acervo:** `celulas_cellpose_cpsam_tecido.groovy`

## O que faz

Cria anotações com tissue. Executa Cellpose em todas as anotações e adiciona medidas de forma às detecções.

## O que precisa

QuPath com uma imagem aberta. Recursos do projeto: `tissue`. Extensão Cellpose e ambiente/modelo correspondente configurados.

Os classificadores treinados e modelos externos não acompanham este pacote. Os nomes de classes/canais e medidas precisam corresponder aos recursos da imagem e do projeto.

## Parâmetros principais

tissue: área mínima 5000; cpsam, Red, pixelSize 3. Medidas: área, comprimento, circularidade, solidez e diâmetros.

## Como usar

Abra a imagem no projeto, prepare as regiões indicadas acima e abra o script no editor Groovy. Ajuste os parâmetros e nomes de recursos conforme a tarefa e execute na imagem atual. As alterações de anotações e detecções ficam na hierarquia; salve os dados da imagem para conservá-las no projeto.

## O que gera

Anotações e detecções com medidas na imagem; sem exportação automática.

## Particularidades para reutilizar

pathObjects usa todas as anotações, inclusive preexistentes. useCellposeSAM está comentado; só o nome do modelo está ativo. Na variante de criptas, a dilatação não é seguida de uma seleção explícita que restrinja o Cellpose apenas às regiões dilatadas.
