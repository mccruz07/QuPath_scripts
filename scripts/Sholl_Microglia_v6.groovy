/* SHOLL - PILOTO QuPath 0.7
 * Execute após gerar anotações Microglia. Não modifica objetos existentes.
 * Para validação, analisa no máximo 50 células por padrão.
 * Método: cruzamentos das arestas do esqueleto com círculos em torno
 * do ponto interior do soma estimado por abertura morfológica.
 */
import qupath.lib.roi.GeometryTools
import qupath.lib.roi.ROIs
import qupath.lib.objects.PathObjects
import qupath.imagej.tools.IJTools
import ij.process.ByteProcessor
import ij.gui.Plot
import ij.IJ
import ij.ImagePlus
import java.awt.Color
import java.awt.BasicStroke
import java.awt.image.BufferedImage
import javax.imageio.ImageIO
import java.nio.file.Files
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

String CELL_CLASS = 'iba' // altere para Microglia se essa for a classe analisada
double SOMA_RADIUS_UM = 1.2
double STEP_UM = 2.0
double MAX_RADIUS_UM = 100.0
int MAX_CELLS = 50 // 0 = todas
int MAX_QC = 50
boolean CREATE_SOMA_ANNOTATIONS = true
boolean CREATE_CENTER_ANNOTATIONS = true
int PAD = 4

def data = getCurrentImageData()
if (data == null || getProjectEntry() == null) throw new IllegalStateException('Abra uma imagem em um projeto QuPath.')
def cal = data.getServer().getPixelCalibration()
if (!cal.hasPixelSizeMicrons()) throw new IllegalStateException('A imagem precisa de calibração em micrômetros.')
double px = cal.getPixelWidthMicrons(), py = cal.getPixelHeightMicrons()
if (!(px > 0 && py > 0 && STEP_UM > 0 && MAX_RADIUS_UM >= STEP_UM)) throw new IllegalStateException('Verifique a calibração e os parâmetros.')
// Abertura morfológica usa distância em pixels; recusar anisotropia relevante.
if (Math.abs(px-py)/Math.max(px,py) > 0.01) throw new IllegalStateException('Pixels anisotrópicos: a estimativa de soma ainda não está implementada para esta calibração.')
// Autoteste da conversão de ROI e da polaridade do esqueleto.
def testROI=ROIs.createRectangleROI(105,205,12,8,qupath.lib.regions.ImagePlane.getDefaultPlane())
def testBP=new ByteProcessor(30,30)
testBP.setValue(255); testBP.fill(IJTools.convertToIJRoi(testROI,-100,-200,1.0))
int testBefore=0
for(int ty=0;ty<30;ty++) for(int tx=0;tx<30;tx++) if(testBP.get(tx,ty)==255) testBefore++
if(testBefore != 96) throw new IllegalStateException('Autoteste falhou: máscara de retângulo esperado 96 pixels, obtido '+testBefore)
testBP.skeletonize(255)
int testAfter=0
for(int ty=0;ty<30;ty++) for(int tx=0;tx<30;tx++) if(testBP.get(tx,ty)==255) testAfter++
if(testAfter<=0 || testAfter>=testBefore) throw new IllegalStateException('Autoteste falhou: esqueleto branco = '+testAfter+' pixels; máscara = '+testBefore)
println('Autoteste ROI/polaridade PASS: máscara='+testBefore+', esqueleto='+testAfter+' pixels')
def cells = getAnnotationObjects().findAll { it.getPathClass()?.toString() == CELL_CLASS && it.getROI() != null }
if (cells.isEmpty()) throw new IllegalStateException('Nenhuma anotação da classe ' + CELL_CLASS + ' encontrada.')
if (MAX_CELLS > 0) cells = cells.take(MAX_CELLS)
String stamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern('yyyyMMdd_HHmmss_SSS'))
File out = new File(makePathInProject('Sholl_Pilot'), stamp)
if (!out.mkdirs()) throw new IllegalStateException('Não foi possível criar: ' + out)
File qc = new File(out, 'QC'); qc.mkdirs()
def quote = { v -> '"' + (v == null ? '' : v.toString().replace('"','""')) + '"' }
File profile = new File(out,'sholl_curves.csv')
File summary = new File(out,'sholl_summary.csv')
profile.write('Image,ObjectID,ObjectName,Radius_um,Intersections\n','UTF-8')
summary.write('Image,ObjectID,ObjectName,Status,Soma_area_um2,Center_X_px,Center_Y_px,Max_intersections,Radius_at_max_um,Max_nonzero_radius_um,AUC_sampled,Notes\n','UTF-8')
int n = (int)Math.floor(MAX_RADIUS_UM/STEP_UM)
double[] radii = (1..n).collect { it * STEP_UM } as double[]
def curves = []
def skeletonIDs = []
def skeletons = [] // segmentos em micrômetros, relativos ao centro do soma
int success=0, failed=0
String imageName = getProjectEntry().getImageName()
cells.eachWithIndex { cell, idx ->
    def roi = cell.getROI()
    String id = cell.getID().toString(), name = cell.getName() ?: ''
    try {
        def geom = GeometryTools.roiToGeometry(roi)
        double openingPx = SOMA_RADIUS_UM / ((px+py)/2.0)
        def eroded = geom.buffer(-openingPx)
        if (eroded.isEmpty()) throw new IllegalStateException('Soma ausente após erosão')
        def opened = eroded.buffer(openingPx).intersection(geom)
        def soma = GeometryTools.findLargestPolygon(opened)
        if (soma == null || soma.isEmpty()) throw new IllegalStateException('Soma não estimado')
        def center = soma.getInteriorPoint().getCoordinate()
        int x0 = (int)Math.floor(roi.getBoundsX()) - PAD
        int y0 = (int)Math.floor(roi.getBoundsY()) - PAD
        int w = (int)Math.ceil(roi.getBoundsX()+roi.getBoundsWidth()) - x0 + PAD + 1
        int h = (int)Math.ceil(roi.getBoundsY()+roi.getBoundsHeight()) - y0 + PAD + 1
        if (w < 3 || h < 3 || ((long)w*h > 25000000L)) throw new IllegalStateException('Recorte inválido ou muito grande')
        def bp = new ByteProcessor(w,h)
        bp.setValue(255)
        bp.fill(IJTools.convertToIJRoi(roi,-x0,-y0,1.0))
        int foregroundBefore = 0
        for (int yy=0; yy<h; yy++) for (int xx=0; xx<w; xx++) if (bp.get(xx,yy)==255) foregroundBefore++
        if (foregroundBefore == 0) throw new IllegalStateException('Máscara vazia antes da esqueletonização; verificar conversão ROI')
        bp.skeletonize(255) // primeiro plano branco, fundo preto
        // Cada aresta é visitada uma vez; distância medida em unidades físicas.
        // Uma transição dentro/fora corresponde a um cruzamento; vértices
        // exatamente sobre o círculo são classificados como fora.
        int[] counts = new int[n]
        int[][] offsets = [[1,0],[0,1],[1,1],[-1,1]] as int[][]
        int skeletonPixels=0
        def segments=[]
        for (int y=1; y<h-1; y++) for (int x=1; x<w-1; x++) {
            if (bp.get(x,y)==0) continue
            skeletonPixels++
            double d1 = Math.hypot((x+x0-center.x)*px,(y+y0-center.y)*py)
            offsets.each { o ->
                int xx=x+o[0], yy=y+o[1]
                if (bp.get(xx,yy)>0) {
                    double d2 = Math.hypot((xx+x0-center.x)*px,(yy+y0-center.y)*py)
                    segments << [(x+x0-center.x)*px, (y+y0-center.y)*py, (xx+x0-center.x)*px, (yy+y0-center.y)*py]
                    for (int k=0;k<n;k++) if ((d1 < radii[k]) != (d2 < radii[k])) counts[k]++
                }
            }
        }
        if (skeletonPixels == 0) throw new IllegalStateException('Esqueleto vazio')
        int peak=counts.max() as int, peakIndex=counts.toList().indexOf(peak)
        int last=-1; for(int k=0;k<n;k++) if(counts[k]>0) last=k
        double auc=0; for(int k=1;k<n;k++) auc+=(counts[k-1]+counts[k])*0.5*STEP_UM
        String note = counts[n-1]>0 ? 'CURVA_TRUNCADA_RAIO_MAXIMO' : ''
        for(int k=0;k<n;k++) profile.append([quote(imageName),quote(id),quote(name),radii[k],counts[k]].join(',')+'\n','UTF-8')
        summary.append([quote(imageName),quote(id),quote(name),'OK',soma.getArea()*px*py,center.x,center.y,peak,radii[peakIndex],last>=0?radii[last]:'',auc,quote(note)].join(',')+'\n','UTF-8')
        curves << (counts.collect { it as double } as double[])
        skeletons << segments
        skeletonIDs << [id,name]
        success++
        // Anotações usam exatamente a geometria e centro usados nesta execução.
        if (CREATE_SOMA_ANNOTATIONS) {
            try {
                String tag=String.format(java.util.Locale.US,'%.3f',SOMA_RADIUS_UM)
                String somaName='Sholl_Soma_'+tag+'um'
                def somaObject=cell.getChildObjects().find { it.getName()==somaName && it.getPathClass()?.toString()=='Sholl soma estimado' }
                boolean newSoma = somaObject == null
                if (newSoma) {
                    somaObject=PathObjects.createAnnotationObject(GeometryTools.geometryToROI(soma,roi.getImagePlane()),getPathClass('Sholl soma estimado'))
                    somaObject.setName(somaName); cell.addChildObject(somaObject)
                } else {
                    // Preserve anotações existentes, inclusive correções manuais.
                    println('Soma existente preservado para '+id+'; pode diferir do soma recalculado nesta execução.')
                }
                if (newSoma && CREATE_CENTER_ANNOTATIONS && !somaObject.getChildObjects().any { it.getName()=='Sholl_Centro_'+tag+'um' }) {
                    def pt=PathObjects.createAnnotationObject(ROIs.createPointsROI(center.x,center.y,roi.getImagePlane()),getPathClass('Sholl centro estimado'))
                    pt.setName('Sholl_Centro_'+tag+'um'); somaObject.addChildObject(pt)
                }
            } catch(Exception ae) { println('Falha ao criar anotação para '+id+': '+ae.toString()) }
        }
        if (idx < MAX_QC) {
            // QC em coordenadas de pixels: máscara cinza, soma verde,
            // esqueleto magenta, centro ciano e círculos azuis.
            def img = new BufferedImage(w,h,BufferedImage.TYPE_INT_RGB)
            def g = img.createGraphics(); g.setColor(Color.BLACK); g.fillRect(0,0,w,h)
            def mask = new ByteProcessor(w,h); mask.setValue(255); mask.fill(IJTools.convertToIJRoi(roi,-x0,-y0,1.0))
            for(int y=0;y<h;y++) for(int x=0;x<w;x++) if(mask.get(x,y)>0) img.setRGB(x,y,0x777777)
            g.setColor(new Color(30,210,90)); g.setStroke(new BasicStroke(1f))
            def boundary = soma.getBoundary()
            for(int c=0;c<boundary.getNumGeometries();c++) {
                def coords=boundary.getGeometryN(c).getCoordinates()
                for(int j=1;j<coords.length;j++) g.drawLine((int)Math.round(coords[j-1].x-x0),(int)Math.round(coords[j-1].y-y0),(int)Math.round(coords[j].x-x0),(int)Math.round(coords[j].y-y0))
            }
            g.setColor(new Color(50,140,255)); g.setStroke(new BasicStroke(1f))
            // Mostrar círculos com espaçamento visual de aproximadamente 10 µm.
            int every=Math.max(1,(int)Math.round(10.0/STEP_UM))
            for(int k=every-1;k<n;k+=every) {
                int rx=(int)Math.round(radii[k]/px), ry=(int)Math.round(radii[k]/py)
                int cx=(int)Math.round(center.x-x0), cy=(int)Math.round(center.y-y0)
                g.drawOval(cx-rx,cy-ry,2*rx,2*ry)
            }
            // Esqueleto por cima dos círculos para facilitar a leitura.
            for(int y=0;y<h;y++) for(int x=0;x<w;x++) if(bp.get(x,y)>0) img.setRGB(x,y,0xff33ff)
            g.setColor(Color.CYAN); int cx=(int)Math.round(center.x-x0), cy=(int)Math.round(center.y-y0)
            g.drawLine(cx-3,cy,cx+3,cy); g.drawLine(cx,cy-3,cx,cy+3); g.dispose()
            ImageIO.write(img,'png',new File(qc,String.format('cell_%04d.png',idx+1)))
        }
    } catch(Exception e) {
        failed++
        summary.append([quote(imageName),quote(id),quote(name),'FAILED','','','','','','','',quote(e.getClass().getSimpleName()+': '+e.getMessage())].join(',')+'\n','UTF-8')
        println('Falha em '+id+': '+e.toString())
    }
}
if (!curves.isEmpty()) {
    def plot = new Plot('Sholl piloto','Distância ao centro do soma (µm)','Interseções')
    plot.setSize(900,600)
    double maxY=0; int lastNonzero=-1
    curves.each { curve -> for (int k=0;k<n;k++) { maxY=Math.max(maxY,curve[k]); if(curve[k]>0) lastNonzero=Math.max(lastNonzero,k) } }
    double plotMaxX = lastNonzero >= 0 ? Math.min(MAX_RADIUS_UM, radii[Math.min(n-1,lastNonzero+2)]) : MAX_RADIUS_UM
    plot.setLimits(0.0,plotMaxX,0.0,Math.max(1.0,Math.ceil(maxY*1.10)))
    plot.setColor(new Color(190,190,190))
    curves.each { curve -> plot.add('line',radii,curve) }
    double[] mean = new double[n]
    for(int k=0;k<n;k++) { double sum=0; curves.each { sum += it[k] }; mean[k]=sum/curves.size() }
    plot.setColor(Color.RED); plot.setLineWidth(2); plot.add('line',radii,mean)
    IJ.saveAs(new ImagePlus('Sholl',plot.getProcessor()),'PNG',new File(out,'sholl_all_curves.png').getAbsolutePath())
}
// Figura adicional: sobreposição de todos os esqueletos sem alterar orientação.
if (!skeletons.isEmpty()) {
    int size=1400, margin=125, cx=(int)(size/2), cy=(int)(size/2)
    double furthest=0.0
    skeletons.each { segs -> segs.each { q ->
        furthest=Math.max(furthest, Math.max(Math.max(Math.abs(q[0] as double),Math.abs(q[1] as double)),Math.max(Math.abs(q[2] as double),Math.abs(q[3] as double))))
    }}
    furthest=Math.max(STEP_UM,furthest)
    double viewRadius=Math.min(MAX_RADIUS_UM,Math.ceil(furthest/(5.0*STEP_UM))*(5.0*STEP_UM))
    viewRadius=Math.max(viewRadius,furthest)
    double scale=(size/2.0-margin)/(viewRadius*1.05)
    def img = new BufferedImage(size,size,BufferedImage.TYPE_INT_RGB)
    def g=img.createGraphics()
    g.setRenderingHint(java.awt.RenderingHints.KEY_ANTIALIASING,java.awt.RenderingHints.VALUE_ANTIALIAS_ON)
    g.setColor(Color.WHITE); g.fillRect(0,0,size,size)
    g.setFont(new java.awt.Font('SansSerif',java.awt.Font.PLAIN,19))
    int every=Math.max(1,(int)Math.round(10.0/STEP_UM))
    g.setColor(new Color(155,175,195)); g.setStroke(new BasicStroke(1.3f))
    for(int k=every-1;k<n && radii[k] <= viewRadius;k+=every) {
        int rad=(int)Math.round(radii[k]*scale)
        g.drawOval(cx-rad,cy-rad,rad*2,rad*2)
        g.drawString(String.format(java.util.Locale.US,'%.1f',radii[k]),cx+rad+3,cy-4)
    }
    g.setStroke(new BasicStroke(1.8f))
    File colorFile=new File(out,'sholl_skeleton_colors.csv')
    colorFile.write('Image,ObjectID,ObjectName,Color_hex\n','UTF-8')
    skeletons.eachWithIndex { segs, cellIndex ->
      float hue=(float)((cellIndex*0.618033988749895)%1.0)
      int rgb=Color.HSBtoRGB(hue,0.8f,0.8f)
      def baseColor=new Color(rgb)
      g.setColor(new Color(baseColor.getRed(),baseColor.getGreen(),baseColor.getBlue(),160))
      String hex=String.format('#%02X%02X%02X',baseColor.getRed(),baseColor.getGreen(),baseColor.getBlue())
      colorFile.append([quote(imageName),quote(skeletonIDs[cellIndex][0]),quote(skeletonIDs[cellIndex][1]),hex].join(',')+'\n','UTF-8')
      segs.each { q ->
        g.drawLine((int)Math.round(cx+(q[0] as double)*scale),(int)Math.round(cy+(q[1] as double)*scale),(int)Math.round(cx+(q[2] as double)*scale),(int)Math.round(cy+(q[3] as double)*scale))
    }}
    g.setColor(new Color(190,30,30)); g.fillOval(cx-4,cy-4,8,8)
    g.setColor(Color.BLACK); g.setFont(new java.awt.Font('SansSerif',java.awt.Font.BOLD,24))
    g.drawString('Esqueletos alinhados pelo centro do soma',margin,45)
    g.setFont(new java.awt.Font('SansSerif',java.awt.Font.PLAIN,19))
    g.drawString('n = '+skeletons.size()+'  |  Distâncias dos círculos em µm',margin,75)
    g.drawString('Orientações originais preservadas; centro em vermelho',margin,size-35)
    g.dispose()
    ImageIO.write(img,'png',new File(out,'sholl_skeletons_overlay.png'))
}
if (CREATE_SOMA_ANNOTATIONS) fireHierarchyUpdate()
println('Sholl piloto concluído. OK: '+success+'; falhas: '+failed)
println('Resultados: '+out.getAbsolutePath())
println('QC: cinza=mascara, verde=soma estimado, magenta=esqueleto, ciano=centro, azul=raios.')
println('A figura de QC é uma máscara, não a imagem original. Curvas são preliminares.')
