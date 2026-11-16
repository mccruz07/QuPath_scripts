# Sholl_Microglia_v6 — análise piloto de Sholl

[← README](../README.md) · [Código](../scripts/Sholl_Microglia_v6.groovy)

## Objetivo e pré-requisitos

Estimar o soma, produzir um esqueleto 2D por célula e contar cruzamentos de suas arestas com círculos concêntricos. Exporta curvas, resumo e figuras de controle de qualidade. Requer imagem de projeto, anotações celulares com ROI e calibração positiva em micrômetros. Recusa anisotropia relativa maior que 1%.

Cabeçalho declara QuPath 0.7. O script é identificado como piloto no cabeçalho.

## Parâmetros

| Constante | Padrão | Uso |
| --- | --- | --- |
| CELL_CLASS | iba | Classe exata das anotações de entrada |
| SOMA_RADIUS_UM | 1.2 | Raio de abertura morfológica |
| STEP_UM | 2.0 | Passo radial em µm |
| MAX_RADIUS_UM | 100.0 | Maior raio solicitado em µm |
| MAX_CELLS | 50 | Máximo de células; 0 usa todas |
| MAX_QC | 50 | Gera QC quando o índice da célula é menor que esse limite |
| CREATE_SOMA_ANNOTATIONS | true | Acrescenta anotações de soma |
| CREATE_CENTER_ANNOTATIONS | true | Acrescenta centro ao criar novo soma |
| PAD | 4 | Margem em pixels da máscara |

**Integração:** `complete_fast` cria a classe `Microglia`. Para usar sua saída, ajuste `CELL_CLASS` de `iba` para `Microglia`; a comparação diferencia maiúsculas. A seleção percorre anotações da imagem inteira, sem limitar automaticamente à ROI Cortex. As primeiras 50 da coleção não são uma amostra aleatória.

## Como usar

1. Gere e revise as segmentações celulares; confira a classe.
2. Ajuste os parâmetros e execute na imagem atual.
3. Verifique o autoteste de ROI/polaridade no Log. Ele testa um retângulo de 96 pixels e a redução da máscara após esqueletonização.
4. Examine os status por célula, curvas e figuras QC.
5. Se desejar persistir as anotações acrescentadas, salve os dados da imagem; os arquivos exportados são gravados diretamente, mas não há `saveImageData` no script.

## Método implementado

A abertura usa erosão e dilatação, intersecta o resultado com a geometria original e escolhe o maior polígono. O centro é um **ponto interior do soma**, não necessariamente seu centroide. O soma desta rotina pode diferir daquele da análise principal: raio padrão 1.2 versus 1.5 µm, além da interseção explícita com a ROI.

A ROI é convertida em máscara local com deslocamentos `-x0,-y0`, esqueletonizada com primeiro plano 255. Para quatro direções de vizinhança, cada ligação é visitada uma vez. Uma transição entre distâncias menor e maior/igual ao raio conta como cruzamento. Vértices exatamente no círculo são classificados como fora. É uma medida discreta por arestas; não equivale a contar ramos independentes nem implementa interseção analítica contínua segmento–círculo em todos os casos.

Os raios vão de STEP_UM até `floor(MAX_RADIUS_UM/STEP_UM) × STEP_UM`, sem amostra em zero. A AUC usa trapézios entre esses raios amostrados; não inclui o intervalo de zero até o primeiro raio. O primeiro raio do pico é escolhido em caso de empate. Quando a última amostra tem cruzamentos, a linha recebe `CURVA_TRUNCADA_RAIO_MAXIMO`.

## Saídas

Cada execução cria `<projeto>/Sholl_Pilot/<yyyyMMdd_HHmmss_SSS>/`:

| Arquivo | Conteúdo |
| --- | --- |
| sholl_curves.csv | Uma linha por raio/célula calculada |
| sholl_summary.csv | Status, soma, centro, pico, raio máximo não zero, AUC e notas |
| sholl_all_curves.png | Curvas individuais cinzas e média vermelha, quando há curvas |
| sholl_skeletons_overlay.png | Esqueletos centralizados pelo soma, orientações preservadas, quando disponíveis |
| sholl_skeleton_colors.csv | Correspondência objeto/cor da sobreposição |
| QC/cell_0001.png etc. | Máscara e elementos de controle por índice elegível |

Nas figuras QC: cinza = máscara, verde = contorno do soma, magenta = esqueleto, ciano = centro e azul = círculos. **Não são imagens originais de microscopia.** A sobreposição não normaliza rotações nem tamanhos.

## Alterações nos objetos e limitações

Apesar do comentário inicial dizer que não modifica objetos existentes, os padrões acrescentam filhos de soma/centro às células. Um soma com mesmo nome/classe é preservado, incluindo correções manuais; sua geometria pode diferir do soma recalculado que gera o CSV. O centro só é criado no ramo de soma novo, portanto ativar a opção não garante acrescentar um centro ausente em soma preexistente. Desativar CREATE_SOMA_ANNOTATIONS também impede a criação de centros.

Células sem soma estimável, máscara inválida/vazia ou recorte com mais de 25 milhões de pixels recebem falha. Há tratamento por célula, mas uma falha tardia, após gravar dados e antes de concluir QC, pode deixar resultados parciais e uma linha FAILED adicional. Confira IDs e Log antes de agregar os CSVs.

Os gráficos agregam apenas curvas adicionadas durante o processamento; o limite de células e as falhas podem enviesar a população. A criação de pasta temporal separa execuções, mas não registra automaticamente todos os parâmetros.
