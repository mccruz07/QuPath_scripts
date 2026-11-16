# Núcleos centrais, periféricos e intersticiais em H&E

[← README](../README.md) · [Script](../scripts/fibras/fibras_nucleos_centrais_perifericos_intersticio_he.groovy) · [Nomes antigos e novos](NOMES_DOS_SCRIPTS.md)

**Arquivo anterior:** `Final_novo.groovy`  
**Arquivo no acervo:** `fibras_nucleos_centrais_perifericos_intersticio_he.groovy`

## O que faz

Cria tecido Positive, segmenta fibras por Cellpose-SAM e calcula interstício como tecido menos união das fibras, com margem negativa. Filtra fibras, detecta células por watershed e classifica seus centroides em Cell_central, Cell_peripheral ou Cell_inter.

## O que precisa

QuPath com uma imagem aberta. Recursos do projeto: `muscle`. Extensão Cellpose e ambiente/modelo correspondente configurados. Imagem vinculada a um projeto para os caminhos e nomes usados pelo script.

Os classificadores treinados e modelos externos não acompanham este pacote. Os nomes de classes/canais e medidas precisam corresponder aos recursos da imagem e do projeto.

## Parâmetros principais

muscle; cp_sam, Red, pixelSize 1, diâmetro 45 e useCellposeSAM. Circularidade mínima 0.3, limiar radial 0.7, buffer intersticial -1.5 em unidades da geometria (pixels). Watershed: 0.5 µm, área 4–60 µm², limiar 0.1, expansão 1 µm.

## Como usar

Abra a imagem no projeto, prepare as regiões indicadas acima e abra o script no editor Groovy. Ajuste os parâmetros e nomes de recursos conforme a tarefa e execute na imagem atual. As alterações de anotações e detecções ficam na hierarquia; salve os dados da imagem para conservá-las no projeto.

## O que gera

fiber_results/<imagem>_resultados.csv: Nome_Imagem, Area_Total_Positive, Num_Fibras, Area_Media_Fibras, Nucleos_Centrais, Nucleos_Perifericos, Area_Total_Inter, Nucleos_Inter. Sobrescreve o arquivo da imagem.

## Particularidades para reutilizar

As áreas exportadas vêm de ROI.getArea() e estão em pixels². O interstício é calculado antes de remover fibras por circularidade; as áreas das fibras removidas não são reincorporadas automaticamente ao interstício. A classificação intersticial só considera células sem classe naquele momento. Um centro coincidente com o da fibra não recebe classificação radial nesse ramo.
