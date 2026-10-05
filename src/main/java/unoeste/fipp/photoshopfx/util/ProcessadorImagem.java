package unoeste.fipp.photoshopfx.util;

import javafx.scene.image.Image;
import javafx.scene.image.PixelReader;
import javafx.scene.image.PixelWriter;
import javafx.scene.image.WritableImage;

public final class ProcessadorImagem {
    private ProcessadorImagem() { }

    public static WritableImage copiar(Image origem) {
        WritableImage copia = new WritableImage((int) origem.getWidth(), (int) origem.getHeight());
        copia.getPixelWriter().setPixels(0, 0, (int) origem.getWidth(), (int) origem.getHeight(), origem.getPixelReader(), 0, 0);
        return copia;
    }

    private static int limitar(int valor) { return Math.max(0, Math.min(255, valor)); }

    private static WritableImage transformar(Image origem, java.util.function.IntUnaryOperator operacao) {
        int largura = (int) origem.getWidth(), altura = (int) origem.getHeight();
        WritableImage saida = new WritableImage(largura, altura);
        PixelReader leitura = origem.getPixelReader();
        PixelWriter escrita = saida.getPixelWriter();
        for (int y = 0; y < altura; y++) for (int x = 0; x < largura; x++)
            escrita.setArgb(x, y, operacao.applyAsInt(leitura.getArgb(x, y)));
        return saida;
    }

    private static int cinza(int argb) {
        return limitar((int) Math.round(0.299 * ((argb >>> 16) & 255)
                + 0.587 * ((argb >>> 8) & 255) + 0.114 * (argb & 255)));
    }

    public static WritableImage converterTonsCinza(Image origem) {
        return transformar(origem, p -> { int v = cinza(p); return (p & 0xff000000) | (v << 16) | (v << 8) | v; });
    }

    public static WritableImage converterPretoBranco(Image origem) {
        return transformar(origem, p -> { int v = cinza(p) >= 128 ? 255 : 0; return (p & 0xff000000) | (v << 16) | (v << 8) | v; });
    }

    public static WritableImage negativo(Image origem) {
        return transformar(origem, p -> (p & 0xff000000) | (~p & 0x00ffffff));
    }

    public static WritableImage ajustarBrilho(Image origem, int porcentagem) {
        int delta = (int) Math.round(porcentagem * 255.0 / 100.0);
        return transformar(origem, p -> {
            int r = limitar(((p >>> 16) & 255) + delta), g = limitar(((p >>> 8) & 255) + delta), b = limitar((p & 255) + delta);
            return (p & 0xff000000) | (r << 16) | (g << 8) | b;
        });
    }

    public static WritableImage espelharHorizontal(Image origem) { return espelhar(origem, true); }
    public static WritableImage espelharVertical(Image origem) { return espelhar(origem, false); }

    private static WritableImage espelhar(Image origem, boolean horizontal) {
        int largura = (int) origem.getWidth(), altura = (int) origem.getHeight();
        WritableImage saida = new WritableImage(largura, altura);
        PixelReader leitura = origem.getPixelReader();
        PixelWriter escrita = saida.getPixelWriter();
        for (int y = 0; y < altura; y++) for (int x = 0; x < largura; x++)
            escrita.setArgb(x, y, leitura.getArgb(horizontal ? largura - 1 - x : x, horizontal ? y : altura - 1 - y));
        return saida;
    }

    public static WritableImage desenharLinha(Image origem, int x1, int y1, int x2, int y2) {
        WritableImage saida = copiar(origem);
        int dx = Math.abs(x2 - x1), dy = Math.abs(y2 - y1), sx = x1 < x2 ? 1 : -1, sy = y1 < y2 ? 1 : -1;
        int erro = dx - dy;
        while (true) {
            ponto(saida, x1, y1);
            if (x1 == x2 && y1 == y2) break;
            int e2 = 2 * erro;
            if (e2 > -dy) { erro -= dy; x1 += sx; }
            if (e2 < dx) { erro += dx; y1 += sy; }
        }
        return saida;
    }

    public static WritableImage desenharRetangulo(Image origem, int x1, int y1, int x2, int y2) {
        WritableImage saida = copiar(origem);
        int esquerda = Math.min(x1, x2), direita = Math.max(x1, x2), topo = Math.min(y1, y2), base = Math.max(y1, y2);
        for (int x = esquerda; x <= direita; x++) { ponto(saida, x, topo); ponto(saida, x, base); }
        for (int y = topo; y <= base; y++) { ponto(saida, esquerda, y); ponto(saida, direita, y); }
        return saida;
    }

    public static WritableImage desenharCirculo(Image origem, int x1, int y1, int x2, int y2) {
        WritableImage saida = copiar(origem);
        double cx = (x1 + x2) / 2.0, cy = (y1 + y2) / 2.0, rx = Math.abs(x2 - x1) / 2.0, ry = Math.abs(y2 - y1) / 2.0;
        if (rx == 0 || ry == 0) return desenharLinha(origem, x1, y1, x2, y2);
        int passos = Math.max(64, (int) (2 * Math.PI * Math.max(rx, ry) * 2));
        for (int i = 0; i <= passos; i++) {
            double angulo = 2 * Math.PI * i / passos;
            ponto(saida, (int) Math.round(cx + rx * Math.cos(angulo)), (int) Math.round(cy + ry * Math.sin(angulo)));
        }
        return saida;
    }

    private static void ponto(WritableImage imagem, int x, int y) {
        int largura = (int) imagem.getWidth(), altura = (int) imagem.getHeight();
        for (int py = y - 1; py <= y + 1; py++) for (int px = x - 1; px <= x + 1; px++)
            if (px >= 0 && py >= 0 && px < largura && py < altura) imagem.getPixelWriter().setArgb(px, py, 0xffff2020);
    }
}
