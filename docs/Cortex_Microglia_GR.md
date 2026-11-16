# Cortex_Microglia_GR — variante sem máximo de área GR

[← README](../README.md) · [Código](../scripts/variants/Cortex_Microglia_GR.groovy)

## Por que esta variante permanece

Esta versão mantém GR a partir do mínimo solicitado de 45 µm², sem o filtro posterior que remove objetos maiores que 1500 µm² na análise principal. Essa escolha muda a população elegível, portanto o arquivo não foi tratado como redundância simples.

## Entradas e configuração

Projeto com imagem calibrada em µm, uma única anotação com nome ou classe `Cortex`, classificadores `IBA` e `GR_1` e as APIs ImageJ/JTS usadas no código. Cabeçalho declara QuPath 0.7.

| Parâmetro | Padrão |
| --- | --- |
| CORTEX_NAME / CORTEX_CLASS | Cortex / Cortex |
| IBA_CLASSIFIER / GR_CLASSIFIER | IBA / GR_1 |
| IBA_MIN_AREA / IBA_MAX_AREA | 65 / 2000 µm² |
| GR_MIN_AREA | 45 µm² |
| GR_MAX_AREA | Não existe neste script |
| MIN_HOLE_AREA | 0 |
| MIN_GR_OVERLAP_FRACTION | 0.20 |
| SOMA_OPENING_RADIUS_UM | 1.5 |
| SKELETON_PADDING | 3 pixels |
| REMOVE_PREVIOUS_OBJECTS | true |

## Uso e processamento

Prepare a ROI e classificadores, escolha esta variante quando a ausência do máximo GR fizer parte do protocolo e execute na imagem atual. Ela define FLUORESCENCE, remove Microglia/GR anteriores com centroide em Cortex, cria novas anotações e as nomeia/classifica como na [análise principal](complete_fast.md).

A associação compara cada GR às IBA, com uma verificação prévia de sobreposição dos retângulos envolventes, sem STRtree. Usa a maior fração `área(GR ∩ IBA)/área(GR)` e o limiar 0.20. Calcula as mesmas famílias de medidas individuais de forma, soma, esqueleto e associação, descritas no guia principal.

## Saídas e diferenças

```text
<projeto>/Microglia_Results/
├── microglia_all_cells.csv
└── microglia_summary.csv
```

São 17 colunas individuais e 29 no resumo. O resumo não tem os contadores do filtro máximo GR nem as quatro densidades de subpopulações da versão completa. O script grava medidas nas Microglia, mas não contém o bloco de medidas populacionais na anotação Cortex presente em `complete_fast`.

As linhas são acrescentadas a arquivos existentes, sem deduplicação ou verificação de cabeçalho. O mesmo destino era usado por outras versões excluídas com resumos de tamanhos diferentes. Use resultados separados. O script atualiza a interface, sem salvar explicitamente os dados da imagem; salve-os após a revisão se quiser persistir as anotações.

## Particularidades de uso

Aplicam-se os problemas de coordenadas/polaridade do esqueleto, aproximação para pixels anisotrópicos, interpretação por centroide, ausência de filtro explícito de classe positiva e associação apenas em X/Y descritos no [guia da análise principal](complete_fast.md). A validação da ROI de área é menos explícita que na principal.

Esta variante pode manter objetos GR maiores que 1500 µm², enquanto a principal os exclui. Mantenha os resultados separados conforme o critério escolhido.
