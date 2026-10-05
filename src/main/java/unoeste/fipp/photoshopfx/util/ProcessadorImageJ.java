package unoeste.fipp.photoshopfx.util;

import ij.ImagePlus;
import ij.process.ImageProcessor;
import javafx.embed.swing.SwingFXUtils;
import javafx.scene.image.Image;

public final class ProcessadorImageJ {
    private ProcessadorImageJ() { }

    private static ImageProcessor processador(Image imagem) {
        return new ImagePlus("Imagem", SwingFXUtils.fromFXImage(imagem, null)).getProcessor();
    }

    private static Image converter(ImageProcessor processador) {
        return SwingFXUtils.toFXImage(processador.getBufferedImage(), null);
    }

    public static Image detectarBordas(Image imagem) { ImageProcessor p = processador(imagem); p.findEdges(); return converter(p); }
    public static Image suavizar(Image imagem, double raio) { ImageProcessor p = processador(imagem); p.blurGaussian(raio); return converter(p); }
    public static Image nitidez(Image imagem) { ImageProcessor p = processador(imagem); p.sharpen(); return converter(p); }
    public static Image mediana(Image imagem) { ImageProcessor p = processador(imagem); p.medianFilter(); return converter(p); }
    public static Image binarizarAutomatico(Image imagem) { ImageProcessor p = processador(imagem); p.autoThreshold(); return converter(p); }
    public static Image ajustarGama(Image imagem, double gama) { ImageProcessor p = processador(imagem); p.gamma(gama); return converter(p); }
}
