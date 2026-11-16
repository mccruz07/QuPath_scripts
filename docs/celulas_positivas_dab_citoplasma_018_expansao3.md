# Positividade DAB citoplasmática — limiar 0.18

[← README](../README.md) · [Script](../scripts/ihq/celulas_positivas_dab_citoplasma_018_expansao3.groovy) · [Nomes antigos e novos](NOMES_DOS_SCRIPTS.md)

**Arquivo anterior:** `positive_cells.groovy`  
**Arquivo no acervo:** `celulas_positivas_dab_citoplasma_018_expansao3.groovy`

## O que faz

Define vetores H-DAB, seleciona todas as anotações e executa PositiveCellDetection.

## O que precisa

QuPath com uma imagem aberta. Anotações de região já existentes.

Os classificadores treinados e modelos externos não acompanham este pacote. Os nomes de classes/canais e medidas precisam corresponder aos recursos da imagem e do projeto.

## Parâmetros principais

Resolução 0.5 µm, área 10–400 µm², detecção 0.05, expansão 3 µm. Cytoplasm: DAB OD mean, limiar 0.18, singleThreshold=true.

## Como usar

Abra a imagem no projeto, prepare as regiões indicadas acima e abra o script no editor Groovy. Ajuste os parâmetros e nomes de recursos conforme a tarefa e execute na imagem atual. As alterações de anotações e detecções ficam na hierarquia; salve os dados da imagem para conservá-las no projeto.

## O que gera

Células, medidas e classes de positividade na imagem; sem CSV.

## Particularidades para reutilizar

O tipo de imagem não é definido explicitamente aqui. Prepare a imagem brightfield e anotações antes da execução. Difere dos outros presets DAB na resolução, expansão e limiares.
