# Fibras e DAPI/PAX por centroide e percurso em pixels

[← README](../README.md) · [Script](../scripts/fibras/fibras_dapi_pax_radial_centroide_passos.groovy) · [Nomes antigos e novos](NOMES_DOS_SCRIPTS.md)

**Arquivo anterior:** `nova.groovy`  
**Arquivo no acervo:** `fibras_dapi_pax_radial_centroide_passos.groovy`

## O que faz

Segmenta tecido/fibras, filtra forma, cria DAPI/PAX e associa por centroide contido na fibra. Remove detecções fora da lista DAPI/PAX e objetos sem fibra. Estima a distância até a borda avançando de um em um pixel e calcula Radial_Index_Real.

## O que precisa

QuPath com uma imagem aberta. Recursos do projeto: `muscle`, `nuclei_classification`. Extensão Cellpose e ambiente/modelo correspondente configurados. Imagem vinculada a um projeto para os caminhos e nomes usados pelo script.

Os classificadores treinados e modelos externos não acompanham este pacote. Os nomes de classes/canais e medidas precisam corresponder aos recursos da imagem e do projeto.

## Parâmetros principais

muscle e nuclei_classification, mínimo nuclear 0; cyto3, pixelSize 1, channels(1), diâmetro 40; circularidade 0.6, aspect ratio 2.5, limiar radial 0.7.

## Como usar

Abra a imagem no projeto, prepare as regiões indicadas acima e abra o script no editor Groovy. Ajuste os parâmetros e nomes de recursos conforme a tarefa e execute na imagem atual. As alterações de anotações e detecções ficam na hierarquia; salve os dados da imagem para conservá-las no projeto.

## O que gera

Classes terminadas em _Central/_Peripheral e contagens por fibra. O trecho final pretende exportar <imagem>_fibers.csv.

## Particularidades para reutilizar

outputDir é usado sem definição no arquivo: configure-o para a exportação. A coleta inicial usa classes dapi/pax minúsculas, mas as contagens finais procuram DAPI/PAX maiúsculos, podendo gerar zeros. O filtro procura a chave Circularidade e usa valor padrão 1.0. O comentário final diz sem filtro, embora o filtro esteja ativo. Essa variante é diferente das que usam interseção de geometrias.
