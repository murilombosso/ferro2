package unoeste.fipp.photoshopfx;

import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.MenuItem;
import javafx.scene.control.ToolBar;
import javafx.scene.control.TextInputDialog;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.VBox;
import javafx.stage.FileChooser;
import javafx.stage.Stage;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

public class MainController {
    @FXML private BorderPane painel;
    @FXML private VBox painelDesenho;
    @FXML private ToolBar barraFerramentas;
    @FXML private MenuItem miSalvar, miSalvarComo, miFechar, miDesenhos, miInformacoes;
    @FXML private MenuItem miCinza, miPretoBranco, miNegativo, miEspelharH, miEspelharV;
    @FXML private MenuItem miBordas, miSuavizar, miNitidez, miMediana, miBinarizar, miGama, miBrilho, miRecarregar;

    private final List<Stage> janelas = new ArrayList<>();
    private Stage stageSelecionado;
    private Button botaoSalvar;
    private final List<Button> botoesImagem = new ArrayList<>();

    @FXML
    private void initialize() {
        barraFerramentas.getItems().add(botao("Abrir", "/icon-open.png", () -> onAbrir()));
        botaoSalvar = botao("Salvar", "/icon-save.png", () -> onSalvar());
        barraFerramentas.getItems().add(botaoSalvar);
        barraFerramentas.getItems().add(botaoImagem("Informações", "/icon-info.png", c -> onInformacoes()));
        barraFerramentas.getItems().add(botaoImagem("Espelhar horizontalmente", "/icon-flip-h.png", c -> c.espelharHorizontal()));
        barraFerramentas.getItems().add(botaoImagem("Espelhar verticalmente", "/icon-flip-v.png", c -> c.espelharVertical()));
        barraFerramentas.getItems().add(botaoImagem("Recarregar original", "/icon-reload.png", c -> c.recarregar()));
        painelDesenho.setManaged(false);
        configurarIcone(btCirculo, "/i-circulo2.png");
        configurarIcone(btRetangulo, "/i-retangulo2.png");
        configurarIcone(btCaneta, "/i-desenhando2.png");
        atualizarDisponibilidade();
    }

    private Button botao(String descricao, String icone, Runnable acao) {
        Button b = new Button();
        b.setTooltip(new javafx.scene.control.Tooltip(descricao));
        ImageView img = new ImageView(new Image(getClass().getResourceAsStream(icone)));
        img.setFitWidth(24); img.setFitHeight(24); img.setPreserveRatio(true);
        b.setGraphic(img);
        b.setMinSize(36, 36);
        b.setOnAction(e -> acao.run());
        return b;
    }

    @FXML private Button btCirculo, btRetangulo, btCaneta;

    private void configurarIcone(Button botao, String caminho) {
        ImageView imagem = new ImageView(new Image(getClass().getResourceAsStream(caminho)));
        imagem.setFitWidth(28); imagem.setFitHeight(28); imagem.setPreserveRatio(true);
        botao.setGraphic(imagem);
    }

    private Button botaoImagem(String descricao, String icone, Consumer<ImageController> acao) {
        Button b = botao(descricao, icone, () -> comImagem(acao));
        botoesImagem.add(b);
        return b;
    }

    private ImageController controllerSelecionado() {
        return stageSelecionado == null ? null : (ImageController) stageSelecionado.getUserData();
    }

    private void comImagem(Consumer<ImageController> acao) {
        ImageController controller = controllerSelecionado();
        if (controller != null) acao.accept(controller);
        atualizarDisponibilidade();
    }

    private void atualizarDisponibilidade() {
        ImageController controller = controllerSelecionado();
        boolean semImagem = controller == null;
        miSalvar.setDisable(semImagem || !controller.isAlterada());
        botaoSalvar.setDisable(semImagem || !controller.isAlterada());
        for (Button b : botoesImagem) b.setDisable(semImagem);
        for (MenuItem item : List.of(miSalvarComo, miFechar, miDesenhos, miInformacoes, miCinza, miPretoBranco, miNegativo,
                miEspelharH, miEspelharV, miBordas, miSuavizar, miNitidez, miMediana, miBinarizar, miGama, miBrilho, miRecarregar))
            item.setDisable(semImagem);
        painelDesenho.setDisable(semImagem);
        if (semImagem) { painelDesenho.setVisible(false); painelDesenho.setManaged(false); }
    }

    @FXML private void onAbrir() {
        FileChooser seletor = new FileChooser();
        seletor.setTitle("Abrir imagem");
        seletor.setInitialDirectory(new File(System.getProperty("user.home")));
        seletor.getExtensionFilters().add(new FileChooser.ExtensionFilter("Imagens JPEG, JPG, GIF e PNG", "*.jpeg", "*.jpg", "*.gif", "*.png"));
        seletor.getExtensionFilters().addAll(
                new FileChooser.ExtensionFilter("JPEG", "*.jpeg", "*.jpg"),
                new FileChooser.ExtensionFilter("GIF", "*.gif"),
                new FileChooser.ExtensionFilter("PNG", "*.png"));
        File arquivo = seletor.showOpenDialog(painel.getScene().getWindow());
        if (arquivo == null) return;
        abrirArquivo(arquivo);
    }

    void abrirArquivo(File arquivo) {
        Image imagem = new Image(arquivo.toURI().toString());
        if (imagem.isError() || imagem.getWidth() == 0 || imagem.getHeight() == 0) {
            erro("Não foi possível abrir a imagem: " + arquivo.getName());
            return;
        }
        try {
            FXMLLoader loader = new FXMLLoader(PhotoShopFX.class.getResource("image-view.fxml"));
            Scene cena = new Scene(loader.load());
            Stage janela = new Stage();
            janela.initOwner(painel.getScene().getWindow());
            janela.setScene(cena);
            ImageController controller = loader.getController();
            janela.setUserData(controller);
            controller.setStage(janela);
            controller.setImage(imagem, arquivo);
            controller.setOnChange(this::atualizarDisponibilidade);
            janela.setOnCloseRequest(e -> { if (!controller.confirmarFechamento()) e.consume(); });
            janela.setOnHidden(e -> {
                janelas.remove(janela);
                if (stageSelecionado == janela) stageSelecionado = janelas.isEmpty() ? null : janelas.get(janelas.size() - 1);
                atualizarDisponibilidade();
            });
            janela.focusedProperty().addListener((obs, antes, agora) -> {
                if (agora) { stageSelecionado = janela; atualizarDisponibilidade(); }
            });
            janelas.add(janela);
            stageSelecionado = janela;
            janela.show();
            atualizarDisponibilidade();
        } catch (IOException ex) { erro("Não foi possível abrir a janela da imagem: " + ex.getMessage()); }
    }

    @FXML private void onSalvar() { comImagem(c -> c.salvar(false)); }
    @FXML private void onSalvarComo() { comImagem(c -> c.salvar(true)); }
    @FXML private void onFechar() {
        if (stageSelecionado != null && controllerSelecionado().confirmarFechamento()) stageSelecionado.hide();
    }
    @FXML private void onSair() { if (fecharTodas()) Platform.exit(); }

    public boolean fecharTodas() {
        for (Stage janela : new ArrayList<>(janelas)) {
            ImageController controller = (ImageController) janela.getUserData();
            if (!controller.confirmarFechamento()) return false;
        }
        for (Stage janela : new ArrayList<>(janelas)) janela.hide();
        return true;
    }

    @FXML private void onVisualizarDesenho() {
        boolean visivel = !painelDesenho.isVisible();
        painelDesenho.setVisible(visivel);
        painelDesenho.setManaged(visivel);
    }

    @FXML private void onCirculo() { comImagem(c -> c.setFerramenta(ImageController.Ferramenta.CIRCULO)); }
    @FXML private void onRetangulo() { comImagem(c -> c.setFerramenta(ImageController.Ferramenta.RETANGULO)); }
    @FXML private void onDesenhar() { comImagem(c -> c.setFerramenta(ImageController.Ferramenta.CANETA)); }
    @FXML private void onSemFerramenta() { comImagem(c -> c.setFerramenta(ImageController.Ferramenta.NENHUMA)); }

    @FXML private void onInformacoes() {
        comImagem(c -> {
            Image img = c.getImage();
            File arquivo = c.getArquivo();
            Alert alerta = new Alert(Alert.AlertType.INFORMATION);
            alerta.initOwner(stageSelecionado);
            alerta.setTitle("Informações da imagem");
            alerta.setHeaderText(arquivo.getName());
            alerta.setContentText("Dimensões: " + (int) img.getWidth() + " × " + (int) img.getHeight() + " px\nArquivo: " + arquivo.getAbsolutePath());
            alerta.showAndWait();
        });
    }

    @FXML private void onSobre() {
        Alert alerta = new Alert(Alert.AlertType.INFORMATION);
        alerta.initOwner(painel.getScene().getWindow());
        alerta.setTitle("Sobre o PhotoPaintFX");
        alerta.setHeaderText("PhotoPaintFX");
        alerta.setContentText("Editor de imagens em JavaFX.\nDesenvolvedores: Leo Barros e Murilo Bosso.");
        alerta.showAndWait();
    }

    @FXML private void onTonsCinza() { comImagem(ImageController::tonsCinza); }
    @FXML private void onPretoBranco() { comImagem(ImageController::pretoBranco); }
    @FXML private void onNegativo() { comImagem(ImageController::negativo); }
    @FXML private void onEspelharHorizontal() { comImagem(ImageController::espelharHorizontal); }
    @FXML private void onEspelharVertical() { comImagem(ImageController::espelharVertical); }
    @FXML private void onDetectarBordas() { comImagem(ImageController::detectarBordas); }
    @FXML private void onSuavizar() { pedirAjuste("Raio de suavização", "Raio (0,1 a 20):", 2, 0.1, 20, ImageController::suavizar); }
    @FXML private void onNitidez() { comImagem(ImageController::nitidez); }
    @FXML private void onMediana() { comImagem(ImageController::mediana); }
    @FXML private void onBinarizar() { comImagem(ImageController::binarizarAutomatico); }
    @FXML private void onGama() { pedirAjuste("Ajustar gama", "Fator de gama (0,1 a 5):", 1.5, 0.1, 5, ImageController::ajustarGama); }
    @FXML private void onRecarregar() { comImagem(ImageController::recarregar); }
    @FXML private void onBrilho() { comImagem(c -> c.focarBrilho()); }

    private void pedirAjuste(String titulo, String mensagem, double padrao, double minimo, double maximo,
                             java.util.function.BiConsumer<ImageController, Double> aplicar) {
        if (controllerSelecionado() == null) return;
        TextInputDialog dialogo = new TextInputDialog(Double.toString(padrao));
        dialogo.initOwner(stageSelecionado);
        dialogo.setTitle(titulo);
        dialogo.setHeaderText(titulo);
        dialogo.setContentText(mensagem);
        dialogo.showAndWait().ifPresent(valor -> {
            try {
                double numero = Double.parseDouble(valor.trim().replace(',', '.'));
                if (!Double.isFinite(numero) || numero < minimo || numero > maximo) throw new NumberFormatException();
                comImagem(c -> aplicar.accept(c, numero));
            } catch (NumberFormatException ex) { erro("Digite um número entre " + minimo + " e " + maximo + "."); }
        });
    }

    private void erro(String mensagem) {
        Alert alerta = new Alert(Alert.AlertType.ERROR, mensagem);
        alerta.initOwner(painel.getScene().getWindow());
        alerta.showAndWait();
    }
}
