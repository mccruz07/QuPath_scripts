# Revisão, limitações e validação

[← README](../README.md)

## O que foi feito

Leitura dos oito scripts, comparação das cinco implementações de microglia, extração dos cabeçalhos CSV e verificação de integridade das cópias distribuídas. Não foram executados QuPath, classificadores ou imagens de teste. A escolha da versão principal é uma decisão de organização, não uma certificação de resultados ou desempenho.

## Pontos prioritários

| Ponto | Evidência no código | Ação antes do uso quantitativo |
| --- | --- | --- |
| Coordenadas do esqueleto | Análises de microglia passam x0,y0; Sholl passa -x0,-y0 | Testar máscara de célula longe da origem; revisar o deslocamento |
| Polaridade do esqueleto | Análises usam skeletonize() em máscara branca; Sholl usa skeletonize(255) | Inspecionar primeiro plano, pixels e comprimento |
| Versão | Cabeçalhos 0.7; origem em pasta v0.6 | Registrar versão real e testar APIs/classificadores |
| Classes | Sholl procura iba; análise cria Microglia | Ajustar CELL_CLASS antes de integrar |
| Limite GR | Variante não possui máximo de 1500 µm² | Escolher protocolo e manter resultados separados |
| CSVs | Mesmo nome de saída em variantes com diferentes cabeçalhos | Usar destino limpo; não misturar esquemas |
| Reexecução | CSVs de colágeno/microglia recebem append | Separar execuções ou deduplicar com identificação adequada |
| Classificadores | Modelos treinados não incluídos | Disponibilizar/validar modelos, canais e classes |
| Cópia Cortex | Mesmas coordenadas, plano padrão | Conferir alinhamento e plano em cada destino |
| Dados multiplanares | Associação microglia usa apenas X/Y | Restringir a um plano ou implementar controle Z/T |

## Fundamento da revisão de esqueleto

O código oficial do [QuPath IJTools](https://github.com/qupath/qupath/blob/main/qupath-core-processing/src/main/java/qupath/imagej/tools/IJTools.java) converte um recorte usando origens negativas derivadas de sua posição. Isso sustenta a suspeita de deslocamento nas chamadas com x0/y0 positivos dos scripts de microglia; a confirmação depende do teste com a versão instalada.

A [API oficial de ByteProcessor do ImageJ](https://imagej.net/ij/developer/api/ij/ij/process/ByteProcessor.html) oferece `skeletonize(int foreground)` com primeiro plano 0 ou 255. Por isso a polaridade deve ser explicitada ou testada quando a máscara e a contagem usam pixels brancos. Essas referências esclarecem os pontos de API; não validam os scripts completos. Consultadas em 16/11/2026.

## Plano mínimo de testes — ainda não executado

1. **Colágeno:** áreas conhecidas, ausência de tecido, metadados de medida sem red/green, nome com vírgula e reexecução. Conferir áreas, unidades e 11 colunas.
2. **Microglia principal:** limites de área, GR com 19/20/21% de sobreposição, empate entre duas células, nenhum objeto e falha na estimativa de soma. Conferir contagens, densidades, 17/35 colunas e medidas em Cortex.
3. **Variante:** repetir o caso com GR maior que 1500 µm² e conferir sua manutenção; validar 17/29 colunas.
4. **Esqueleto:** retângulo e formas ramificadas na origem e longe dela; inspecionar máscara, primeiro plano e medidas contra referência independente.
5. **Cortex:** projeto pequeno com referência, destino vazio e destino já anotado; conferir salvamento, duplicatas, dimensões/calibração diferentes e Z/T.
6. **Sholl:** formas com cruzamentos esperados, raios pequenos e truncados, isotropia/anisotropia, reexecução com soma existente e conferência visual dos QC.
7. **Migração:** comparar versões antiga e principal em cópias do mesmo projeto; casar células por geometria, não apenas por índice, e verificar divergências/empates.

## Registro de validação

```text
Data e responsável:
Script / hash / commit:
QuPath e sistema operacional:
Projeto / imagem / plano:
Classificadores e versões:
Calibração:
Parâmetros:
Resultado esperado:
Resultado observado:
Arquivos e figuras revisados:
Falhas, exclusões e decisão:
```

A presença de CSV ou mensagem de conclusão não prova correção científica. Registre também o número de células válidas por medida, pois falhas e exclusões de valores não finitos afetam médias.
