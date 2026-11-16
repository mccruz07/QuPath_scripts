# Positividade DAB citoplasmática — limiar 0.20

[← README](../README.md) · [Script](../scripts/ihq/celulas_positivas_dab_citoplasma_020_expansao8.groovy) · [Nomes antigos e novos](NOMES_DOS_SCRIPTS.md)

**Arquivo anterior:** `Milena (Lab Lourdes fev.2026).groovy`  
**Arquivo no acervo:** `celulas_positivas_dab_citoplasma_020_expansao8.groovy`

## O que faz

Seleciona todas as anotações e executa PositiveCellDetection, usando hematoxilina para detectar e DAB citoplasmático para classificar.

## O que precisa

QuPath com uma imagem aberta. Anotações de região já existentes.

Os classificadores treinados e modelos externos não acompanham este pacote. Os nomes de classes/canais e medidas precisam corresponder aos recursos da imagem e do projeto.

## Parâmetros principais

Resolução 1.0 µm, área 5–400 µm², limiar de detecção 0.05, expansão 8 µm. Cytoplasm: DAB OD mean ≥0.2; singleThreshold=true.

## Como usar

Abra a imagem no projeto, prepare as regiões indicadas acima e abra o script no editor Groovy. Ajuste os parâmetros e nomes de recursos conforme a tarefa e execute na imagem atual. As alterações de anotações e detecções ficam na hierarquia; salve os dados da imagem para conservá-las no projeto.

## O que gera

Células e medidas na imagem atual, sem CSV.

## Particularidades para reutilizar

Não define tipo de imagem ou vetores de cor. Prepare H-DAB e as anotações antes de executar. O nome antigo registra uma sessão de fevereiro de 2026, preservada na tabela de nomes.
