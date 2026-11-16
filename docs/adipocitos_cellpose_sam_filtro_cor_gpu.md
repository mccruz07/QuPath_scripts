# Adipócitos com medidas RGB e filtro de cor — GPU

[← README](../README.md) · [Script](../scripts/adipocitos/adipocitos_cellpose_sam_filtro_cor_gpu.groovy) · [Nomes antigos e novos](NOMES_DOS_SCRIPTS.md)

**Arquivo anterior:** `adipocitos.groovy`  
**Arquivo no acervo:** `adipocitos_cellpose_sam_filtro_cor_gpu.groovy`

## O que faz

Limpa todos os objetos, cria tecido Positive usando adipo e segmenta Adipo por Cellpose-SAM. Registra médias RGB, saturação e brilho em cada adipócito antes de filtrar e gerar estatísticas de área.

## O que precisa

QuPath com uma imagem aberta. Recursos do projeto: `adipo`. Extensão Cellpose e ambiente/modelo correspondente configurados. Imagem vinculada a um projeto para os caminhos e nomes usados pelo script.

Os classificadores treinados e modelos externos não acompanham este pacote. Os nomes de classes/canais e medidas precisam corresponder aos recursos da imagem e do projeto.

## Parâmetros principais

Área 400–8000; circularidade ≥0.35; saturação ≤0.20; brilho ≥0.60. adipo: área mínima 1000, buracos 0. cpsam, Red, pixelSize 1.0, GPU ativada.

## Como usar

Abra a imagem no projeto, prepare as regiões indicadas acima e abra o script no editor Groovy. Ajuste os parâmetros e nomes de recursos conforme a tarefa e execute na imagem atual. As alterações de anotações e detecções ficam na hierarquia; salve os dados da imagem para conservá-las no projeto.

## O que gera

Acrescenta linhas em adipo_results/consolidado_adipocytes.csv e medidas resumidas na primeira Positive; medidas individuais Adipo_Mean_Red/Green/Blue/Saturation/Brightness.

## Particularidades para reutilizar

Difere da versão CPU no uso de GPU, nas medidas individuais gravadas e na contagem de exclusões. Rejected_Missing_Color é incrementado antes dos outros filtros e pode sobrepor-se aos demais motivos: não some esses contadores como categorias exclusivas. A área do tecido usa pixels² apesar do sufixo um2. Ambas as variantes escrevem no mesmo CSV; escolha a variante antes de processar.
