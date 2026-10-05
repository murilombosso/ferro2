package unoeste.fipp.photoshopfx;

import javafx.application.Platform;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.scene.control.MenuItem;
import javafx.scene.image.WritableImage;
import javafx.stage.Stage;
import javafx.stage.Window;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import unoeste.fipp.photoshopfx.util.ProcessadorImageJ;
import unoeste.fipp.photoshopfx.util.ProcessadorImagem;

import javax.imageio.ImageIO;
import java.io.File;
import java.nio.file.Files;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.*;

class PhotoPaintFXTest {
    @BeforeAll
    static void iniciarJavaFX() throws Exception {
        CountDownLatch iniciado = new CountDownLatch(1);
        Platform.startup(iniciado::countDown);
        assertTrue(iniciado.await(10, TimeUnit.SECONDS));
        Platform.setImplicitExit(false);
    }

    private static void naThreadFX(Runnable tarefa) throws Exception {
        CountDownLatch fim = new CountDownLatch(1);
        AtomicReference<Throwable> erro = new AtomicReference<>();
        Platform.runLater(() -> {
            try { tarefa.run(); } catch (Throwable ex) { erro.set(ex); } finally { fim.countDown(); }
        });
        assertTrue(fim.await(15, TimeUnit.SECONDS));
        if (erro.get() != null) throw new AssertionError(erro.get());
    }

    private static WritableImage amostra() {
        WritableImage img = new WritableImage(3, 2);
        img.getPixelWriter().setArgb(0, 0, 0x80ff0000);
        img.getPixelWriter().setArgb(1, 0, 0xff00ff00);
        img.getPixelWriter().setArgb(2, 0, 0xff0000ff);
        img.getPixelWriter().setArgb(0, 1, 0xffffffff);
        img.getPixelWriter().setArgb(1, 1, 0xff000000);
        img.getPixelWriter().setArgb(2, 1, 0xff606060);
        return img;
    }

    @Test
    void transformacoesPreservamDimensoesAlfaEOrientacao() {
        WritableImage img = amostra();
        assertEquals(0x8000ffff, ProcessadorImagem.negativo(img).getPixelReader().getArgb(0, 0));
        assertEquals(0xff0000ff, ProcessadorImagem.espelharHorizontal(img).getPixelReader().getArgb(0, 0));
        assertEquals(0xffffffff, ProcessadorImagem.espelharVertical(img).getPixelReader().getArgb(0, 0));
        int cinza = ProcessadorImagem.converterTonsCinza(img).getPixelReader().getArgb(0, 0);
        assertEquals(0x80, cinza >>> 24);
        assertEquals((cinza >>> 16) & 255, cinza & 255);
        assertEquals(0xff000000, ProcessadorImagem.converterPretoBranco(img).getPixelReader().getArgb(2, 1));
        assertEquals(0xffffffff, ProcessadorImagem.ajustarBrilho(img, 100).getPixelReader().getArgb(1, 1));
        assertEquals(3, ProcessadorImagem.copiar(img).getWidth());
    }

    @Test
    void ferramentasDesenhamNosPixelsEsperados() {
        WritableImage branco = new WritableImage(20, 20);
        for (int y = 0; y < 20; y++) for (int x = 0; x < 20; x++) branco.getPixelWriter().setArgb(x, y, 0xffffffff);
        assertEquals(0xffff2020, ProcessadorImagem.desenharLinha(branco, 2, 2, 15, 15).getPixelReader().getArgb(8, 8));
        assertEquals(0xffff2020, ProcessadorImagem.desenharRetangulo(branco, 2, 2, 15, 15).getPixelReader().getArgb(8, 2));
        assertEquals(0xffff2020, ProcessadorImagem.desenharCirculo(branco, 2, 2, 16, 16).getPixelReader().getArgb(16, 9));
        assertEquals(0xffffffff, branco.getPixelReader().getArgb(8, 8));
    }

    @Test
    void imageJProcessaSemAlterarTamanho() {
        WritableImage img = amostra();
        for (var processada : new javafx.scene.image.Image[] {
                ProcessadorImageJ.detectarBordas(img), ProcessadorImageJ.suavizar(img, 2),
                ProcessadorImageJ.nitidez(img), ProcessadorImageJ.mediana(img), ProcessadorImageJ.binarizarAutomatico(img),
                ProcessadorImageJ.ajustarGama(img, 1.5) }) {
            assertEquals(3, processada.getWidth());
            assertEquals(2, processada.getHeight());
        }
    }

    @Test
    void telasCarregamESalvamentoPngFunciona() throws Exception {
        naThreadFX(() -> {
            try {
                assertNotNull(new FXMLLoader(PhotoShopFX.class.getResource("main-view.fxml")).load());
                FXMLLoader loader = new FXMLLoader(PhotoShopFX.class.getResource("image-view.fxml"));
                assertNotNull(loader.load());
                ImageController controller = loader.getController();
                File destino = Files.createTempFile("photopaintfx-teste-", ".png").toFile();
                try {
                    controller.setImage(amostra(), destino);
                    controller.espelharHorizontal();
                    assertTrue(controller.isAlterada());
                    assertTrue(controller.salvar(false));
                    assertFalse(controller.isAlterada());
                    assertEquals(0xff0000ff, ImageIO.read(destino).getRGB(0, 0));
                    controller.recarregar();
                    assertEquals(0x80ff0000, controller.getImage().getPixelReader().getArgb(0, 0));
                } finally { Files.deleteIfExists(destino.toPath()); }
            } catch (Exception ex) { throw new RuntimeException(ex); }
        });
    }

    @Test
    void salvaJpegEGifEAbreJanelaComMenusCorretos() throws Exception {
        naThreadFX(() -> {
            try {
                for (String extensao : new String[] {"jpg", "gif"}) {
                    File destino = Files.createTempFile("photopaintfx-formato-", "." + extensao).toFile();
                    try {
                        FXMLLoader imagemLoader = new FXMLLoader(PhotoShopFX.class.getResource("image-view.fxml"));
                        imagemLoader.load();
                        ImageController imagemController = imagemLoader.getController();
                        imagemController.setImage(amostra(), destino);
                        imagemController.negativo();
                        assertTrue(imagemController.salvar(false));
                        assertNotNull(ImageIO.read(destino));
                        assertEquals(3, ImageIO.read(destino).getWidth());
                        assertTrue(imagemController.confirmarFechamento());
                    } finally { Files.deleteIfExists(destino.toPath()); }
                }

                File entrada = Files.createTempFile("photopaintfx-abertura-", ".png").toFile();
                try {
                    var buffer = new java.awt.image.BufferedImage(3, 2, java.awt.image.BufferedImage.TYPE_INT_ARGB);
                    assertTrue(ImageIO.write(buffer, "png", entrada));
                    FXMLLoader loader = new FXMLLoader(PhotoShopFX.class.getResource("main-view.fxml"));
                    Stage principal = new Stage();
                    principal.setScene(new Scene(loader.load()));
                    MainController principalController = loader.getController();
                    MenuItem salvar = (MenuItem) loader.getNamespace().get("miSalvar");
                    MenuItem salvarComo = (MenuItem) loader.getNamespace().get("miSalvarComo");
                    assertTrue(salvar.isDisable());
                    assertTrue(salvarComo.isDisable());
                    principal.show();
                    principalController.abrirArquivo(entrada);
                    assertFalse(salvarComo.isDisable());
                    assertTrue(salvar.isDisable());
                    Stage imagem = Window.getWindows().stream().filter(w -> w instanceof Stage && ((Stage) w).getOwner() == principal)
                            .map(w -> (Stage) w).findFirst().orElseThrow();
                    ImageController ic = (ImageController) imagem.getUserData();
                    ic.negativo();
                    assertFalse(salvar.isDisable());
                    assertTrue(ic.salvar(false));
                    assertTrue(salvar.isDisable());
                    imagem.hide(); principal.hide();
                } finally { Files.deleteIfExists(entrada.toPath()); }
            } catch (Exception ex) { throw new RuntimeException(ex); }
        });
    }
}
