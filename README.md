# PhotoPaintFX

Editor de imagens em JavaFX desenvolvido por **Leo Barros** e **Murilo Bosso**.

## Executar

Requer JDK 24. No Windows, configure `JAVA_HOME` para o JDK e execute:

```powershell
.\mvnw.cmd javafx:run
```

Para compilar e testar:

```powershell
.\mvnw.cmd clean test
```

## Recursos

- Abertura de JPEG, JPG, GIF e PNG; salvamento e “Salvar como” nesses formatos.
- Barra de status com dimensões, zoom de 0 a 100% e ajuste de brilho.
- Tons de cinza, preto e branco, negativo, espelhamento horizontal e vertical.
- Filtros ImageJ: bordas, suavização com raio ajustável, nitidez, mediana, limiar automático e gama ajustável.
- Ferramentas de caneta, retângulo e círculo; comando para recarregar a imagem original.
- Aviso de alterações não salvas ao fechar e menus que respondem à imagem selecionada.

Os ícones de desenho vieram de `icones.7z`, fornecido junto com o projeto original.
