# QuPath scripts

Acervo de scripts Groovy para consulta e reutilização em análise de imagens no QuPath. Cada guia explica o que a rotina faz, quais recursos utiliza, seus parâmetros e o que gera.

**Repositório:** [mccruz07/QuPath_scripts](https://github.com/mccruz07/QuPath_scripts).

O pacote contém **39 scripts**: cinco da primeira coleção e 34 arquivos distintos da nova inclusão. Os 35 nomes novos recebidos estão mapeados na [tabela de nomes antigos e novos](docs/NOMES_DOS_SCRIPTS.md); uma duplicata exata de exportação de tiles foi consolidada. As diferenças de modelos, limiares e métodos foram preservadas em variantes com nomes descritivos.

## Como consultar e usar

1. Escolha a tarefa no catálogo e abra seu guia.
2. Abra a imagem no QuPath e prepare as regiões, classificadores e modelos indicados.
3. Abra o `.groovy` no editor de scripts e ajuste os parâmetros necessários.
4. Execute na imagem atual, observando os efeitos sobre objetos e arquivos descritos no guia.
5. Salve os dados da imagem para persistir as alterações na hierarquia. A exportação de CSV/TIFF é uma etapa separada e só ocorre nos scripts que a incluem.

As rotinas não formam uma sequência obrigatória. Várias começam apagando objetos ou substituindo resultados; outras trabalham com a seleção atual ou com todas as anotações. O guia de cada arquivo explica essas diferenças.

## Recursos necessários

- Classificadores de pixels/objetos com os nomes usados no código. Eles pertencem ao projeto e não acompanham este acervo.
- Para os scripts Cellpose, a extensão e o ambiente correspondente ao modelo usado. Os nomes `cyto3`, `cpsam` e `cp_sam` foram preservados como aparecem nos arquivos.
- Calibração e canais compatíveis com os parâmetros. Campos obtidos por `ROI.getArea()` representam área em pixels²; alguns scripts usam nomes de coluna que sugerem µm², conforme explicado nos guias.
- Projeto aberto para as rotinas que criam pastas de resultado ou usam o nome da entrada do projeto.

Opções comentadas nos templates não estão ativas. Renomear o arquivo do script não renomeia os classificadores ou os arquivos de saída definidos dentro dele.

## Catálogo da primeira coleção

| Script | Finalidade | Guia |
| --- | --- | --- |
| [audrei_pol.groovy](scripts/audrei_pol.groovy) | Colágeno total e classes red/green | [Colágeno](docs/audrei_pol.md) |
| [complete_fast.groovy](scripts/complete_fast.groovy) | Microglia IBA, associação GR e morfologia cortical | [Microglia](docs/complete_fast.md) |
| [Cortex_Microglia_GR.groovy](scripts/variants/Cortex_Microglia_GR.groovy) | Variante sem máximo de área GR | [Variante](docs/Cortex_Microglia_GR.md) |
| [draw_regions.groovy](scripts/draw_regions.groovy) | Copiar uma ROI Cortex para outras imagens do projeto | [Transferência](docs/draw_regions.md) |
| [Sholl_Microglia_v6.groovy](scripts/Sholl_Microglia_v6.groovy) | Curvas de Sholl e figuras dos esqueletos | [Sholl](docs/Sholl_Microglia_v6.md) |

Para usar Sholl após a análise principal, ajuste `CELL_CLASS` de `iba` para `Microglia`. `draw_regions` copia coordenadas, sem alinhar as imagens. A seleção das versões da primeira coleção está em [SELECAO](docs/SELECAO.md), e suas colunas de saída no [dicionário de dados](docs/DICIONARIO_DADOS.md).

## Adipócitos

| Script | O que faz | Guia |
| --- | --- | --- |
| [adipocitos_cellpose_sam_filtro_cor_cpu.groovy](scripts/adipocitos/adipocitos_cellpose_sam_filtro_cor_cpu.groovy) | Adipócitos com filtros de forma e cor — CPU | [Guia](docs/adipocitos_cellpose_sam_filtro_cor_cpu.md) |
| [adipocitos_cellpose_sam_filtro_cor_gpu.groovy](scripts/adipocitos/adipocitos_cellpose_sam_filtro_cor_gpu.groovy) | Adipócitos com medidas RGB e filtro de cor — GPU | [Guia](docs/adipocitos_cellpose_sam_filtro_cor_gpu.md) |

## Fibras e posição de núcleos

| Script | O que faz | Guia |
| --- | --- | --- |
| [fibras_cellpose_sam_imagem_inteira_escala_fixa.groovy](scripts/fibras/fibras_cellpose_sam_imagem_inteira_escala_fixa.groovy) | Fibras na imagem inteira com escala fixa | [Guia](docs/fibras_cellpose_sam_imagem_inteira_escala_fixa.md) |
| [fibras_intersticio_cellpose_blocos_duplicados.groovy](scripts/fibras/fibras_intersticio_cellpose_blocos_duplicados.groovy) | Fibras e interstício com blocos concatenados | [Guia](docs/fibras_intersticio_cellpose_blocos_duplicados.md) |
| [fibras_cellpose_cpsam_red_05um.groovy](scripts/fibras/fibras_cellpose_cpsam_red_05um.groovy) | Fibras com modelo cpsam no canal Red | [Guia](docs/fibras_cellpose_cpsam_red_05um.md) |
| [fibras_cellpose_cyto3_classificacao_tipo.groovy](scripts/fibras/fibras_cellpose_cyto3_classificacao_tipo.groovy) | Fibras com classificação de tipo | [Guia](docs/fibras_cellpose_cyto3_classificacao_tipo.md) |
| [fibras_cellpose_cyto3_anotacoes_selecionadas.groovy](scripts/fibras/fibras_cellpose_cyto3_anotacoes_selecionadas.groovy) | Cellpose cyto3 em anotações selecionadas | [Guia](docs/fibras_cellpose_cyto3_anotacoes_selecionadas.md) |
| [fibras_cellpose_cpsam_filtro_forma.groovy](scripts/fibras/fibras_cellpose_cpsam_filtro_forma.groovy) | Fibras cpsam com filtros de área e circularidade | [Guia](docs/fibras_cellpose_cpsam_filtro_forma.md) |
| [fibras_cellpose_sam_resumo_area_cpu.groovy](scripts/fibras/fibras_cellpose_sam_resumo_area_cpu.groovy) | Fibras com resumo de áreas e processamento em CPU | [Guia](docs/fibras_cellpose_sam_resumo_area_cpu.md) |
| [fibras_dapi_pax_radial_intersecao.groovy](scripts/fibras/fibras_dapi_pax_radial_intersecao.groovy) | Fibras e DAPI/PAX por interseção — muscle | [Guia](docs/fibras_dapi_pax_radial_intersecao.md) |
| [fibras_dapi_pax_radial_muscle2_nuclei3.groovy](scripts/fibras/fibras_dapi_pax_radial_muscle2_nuclei3.groovy) | Fibras e DAPI/PAX por interseção — muscle2 | [Guia](docs/fibras_dapi_pax_radial_muscle2_nuclei3.md) |
| [fibras_dapi_pax_radial_muscle_2_nuclei2.groovy](scripts/fibras/fibras_dapi_pax_radial_muscle_2_nuclei2.groovy) | Fibras e DAPI/PAX por interseção — muscle_2 | [Guia](docs/fibras_dapi_pax_radial_muscle_2_nuclei2.md) |
| [fibras_nucleos_centrais_perifericos_intersticio_he.groovy](scripts/fibras/fibras_nucleos_centrais_perifericos_intersticio_he.groovy) | Núcleos centrais, periféricos e intersticiais em H&E | [Guia](docs/fibras_nucleos_centrais_perifericos_intersticio_he.md) |
| [fibras_dapi_pax_radial_centroide_passos.groovy](scripts/fibras/fibras_dapi_pax_radial_centroide_passos.groovy) | Fibras e DAPI/PAX por centroide e percurso em pixels | [Guia](docs/fibras_dapi_pax_radial_centroide_passos.md) |

## Células imunes

| Script | O que faz | Guia |
| --- | --- | --- |
| [celulas_imunes_he_classificacao_duas_etapas.groovy](scripts/imunidade/celulas_imunes_he_classificacao_duas_etapas.groovy) | Células imunes em H&E com duas classificações | [Guia](docs/celulas_imunes_he_classificacao_duas_etapas.md) |
| [celulas_imunes_coracao_interfibra.groovy](scripts/imunidade/celulas_imunes_coracao_interfibra.groovy) | Células imunes em regiões de fibras cardíacas | [Guia](docs/celulas_imunes_coracao_interfibra.md) |
| [celulas_imunes_interfibra_filtro_foco.groovy](scripts/imunidade/celulas_imunes_interfibra_filtro_foco.groovy) | Células imunes com filtro de foco por tiles | [Guia](docs/celulas_imunes_interfibra_filtro_foco.md) |
| [celulas_imunes_filtro_foco_cellpose_fibras.groovy](scripts/imunidade/celulas_imunes_filtro_foco_cellpose_fibras.groovy) | Células imunes com foco e etapa Cellpose | [Guia](docs/celulas_imunes_filtro_foco_cellpose_fibras.md) |
| [celulas_imunes_filtro_foco_fibras_csa.groovy](scripts/imunidade/celulas_imunes_filtro_foco_fibras_csa.groovy) | Células imunes, foco e área transversal das fibras | [Guia](docs/celulas_imunes_filtro_foco_fibras_csa.md) |

## Colágeno e células isoladas

| Script | O que faz | Guia |
| --- | --- | --- |
| [colageno_total_regioes_muscle_picro.groovy](scripts/colageno/colageno_total_regioes_muscle_picro.groovy) | Colágeno total em regiões muscle_picro | [Guia](docs/colageno_total_regioes_muscle_picro.md) |
| [celulas_isoladas_colageno_pixel_classifier.groovy](scripts/colageno/celulas_isoladas_colageno_pixel_classifier.groovy) | Células isoladas e colágeno — collagen | [Guia](docs/celulas_isoladas_colageno_pixel_classifier.md) |
| [celulas_isoladas_colageno_masson.groovy](scripts/colageno/celulas_isoladas_colageno_masson.groovy) | Células isoladas e colágeno — masson_final | [Guia](docs/celulas_isoladas_colageno_masson.md) |

## Positividade DAB

| Script | O que faz | Guia |
| --- | --- | --- |
| [celulas_positivas_dab_citoplasma_010_expansao5.groovy](scripts/ihq/celulas_positivas_dab_citoplasma_010_expansao5.groovy) | Positividade DAB citoplasmática — limiar 0.10 | [Guia](docs/celulas_positivas_dab_citoplasma_010_expansao5.md) |
| [celulas_positivas_dab_citoplasma_020_expansao8.groovy](scripts/ihq/celulas_positivas_dab_citoplasma_020_expansao8.groovy) | Positividade DAB citoplasmática — limiar 0.20 | [Guia](docs/celulas_positivas_dab_citoplasma_020_expansao8.md) |
| [historico_ajustes_deteccao_positividade_dab.groovy](scripts/ihq/historico_ajustes_deteccao_positividade_dab.groovy) | Sequência gravada de ajustes de detecção DAB | [Guia](docs/historico_ajustes_deteccao_positividade_dab.md) |
| [celulas_positivas_dab_imagem_inteira_csv.groovy](scripts/ihq/celulas_positivas_dab_imagem_inteira_csv.groovy) | Positividade DAB na imagem inteira com resumo CSV | [Guia](docs/celulas_positivas_dab_imagem_inteira_csv.md) |
| [celulas_positivas_dab_citoplasma_018_expansao3.groovy](scripts/ihq/celulas_positivas_dab_citoplasma_018_expansao3.groovy) | Positividade DAB citoplasmática — limiar 0.18 | [Guia](docs/celulas_positivas_dab_citoplasma_018_expansao3.md) |

## Regiões, amostragem e recortes

| Script | O que faz | Guia |
| --- | --- | --- |
| [exportar_tiles_320um_tecido_retangulo_fixo.groovy](scripts/regioes/exportar_tiles_320um_tecido_retangulo_fixo.groovy) | Tiles de 320 µm e exportação TIFF | [Guia](docs/exportar_tiles_320um_tecido_retangulo_fixo.md) |
| [rois_aleatorias_20porcento_exportacao_tecido.groovy](scripts/regioes/rois_aleatorias_20porcento_exportacao_tecido.groovy) | ROIs aleatórias para 20% do tecido e exportação | [Guia](docs/rois_aleatorias_20porcento_exportacao_tecido.md) |
| [retangulos_aleatorios_calibrados_semente_fixa.groovy](scripts/regioes/retangulos_aleatorios_calibrados_semente_fixa.groovy) | Retângulos aleatórios com dimensões físicas e semente fixa | [Guia](docs/retangulos_aleatorios_calibrados_semente_fixa.md) |
| [criar_rois_quadradas_linha_central.groovy](scripts/regioes/criar_rois_quadradas_linha_central.groovy) | Criar ROIs quadradas em uma linha central | [Guia](docs/criar_rois_quadradas_linha_central.md) |

## Segmentação por classificadores e Cellpose

| Script | O que faz | Guia |
| --- | --- | --- |
| [anotacoes_pixel_classifier_c4d.groovy](scripts/classificadores/anotacoes_pixel_classifier_c4d.groovy) | Anotações pelo classificador c4d_final | [Guia](docs/anotacoes_pixel_classifier_c4d.md) |
| [celulas_cellpose_cpsam_criptas_dilatadas.groovy](scripts/classificadores/celulas_cellpose_cpsam_criptas_dilatadas.groovy) | Cellpose cpsam em criptas dilatadas | [Guia](docs/celulas_cellpose_cpsam_criptas_dilatadas.md) |
| [celulas_cellpose_cpsam_tecido.groovy](scripts/classificadores/celulas_cellpose_cpsam_tecido.groovy) | Cellpose cpsam em tecido | [Guia](docs/celulas_cellpose_cpsam_tecido.md) |

## Organização

```text
QuPath_scripts/
├── README.md
├── scripts/
│   ├── arquivos da primeira coleção
│   ├── variants/
│   ├── adipocitos/
│   ├── fibras/
│   ├── imunidade/
│   ├── colageno/
│   ├── ihq/
│   ├── regioes/
│   └── classificadores/
└── docs/
    ├── um guia por script
    ├── NOMES_DOS_SCRIPTS.md
    ├── SELECAO.md
    ├── DICIONARIO_DADOS.md
    └── MANIFESTO.json
```

## Autoria e licença

Os comentários e atribuições dos códigos foram preservados. Alguns arquivos derivados de templates Cellpose citam Olivier Burri; isso não atribui automaticamente todas as adaptações ao autor do template. Os nomes de pessoas nos arquivos antigos foram mantidos na tabela de correspondência como contexto, sem inferir autoria.

Uma licença não foi definida nesta organização do acervo. Registre a autoria e a licença desejada antes de conceder direitos de uso e redistribuição. Para localizar uma versão usada anteriormente, o manifesto relaciona nomes originais, destinos e hashes dos arquivos.
