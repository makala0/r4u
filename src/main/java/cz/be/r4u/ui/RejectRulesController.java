package cz.be.r4u.ui;

import cz.be.r4u.entity.UserAccount;
import cz.be.r4u.enums.DefectSizeClass;
import cz.be.r4u.enums.DefectType;
import cz.be.r4u.service.DashboardService;
import cz.be.r4u.service.DefectRejectRuleService;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.scene.control.CheckBox;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.TableCell;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import lombok.Setter;
import org.springframework.stereotype.Component;

@Component
public class RejectRulesController {

    private final SceneManager sceneManager;
    private final DefectRejectRuleService defectRejectRuleService;

    @FXML
    private Label operatorLabel;

    @FXML
    private Label messageLabel;

    @FXML
    private TextField sMaxAreaField;

    @FXML
    private TextField mMaxAreaField;

    @FXML
    private TextField lMaxAreaField;

    @FXML
    private TableView<RejectRuleRow> rulesTable;

    @FXML
    private TableColumn<RejectRuleRow, DefectType> defectTypeColumn;

    @FXML
    private TableColumn<RejectRuleRow, DefectSizeClass> rejectFromSizeColumn;

    @FXML
    private TableColumn<RejectRuleRow, Boolean> enabledColumn;

    private UserAccount loggedUser;
    private DashboardService.DashboardRow selectedRoll;

    public RejectRulesController(SceneManager sceneManager, DefectRejectRuleService defectRejectRuleService) {
        this.sceneManager = sceneManager;
        this.defectRejectRuleService = defectRejectRuleService;
    }

    @FXML
    public void initialize() {
        defectTypeColumn.setCellValueFactory(cellData -> new SimpleObjectProperty<>(cellData.getValue().defectType()));
        rejectFromSizeColumn.setCellValueFactory(cellData -> new SimpleObjectProperty<>(cellData.getValue().rejectFromSizeClass()));
        enabledColumn.setCellValueFactory(cellData -> new SimpleBooleanProperty(cellData.getValue().enabled()));

        configureColumns();
        loadThresholds();
        loadRules();
    }

    public void setContext(UserAccount loggedUser, DashboardService.DashboardRow selectedRoll) {
        this.loggedUser = loggedUser;
        this.selectedRoll = selectedRoll;
        operatorLabel.setText(loggedUser == null ? "-" : "Operátor: " + loggedUser.getUsername());
    }

    @FXML
    public void onBackClick() {
        if (loggedUser != null && selectedRoll != null) {
            sceneManager.showRoleDetail(loggedUser, selectedRoll);
            return;
        }

        if (loggedUser != null) {
            sceneManager.showHome(loggedUser);
            return;
        }

        sceneManager.showLogin();
    }

    @FXML
    public void onRefreshClick() {
        clearMessage();
        loadThresholds();
        loadRules();
    }

    @FXML
    public void onSaveThresholdsClick() {
        try {
            Double sMaxArea = parseThreshold(sMaxAreaField.getText(), "S");
            Double mMaxArea = parseThreshold(mMaxAreaField.getText(), "M");
            Double lMaxArea = parseThreshold(lMaxAreaField.getText(), "L");

            defectRejectRuleService.updateThresholds(sMaxArea, mMaxArea, lMaxArea);
            showInfo("Hranice velikostí byly uloženy.");
            loadThresholds();
        } catch (Exception ex) {
            showError("Nepodařilo se uložit hranice velikostí: " + ex.getMessage());
        }
    }

    private void configureColumns() {
        defectTypeColumn.setCellFactory(column -> new TableCell<>() {
            @Override
            protected void updateItem(DefectType item, boolean empty) {
                super.updateItem(item, empty);
                setText(empty || item == null ? null : item.name());
            }
        });

        rejectFromSizeColumn.setCellFactory(column -> new TableCell<>() {
            private final ComboBox<DefectSizeClass> comboBox =
                    new ComboBox<>(FXCollections.observableArrayList(DefectSizeClass.values()));

            {
                comboBox.setOnAction(event -> {
                    RejectRuleRow row = getTableRow() == null ? null : getTableRow().getItem();
                    if (row == null) {
                        return;
                    }

                    DefectSizeClass selectedValue = comboBox.getValue();
                    if (selectedValue == null) {
                        return;
                    }

                    row.setRejectFromSizeClass(selectedValue);
                    saveRow(row);
                });
            }

            @Override
            protected void updateItem(DefectSizeClass item, boolean empty) {
                super.updateItem(item, empty);

                if (empty || getTableRow() == null || getTableRow().getItem() == null) {
                    setText(null);
                    setGraphic(null);
                    return;
                }

                comboBox.setValue(item);
                setText(null);
                setGraphic(comboBox);
            }
        });

        enabledColumn.setCellFactory(column -> new TableCell<>() {
            private final CheckBox checkBox = new CheckBox();

            {
                checkBox.setOnAction(event -> {
                    RejectRuleRow row = getTableRow() == null ? null : getTableRow().getItem();
                    if (row == null) {
                        return;
                    }

                    row.setEnabled(checkBox.isSelected());
                    saveRow(row);
                });
            }

            @Override
            protected void updateItem(Boolean item, boolean empty) {
                super.updateItem(item, empty);

                if (empty || getTableRow() == null || getTableRow().getItem() == null) {
                    setText(null);
                    setGraphic(null);
                    return;
                }

                checkBox.setSelected(Boolean.TRUE.equals(item));
                setText(null);
                setGraphic(checkBox);
            }
        });
    }

    private void loadThresholds() {
        DefectRejectRuleService.SizeThresholds thresholds = defectRejectRuleService.getThresholds();
        sMaxAreaField.setText(String.valueOf(thresholds.sMaxArea()));
        mMaxAreaField.setText(String.valueOf(thresholds.mMaxArea()));
        lMaxAreaField.setText(String.valueOf(thresholds.lMaxArea()));
    }

    private void loadRules() {
        rulesTable.setItems(FXCollections.observableArrayList(
                defectRejectRuleService.getOrCreateAllRules().stream()
                        .map(rule -> new RejectRuleRow(
                                rule.getId(),
                                rule.getDefectType(),
                                rule.getRejectFromSizeClass(),
                                rule.isEnabled()
                        ))
                        .toList()
        ));
    }

    private void saveRow(RejectRuleRow row) {
        try {
            defectRejectRuleService.updateRule(
                    row.id(),
                    row.rejectFromSizeClass(),
                    row.enabled()
            );
            showInfo("Pravidlo bylo uloženo.");
        } catch (Exception ex) {
            showError("Nepodařilo se uložit pravidlo: " + ex.getMessage());
        }
    }

    private Double parseThreshold(String value, String label) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException("Hranice " + label + " musí být vyplněna.");
        }

        try {
            return Double.valueOf(value.trim().replace(',', '.'));
        } catch (NumberFormatException ex) {
            throw new IllegalArgumentException("Hranice " + label + " musí být číslo.");
        }
    }

    private void showInfo(String message) {
        messageLabel.setStyle("-fx-text-fill: #1B7F3B; -fx-font-weight: bold;");
        messageLabel.setText(message);
    }

    private void showError(String message) {
        messageLabel.setStyle("-fx-text-fill: #C62828; -fx-font-weight: bold;");
        messageLabel.setText(message);
    }

    private void clearMessage() {
        messageLabel.setText("");
    }

    public static class RejectRuleRow {
        private final Long id;
        private final DefectType defectType;
        @Setter
        private DefectSizeClass rejectFromSizeClass;
        @Setter
        private boolean enabled;

        public RejectRuleRow(Long id, DefectType defectType, DefectSizeClass rejectFromSizeClass, boolean enabled) {
            this.id = id;
            this.defectType = defectType;
            this.rejectFromSizeClass = rejectFromSizeClass;
            this.enabled = enabled;
        }

        public Long id() {
            return id;
        }

        public DefectType defectType() {
            return defectType;
        }

        public DefectSizeClass rejectFromSizeClass() {
            return rejectFromSizeClass;
        }

        public boolean enabled() {
            return enabled;
        }
    }
}