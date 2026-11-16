# complete_fast — Cortex, microglia IBA e GR

[← README](../README.md) · [Código](../scripts/complete_fast.groovy)

## Finalidade e requisitos

Criar anotações a partir dos classificadores IBA e GR, filtrar por área, associar cada GR à microglia com maior sobreposição e calcular morfologia individual e estatísticas da região Cortex. A busca usa um índice espacial STRtree.

Exige imagem aberta em projeto, calibração em micrômetros, exatamente uma anotação com nome **ou** classe `Cortex`, ROI de área válida e classificadores `IBA` e `GR_1`. O cabeçalho declara QuPath 0.7; compatibilidade não foi testada. A análise é de geometrias 2D e não implementa reconstrução ou associação 3D.

## Parâmetros no início do script

| Constante | Padrão | Significado |
| --- | --- | --- |
| CORTEX_NAME / CORTEX_CLASS | Cortex / Cortex | Identificação da região |
| IBA_CLASSIFIER / GR_CLASSIFIER | IBA / GR_1 | Nomes dos classificadores |
| IBA_MIN_AREA / IBA_MAX_AREA | 65 / 2000 | Mínimo solicitado ao classificador e máximo posterior de IBA, em µm² com calibração |
| GR_MIN_AREA / GR_MAX_AREA | 45 / 1500 | Mínimo solicitado e máximo posterior de GR, em µm² com calibração |
| MIN_HOLE_AREA | 0 | Parâmetro de buracos na criação de anotações |
| MIN_GR_OVERLAP_FRACTION | 0.20 | Fração mínima de área do GR sobreposta à IBA |
| SOMA_OPENING_RADIUS_UM | 1.5 | Raio da abertura morfológica para estimar soma |
| SKELETON_PADDING | 3 | Margem do recorte binário em pixels |
| REMOVE_PREVIOUS_OBJECTS | true | Remove Microglia/GR anteriores com centroide dentro de Cortex |

Os valores são parâmetros do arquivo recebido, não limiares biológicos validados ou universais.

## Fluxo e uso

1. Abra uma imagem de teste, confira a ROI e os classificadores.
2. Ajuste as constantes; execute somente esta análise ou a variante, conforme o protocolo.
3. A imagem é definida como FLUORESCENCE e os objetos anteriores são removidos conforme a configuração.
4. O script cria anotações em Cortex, mantém as novas com centroide dentro da ROI e exclui as maiores que os máximos.
5. Nomeia as microglias `IBA_1`, `IBA_2` etc., classe `Microglia`, e os objetos GR `GR_1`, `GR_2` etc., classe `GR`.
6. Associa GR, mede as ROIs, acrescenta CSVs e atualiza a interface.
7. Revise os resultados e salve os dados da imagem para persistir os objetos e medidas. Não há chamada explícita de `saveImageData` neste arquivo.

A filtragem das novas anotações não verifica uma classe positiva específica retornada pelos classificadores. Verifique se eles geram apenas as classes que devem ser contadas. O critério de centroide não garante contenção integral; não há recorte adicional explícito das geometrias à Cortex nessa seleção.

## Associação GR→IBA

Para cada GR, calcula `área(GR ∩ IBA) / área(GR)` nos candidatos espaciais. Associa ao candidato de maior fração se ela for pelo menos 0.20. Cada GR recebe no máximo uma IBA; uma IBA pode receber vários GR. A microglia é `GR_Positive=1` quando tem ao menos um GR associado.

O denominador é a área de GR, não a área de IBA nem a união. A medida indica associação geométrica conforme o limiar; não é uma correlação de intensidades. Empates de fração usam o primeiro candidato visitado, sem desempate estável por ID.

## Medidas

- Área e perímetro da ROI IBA; área e perímetro do envoltório convexo.
- Circularidade `4πA/P²`; solidez `A/A_convexa`; rugosidade `P/P_convexo`; complexidade `P²/(4πA)`.
- Soma estimado por erosão seguida de dilatação, retendo o maior polígono; fração soma/IBA. Essa estimativa não é uma segmentação independente do corpo celular.
- Comprimento do esqueleto por soma de ligações horizontais, verticais e diagonais; densidade = comprimento/área IBA.
- Contagem de GR associados e maior fração de sobreposição entre os associados.
- Em Cortex: contagens, densidades por mm², percentuais das subpopulações, razão IBA/GR e médias morfológicas. As médias excluem valores não finitos, podendo usar quantidades diferentes de células por medida.

## Saídas e reexecução

```text
<projeto>/Microglia_Results_FAST/
├── microglia_all_cells_FAST.csv
└── microglia_summary_FAST.csv
```

CSV individual: 17 colunas, uma linha por microglia. Resumo: 35 colunas, uma linha por execução/imagem. O [dicionário](DICIONARIO_DADOS.md) lista todas as colunas. Valores não finitos são gravados como campos vazios. Os arquivos recebem novas linhas; não há remoção dos resultados anteriores nem identificador de execução.

Os IDs do CSV são sequenciais nessa execução, não UUIDs persistentes. **Não reutilize o resumo da antiga `cortex_glia_gr_fast`**, que usa o mesmo nome com apenas 31 colunas. O script só verifica a existência do arquivo, não a compatibilidade do cabeçalho.

## Limitações prioritárias

- A conversão da ROI para o recorte passa `x0,y0` a `convertToIJRoi`, enquanto o Sholl fornecido usa `-x0,-y0`. É necessário testar máscaras fora da origem; há risco de deslocamento e máscara vazia. Consulte [validação e fontes](VALIDACAO.md).
- `skeletonize()` é chamado sem explicitar a polaridade; o código preenche a célula com 255 e depois mede pixels não zero. Valide o primeiro plano e o esqueleto antes de usar comprimento/densidade.
- Perímetros e raio do soma usam a média de tamanho dos pixels; pixels anisotrópicos não recebem uma transformação geométrica física adequada nessas etapas.
- As ROIs são tratadas em X/Y; a associação não confere se GR e IBA pertencem ao mesmo plano Z/T. Use dados de um único plano ou revise o algoritmo para dados multiplanares.
- Não há teto de memória para as máscaras das células. Uma ROI extensa ou geometria problemática pode interromper o processamento fora dos blocos locais de tratamento de erro.
- Remover objetos anteriores não limpa os CSVs; desativar a remoção pode acumular objetos e não implementa deduplicação.

## Teste sugerido

Comece com poucas ROIs conhecidas, GR sobreposto a 19%, 20% e 21%, um GR sobre duas IBA e objetos acima/abaixo dos limites. Compare contagens e fórmulas manualmente. Teste uma célula longe da origem, confira máscara e esqueleto e examine o caso sem células. Registre a versão do QuPath, os classificadores e os parâmetros. Nenhuma validação experimental foi realizada nesta entrega.
