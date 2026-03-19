package cz.be.r4u.ui;

import cz.be.r4u.entity.UserAccount;
import cz.be.r4u.enums.DefectSizeClass;
import cz.be.r4u.enums.DefectType;
import cz.be.r4u.enums.RollStatus;
import cz.be.r4u.service.DashboardService;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.geometry.Insets;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.chart.CategoryAxis;
import javafx.scene.chart.NumberAxis;
import javafx.scene.chart.ScatterChart;
import javafx.scene.chart.XYChart;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.TableCell;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableRow;
import javafx.scene.control.TableView;
import javafx.scene.control.Tooltip;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Region;
import javafx.stage.Modality;
import javafx.stage.Stage;
import org.springframework.stereotype.Component;

import java.io.ByteArrayInputStream;
import java.text.DecimalFormat;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.Map;
import java.util.stream.IntStream;

@Component
public class RoleDetailController {

    private static final DateTimeFormatter DATE_TIME_FORMATTER = DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm");
    private static final DecimalFormat DECIMAL_FORMAT = new DecimalFormat("0.00");
    private static final double MAX_ROLL_LENGTH = 5000.0;
    private static final int MAX_BOBINA = 18;

    private final SceneManager sceneManager;
    private final DashboardService dashboardService;

    @FXML
    private Label titleLabel;

    @FXML
    private Label operatorLabel;

    @FXML
    private Label createdAtLabel;

    @FXML
    private Label orderNumberLabel;

    @FXML
    private Label minLabel;

    @FXML
    private Label statusLabel;

    @FXML
    private Label defectsCountLabel;

    @FXML
    private FlowPane legendPane;

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
    private TableColumn<DashboardService.DefectRow, Void> defectImageColumn;

    @FXML
    private ScatterChart<Number, String> rollChart;

    @FXML
    private NumberAxis rollChartXAxis;

    @FXML
    private CategoryAxis rollChartYAxis;

    private final Map<Long, Node> defectNodeMap = new HashMap<>();

    private UserAccount loggedUser;
    private DashboardService.DashboardRow selectedRoll;
    private Long highlightedDefectId;

    public RoleDetailController(SceneManager sceneManager, DashboardService dashboardService) {
        this.sceneManager = sceneManager;
        this.dashboardService = dashboardService;
    }

    @FXML
    public void initialize() {
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
        configureImageColumn();
        configureRollChart();
        configureSelectionHandling();
        configureDefectTableRowFactory();
        buildLegend();
    }

    public void setContext(UserAccount loggedUser, DashboardService.DashboardRow selectedRoll) {
        this.loggedUser = loggedUser;
        this.selectedRoll = dashboardService.loadDashboardRowByRollId(selectedRoll.rollId());

        operatorLabel.setText(loggedUser == null ? "-" : "Operátor: " + loggedUser.getUsername());
        titleLabel.setText("Detail role");
        createdAtLabel.setText(formatDateTime(this.selectedRoll.createdAt()));
        orderNumberLabel.setText(valueOrDash(this.selectedRoll.orderNumber()));
        minLabel.setText(valueOrDash(this.selectedRoll.min()));
        statusLabel.setText(this.selectedRoll.status() == null ? "-" : this.selectedRoll.status().name());
        defectsCountLabel.setText(valueOrDash(this.selectedRoll.defectsCount()));

        if (this.selectedRoll.status() == RollStatus.OK) {
            statusLabel.setStyle("-fx-text-fill: #1B7F3B; -fx-font-weight: bold;");
        } else if (this.selectedRoll.status() == RollStatus.NOK) {
            statusLabel.setStyle("-fx-text-fill: #C62828; -fx-font-weight: bold;");
        } else {
            statusLabel.setStyle("");
        }

        defectTable.setItems(FXCollections.observableArrayList(this.selectedRoll.defects()));
        renderRollChart(this.selectedRoll);

        if (!this.selectedRoll.defects().isEmpty()) {
            defectTable.getSelectionModel().selectFirst();
        }
    }

    @FXML
    public void onBackToDashboardClick() {
        if (loggedUser != null) {
            sceneManager.showDashboard(loggedUser);
            return;
        }
        sceneManager.showLogin();
    }

    @FXML
    public void onOpenRejectRulesClick() {
        sceneManager.showRejectRules(loggedUser, selectedRoll);
    }

    private void configureTableFormatting() {
        defectCreatedAtColumn.setCellFactory(column -> new TableCell<>() {
            @Override
            protected void updateItem(LocalDateTime item, boolean empty) {
                super.updateItem(item, empty);
                setText(empty || item == null ? null : item.format(DATE_TIME_FORMATTER));
                setStyle("-fx-text-fill: #1F1F1F;");
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

                String baseStyle = "-fx-font-weight: bold;";
                switch (item) {
                    case DRAHA_MASCOTTE -> setStyle(baseStyle + "-fx-text-fill: #2E7D32;");
                    case EDGE_CUT -> setStyle(baseStyle + "-fx-text-fill: #1E5AA8;");
                    case EDGE_TEARED -> setStyle(baseStyle + "-fx-text-fill: #00897B;");
                    case PERFO -> setStyle(baseStyle + "-fx-text-fill: #EF6C00;");
                    case WHITE_SPOT -> setStyle(baseStyle + "-fx-text-fill: #42A5F5;");
                    case WRINKLE -> setStyle(baseStyle + "-fx-text-fill: #6A1B9A;");
                    default -> setStyle(baseStyle + "-fx-text-fill: #1F1F1F;");
                }
            }
        });

        defectCameraColumn.setCellFactory(column -> createDefaultTextCell());
        defectBobinaColumn.setCellFactory(column -> new TableCell<>() {
            @Override
            protected void updateItem(Integer item, boolean empty) {
                super.updateItem(item, empty);
                setText(empty || item == null ? null : String.valueOf(item));
                setStyle("-fx-text-fill: #1F1F1F;");
            }
        });

        defectPositionColumn.setCellFactory(column -> new TableCell<>() {
            @Override
            protected void updateItem(Double item, boolean empty) {
                super.updateItem(item, empty);
                setText(empty || item == null ? null : DECIMAL_FORMAT.format(item));
                setStyle("-fx-text-fill: #1F1F1F;");
            }
        });

        defectAreaColumn.setCellFactory(column -> new TableCell<>() {
            @Override
            protected void updateItem(Double item, boolean empty) {
                super.updateItem(item, empty);
                setText(empty || item == null ? null : DECIMAL_FORMAT.format(item));
                setStyle("-fx-text-fill: #1F1F1F;");
            }
        });

        defectSizeClassColumn.setCellFactory(column -> new TableCell<>() {
            @Override
            protected void updateItem(DefectSizeClass item, boolean empty) {
                super.updateItem(item, empty);
                setText(empty || item == null ? null : item.name());
                setStyle("-fx-text-fill: #1F1F1F; -fx-font-weight: bold;");
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

        defectLocationColumn.setCellFactory(column -> createDefaultTextCell());
        defectClassificationColumn.setCellFactory(column -> createDefaultTextCell());
    }

    private TableCell<DashboardService.DefectRow, String> createDefaultTextCell() {
        return new TableCell<>() {
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                setText(empty || item == null ? null : item);
                setStyle("-fx-text-fill: #1F1F1F;");
            }
        };
    }

    private void configureImageColumn() {
        defectImageColumn.setCellFactory(column -> new TableCell<>() {
            private final Button showButton = new Button("Zobrazit");

            {
                showButton.setOnAction(event -> {
                    DashboardService.DefectRow defectRow = getTableView().getItems().get(getIndex());
                    openDefectImageModal(defectRow);
                });
            }

            @Override
            protected void updateItem(Void item, boolean empty) {
                super.updateItem(item, empty);

                if (empty || getIndex() >= getTableView().getItems().size()) {
                    setText(null);
                    setGraphic(null);
                    return;
                }

                DashboardService.DefectRow defectRow = getTableView().getItems().get(getIndex());

                if (!defectRow.hasImage()) {
                    setText("-");
                    setGraphic(null);
                    setStyle("-fx-text-fill: #1F1F1F;");
                    return;
                }

                setText(null);
                setGraphic(showButton);
                setStyle("");
            }
        });
    }

    private void configureRollChart() {
        rollChartXAxis.setAutoRanging(false);
        rollChartXAxis.setLowerBound(0);
        rollChartXAxis.setUpperBound(MAX_ROLL_LENGTH);
        rollChartXAxis.setTickUnit(500);
        rollChartXAxis.setLabel("Pozice v roli (m)");

        rollChartYAxis.setLabel("Bobina");
        rollChartYAxis.setCategories(FXCollections.observableArrayList(
                IntStream.rangeClosed(1, MAX_BOBINA)
                        .mapToObj(String::valueOf)
                        .toList()
        ));

        rollChart.setLegendVisible(false);
        rollChart.setAnimated(false);
        rollChart.setHorizontalGridLinesVisible(true);
        rollChart.setVerticalGridLinesVisible(true);
        rollChart.setStyle("-fx-font-size: 10px;");
    }

    private void configureSelectionHandling() {
        defectTable.getSelectionModel().selectedItemProperty().addListener((observable, oldValue, newValue) -> {
            if (newValue == null) {
                highlightDefectNode(null);
                return;
            }

            highlightDefectNode(newValue.defectId());
        });
    }

    private void configureDefectTableRowFactory() {
        defectTable.setRowFactory(tableView -> new TableRow<>() {
            @Override
            protected void updateItem(DashboardService.DefectRow item, boolean empty) {
                super.updateItem(item, empty);

                if (empty || item == null) {
                    setStyle("");
                    return;
                }

                if (item.defectId() != null && item.defectId().equals(highlightedDefectId)) {
                    setStyle("-fx-background-color: #DCEBFF; -fx-text-background-color: #1F1F1F;");
                    return;
                }

                if (item.reject()) {
                    setStyle("-fx-background-color: #FFF1F1; -fx-text-background-color: #1F1F1F;");
                    return;
                }

                setStyle("-fx-text-background-color: #1F1F1F;");
            }
        });
    }

    private void buildLegend() {
        legendPane.getChildren().clear();

        for (DefectType defectType : DefectType.values()) {
            Region colorBox = new Region();
            colorBox.setMinSize(12, 12);
            colorBox.setPrefSize(12, 12);
            colorBox.setStyle(
                    "-fx-background-color: " + getColorForDefectType(defectType) + ";" +
                            "-fx-background-radius: 6px;"
            );

            Label label = new Label(defectType.name());
            label.setStyle("-fx-font-size: 12px;");

            HBox item = new HBox(6, colorBox, label);
            item.setStyle("-fx-alignment: center-left;");

            legendPane.getChildren().add(item);
        }

        Region rejectBox = new Region();
        rejectBox.setMinSize(12, 12);
        rejectBox.setPrefSize(12, 12);
        rejectBox.setStyle(
                "-fx-background-color: #FFFFFF;" +
                        "-fx-border-color: #C62828;" +
                        "-fx-border-width: 3;" +
                        "-fx-background-radius: 6px;" +
                        "-fx-border-radius: 6px;"
        );

        Label rejectLabel = new Label("Vyřazení");
        rejectLabel.setStyle("-fx-font-size: 12px; -fx-font-weight: bold;");

        HBox rejectItem = new HBox(6, rejectBox, rejectLabel);
        rejectItem.setStyle("-fx-alignment: center-left;");

        legendPane.getChildren().add(rejectItem);

        Region selectedBox = new Region();
        selectedBox.setMinSize(12, 12);
        selectedBox.setPrefSize(12, 12);
        selectedBox.setStyle(
                "-fx-background-color: #9E9E9E;" +
                        "-fx-border-color: #1F1F1F;" +
                        "-fx-border-width: 3;" +
                        "-fx-background-radius: 6px;" +
                        "-fx-border-radius: 6px;"
        );

        Label selectedLabel = new Label("Vybraný bod");
        selectedLabel.setStyle("-fx-font-size: 12px; -fx-font-weight: bold;");

        HBox selectedItem = new HBox(6, selectedBox, selectedLabel);
        selectedItem.setStyle("-fx-alignment: center-left;");

        legendPane.getChildren().add(selectedItem);
    }

    private void renderRollChart(DashboardService.DashboardRow dashboardRow) {
        rollChart.getData().clear();
        defectNodeMap.clear();
        highlightedDefectId = null;

        for (DefectType defectType : DefectType.values()) {
            XYChart.Series<Number, String> series = new XYChart.Series<>();
            series.setName(defectType.name());

            dashboardRow.defects().stream()
                    .filter(defect -> defect.type() == defectType)
                    .filter(defect -> defect.positionInRoll() != null && defect.bobinaColumnNumber() != null)
                    .forEach(defect -> {
                        XYChart.Data<Number, String> dataPoint =
                                new XYChart.Data<>(defect.positionInRoll(), String.valueOf(defect.bobinaColumnNumber()));

                        dataPoint.nodeProperty().addListener((obs, oldNode, newNode) -> {
                            if (newNode != null) {
                                defectNodeMap.put(defect.defectId(), newNode);
                                applyDefectNodeStyle(
                                        newNode,
                                        defect.type(),
                                        defect.defectId().equals(highlightedDefectId),
                                        defect.reject()
                                );

                                Tooltip tooltip = new Tooltip(
                                        "Typ: " + defect.type().name() + "\n" +
                                                "Bobina: " + defect.bobinaColumnNumber() + "\n" +
                                                "Pozice: " + DECIMAL_FORMAT.format(defect.positionInRoll()) + " m\n" +
                                                "Velikost: " + (defect.sizeClass() == null ? "-" : defect.sizeClass().name()) + "\n" +
                                                "Vyřadit: " + (defect.reject() ? "ANO" : "NE") + "\n" +
                                                "Kamera: " + valueOrDash(defect.camera())
                                );
                                Tooltip.install(newNode, tooltip);

                                newNode.setOnMouseClicked(event -> selectDefectInTable(defect.defectId()));
                            }
                        });

                        series.getData().add(dataPoint);
                    });

            if (!series.getData().isEmpty()) {
                rollChart.getData().add(series);
            }
        }
    }

    private void selectDefectInTable(Long defectId) {
        if (defectId == null) {
            return;
        }

        for (DashboardService.DefectRow defectRow : defectTable.getItems()) {
            if (defectId.equals(defectRow.defectId())) {
                defectTable.getSelectionModel().select(defectRow);
                defectTable.scrollTo(defectRow);
                break;
            }
        }
    }

    private void highlightDefectNode(Long defectId) {
        highlightedDefectId = defectId;

        for (DashboardService.DefectRow defectRow : defectTable.getItems()) {
            Node node = defectNodeMap.get(defectRow.defectId());
            if (node == null) {
                continue;
            }

            applyDefectNodeStyle(
                    node,
                    defectRow.type(),
                    defectRow.defectId().equals(defectId),
                    defectRow.reject()
            );
        }

        defectTable.refresh();
    }

    private void applyDefectNodeStyle(Node node, DefectType defectType, boolean highlighted, boolean reject) {
        String color = getColorForDefectType(defectType);
        String circleShape = "-fx-shape: \"M 0,4 A 4,4 0 1,1 8,4 A 4,4 0 1,1 0,4 Z\";";

        if (highlighted) {
            node.setStyle(
                    circleShape +
                            "-fx-background-color: " + color + ";" +
                            "-fx-background-radius: 8px;" +
                            "-fx-padding: 8px;" +
                            "-fx-border-color: #1F1F1F;" +
                            "-fx-border-width: 3px;" +
                            "-fx-border-radius: 8px;"
            );
            return;
        }

        if (reject) {
            node.setStyle(
                    circleShape +
                            "-fx-background-color: " + color + ";" +
                            "-fx-background-radius: 7px;" +
                            "-fx-padding: 6px;" +
                            "-fx-border-color: #C62828;" +
                            "-fx-border-width: 3px;" +
                            "-fx-border-radius: 7px;"
            );
            return;
        }

        node.setStyle(
                circleShape +
                        "-fx-background-color: " + color + ";" +
                        "-fx-background-radius: 6px;" +
                        "-fx-padding: 5px;" +
                        "-fx-border-width: 0px;"
        );
    }

    private String getColorForDefectType(DefectType defectType) {
        return switch (defectType) {
            case DRAHA_MASCOTTE -> "#2E7D32";
            case EDGE_CUT -> "#1E5AA8";
            case EDGE_TEARED -> "#00897B";
            case PERFO -> "#EF6C00";
            case WHITE_SPOT -> "#42A5F5";
            case WRINKLE -> "#6A1B9A";
        };
    }

    private void openDefectImageModal(DashboardService.DefectRow defectRow) {
        byte[] imageBytes = dashboardService.loadDefectImage(defectRow.defectId());

        if (imageBytes == null || imageBytes.length == 0) {
            return;
        }

        Image image = new Image(new ByteArrayInputStream(imageBytes));

        ImageView fullImageView = new ImageView(image);
        fullImageView.setPreserveRatio(true);
        fullImageView.setFitWidth(1400);
        fullImageView.setFitHeight(900);

        ScrollPane scrollPane = new ScrollPane(fullImageView);
        scrollPane.setFitToWidth(true);
        scrollPane.setFitToHeight(true);
        scrollPane.setPannable(true);

        BorderPane root = new BorderPane(scrollPane);
        root.setPadding(new Insets(12));

        Scene scene = new Scene(root, 1100, 760);

        Stage modalStage = new Stage();
        modalStage.initModality(Modality.APPLICATION_MODAL);
        modalStage.setTitle("Originální obrázek defektu");
        modalStage.setScene(scene);
        modalStage.showAndWait();
    }

    private String formatDateTime(LocalDateTime value) {
        return value == null ? "-" : value.format(DATE_TIME_FORMATTER);
    }

    private String valueOrDash(Object value) {
        return value == null ? "-" : String.valueOf(value);
    }
}