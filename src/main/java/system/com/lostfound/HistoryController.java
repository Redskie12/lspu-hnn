package system.com.lostfound;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import javafx.fxml.FXML;
import javafx.stage.FileChooser;
import javafx.stage.Window;
import javafx.scene.Cursor;
import javafx.scene.Group;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonType;
import javafx.scene.control.Dialog;
import javafx.scene.control.Label;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.Pane;
import javafx.scene.layout.StackPane;
import javafx.scene.paint.Color;
import javafx.scene.shape.Circle;
import javafx.scene.shape.Line;
import javafx.scene.transform.Scale;
import javafx.geometry.Insets;

public class HistoryController {

    // ==========================================
    // FXML COMPONENTS
    // ==========================================

    @FXML private Pane treePane;
    @FXML private Pane treeContainer;

    @FXML private Label yearLabel;
    @FXML private Label titleLabel;
    @FXML private Label descriptionLabel;
    @FXML private Label zoomLabel;

    @FXML private ImageView eventImage;

    // ==========================================
    // BST DATA
    // ==========================================

    private final BST historyBST = new BST();

    private final Map<Integer, HistoryEvent> events =
            new HashMap<>();

    private final Map<Integer, StackPane> nodeViews =
            new HashMap<>();

    private final Map<Integer, Circle> nodeCircles =
            new HashMap<>();

    private final Map<Integer, Circle> nodeHighlights =
            new HashMap<>();

    private final Map<Integer, double[]> nodePositions =
            new HashMap<>();

    // ==========================================
    // BST ZOOM
    // ==========================================

    private final Group treeGroup = new Group();

    private final Scale treeScale =
            new Scale(1, 1, 0, 0);

    // ==========================================
    // TREE DESIGN
    // ==========================================

    private static final double NODE_RADIUS = 38;
    private static final double HIGHLIGHT_RADIUS = 43;

    private static final double HORIZONTAL_GAP = 145;
    private static final double VERTICAL_GAP = 135;

    private static final double START_X = 100;
    private static final double START_Y = 85;

    // ==========================================
    // NODE COLORS
    // ==========================================

    private static final Color NODE_COLOR =
            Color.web("#28764E");

    private static final Color NODE_HOVER_COLOR =
            Color.web("#3D9664");

    private static final Color NODE_SELECTED_COLOR =
            Color.web("#145A32");

    private static final Color HIGHLIGHT_COLOR =
            Color.web("#A7E3B5");

    private static final Color SELECTED_HIGHLIGHT_COLOR =
            Color.web("#C7F5D2");

    // ==========================================
    // ZOOM SETTINGS
    // ==========================================

    private static final double MIN_ZOOM = 0.5;
    private static final double MAX_ZOOM = 2.5;
    private static final double ZOOM_STEP = 0.2;

    private double zoomLevel = 1.0;

    private double originalWidth;
    private double originalHeight;

    private int currentIndex = 0;
    private int selectedYear = -1;

    // ==========================================
    // INITIALIZE
    // ==========================================

    @FXML
    public void initialize() {

        treeGroup.getTransforms().add(treeScale);

        treePane.getChildren().clear();
        treePane.getChildren().add(treeGroup);

        createHistoryTable();

        insertDefaultEvents();

        loadHistoryEvents();

        drawBinarySearchTree();

        if (!events.isEmpty()) {

            int firstYear = historyBST.getInOrder()
                    .get(0)
                    .getYear();

            showEvent(firstYear);
        }

        updateZoomLabel();
    }

    // ==========================================
    // CREATE DATABASE TABLE
    // ==========================================

    private void createHistoryTable() {

        String sql =
                "CREATE TABLE IF NOT EXISTS history_events (" +
                "year INT PRIMARY KEY, " +
                "title VARCHAR(255) NOT NULL, " +
                "description TEXT NOT NULL, " +
                "image_path VARCHAR(1000)" +
                ")";

        try (Connection connection = DBConnection.getConnection()) {

            if (connection == null) {
                return;
            }

            try (Statement statement = connection.createStatement()) {
                statement.executeUpdate(sql);
            }

        } catch (SQLException e) {
            showError("Database Error",
                    "Unable to create the history events table.");
            e.printStackTrace();
        }
    }

    // ==========================================
    // INSERT DEFAULT HISTORICAL EVENTS
    // ==========================================

    private void insertDefaultEvents() {

        List<HistoryEvent> defaults = new ArrayList<>();

        defaults.add(new HistoryEvent(
                1983,
                "Establishment of Laguna State Polytechnic College (LSPC)",
                "The institution was established as Laguna State Polytechnic College.",
                "/images/history/1983.jpeg"
        ));

        defaults.add(new HistoryEvent(
                1957,
                "Conversion into Baybay Agricultural and Vocational School",
                "The school was converted into Baybay Agricultural and Vocational School.",
                "/images/history/1957.jpeg"
        ));

        defaults.add(new HistoryEvent(
                1952,
                "Establishment of Baybay Provincial High School",
                "Baybay Provincial High School was established.",
                "/images/history/1952.jpeg"
        ));

        defaults.add(new HistoryEvent(
                1971,
                "Establishment of Baybay National College of Agriculture and Technology (BNCAT)",
                "The institution became Baybay National College of Agriculture and Technology.",
                "/images/history/1971.jpeg"
        ));

        defaults.add(new HistoryEvent(
                2000,
                "Integration of San Pablo City Campus",
                "San Pablo City Campus became part of the institution.",
                "/images/history/2000.jpeg"
        ));

        defaults.add(new HistoryEvent(
                1999,
                "Integration of Santa Cruz and Los Baños Campuses",
                "Santa Cruz and Los Baños campuses were integrated.",
                "/images/history/1999.jpeg"
        ));

        defaults.add(new HistoryEvent(
                2007,
                "Conversion into Laguna State Polytechnic University (LSPU)",
                "The institution was converted into Laguna State Polytechnic University.",
                "/images/history/2007.jpeg"
        ));

        defaults.add(new HistoryEvent(
                2025,
                "LSPU 73rd Founding Anniversary",
                "The university celebrated its 73rd founding anniversary.",
                "/images/history/2025.jpeg"
        ));

        String sql =
                "INSERT IGNORE INTO history_events " +
                "(year, title, description, image_path) " +
                "VALUES (?, ?, ?, ?)";

        try (Connection connection = DBConnection.getConnection()) {

            if (connection == null) {
                return;
            }

            try (PreparedStatement statement =
                         connection.prepareStatement(sql)) {

                for (HistoryEvent event : defaults) {

                    statement.setInt(1, event.getYear());
                    statement.setString(2, event.getTitle());
                    statement.setString(3, event.getDescription());
                    statement.setString(4, event.getImagePath());

                    statement.addBatch();
                }

                statement.executeBatch();
            }

        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    // ==========================================
    // LOAD EVENTS FROM DATABASE
    // ==========================================

    private void loadHistoryEvents() {

        events.clear();

        String sql =
                "SELECT year, title, description, image_path " +
                "FROM history_events ORDER BY year";

        try (Connection connection = DBConnection.getConnection()) {

            if (connection == null) {
                return;
            }

            try (PreparedStatement statement =
                         connection.prepareStatement(sql);
                 ResultSet result = statement.executeQuery()) {

                while (result.next()) {

                    HistoryEvent event = new HistoryEvent(
                            result.getInt("year"),
                            result.getString("title"),
                            result.getString("description"),
                            result.getString("image_path")
                    );

                    events.put(event.getYear(), event);
                }
            }

            historyBST.buildBalanced(
                    new ArrayList<>(events.values())
            );

        } catch (SQLException e) {

            showError("Database Error",
                    "Unable to load historical events.");

            e.printStackTrace();
        }
    }

    // ==========================================
    // ADD HISTORICAL EVENT BUTTON
    // ==========================================

    @FXML
    private void handleAddEvent() {

        Dialog<ButtonType> dialog = new Dialog<>();

        dialog.setTitle("Add Historical Event");
        dialog.setHeaderText("Enter the information for the new historical event.");

        dialog.getDialogPane().getButtonTypes().addAll(
                ButtonType.CANCEL,
                new ButtonType("Save Event",
                        javafx.scene.control.ButtonBar.ButtonData.OK_DONE)
        );

        TextField yearField = new TextField();
        yearField.setPromptText("Example: 2026");

        TextField titleField = new TextField();
        titleField.setPromptText("Enter event title");

        TextArea descriptionField = new TextArea();
        descriptionField.setPromptText(
                "Enter the historical event description"
        );
        descriptionField.setWrapText(true);
        descriptionField.setPrefRowCount(4);

        Label imageNameLabel = new Label("No image selected");

        Button chooseImageButton = new Button("Choose Image");

        File[] selectedImage = new File[1];

        chooseImageButton.setOnAction(event -> {

            FileChooser chooser = new FileChooser();

            chooser.setTitle("Choose Historical Event Image");

            chooser.getExtensionFilters().addAll(
                    new FileChooser.ExtensionFilter(
                            "Image Files",
                            "*.png", "*.jpg", "*.jpeg", "*.webp"
                    )
            );

            Window window = dialog.getDialogPane().getScene().getWindow();

            File file = chooser.showOpenDialog(window);

            if (file != null) {

                selectedImage[0] = file;

                imageNameLabel.setText(file.getName());
            }
        });

        GridPane grid = new GridPane();

        grid.setHgap(12);
        grid.setVgap(14);
        grid.setPadding(new Insets(15));

        Label yearLabel = new Label("Event Year:");
        Label titleLabel = new Label("Event Title:");
        Label descriptionLabel = new Label("Description:");
        Label imageLabel = new Label("Event Image:");

        yearLabel.setMinWidth(105);
        titleLabel.setMinWidth(105);
        descriptionLabel.setMinWidth(105);
        imageLabel.setMinWidth(105);

        grid.add(yearLabel, 0, 0);
        grid.add(yearField, 1, 0);

        grid.add(titleLabel, 0, 1);
        grid.add(titleField, 1, 1);

        grid.add(descriptionLabel, 0, 2);
        grid.add(descriptionField, 1, 2);

        grid.add(imageLabel, 0, 3);

        GridPane imageRow = new GridPane();
        imageRow.setHgap(10);
        imageRow.add(chooseImageButton, 0, 0);
        imageRow.add(imageNameLabel, 1, 0);

        grid.add(imageRow, 1, 3);

        yearField.setPrefWidth(350);
        titleField.setPrefWidth(350);
        descriptionField.setPrefWidth(350);

        dialog.getDialogPane().setContent(grid);
        dialog.getDialogPane().setPrefWidth(570);

        Button saveButton = (Button) dialog.getDialogPane()
                .lookupButton(dialog.getDialogPane()
                .getButtonTypes().get(1));

        saveButton.addEventFilter(
                javafx.event.ActionEvent.ACTION,
                event -> {

                    if (!yearField.getText().trim().matches("\\d{4}")) {

                        event.consume();

                        showError("Invalid Year",
                                "Please enter a valid four-digit year.");

                        return;
                    }

                    if (titleField.getText().trim().isEmpty()) {

                        event.consume();

                        showError("Missing Title",
                                "Please enter the event title.");

                        return;
                    }

                    if (descriptionField.getText().trim().isEmpty()) {

                        event.consume();

                        showError("Missing Description",
                                "Please enter the event description.");
                    }
                }
        );

        dialog.showAndWait().ifPresent(result -> {

            if (result.getButtonData()
                    == javafx.scene.control.ButtonBar.ButtonData.OK_DONE) {

                int year = Integer.parseInt(
                        yearField.getText().trim()
                );

                if (events.containsKey(year)) {

                    showError("Duplicate Year",
                            "An event for this year already exists.");

                    return;
                }

                String imagePath = "";

                if (selectedImage[0] != null) {

                    imagePath = saveImage(selectedImage[0]);

                    if (imagePath == null) {
                        return;
                    }
                }

                HistoryEvent newEvent = new HistoryEvent(
                        year,
                        titleField.getText().trim(),
                        descriptionField.getText().trim(),
                        imagePath
                );

                if (saveEventToDatabase(newEvent)) {

                    events.put(year, newEvent);

                    historyBST.buildBalanced(
                            new ArrayList<>(events.values())
                    );

                    selectedYear = year;

                    drawBinarySearchTree();

                    showEvent(year);

                    showInformation(
                            "Event Added",
                            "The historical event was successfully added."
                    );
                }
            }
        });
    }

    // ==========================================
    // SAVE IMAGE TO COMPUTER
    // ==========================================

    private String saveImage(File sourceFile) {

        try {

            Path folder = Paths.get(
                    System.getProperty("user.home"),
                    "LSPU-HNN",
                    "history-images"
            );

            Files.createDirectories(folder);

            String extension = "";

            String fileName = sourceFile.getName();

            int dot = fileName.lastIndexOf('.');

            if (dot >= 0) {
                extension = fileName.substring(dot);
            }

            String newFileName =
                    UUID.randomUUID() + extension;

            Path destination = folder.resolve(newFileName);

            Files.copy(
                    sourceFile.toPath(),
                    destination,
                    StandardCopyOption.REPLACE_EXISTING
            );

            return destination.toAbsolutePath().toString();

        } catch (IOException e) {

            showError("Image Error",
                    "Unable to save the selected image.");

            e.printStackTrace();

            return null;
        }
    }

    // ==========================================
    // SAVE EVENT TO MYSQL
    // ==========================================

    private boolean saveEventToDatabase(HistoryEvent event) {

        String sql =
                "INSERT INTO history_events " +
                "(year, title, description, image_path) " +
                "VALUES (?, ?, ?, ?)";

        try (Connection connection = DBConnection.getConnection()) {

            if (connection == null) {

                showError("Database Error",
                        "Could not connect to the database.");

                return false;
            }

            try (PreparedStatement statement =
                         connection.prepareStatement(sql)) {

                statement.setInt(1, event.getYear());
                statement.setString(2, event.getTitle());
                statement.setString(3, event.getDescription());
                statement.setString(4, event.getImagePath());

                statement.executeUpdate();

                return true;
            }

        } catch (SQLException e) {

            showError("Save Failed",
                    "Unable to save the historical event.");

            e.printStackTrace();

            return false;
        }
    }

    // ==========================================
    // DRAW BINARY SEARCH TREE
    // ==========================================

    private void drawBinarySearchTree() {

        treeGroup.getChildren().clear();

        nodeViews.clear();
        nodeCircles.clear();
        nodeHighlights.clear();
        nodePositions.clear();

        currentIndex = 0;

        BST.Node root = historyBST.getRoot();

        if (root == null) {
            return;
        }

        calculatePositions(root, 0);

        int totalNodes = historyBST.getInOrder().size();

        int maxDepth = getMaxDepth(root);

        originalWidth = Math.max(
                950,
                totalNodes * HORIZONTAL_GAP + 200
        );

        originalHeight =
                START_Y + (maxDepth + 1) * VERTICAL_GAP + 100;

        treePane.setMinSize(originalWidth, originalHeight);
        treePane.setPrefSize(originalWidth, originalHeight);

        treeContainer.setMinSize(originalWidth, originalHeight);
        treeContainer.setPrefSize(originalWidth, originalHeight);

        drawConnections(root);
        drawNodes(root);

        updateZoom();
    }

    // ==========================================
    // CALCULATE NODE POSITIONS
    // ==========================================

    private void calculatePositions(BST.Node node, int depth) {

        if (node == null) {
            return;
        }

        calculatePositions(node.getLeft(), depth + 1);

        double x = START_X + currentIndex * HORIZONTAL_GAP;
        double y = START_Y + depth * VERTICAL_GAP;

        nodePositions.put(
                node.getEvent().getYear(),
                new double[]{x, y}
        );

        currentIndex++;

        calculatePositions(node.getRight(), depth + 1);
    }

    // ==========================================
    // DRAW CONNECTION LINES
    // ==========================================

    private void drawConnections(BST.Node node) {

        if (node == null) {
            return;
        }

        double[] parentPosition =
                nodePositions.get(node.getEvent().getYear());

        if (node.getLeft() != null) {

            double[] childPosition =
                    nodePositions.get(
                            node.getLeft().getEvent().getYear()
                    );

            Line line = new Line(
                    parentPosition[0],
                    parentPosition[1],
                    childPosition[0],
                    childPosition[1]
            );

            styleConnection(line);

            treeGroup.getChildren().add(line);
        }

        if (node.getRight() != null) {

            double[] childPosition =
                    nodePositions.get(
                            node.getRight().getEvent().getYear()
                    );

            Line line = new Line(
                    parentPosition[0],
                    parentPosition[1],
                    childPosition[0],
                    childPosition[1]
            );

            styleConnection(line);

            treeGroup.getChildren().add(line);
        }

        drawConnections(node.getLeft());
        drawConnections(node.getRight());
    }

    private void styleConnection(Line line) {

        line.setStroke(Color.web("#9BB9A5"));
        line.setStrokeWidth(3.5);

        line.setStrokeLineCap(
                javafx.scene.shape.StrokeLineCap.ROUND
        );
    }

    // ==========================================
    // DRAW TREE NODES
    // ==========================================

    private void drawNodes(BST.Node node) {

        if (node == null) {
            return;
        }

        int year = node.getEvent().getYear();

        double[] position = nodePositions.get(year);

        Circle highlight = new Circle(HIGHLIGHT_RADIUS);

        highlight.setFill(
                year == selectedYear
                        ? SELECTED_HIGHLIGHT_COLOR
                        : HIGHLIGHT_COLOR
        );

        Circle circle = new Circle(NODE_RADIUS);

        circle.setFill(
                year == selectedYear
                        ? NODE_SELECTED_COLOR
                        : NODE_COLOR
        );

        circle.setStroke(Color.WHITE);
        circle.setStrokeWidth(2);

        Label yearText = new Label(String.valueOf(year));

        yearText.setStyle(
                "-fx-text-fill: white;" +
                "-fx-font-size: 14px;" +
                "-fx-font-weight: bold;"
        );

        StackPane nodeView = new StackPane(
                highlight,
                circle,
                yearText
        );

        nodeView.setPrefSize(
                HIGHLIGHT_RADIUS * 2,
                HIGHLIGHT_RADIUS * 2
        );

        nodeView.setMinSize(
                HIGHLIGHT_RADIUS * 2,
                HIGHLIGHT_RADIUS * 2
        );

        nodeView.setMaxSize(
                HIGHLIGHT_RADIUS * 2,
                HIGHLIGHT_RADIUS * 2
        );

        nodeView.setLayoutX(
                position[0] - HIGHLIGHT_RADIUS
        );

        nodeView.setLayoutY(
                position[1] - HIGHLIGHT_RADIUS
        );

        nodeView.setCursor(Cursor.HAND);

        nodeView.setOnMouseClicked(event -> showEvent(year));

        nodeView.setOnMouseEntered(event -> {

            if (year != selectedYear) {

                circle.setFill(NODE_HOVER_COLOR);

                highlight.setFill(Color.web("#D0F0D8"));
            }
        });

        nodeView.setOnMouseExited(event ->
                updateSingleNodeStyle(year)
        );

        nodeViews.put(year, nodeView);
        nodeCircles.put(year, circle);
        nodeHighlights.put(year, highlight);

        treeGroup.getChildren().add(nodeView);

        drawNodes(node.getLeft());
        drawNodes(node.getRight());
    }

    // ==========================================
    // UPDATE NODE STYLE
    // ==========================================

    private void updateSingleNodeStyle(int year) {

        Circle circle = nodeCircles.get(year);
        Circle highlight = nodeHighlights.get(year);

        if (circle == null || highlight == null) {
            return;
        }

        if (year == selectedYear) {

            circle.setFill(NODE_SELECTED_COLOR);

            highlight.setFill(SELECTED_HIGHLIGHT_COLOR);

        } else {

            circle.setFill(NODE_COLOR);

            highlight.setFill(HIGHLIGHT_COLOR);
        }
    }

    private void updateNodeColors() {

        for (Integer year : nodeViews.keySet()) {
            updateSingleNodeStyle(year);
        }
    }

    // ==========================================
    // GET MAXIMUM TREE DEPTH
    // ==========================================

    private int getMaxDepth(BST.Node node) {

        if (node == null) {
            return -1;
        }

        int leftDepth = getMaxDepth(node.getLeft());
        int rightDepth = getMaxDepth(node.getRight());

        return Math.max(leftDepth, rightDepth) + 1;
    }

    // ==========================================
    // DISPLAY EVENT INFORMATION
    // ==========================================

    private void showEvent(int year) {

        HistoryEvent event = historyBST.search(year);

        if (event == null) {
            return;
        }

        selectedYear = year;

        yearLabel.setText(String.valueOf(event.getYear()));

        titleLabel.setText(event.getTitle());

        descriptionLabel.setText(event.getDescription());

        loadEventImage(event.getImagePath());

        updateNodeColors();
    }

    // ==========================================
    // LOAD EVENT IMAGE
    // ==========================================

    private void loadEventImage(String imagePath) {

        eventImage.setImage(null);

        if (imagePath == null || imagePath.trim().isEmpty()) {
            return;
        }

        try {

            if (imagePath.startsWith("/")) {

                var resource = getClass().getResource(imagePath);

                if (resource != null) {

                    eventImage.setImage(
                            new Image(resource.toExternalForm())
                    );
                }

            } else {

                File file = new File(imagePath);

                if (file.exists()) {

                    eventImage.setImage(
                            new Image(file.toURI().toString())
                    );
                }
            }

        } catch (Exception e) {

            eventImage.setImage(null);
            e.printStackTrace();
        }
    }

    // ==========================================
    // ZOOM IN
    // ==========================================

    @FXML
    private void handleZoomIn() {

        zoomLevel = Math.min(
                MAX_ZOOM,
                zoomLevel + ZOOM_STEP
        );

        updateZoom();
    }

    // ==========================================
    // ZOOM OUT
    // ==========================================

    @FXML
    private void handleZoomOut() {

        zoomLevel = Math.max(
                MIN_ZOOM,
                zoomLevel - ZOOM_STEP
        );

        updateZoom();
    }

    // ==========================================
    // UPDATE ZOOM
    // ==========================================

    private void updateZoom() {

        treeScale.setX(zoomLevel);
        treeScale.setY(zoomLevel);

        updateZoomLabel();
    }

    private void updateZoomLabel() {

        if (zoomLabel != null) {

            zoomLabel.setText(
                    Math.round(zoomLevel * 100) + "%"
            );
        }
    }

    // ==========================================
    // RESET BUTTON
    // ==========================================

    @FXML
    private void handleReset() {

        zoomLevel = 1.0;

        selectedYear = -1;

        drawBinarySearchTree();

        yearLabel.setText("All Events");

        titleLabel.setText("LSPU Historical Timeline");

        descriptionLabel.setText(
                "All historical events are displayed in the Binary Search Tree. " +
                "Select a year to view its historical information."
        );

        eventImage.setImage(null);

        updateNodeColors();
    }

    // ==========================================
    // INFORMATION ALERT
    // ==========================================

    private void showInformation(String title, String message) {

        Alert alert = new Alert(Alert.AlertType.INFORMATION);

        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);

        alert.showAndWait();
    }

    // ==========================================
    // ERROR ALERT
    // ==========================================

    private void showError(String title, String message) {

        Alert alert = new Alert(Alert.AlertType.ERROR);

        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);

        alert.showAndWait();
    }
}