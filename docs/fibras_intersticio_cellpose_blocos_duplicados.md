# Fibras e interstício com blocos concatenados

[← README](../README.md) · [Script](../scripts/fibras/fibras_intersticio_cellpose_blocos_duplicados.groovy) · [Nomes antigos e novos](NOMES_DOS_SCRIPTS.md)

**Arquivo anterior:** `analysis.groovy`  
**Arquivo no acervo:** `fibras_intersticio_cellpose_blocos_duplicados.groovy`

## O que faz

Reúne dois blocos de segmentação de tecido/fibras, criação de interstício por subtração de fiber_mask e filtragem por forma. O trecho inicial usa cp_sam e o seguinte cyto3.

## O que precisa

QuPath com uma imagem aberta. Recursos do projeto: `muscle`, `fiber_mask`. Extensão Cellpose e ambiente/modelo correspondente configurados. Imagem vinculada a um projeto para os caminhos e nomes usados pelo script.

Os classificadores treinados e modelos externos não acompanham este pacote. Os nomes de classes/canais e medidas precisam corresponder aos recursos da imagem e do projeto.

## Parâmetros principais

muscle e fiber_mask; classe Positive para tecido e Fiber para fibras. pixelSize 1, Red, diâmetro 40; circularidade mínima 0.6 e aspect ratio máximo 2.5.

## Como usar

Abra a imagem no projeto, prepare as regiões indicadas acima e abra o script no editor Groovy. Ajuste os parâmetros e nomes de recursos conforme a tarefa e execute na imagem atual. As alterações de anotações e detecções ficam na hierarquia; salve os dados da imagem para conservá-las no projeto.

## O que gera

Pretende criar anotações Fiber e inter. Cria a pasta fiber_results, mas não contém exportação ativa de tabela.

## Particularidades para reutilizar

O arquivo repete declarações de variáveis no mesmo escopo, como minCircularidade, parentROIs, imageData e fibers. É uma composição de trechos que precisa ser separada antes de reutilizar como um único script. O campo radialThreshold é declarado, mas não há cálculo radial nesse arquivo. A conversão da geometria usa ROIs.createROI; confira a API ao adaptar.
