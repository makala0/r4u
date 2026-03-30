package cz.be.r4u.ui;

import cz.be.r4u.entity.UserAccount;
import cz.be.r4u.enums.DefectSizeClass;
import cz.be.r4u.enums.DefectType;
import cz.be.r4u.enums.RollStatus;
import cz.be.r4u.service.DashboardExportService;
import cz.be.r4u.service.DashboardService;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.geometry.Insets;
import javafx.scene.Scene;
import javafx.scene.control.ComboBox;
import javafx.scene.control.DatePicker;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.TableCell;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableRow;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.BorderPane;
import javafx.stage.FileChooser;
import javafx.stage.Modality;
import javafx.stage.Stage;
import org.springframework.stereotype.Component;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.text.DecimalFormat;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Component
public class DashboardController {

    private static final DateTimeFormatter DATE_TIME_FORMATTER = DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm");
    private static final DateTimeFormatter TIME_FORMATTER = DateTimeFormatter.ofPattern("HH:mm");
    private static final DecimalFormat DECIMAL_FORMAT = new DecimalFormat("0.00");

    private final SceneManager sceneManager;
    private final DashboardService dashboardService;
    private final DashboardExportService dashboardExportService;

    @FXML
    private Label welcomeLabel;

    @FXML
    private DatePicker fromDatePicker;

    @FXML
    private TextField fromTimeField;

    @FXML
    private DatePicker toDatePicker;

    @FXML
    private TextField toTimeField;

    @FXML
    private TextField orderNumberField;

    @FXML
    private ComboBox<RollStatus> statusComboBox;

    @FXML
    private ComboBox<DefectType> defectTypeComboBox;

    @FXML
    private Label totalCountLabel;

    @FXML
    private Label okCountLabel;

    @FXML
    private Label nokCountLabel;

    @FXML
    private Label defectRollCountLabel;

    @FXML
    private Label messageLabel;

    @FXML
    private TableView<DashboardService.DashboardRow> resultTable;

    @FXML
    private TableColumn<DashboardService.DashboardRow, LocalDateTime> createdAtColumn;

    @FXML
    private TableColumn<DashboardService.DashboardRow, Integer> orderNumberColumn;

    @FXML
    private TableColumn<DashboardService.DashboardRow, Integer> minColumn;

    @FXML
    private TableColumn<DashboardService.DashboardRow, RollStatus> statusColumn;

    @FXML
    private TableColumn<DashboardService.DashboardRow, Integer> defectsCountColumn;

    @FXML
    private TableView<DashboardService.DefectRow> defectTable;

    @FXML
    private TableColumn<DashboardService.DefectRow, LocalDateTime> defectCreatedAtColumn;

    @FXML
    private TableColumn<DashboardService.DefectRow, DefectType> defectTypeColumn;

    @FXML
    private TableColumn<DashboardService.DefectRow, String> defectCameraColumn;

    @FXML
    private TableColumn<DashboardService.DefectRow, Integer> defectBobinaColumn;

    @FXML
    private TableColumn<DashboardService.DefectRow, Double> defectPositionColumn;

    @FXML
    private TableColumn<DashboardService.DefectRow, Double> defectAreaColumn;

    @FXML
    private TableColumn<DashboardService.DefectRow, DefectSizeClass> defectSizeClassColumn;

    @FXML
    private TableColumn<DashboardService.DefectRow, Boolean> defectRejectColumn;

    @FXML
    private TableColumn<DashboardService.DefectRow, String> defectLocationColumn;

    @FXML
    private TableColumn<DashboardService.DefectRow, String> defectClassificationColumn;

    @FXML
    private ImageView defectPreviewImageView;

    @FXML
    private Label defectPreviewPlaceholderLabel;

    private final Map<Long, Image> defectImageCache = new HashMap<>();

    private UserAccount loggedUser;

    public DashboardController(
            SceneManager sceneManager,
            DashboardService dashboardService,
            DashboardExportService dashboardExportService
    ) {
        this.sceneManager = sceneManager;
        this.dashboardService = dashboardService;
        this.dashboardExportService = dashboardExportService;
    }

    @FXML
    public void initialize() {
        statusComboBox.setItems(FXCollections.observableArrayList(RollStatus.values()));
        defectTypeComboBox.setItems(FXCollections.observableArrayList(DefectType.values()));

        createdAtColumn.setCellValueFactory(cellData -> new SimpleObjectProperty<>(cellData.getValue().createdAt()));
        orderNumberColumn.setCellValueFactory(cellData -> new SimpleObjectProperty<>(cellData.getValue().orderNumber()));
        minColumn.setCellValueFactory(cellData -> new SimpleObjectProperty<>(cellData.getValue().min()));
        statusColumn.setCellValueFactory(cellData -> new SimpleObjectProperty<>(cellData.getValue().status()));
        defectsCountColumn.setCellValueFactory(cellData -> new SimpleObjectProperty<>(cellData.getValue().defectsCount()));

        defectCreatedAtColumn.setCellValueFactory(cellData -> new SimpleObjectProperty<>(cellData.getValue().createdAt()));
        defectTypeColumn.setCellValueFactory(cellData -> new SimpleObjectProperty<>(cellData.getValue().type()));
        defectCameraColumn.setCellValueFactory(cellData -> new SimpleStringProperty(cellData.getValue().camera()));
        defectBobinaColumn.setCellValueFactory(cellData -> new SimpleObjectProperty<>(cellData.getValue().bobinaColumnNumber()));
        defectPositionColumn.setCellValueFactory(cellData -> new SimpleObjectProperty<>(cellData.getValue().positionInRoll()));
        defectAreaColumn.setCellValueFactory(cellData -> new SimpleObjectProperty<>(cellData.getValue().defectArea()));
        defectSizeClassColumn.setCellValueFactory(cellData -> new SimpleObjectProperty<>(cellData.getValue().sizeClass()));
        defectRejectColumn.setCellValueFactory(cellData -> new SimpleObjectProperty<>(cellData.getValue().reject()));
        defectLocationColumn.setCellValueFactory(cellData -> new SimpleStringProperty(cellData.getValue().defectLocation()));
        defectClassificationColumn.setCellValueFactory(cellData -> new SimpleStringProperty(cellData.getValue().classification()));

        configureTableFormatting();
        configureSelectionHandling();
        configurePreviewImage();
        configureResultTableRowFactory();

        setDefaultFilters();
        clearMessage();
        clearDefectTable();
        loadInitialData();
    }

    public void setLoggedUser(UserAccount loggedUser) {
        this.loggedUser = loggedUser;
        if (welcomeLabel != null && loggedUser != null) {
            welcomeLabel.setText("Operátor: " + loggedUser.getUsername());
        }
    }

    @FXML
    public void onFilterClick() {
        try {
            clearMessage();

            LocalDateTime from = parseDateTime(fromDatePicker.getValue(), fromTimeField.getText(), LocalTime.MIN, "Čas od");
            LocalDateTime to = parseDateTime(toDatePicker.getValue(), toTimeField.getText(), LocalTime.MAX, "Čas do");

            if (from != null && to != null && from.isAfter(to)) {
                showError("Datum a čas od nesmí být později než datum a čas do.");
                return;
            }

            Integer orderNumber = parseOrderNumber(orderNumberField.getText());
            RollStatus status = statusComboBox.getValue();
            DefectType defectType = defectTypeComboBox.getValue();

            DashboardService.DashboardFilter filter = new DashboardService.DashboardFilter(
                    from,
                    to,
                    orderNumber,
                    status,
                    defectType
            );

            applyDashboardData(dashboardService.filterDashboard(filter));
        } catch (IllegalArgumentException ex) {
            showError(ex.getMessage());
        }
    }

    @FXML
    public void onResetClick() {
        setDefaultFilters();
        orderNumberField.clear();
        statusComboBox.setValue(null);
        defectTypeComboBox.setValue(null);
        clearMessage();
        loadInitialData();
    }

    @FXML
    public void onBackToHomeClick() {
        if (loggedUser != null) {
            sceneManager.showHome(loggedUser);
            return;
        }
        sceneManager.showLogin();
    }

    @FXML
    public void onExportCsvClick() {
        List<DashboardService.DashboardRow> rows = List.copyOf(resultTable.getItems());
        if (rows.isEmpty()) {
            showError("Neni co exportovat. Tabulka je prazdna.");
            return;
        }

        File selectedFile = chooseExportFile("CSV export", "dashboard-export.csv", "CSV soubor", "*.csv");
        if (selectedFile == null) {
            return;
        }

        Path targetPath = appendExtensionIfMissing(selectedFile.toPath(), ".csv");
        try {
            dashboardExportService.exportDashboardRowsToCsv(targetPath, rows);
            showSuccess("CSV export uspesne ulozen: " + targetPath.getFileName());
        } catch (IOException ex) {
            showError("CSV export selhal: " + ex.getMessage());
        }
    }

    @FXML
    public void onExportXlsxClick() {
        List<DashboardService.DashboardRow> rows = List.copyOf(resultTable.getItems());
        if (rows.isEmpty()) {
            showError("Neni co exportovat. Tabulka je prazdna.");
            return;
        }

        List<DashboardService.DefectRow> defects = List.copyOf(defectTable.getItems());
        File selectedFile = chooseExportFile("XLSX export", "dashboard-export.xlsx", "Excel soubor", "*.xlsx");
        if (selectedFile == null) {
            return;
        }

        Path targetPath = appendExtensionIfMissing(selectedFile.toPath(), ".xlsx");
        try {
            dashboardExportService.exportDashboardRowsToXlsx(targetPath, rows, defects);
            showSuccess("XLSX export uspesne ulozen: " + targetPath.getFileName());
        } catch (IOException ex) {
            showError("XLSX export selhal: " + ex.getMessage());
        }
    }

    private void configureTableFormatting() {
        createdAtColumn.setCellFactory(column -> new TableCell<>() {
            @Override
            protected void updateItem(LocalDateTime item, boolean empty) {
                super.updateItem(item, empty);
                setText(empty || item == null ? null : item.format(DATE_TIME_FORMATTER));
            }
        });

        statusColumn.setCellFactory(column -> new TableCell<>() {
            @Override
            protected void updateItem(RollStatus item, boolean empty) {
                super.updateItem(item, empty);

                if (empty || item == null) {
                    setText(null);
                    setStyle("");
                    return;
                }

                setText(item.name());

                if (item == RollStatus.OK) {
                    setStyle("-fx-text-fill: #1B7F3B; -fx-font-weight: bold;");
                } else {
                    setStyle("-fx-text-fill: #C62828; -fx-font-weight: bold;");
                }
            }
        });

        defectCreatedAtColumn.setCellFactory(column -> new TableCell<>() {
            @Override
            protected void updateItem(LocalDateTime item, boolean empty) {
                super.updateItem(item, empty);
                setText(empty || item == null ? null : item.format(DATE_TIME_FORMATTER));
            }
        });

        defectTypeColumn.setCellFactory(column -> new TableCell<>() {
            @Override
            protected void updateItem(DefectType item, boolean empty) {
                super.updateItem(item, empty);

                if (empty || item == null) {
                    setText(null);
                    setStyle("");
                    return;
                }

                setText(item.name());

                switch (item) {
                    case EDGE_CUT, EDGE_TEARED -> setStyle("-fx-text-fill: #C62828; -fx-font-weight: bold;");
                    case WHITE_SPOT -> setStyle("-fx-text-fill: #1565C0; -fx-font-weight: bold;");
                    case WRINKLE -> setStyle("-fx-text-fill: #6A1B9A; -fx-font-weight: bold;");
                    case PERFO -> setStyle("-fx-text-fill: #EF6C00; -fx-font-weight: bold;");
                    case DRAHA_MASCOTTE -> setStyle("-fx-text-fill: #2E7D32; -fx-font-weight: bold;");
                    default -> setStyle("-fx-font-weight: bold;");
                }
            }
        });

        defectBobinaColumn.setCellFactory(column -> new TableCell<>() {
            @Override
            protected void updateItem(Integer item, boolean empty) {
                super.updateItem(item, empty);
                setText(empty || item == null ? null : String.valueOf(item));
            }
        });

        defectPositionColumn.setCellFactory(column -> new TableCell<>() {
            @Override
            protected void updateItem(Double item, boolean empty) {
                super.updateItem(item, empty);
                setText(empty || item == null ? null : DECIMAL_FORMAT.format(item));
            }
        });

        defectAreaColumn.setCellFactory(column -> new TableCell<>() {
            @Override
            protected void updateItem(Double item, boolean empty) {
                super.updateItem(item, empty);
                setText(empty || item == null ? null : DECIMAL_FORMAT.format(item));
            }
        });

        defectSizeClassColumn.setCellFactory(column -> new TableCell<>() {
            @Override
            protected void updateItem(DefectSizeClass item, boolean empty) {
                super.updateItem(item, empty);
                setText(empty || item == null ? null : item.name());
            }
        });

        defectRejectColumn.setCellFactory(column -> new TableCell<>() {
            @Override
            protected void updateItem(Boolean item, boolean empty) {
                super.updateItem(item, empty);

                if (empty || item == null) {
                    setText(null);
                    setStyle("");
                    return;
                }

                setText(item ? "ANO" : "NE");

                if (item) {
                    setStyle("-fx-text-fill: #C62828; -fx-font-weight: bold;");
                } else {
                    setStyle("-fx-text-fill: #1B7F3B; -fx-font-weight: bold;");
                }
            }
        });
    }

    private void configureSelectionHandling() {
        resultTable.getSelectionModel().selectedItemProperty().addListener((observable, oldValue, newValue) -> {
            defectImageCache.clear();

            if (newValue == null) {
                clearDefectTable();
                return;
            }

            defectTable.setItems(FXCollections.observableArrayList(newValue.defects()));
            if (!newValue.defects().isEmpty()) {
                defectTable.getSelectionModel().selectFirst();
            } else {
                updateDefectPreview(null);
            }
        });

        defectTable.getSelectionModel().selectedItemProperty().addListener((observable, oldValue, newValue) -> updateDefectPreview(newValue));
    }

    private void configureResultTableRowFactory() {
        resultTable.setRowFactory(tableView -> {
            TableRow<DashboardService.DashboardRow> row = new TableRow<>();

            row.setOnMouseClicked(event -> {
                if (event.getClickCount() == 2 && !row.isEmpty()) {
                    DashboardService.DashboardRow selectedRoll = row.getItem();
                    sceneManager.showRoleDetail(loggedUser, selectedRoll);
                }
            });

            return row;
        });
    }

    private void configurePreviewImage() {
        if (defectPreviewImageView != null) {
            defectPreviewImageView.setOnMouseClicked(event -> {
                DashboardService.DefectRow selectedDefect = defectTable.getSelectionModel().getSelectedItem();
                if (selectedDefect != null && selectedDefect.hasImage()) {
                    openDefectImageModal(selectedDefect);
                }
            });
        }
    }

    private Image loadDefectImage(Long defectId) {
        if (defectId == null) {
            return null;
        }

        Image cachedImage = defectImageCache.get(defectId);
        if (cachedImage != null) {
            return cachedImage;
        }

        byte[] imageBytes = dashboardService.loadDefectImage(defectId);
        if (imageBytes == null || imageBytes.length == 0) {
            return null;
        }

        Image image = new Image(new ByteArrayInputStream(imageBytes));
        defectImageCache.put(defectId, image);
        return image;
    }

    private void openDefectImageModal(DashboardService.DefectRow defectRow) {
        Image image = loadDefectImage(defectRow.defectId());

        if (image == null) {
            showError("Pro vybraný defekt není obrázek k dispozici.");
            return;
        }

        ImageView fullImageView = new ImageView(image);
        fullImageView.setPreserveRatio(true);
        fullImageView.setFitWidth(1200);
        fullImageView.setFitHeight(800);

        ScrollPane scrollPane = new ScrollPane(fullImageView);
        scrollPane.setFitToWidth(true);
        scrollPane.setFitToHeight(true);
        scrollPane.setPannable(true);

        BorderPane root = new BorderPane(scrollPane);
        root.setPadding(new Insets(12));

        Scene scene = new Scene(root, 1000, 700);

        Stage modalStage = new Stage();
        modalStage.initModality(Modality.APPLICATION_MODAL);
        modalStage.setTitle("Originální obrázek defektu");
        modalStage.setScene(scene);
        modalStage.showAndWait();
    }

    private void setDefaultFilters() {
        LocalDate today = LocalDate.now();

        fromDatePicker.setValue(today);
        fromTimeField.setText("00:00");

        toDatePicker.setValue(today);
        toTimeField.setText(LocalTime.now().withSecond(0).withNano(0).format(TIME_FORMATTER));
    }

    private void loadInitialData() {
        applyDashboardData(dashboardService.loadInitialDashboard());
    }

    private void applyDashboardData(DashboardService.DashboardData dashboardData) {
        resultTable.setItems(FXCollections.observableArrayList(dashboardData.rows()));

        totalCountLabel.setText(String.valueOf(dashboardData.summary().totalCount()));
        okCountLabel.setText(String.valueOf(dashboardData.summary().okCount()));
        nokCountLabel.setText(String.valueOf(dashboardData.summary().nokCount()));
        defectRollCountLabel.setText(String.valueOf(dashboardData.summary().defectRollCount()));

        if (dashboardData.rows().isEmpty()) {
            clearDefectTable();
            return;
        }

        resultTable.getSelectionModel().selectFirst();
    }

    private void clearDefectTable() {
        defectImageCache.clear();
        defectTable.setItems(FXCollections.observableArrayList());
        updateDefectPreview(null);
    }

    private void updateDefectPreview(DashboardService.DefectRow defectRow) {
        if (defectPreviewImageView == null || defectPreviewPlaceholderLabel == null) {
            return;
        }

        if (defectRow == null || !defectRow.hasImage()) {
            defectPreviewImageView.setImage(null);
            defectPreviewImageView.setVisible(false);
            defectPreviewImageView.setManaged(false);
            defectPreviewPlaceholderLabel.setText("Vyber defekt se snímkem.");
            defectPreviewPlaceholderLabel.setVisible(true);
            defectPreviewPlaceholderLabel.setManaged(true);
            return;
        }

        Image image = loadDefectImage(defectRow.defectId());
        if (image == null) {
            defectPreviewImageView.setImage(null);
            defectPreviewImageView.setVisible(false);
            defectPreviewImageView.setManaged(false);
            defectPreviewPlaceholderLabel.setText("Pro vybraný defekt není obrázek k dispozici.");
            defectPreviewPlaceholderLabel.setVisible(true);
            defectPreviewPlaceholderLabel.setManaged(true);
            return;
        }

        defectPreviewImageView.setImage(image);
        defectPreviewImageView.setVisible(true);
        defectPreviewImageView.setManaged(true);
        defectPreviewPlaceholderLabel.setVisible(false);
        defectPreviewPlaceholderLabel.setManaged(false);
    }

    private LocalDateTime parseDateTime(LocalDate date, String timeText, LocalTime fallbackTime, String fieldLabel) {
        if (date == null) {
            return null;
        }

        if (timeText == null || timeText.isBlank()) {
            return LocalDateTime.of(date, fallbackTime);
        }

        try {
            LocalTime parsedTime = LocalTime.parse(timeText.trim(), TIME_FORMATTER);
            return LocalDateTime.of(date, parsedTime);
        } catch (Exception ex) {
            throw new IllegalArgumentException(fieldLabel + " musí být ve formátu HH:mm.");
        }
    }

    private Integer parseOrderNumber(String orderNumberText) {
        if (orderNumberText == null || orderNumberText.isBlank()) {
            return null;
        }

        try {
            return Integer.valueOf(orderNumberText.trim());
        } catch (NumberFormatException ex) {
            throw new IllegalArgumentException("Order number musí být celé číslo.");
        }
    }

    private void showError(String message) {
        messageLabel.setStyle("-fx-text-fill: #C62828; -fx-font-weight: bold;");
        messageLabel.setText(message);
    }

    private void showSuccess(String message) {
        messageLabel.setStyle("-fx-text-fill: #1B7F3B; -fx-font-weight: bold;");
        messageLabel.setText(message);
    }

    private void clearMessage() {
        messageLabel.setText("");
    }

    private File chooseExportFile(String title, String defaultName, String filterLabel, String extensionPattern) {
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle(title);
        fileChooser.setInitialFileName(defaultName);
        fileChooser.getExtensionFilters().add(new FileChooser.ExtensionFilter(filterLabel, extensionPattern));
        return fileChooser.showSaveDialog(resultTable.getScene().getWindow());
    }

    private Path appendExtensionIfMissing(Path path, String extension) {
        String normalizedName = path.getFileName().toString().toLowerCase();
        if (normalizedName.endsWith(extension)) {
            return path;
        }
        return path.resolveSibling(path.getFileName() + extension);
    }
}
