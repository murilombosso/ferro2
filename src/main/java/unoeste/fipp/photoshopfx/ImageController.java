package unoeste.fipp.photoshopfx;

import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.Alert;
import javafx.scene.control.ButtonBar;
import javafx.scene.control.ButtonType;
import javafx.scene.control.Label;
import javafx.scene.control.Slider;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.image.WritableImage;
import javafx.scene.input.MouseButton;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.BorderPane;
import javafx.stage.FileChooser;
import javafx.stage.Stage;
import javafx.embed.swing.SwingFXUtils;
import unoeste.fipp.photoshopfx.util.ProcessadorImageJ;
import unoeste.fipp.photoshopfx.util.ProcessadorImagem;

import javax.imageio.ImageIO;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.util.ResourceBundle;
import java.util.function.UnaryOperator;

public class ImageController implements Initializable {
    public enum Ferramenta { NENHUMA, CANETA, RETANGULO, CIRCULO }

    @FXML private ImageView imageView;
    @FXML private BorderPane painel;
    @FXML private Label lbDimImagem;
    @FXML private Label lbZoom;
    @FXML private Slider slZoom;
    @FXML private Slider slBrilho;

    private Image original;
    private Image image;
    private Image baseBrilho;
    private Image baseDesenho;
    private File arquivo;
    private Stage stage;
    private boolean alterada;
    private boolean ignorarBrilho;
    private Runnable onChange = () -> { };
    private Ferramenta ferramenta = Ferramenta.NENHUMA;
    private int inicioX, inicioY, anteriorX, anteriorY;

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        slZoom.valueProperty().addListener((obs, antes, depois) -> atualizarImagem());
        slBrilho.valueProperty().addListener((obs, antes, depois) -> {
            if (!ignorarBrilho && baseBrilho != null) {
                image = ProcessadorImagem.ajustarBrilho(baseBrilho, depois.intValue());
                if (depois.intValue() != 0) alterada = true;
                atualizarImagem();
            }
        });
        imageView.setOnMousePressed(this::iniciarDesenho);
        imageView.setOnMouseDragged(this::arrastarDesenho);
        imageView.setOnMouseReleased(this::terminarDesenho);
    }

    public void setStage(Stage stage) { this.stage = stage; atualizarTitulo(); }
    public void setOnChange(Runnable onChange) { this.onChange = onChange; }
    public Image getImage() { return image; }
    public boolean isAlterada() { return alterada; }
    public File getArquivo() { return arquivo; }

    public void setImage(Image imagem, File arquivo) {
        this.original = ProcessadorImagem.copiar(imagem);
        this.image = ProcessadorImagem.copiar(imagem);
        this.baseBrilho = image;
        this.arquivo = arquivo;
        this.alterada = false;
        lbDimImagem.setText((int) imagem.getWidth() + " × " + (int) imagem.getHeight() + " px");
        atualizarImagem();
    }

    private void atualizarImagem() {
        if (image == null) return;
        double escala = Math.max(0.01, slZoom.getValue() / 100.0);
        imageView.setImage(image);
        imageView.setFitWidth(image.getWidth() * escala);
        imageView.setFitHeight(image.getHeight() * escala);
        lbZoom.setText((int) Math.round(slZoom.getValue()) + "%");
        atualizarTitulo();
    }

    private void atualizarTitulo() {
        if (stage != null && arquivo != null) stage.setTitle((alterada ? "* " : "") + arquivo.getName() + " — PhotoPaintFX");
        onChange.run();
    }

    private void confirmarBrilho() {
        if (slBrilho.getValue() != 0) {
            baseBrilho = image;
            ignorarBrilho = true;
            slBrilho.setValue(0);
            ignorarBrilho = false;
        }
    }

    private void aplicar(UnaryOperator<Image> operacao) {
        confirmarBrilho();
        image = operacao.apply(image);
        baseBrilho = image;
        alterada = true;
        atualizarImagem();
    }

    public void tonsCinza() { aplicar(ProcessadorImagem::converterTonsCinza); }
    public void pretoBranco() { aplicar(ProcessadorImagem::converterPretoBranco); }
    public void negativo() { aplicar(ProcessadorImagem::negativo); }
    public void espelharHorizontal() { aplicar(ProcessadorImagem::espelharHorizontal); }
    public void espelharVertical() { aplicar(ProcessadorImagem::espelharVertical); }
    public void detectarBordas() { aplicar(ProcessadorImageJ::detectarBordas); }
    public void suavizar(double raio) { aplicar(img -> ProcessadorImageJ.suavizar(img, raio)); }
    public void nitidez() { aplicar(ProcessadorImageJ::nitidez); }
    public void mediana() { aplicar(ProcessadorImageJ::mediana); }
    public void binarizarAutomatico() { aplicar(ProcessadorImageJ::binarizarAutomatico); }
    public void ajustarGama(double gama) { aplicar(img -> ProcessadorImageJ.ajustarGama(img, gama)); }
    public void focarBrilho() { slBrilho.requestFocus(); }

    public void recarregar() {
        confirmarBrilho();
        image = ProcessadorImagem.copiar(original);
        baseBrilho = image;
        alterada = true;
        atualizarImagem();
    }

    public void setFerramenta(Ferramenta ferramenta) {
        this.ferramenta = ferramenta;
        imageView.setStyle(ferramenta == Ferramenta.NENHUMA ? "" : "-fx-cursor: crosshair;");
    }

    private int pixelX(MouseEvent e) { return Math.max(0, Math.min((int) image.getWidth() - 1, (int) (e.getX() / Math.max(0.01, slZoom.getValue() / 100)))); }
    private int pixelY(MouseEvent e) { return Math.max(0, Math.min((int) image.getHeight() - 1, (int) (e.getY() / Math.max(0.01, slZoom.getValue() / 100)))); }

    private void iniciarDesenho(MouseEvent e) {
        if (e.getButton() != MouseButton.PRIMARY || ferramenta == Ferramenta.NENHUMA || image == null) return;
        confirmarBrilho();
        baseDesenho = ProcessadorImagem.copiar(image);
        inicioX = anteriorX = pixelX(e);
        inicioY = anteriorY = pixelY(e);
        if (ferramenta == Ferramenta.CANETA) image = ProcessadorImagem.desenharLinha(image, inicioX, inicioY, inicioX, inicioY);
        atualizarImagem();
        e.consume();
    }

    private void arrastarDesenho(MouseEvent e) {
        if (!e.isPrimaryButtonDown() || baseDesenho == null) return;
        int x = pixelX(e), y = pixelY(e);
        switch (ferramenta) {
            case CANETA -> image = ProcessadorImagem.desenharLinha(image, anteriorX, anteriorY, x, y);
            case RETANGULO -> image = ProcessadorImagem.desenharRetangulo(baseDesenho, inicioX, inicioY, x, y);
            case CIRCULO -> image = ProcessadorImagem.desenharCirculo(baseDesenho, inicioX, inicioY, x, y);
            default -> { return; }
        }
        anteriorX = x; anteriorY = y;
        atualizarImagem();
        e.consume();
    }

    private void terminarDesenho(MouseEvent e) {
        if (baseDesenho == null) return;
        int x = pixelX(e), y = pixelY(e);
        switch (ferramenta) {
            case CANETA -> image = ProcessadorImagem.desenharLinha(image, anteriorX, anteriorY, x, y);
            case RETANGULO -> image = ProcessadorImagem.desenharRetangulo(baseDesenho, inicioX, inicioY, x, y);
            case CIRCULO -> image = ProcessadorImagem.desenharCirculo(baseDesenho, inicioX, inicioY, x, y);
            default -> { }
        }
        baseDesenho = null;
        baseBrilho = image;
        alterada = true;
        atualizarTitulo();
        e.consume();
    }

    public boolean salvar(boolean como) {
        confirmarBrilho();
        File destino = arquivo;
        if (como || destino == null) {
            FileChooser seletor = new FileChooser();
            seletor.setTitle("Salvar imagem como");
            seletor.setInitialFileName("imagem-editada.png");
            seletor.getExtensionFilters().addAll(
                    new FileChooser.ExtensionFilter("PNG", "*.png"),
                    new FileChooser.ExtensionFilter("JPEG", "*.jpg", "*.jpeg"),
                    new FileChooser.ExtensionFilter("GIF", "*.gif"));
            destino = seletor.showSaveDialog(stage);
            if (destino == null) return false;
            if (!destino.getName().contains(".")) {
                String extensao = seletor.getSelectedExtensionFilter().getDescription().equals("JPEG") ? ".jpg"
                        : seletor.getSelectedExtensionFilter().getDescription().equals("GIF") ? ".gif" : ".png";
                destino = new File(destino.getParentFile(), destino.getName() + extensao);
            }
        }
        String nome = destino.getName().toLowerCase();
        String formato = nome.endsWith(".jpg") || nome.endsWith(".jpeg") ? "jpg" : nome.endsWith(".gif") ? "gif" : nome.endsWith(".png") ? "png" : null;
        if (formato == null) { erro("Use a extensão .png, .jpg, .jpeg ou .gif."); return false; }
        try {
            BufferedImage imagem = SwingFXUtils.fromFXImage(image, null);
            if (formato.equals("jpg")) {
                BufferedImage rgb = new BufferedImage(imagem.getWidth(), imagem.getHeight(), BufferedImage.TYPE_INT_RGB);
                Graphics2D g = rgb.createGraphics();
                g.setColor(java.awt.Color.WHITE); g.fillRect(0, 0, rgb.getWidth(), rgb.getHeight());
                g.drawImage(imagem, 0, 0, null); g.dispose(); imagem = rgb;
            }
            File temporario = File.createTempFile("photopaintfx-", "." + formato, destino.getAbsoluteFile().getParentFile());
            try {
                if (!ImageIO.write(imagem, formato, temporario)) throw new IOException("Formato sem codificador: " + formato);
                Files.move(temporario.toPath(), destino.toPath(), StandardCopyOption.REPLACE_EXISTING);
            } finally { Files.deleteIfExists(temporario.toPath()); }
            arquivo = destino;
            alterada = false;
            atualizarTitulo();
            return true;
        } catch (IOException ex) { erro("Não foi possível salvar: " + ex.getMessage()); return false; }
    }

    public boolean confirmarFechamento() {
        if (!alterada) return true;
        ButtonType sim = new ButtonType("Salvar", ButtonBar.ButtonData.YES);
        ButtonType nao = new ButtonType("Não salvar", ButtonBar.ButtonData.NO);
        ButtonType cancelar = new ButtonType("Cancelar", ButtonBar.ButtonData.CANCEL_CLOSE);
        Alert alerta = new Alert(Alert.AlertType.CONFIRMATION, "A imagem foi alterada. Deseja salvá-la antes de fechar?", sim, nao, cancelar);
        alerta.initOwner(stage);
        alerta.setTitle("Alterações não salvas");
        ButtonType escolha = alerta.showAndWait().orElse(cancelar);
        if (escolha == sim) return salvar(false);
        return escolha == nao;
    }

    private void erro(String mensagem) {
        Alert alerta = new Alert(Alert.AlertType.ERROR, mensagem);
        alerta.initOwner(stage);
        alerta.showAndWait();
    }
}
