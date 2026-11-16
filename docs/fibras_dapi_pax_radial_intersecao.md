# Fibras e DAPI/PAX por interseção — muscle

[← README](../README.md) · [Script](../scripts/fibras/fibras_dapi_pax_radial_intersecao.groovy) · [Nomes antigos e novos](NOMES_DOS_SCRIPTS.md)

**Arquivo anterior:** `Final.groovy`  
**Arquivo no acervo:** `fibras_dapi_pax_radial_intersecao.groovy`

## O que faz

Cria tecido, remove Negative e filhos das Positive, detecta Fiber com Cellpose e filtra forma. Gera detecções DAPI/PAX e liga cada uma à primeira fibra cuja geometria intersecta o objeto. Calcula índice radial até a borda e classifica núcleos centrais/periféricos; resume contagens por fibra.

## O que precisa

QuPath com uma imagem aberta. Recursos do projeto: `muscle`, `nuclei_classification`. Extensão Cellpose e ambiente/modelo correspondente configurados. Imagem vinculada a um projeto para os caminhos e nomes usados pelo script.

Os classificadores treinados e modelos externos não acompanham este pacote. Os nomes de classes/canais e medidas precisam corresponder aos recursos da imagem e do projeto.

## Parâmetros principais

muscle e nuclei_classification (mínimo nuclear 15). cyto3, pixelSize 1, channels(1), diâmetro 40. Circularidade ≥0.6; aspect ratio ≤2.5; radialThreshold 0.7.

## Como usar

Abra a imagem no projeto, prepare as regiões indicadas acima e abra o script no editor Groovy. Ajuste os parâmetros e nomes de recursos conforme a tarefa e execute na imagem atual. As alterações de anotações e detecções ficam na hierarquia; salve os dados da imagem para conservá-las no projeto.

## O que gera

Medida Radial_Index nos núcleos, classes com _central/_peripheral e DAPI_total/PAX_total/DAPI_central/DAPI_peripheral/PAX_central/PAX_peripheral nas fibras. Exporta por saveAnnotationMeasurements para fiber_results/<imagem>_fibers.csv.

## Particularidades para reutilizar

O índice é distância centro→objeto dividida pela maior distância até a interseção da fibra com um raio de 10000 pixels. Objetos com centro exatamente coincidente são pulados nessa etapa. Não há escolha pela maior sobreposição: fica a primeira fibra que intersecta. Objetos não vinculados permanecem. As três variantes diferem apenas nos dois nomes de classificadores, mas mantêm caminhos de saída iguais; não execute todas esperando arquivos separados. A chamada de exportação não passa uma lista de colunas ou objetos; selecionar fibras antes dela não deve ser confundido com um filtro explícito no método de exportação.
