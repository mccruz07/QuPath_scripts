# Dicionário de dados — primeira coleção

[← README](../README.md)

## Convenções

CSV com vírgula e ponto decimal. Microglia usa UTF-8 e campos vazios para valores não finitos; colágeno usa o escritor padrão do ambiente e `NA` para razões indefinidas; Sholl explicita UTF-8. Os nomes abaixo foram extraídos dos cabeçalhos do código distribuído.

## Microglia — arquivo individual

Uma linha por célula processada. `Microglia_ID` é índice da execução, não o UUID da anotação.

| Coluna | Significado |
| --- | --- |
| `Image` | Nome da imagem no projeto |
| `Microglia_ID` | Índice sequencial da microglia |
| `IBA_Area_um2` | Área da geometria IBA, µm² |
| `Perimeter_um` | Perímetro aproximado, µm |
| `Circularity` | 4πA/P² |
| `Convex_Hull_Area_um2` | Área do envoltório convexo, µm² |
| `Convex_Hull_Perimeter_um` | Perímetro do envoltório convexo, µm |
| `Solidity` | Área / área convexa |
| `Roughness` | Perímetro / perímetro convexo |
| `Shape_Complexity` | P²/(4πA) |
| `Estimated_Soma_Area_um2` | Área do soma estimado por abertura, µm² |
| `Soma_Fraction` | Área soma / área IBA |
| `Skeleton_Length_um` | Comprimento estimado por ligações do esqueleto, µm |
| `Skeleton_Density_um-1` | Comprimento do esqueleto / área IBA, µm⁻¹ |
| `GR_Positive` | 1 se houver GR associado, senão 0 |
| `Associated_GR_Count` | Número de objetos GR atribuídos à célula |
| `Max_GR_Fraction_Inside_IBA` | Maior área de interseção / área GR entre os associados; 0 se nenhum |

## Microglia — resumo

`IBA_GR_Count` conta células IBA com pelo menos um GR; `GR_with_IBA_Count` conta objetos GR associados. Esses números podem ser diferentes. `IBA_Only` e `GR_Only` indicam ausência da associação segundo o limiar, não ausência de sinal biológico. Densidades usam área de Cortex em mm²; percentuais usam o denominador indicado no nome; `IBA_GR_Ratio` é total IBA / total GR. Contadores `Before_Max_Filter` ocorrem após a geração com área mínima, não representam todos os pixels positivos. Médias `Mean_*` usam apenas os valores finitos da medida correspondente.

| Ordem | complete_fast (35 colunas) |
| --- | --- |
| 1 | `Image` |
| 2 | `ROI_Area_mm2` |
| 3 | `IBA_Detected_Before_Max_Filter` |
| 4 | `IBA_Excluded_Above_Max_Area` |
| 5 | `Microglia_Count` |
| 6 | `Microglia_Density_cells_mm2` |
| 7 | `GR_Detected_Before_Max_Filter` |
| 8 | `GR_Excluded_Above_Max_Area` |
| 9 | `GR_Count` |
| 10 | `GR_Density_cells_mm2` |
| 11 | `IBA_GR_Ratio` |
| 12 | `IBA_GR_Count` |
| 13 | `IBA_GR_Density_cells_mm2` |
| 14 | `IBA_GR_pct_of_IBA` |
| 15 | `IBA_Only_Count` |
| 16 | `IBA_Only_Density_cells_mm2` |
| 17 | `IBA_Only_pct_of_IBA` |
| 18 | `GR_with_IBA_Count` |
| 19 | `GR_with_IBA_Density_cells_mm2` |
| 20 | `GR_with_IBA_pct_of_GR` |
| 21 | `GR_Only_Count` |
| 22 | `GR_Only_Density_cells_mm2` |
| 23 | `GR_Only_pct_of_GR` |
| 24 | `Mean_IBA_Area_um2` |
| 25 | `Mean_Perimeter_um` |
| 26 | `Mean_Circularity` |
| 27 | `Mean_Convex_Hull_Area_um2` |
| 28 | `Mean_Convex_Hull_Perimeter_um` |
| 29 | `Mean_Solidity` |
| 30 | `Mean_Roughness` |
| 31 | `Mean_Shape_Complexity` |
| 32 | `Mean_Estimated_Soma_Area_um2` |
| 33 | `Mean_Soma_Fraction` |
| 34 | `Mean_Skeleton_Length_um` |
| 35 | `Mean_Skeleton_Density_um-1` |

A variante `Cortex_Microglia_GR` usa estas 29 colunas, nesta ordem:

```text
Image,ROI_Area_mm2,IBA_Detected_Before_Max_Filter,IBA_Excluded_Above_Max_Area,Microglia_Count,Microglia_Density_cells_mm2,GR_Count,GR_Density_cells_mm2,IBA_GR_Ratio,IBA_GR_Count,IBA_GR_pct_of_IBA,IBA_Only_Count,IBA_Only_pct_of_IBA,GR_with_IBA_Count,GR_with_IBA_pct_of_GR,GR_Only_Count,GR_Only_pct_of_GR,Mean_IBA_Area_um2,Mean_Perimeter_um,Mean_Circularity,Mean_Convex_Hull_Area_um2,Mean_Convex_Hull_Perimeter_um,Mean_Solidity,Mean_Roughness,Mean_Shape_Complexity,Mean_Estimated_Soma_Area_um2,Mean_Soma_Fraction,Mean_Skeleton_Length_um,Mean_Skeleton_Density_um-1
```

## Colágeno

| Coluna | Significado |
| --- | --- |
| Image_Name | Nome no servidor de imagem |
| Unit | um^2 ou px^2, conforme calibração das áreas de ROI |
| Area_Tissue | Soma de áreas tissue |
| Area_Positive_ColTotal | Soma de áreas Positive |
| Pct_ColTotal_in_Tissue | 100 × Positive / tissue |
| Area_Red / Area_Green | Soma das medidas selecionadas por nome; conferir unidade |
| Pct_Red_in_Positive / Pct_Green_in_Positive | 100 × área da classe / Positive |
| Ratio_Red_to_Green / Ratio_Green_to_Red | Razão entre áreas; NA se denominador zero |

## Sholl

| Arquivo | Colunas em ordem |
| --- | --- |
| sholl_curves.csv | Image, ObjectID, ObjectName, Radius_um, Intersections |
| sholl_summary.csv | Image, ObjectID, ObjectName, Status, Soma_area_um2, Center_X_px, Center_Y_px, Max_intersections, Radius_at_max_um, Max_nonzero_radius_um, AUC_sampled, Notes |
| sholl_skeleton_colors.csv | Image, ObjectID, ObjectName, Color_hex |

`ObjectID` é o identificador do objeto, `ObjectName` pode estar vazio. `Status` é OK ou FAILED. O centro está em pixels globais; raios estão em µm. `Max_nonzero_radius_um` fica vazio quando não há interseções. `AUC_sampled` é a integral trapezoidal apenas entre os raios amostrados, em interseções × µm. `Notes` sinaliza curva truncada ou registra erro. `Color_hex` relaciona cada célula à cor na figura de esqueletos.

Quando todas as contagens são zero, o pico também é zero e o código ainda registra o primeiro raio como `Radius_at_max_um`; isso não indica um pico biológico nesse raio.
