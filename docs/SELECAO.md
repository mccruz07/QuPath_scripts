# Seleção e redução de redundância

[← README](../README.md)

## Critério

Comparação de configuração, segmentação, associação GR→IBA, medidas e exportação. Redundância aqui significa sobreposição funcional para manter uma coleção principal enxuta; **não significa identidade de arquivos nem equivalência numérica comprovada**. Nenhum original foi apagado da pasta de origem.

| Arquivo recebido | Decisão | Motivo / diferença relevante |
| --- | --- | --- |
| audrei_pol.groovy | Manter | Fluxo próprio de colágeno |
| complete_fast.groovy | Manter como principal | Índice espacial, medidas individuais, medidas na ROI Cortex e densidades de subpopulações |
| complete_normal.groovy | Retirar do pacote | Mesmo conjunto de cabeçalhos CSV da principal; busca GR×IBA sem STRtree e nomes de saída sem FAST |
| cortex_glia_gr_analysis.groovy | Retirar do pacote | Mesmo núcleo de análise, com menos saídas: sem medidas populacionais gravadas em Cortex e sem quatro densidades de subpopulações no resumo |
| cortex_glia_gr_fast.groovy | Retirar do pacote | Variante com STRtree e saídas mais restritas que complete_fast; validações e contagem de falhas também diferem |
| Cortex_Microglia_GR.groovy | Manter em scripts/variants | Não aplica GR_MAX_AREA; pode incluir objetos GR maiores que 1500 µm², alterando a população analisada |
| draw_regions.groovy | Manter | Preparação de ROI em várias imagens |
| Sholl_Microglia_v6.groovy | Manter | Curvas radiais e figuras, ausentes nas análises de morfologia |

## Por que a variante sem máximo foi preservada

`Cortex_Microglia_GR` tem mínimo GR de 45 µm², mas não possui a exclusão posterior por máximo de 1500 µm² presente em `complete_fast`. Essa diferença pode mudar contagens, associações e médias. Foi mantida para não descartar silenciosamente um critério analítico distinto. Sua preservação não implica preferência científica ou validação.

## Diferenças ao migrar para complete_fast

- A versão principal usa `Microglia_Results_FAST/`; as versões normais usam `Microglia_Results/`.
- `complete_fast` e `cortex_glia_gr_fast` usam os mesmos nomes de arquivos, mas seus resumos têm **35 e 31 colunas**, respectivamente. Não acrescente resultados da principal a um CSV criado pela versão reduzida: use um destino limpo ou separe os resultados antigos antes da execução.
- O CSV individual possui 17 colunas nas cinco análises comparadas.
- `complete_normal` também tem 35 colunas no resumo; `cortex_glia_gr_analysis` tem 31; `Cortex_Microglia_GR` tem 29, pois não exporta os dois contadores do filtro máximo de GR.
- Na associação, o melhor candidato só muda com fração estritamente maior. Em empates, a ordem de visita pode afetar qual célula recebe o GR. A ordem de um índice espacial não deve ser presumida igual à busca sequencial.
- A versão FAST filtra geometrias inválidas antes de certos contadores; os ramos de falha e validações não são idênticos entre versões.

A seleção reduz três arquivos distribuídos, sem afirmar que reanalisar um experimento antigo será numericamente idêntico. Para reproduzir análises anteriores, use o original e os parâmetros correspondentes, preservados pelo proprietário fora deste pacote.
