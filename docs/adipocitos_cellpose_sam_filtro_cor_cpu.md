# Adipócitos com filtros de forma e cor — CPU

[← README](../README.md) · [Script](../scripts/adipocitos/adipocitos_cellpose_sam_filtro_cor_cpu.groovy) · [Nomes antigos e novos](NOMES_DOS_SCRIPTS.md)

**Arquivo anterior:** `adipocitos_final.groovy`  
**Arquivo no acervo:** `adipocitos_cellpose_sam_filtro_cor_cpu.groovy`

## O que faz

Limpa todos os objetos, segmenta tecido com adipo, seleciona Positive e cria anotações Adipo com Cellpose-SAM. Mede forma e médias RGB, remove objetos por área, circularidade ou cor e calcula contagem, área total, média, mínimo, máximo e desvio padrão amostral.

## O que precisa

QuPath com uma imagem aberta. Recursos do projeto: `adipo`. Extensão Cellpose e ambiente/modelo correspondente configurados. Imagem vinculada a um projeto para os caminhos e nomes usados pelo script.

Os classificadores treinados e modelos externos não acompanham este pacote. Os nomes de classes/canais e medidas precisam corresponder aos recursos da imagem e do projeto.

## Parâmetros principais

Área 400–8000; circularidade mínima 0.35; saturação máxima 0.20; brilho mínimo 0.60. Classificador adipo: mínimo 1000. Modelo cpsam, Red, pixelSize 1.0, GPU desativada.

## Como usar

Abra a imagem no projeto, prepare as regiões indicadas acima e abra o script no editor Groovy. Ajuste os parâmetros e nomes de recursos conforme a tarefa e execute na imagem atual. As alterações de anotações e detecções ficam na hierarquia; salve os dados da imagem para conservá-las no projeto.

## O que gera

Acrescenta uma linha por imagem em adipo_results/consolidado_adipocytes.csv. Grava resumo na primeira anotação Positive. Colunas incluem contagens totais/filtradas, áreas e motivos de exclusão.

## Particularidades para reutilizar

A área do tecido é obtida por ROI.getArea(), em pixels², embora o campo seja chamado Area_Total_Tecido_um2. As áreas dos adipócitos podem vir de medidas calibradas ou de pixels; ajuste as unidades antes de comparar. Os motivos de exclusão são mutuamente exclusivos nesta variante. Brilho é a média RGB, dividida por 255 quando maior que 1.5; saturação é (máximo−mínimo)/máximo. Reexecutar acrescenta outra linha ao mesmo CSV.
