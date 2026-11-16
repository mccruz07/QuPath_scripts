# Positividade DAB citoplasmática — limiar 0.10

[← README](../README.md) · [Script](../scripts/ihq/celulas_positivas_dab_citoplasma_010_expansao5.groovy) · [Nomes antigos e novos](NOMES_DOS_SCRIPTS.md)

**Arquivo anterior:** `juncao.groovy`  
**Arquivo no acervo:** `celulas_positivas_dab_citoplasma_010_expansao5.groovy`

## O que faz

Define BRIGHTFIELD_H_DAB e vetores de cor, seleciona todas as anotações e executa PositiveCellDetection.

## O que precisa

QuPath com uma imagem aberta. Anotações de região já existentes.

Os classificadores treinados e modelos externos não acompanham este pacote. Os nomes de classes/canais e medidas precisam corresponder aos recursos da imagem e do projeto.

## Parâmetros principais

Detecção Hematoxylin OD, resolução 0.5 µm; área 10–400 µm²; threshold 0.1; expansão 5 µm. Positividade Cytoplasm: DAB OD mean, limiar 0.1, singleThreshold=true.

## Como usar

Abra a imagem no projeto, prepare as regiões indicadas acima e abra o script no editor Groovy. Ajuste os parâmetros e nomes de recursos conforme a tarefa e execute na imagem atual. As alterações de anotações e detecções ficam na hierarquia; salve os dados da imagem para conservá-las no projeto.

## O que gera

Células com medidas e classificação de positividade; sem exportação automática.

## Particularidades para reutilizar

Precisa de anotações de região antes da execução. Os limiares 0.4 e 0.6 também estão no bloco de parâmetros, mas singleThreshold está ativado.
