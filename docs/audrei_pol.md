# audrei_pol — quantificação de colágeno

[← README](../README.md) · [Código](../scripts/audrei_pol.groovy)

## Objetivo e entradas

Segmentar tecido e colágeno total, medir classes red/green dentro do colágeno e exportar áreas, percentuais e razões. Opera na imagem aberta. Exige os classificadores de pixels `tissue`, `col_total` e `col_type` disponíveis no contexto do projeto. Sem projeto, o destino é a pasta pessoal do usuário; isso não garante que classificadores nomeados possam ser carregados fora de um projeto.

O script força o tipo de imagem `FLUORESCENCE` e executa `removeAnnotations()` antes de verificar se há tecido detectável. Execute em uma cópia de teste ou após salvar as anotações que pretende preservar.

## Configuração implementada

| Etapa | Configuração |
| --- | --- |
| Tecido | `tissue`, área mínima 500000.0, buracos 5000.0, opção SPLIT |
| Colágeno total | `col_total`, área mínima 5.0, buracos 0.0, dentro de cada tissue |
| Identificação das anotações | Classes `tissue` e `Positive`, comparação sem diferenciar maiúsculas |
| Tipos de colágeno | Medidas de `col_type`, prefixo `Col_Type`, em cada Positive |

As áreas mínimas são passadas diretamente ao QuPath; confirme a unidade aplicada pelo classificador na instalação e a calibração do projeto. Para o cálculo das áreas das ROIs, o próprio código escolhe µm² se houver calibração, caso contrário px².

## Como usar

1. Confirme que os classificadores e suas classes correspondem aos nomes acima.
2. Abra a imagem e confira a calibração.
3. Execute o script no editor Groovy.
4. Revise as ROIs tissue e Positive e as medidas `Col_Type` geradas.
5. Compare as áreas exportadas com as ROIs; salve os dados da imagem para persistir as anotações.

O script para sem exportar quando não encontra tissue ou Positive. Isso não desfaz a limpeza inicial nem as alterações intermediárias.

## Cálculos

- Área do tecido = soma das áreas das anotações tissue.
- Colágeno total = soma das áreas Positive.
- Red/green = soma de medidas cujo nome contém `col_type`, `area` e, respectivamente, `red` ou `green`.
- Percentual de colágeno = 100 × Positive / tissue.
- Percentuais red/green = 100 × área da classe / Positive.
- Razões red/green e green/red; denominador zero gera `NA` na razão.
- Percentuais com denominador zero são gravados como zero, não como valor ausente.

A associação por texto exige conferir os nomes efetivos das medidas: se não houver correspondência, os totais red/green podem permanecer zero. Não se pode concluir ausência biológica somente desse zero. O script não estabelece que as classes red/green equivalham a tipos moleculares específicos de colágeno.

## Saídas

Na pasta `collagen_results` ao lado do arquivo de projeto; sem projeto, em `user.home/collagen_results`:

- `<nome da imagem>_collagen.csv`: cabeçalho e uma linha; sobrescrito ao repetir o mesmo nome.
- `compiled_collagen_summary.csv`: linha acrescentada a cada execução, sem deduplicação.

São 11 colunas, descritas no [dicionário](DICIONARIO_DADOS.md). Áreas têm três casas decimais; percentuais e razões têm quatro, com ponto decimal.

## Particularidades de uso

Não há escape CSV para o nome da imagem nem limpeza para uso como nome de arquivo. Vírgulas, aspas, quebras de linha e separadores de caminho podem causar resultados inadequados. Áreas sobrepostas podem ser somadas mais de uma vez; red/green não são automaticamente normalizados para somar 100%. Confira se as unidades das medidas do classificador correspondem à unidade das áreas de ROI antes de usar os percentuais.

O bloco `synchronized(compiledCsv)` não constitui garantia de coordenação entre processos ou execuções independentes. Evite gravações simultâneas no mesmo compilado.
