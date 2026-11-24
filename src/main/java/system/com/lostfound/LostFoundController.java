
package system.com.lostfound;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.sql.Connection;
import java.sql.Date;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDate;
import java.util.Locale;
import java.util.Optional;
import java.util.UUID;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.transformation.FilteredList;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.ButtonType;
import javafx.scene.control.ComboBox;
import javafx.scene.control.DatePicker;
import javafx.scene.control.Label;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.VBox;
import javafx.stage.FileChooser;
import javafx.stage.Stage;

public class LostFoundController {

    // ==========================================
    // FORM FIELDS
    // ==========================================

    @FXML private TextField itemNameField;
    @FXML private TextField descriptionField;
    @FXML private TextField categoryField;
    @FXML private TextField locationField;
    @FXML private TextField searchField;

    @FXML private DatePicker dateReportedPicker;

    @FXML private ComboBox<String> typeComboBox;
    @FXML private ComboBox<String> statusComboBox;
    @FXML private ComboBox<String> campusComboBox;
    @FXML private ComboBox<String> filterComboBox;

    // ==========================================
    // IMAGE FIELDS
    // ==========================================

    @FXML private ImageView imagePreview;
    @FXML private Label imageNameLabel;

    private String selectedImagePath;
    private String currentImagePath;

    // ==========================================
    // TABLE FIELDS
    // ==========================================

    @FXML private TableView<Item> itemsTable;

    @FXML private TableColumn<Item, Integer> idColumn;
    @FXML private TableColumn<Item, String> itemNameColumn;
    @FXML private TableColumn<Item, String> descriptionColumn;
    @FXML private TableColumn<Item, String> categoryColumn;
    @FXML private TableColumn<Item, String> locationColumn;
    @FXML private TableColumn<Item, String> campusColumn;
    @FXML private TableColumn<Item, LocalDate> dateColumn;
    @FXML private TableColumn<Item, String> typeColumn;
    @FXML private TableColumn<Item, String> statusColumn;

    // ==========================================
    // STATISTICS
    // ==========================================

    @FXML private Label totalItemsLabel;
    @FXML private Label lostItemsLabel;
    @FXML private Label foundItemsLabel;

    // ==========================================
    // FEED AND COMMENTS
    // ==========================================

    @FXML private VBox postFeedBox;
    @FXML private VBox commentsBox;
    @FXML private TextField commentField;
    @FXML private Label selectedPostLabel;

    private Item selectedFeedItem;

    // ==========================================
    // DATA
    // ==========================================

    private final ObservableList<Item> itemList =
            FXCollections.observableArrayList();

    private FilteredList<Item> filteredItems;

    // ==========================================
    // INITIALIZE
    // ==========================================

    @FXML
    public void initialize() {

        typeComboBox.setItems(
                FXCollections.observableArrayList("Lost", "Found")
        );

        statusComboBox.setItems(
                FXCollections.observableArrayList(
                        "Pending", "Claimed", "Returned"
                )
        );

        campusComboBox.setItems(
                FXCollections.observableArrayList(
                        "Siniloan Campus",
                        "Santa Cruz Campus",
                        "Los Baños Campus",
                        "San Pablo City Campus"
                )
        );

        filterComboBox.setItems(
                FXCollections.observableArrayList(
                        "All", "Lost", "Found"
                )
        );

        filterComboBox.setValue("All");
        statusComboBox.setValue("Pending");

        idColumn.setCellValueFactory(
                new PropertyValueFactory<>("id")
        );

        itemNameColumn.setCellValueFactory(
                new PropertyValueFactory<>("itemName")
        );

        descriptionColumn.setCellValueFactory(
                new PropertyValueFactory<>("description")
        );

        categoryColumn.setCellValueFactory(
                new PropertyValueFactory<>("category")
        );

        locationColumn.setCellValueFactory(
                new PropertyValueFactory<>("location")
        );

        campusColumn.setCellValueFactory(
                new PropertyValueFactory<>("campus")
        );

        dateColumn.setCellValueFactory(
                new PropertyValueFactory<>("dateReported")
        );

        typeColumn.setCellValueFactory(
                new PropertyValueFactory<>("type")
        );

        statusColumn.setCellValueFactory(
                new PropertyValueFactory<>("status")
        );

        itemsTable.setColumnResizePolicy(
                TableView.CONSTRAINED_RESIZE_POLICY_FLEX_LAST_COLUMN
        );

        filteredItems = new FilteredList<>(itemList, item -> true);
        itemsTable.setItems(filteredItems);

        searchField.textProperty().addListener(
                (observable, oldValue, newValue) -> applyFilters()
        );

        filterComboBox.valueProperty().addListener(
                (observable, oldValue, newValue) -> applyFilters()
        );

        itemsTable.getSelectionModel()
                .selectedItemProperty()
                .addListener((obs, oldItem, selectedItem) -> {

                    if (selectedItem != null) {
                        fillFields(selectedItem);
                    }
                });

        imagePreview.setPreserveRatio(true);

        showNoImage();

        selectedPostLabel.setText(
                "Select a post to view its comments."
        );

        loadItems();
        loadFeed();
    }

    // ==========================================
    // IMAGE UPLOAD
    // ==========================================

    @FXML
    private void handleChooseImage() {

        FileChooser chooser = new FileChooser();

        chooser.setTitle("Choose Item Picture");

        chooser.getExtensionFilters().add(
                new FileChooser.ExtensionFilter(
                        "Image Files",
                        "*.png", "*.jpg", "*.jpeg", "*.gif"
                )
        );

        File file = chooser.showOpenDialog(
                imagePreview.getScene().getWindow()
        );

        if (file == null) {
            return;
        }

        try {

            Path uploadDirectory = Path.of(
                    System.getProperty("user.dir"),
                    "uploads",
                    "lost_found"
            );

            Files.createDirectories(uploadDirectory);

            String extension = getFileExtension(file.getName());

            String newFileName = UUID.randomUUID() + extension;

            Path destination = uploadDirectory.resolve(newFileName);

            Files.copy(
                    file.toPath(),
                    destination,
                    StandardCopyOption.REPLACE_EXISTING
            );

            selectedImagePath = Path.of(
                    "uploads",
                    "lost_found",
                    newFileName
            ).toString();

            currentImagePath = selectedImagePath;

            imagePreview.setImage(
                    new Image(destination.toUri().toString())
            );

            imageNameLabel.setText(file.getName());

        } catch (IOException e) {

            showAlert(
                    Alert.AlertType.ERROR,
                    "Image Error",
                    "Unable to upload image: " + e.getMessage()
            );
        }
    }

    private String getFileExtension(String fileName) {

        int index = fileName.lastIndexOf('.');

        if (index < 0) {
            return ".jpg";
        }

        return fileName.substring(index).toLowerCase(Locale.ROOT);
    }

    private void showNoImage() {

        imagePreview.setImage(null);
        imageNameLabel.setText("No image selected");
    }

    private void displayImage(String imagePath) {

        if (imagePath == null || imagePath.isBlank()) {
            showNoImage();
            return;
        }

        try {

            Path path = Path.of(imagePath);

            if (!path.isAbsolute()) {
                path = Path.of(
                        System.getProperty("user.dir")
                ).resolve(path);
            }

            if (!Files.exists(path)) {
                showNoImage();
                return;
            }

            imagePreview.setImage(
                    new Image(path.toUri().toString())
            );

            imageNameLabel.setText(path.getFileName().toString());

        } catch (Exception e) {

            showNoImage();
            e.printStackTrace();
        }
    }

    // ==========================================
    // CREATE ITEM - STUDENT ONLY
    // ==========================================

    @FXML
    private void handleAdd() {

        if (!Session.isStudent()) {

            showAlert(
                    Alert.AlertType.WARNING,
                    "Access Denied",
                    "Only student accounts can create Lost & Found posts."
            );

            return;
        }

        if (!validateFields()) {
            return;
        }

        String studentId = Session.getStudentId();

        if (studentId == null || studentId.isBlank()) {

            showAlert(
                    Alert.AlertType.WARNING,
                    "Login Required",
                    "Please log in before posting an item."
            );

            return;
        }

        String sql = """
                INSERT INTO lost_found_items
                (item_name, description, category, location, campus,
                 date_reported, type, status, image_path, posted_by)
                VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
                """;

        try (Connection conn = DBConnection.getConnection()) {

            if (conn == null) {
                return;
            }

            try (PreparedStatement stmt = conn.prepareStatement(sql)) {

                stmt.setString(1, itemNameField.getText().trim());
                stmt.setString(2, itemText(descriptionField));
                stmt.setString(3, itemText(categoryField));
                stmt.setString(4, itemText(locationField));
                stmt.setString(5, campusComboBox.getValue());

                LocalDate date = dateReportedPicker.getValue();

                if (date == null) {
                    stmt.setNull(6, java.sql.Types.DATE);
                } else {
                    stmt.setDate(6, Date.valueOf(date));
                }

                stmt.setString(7, typeComboBox.getValue());
                stmt.setString(8, "Pending");
                stmt.setString(9, currentImagePath);
                stmt.setString(10, studentId);

                stmt.executeUpdate();
            }

            showAlert(
                    Alert.AlertType.INFORMATION,
                    "Success",
                    "Item posted successfully!"
            );

            clearFields();
            loadItems();
            loadFeed();

        } catch (SQLException e) {

            showAlert(
                    Alert.AlertType.ERROR,
                    "Database Error",
                    e.getMessage()
            );
        }
    }

    // ==========================================
    // READ ITEMS
    // ==========================================

    private void loadItems() {

        itemList.clear();

        String sql = """
                SELECT * FROM lost_found_items
                ORDER BY date_reported DESC, id DESC
                """;

        try (Connection conn = DBConnection.getConnection()) {

            if (conn == null) {
                return;
            }

            try (PreparedStatement stmt = conn.prepareStatement(sql);
                 ResultSet rs = stmt.executeQuery()) {

                while (rs.next()) {

                    Date sqlDate = rs.getDate("date_reported");

                    LocalDate date = sqlDate == null
                            ? null
                            : sqlDate.toLocalDate();

                    itemList.add(new Item(
                            rs.getInt("id"),
                            rs.getString("item_name"),
                            rs.getString("description"),
                            rs.getString("category"),
                            rs.getString("location"),
                            rs.getString("campus"),
                            date,
                            rs.getString("type"),
                            rs.getString("status"),
                            rs.getString("image_path"),
                            rs.getString("posted_by")
                    ));
                }
            }

            applyFilters();
            updateStatistics();

        } catch (SQLException e) {

            showAlert(
                    Alert.AlertType.ERROR,
                    "Database Error",
                    e.getMessage()
            );
        }
    }

    // ==========================================
    // UPDATE ITEM - ADMIN OR OWNER
    // ==========================================

    @FXML
    private void handleUpdate() {

        Item selected = itemsTable.getSelectionModel()
                .getSelectedItem();

        if (selected == null) {

            showAlert(
                    Alert.AlertType.WARNING,
                    "No Selection",
                    "Please select an item from the table to update."
            );

            return;
        }

        boolean isAdmin = Session.isAdmin();
        String studentId = Session.getStudentId();

        // Students can only update their own posts.
        // Admins can update any post.

        if (!isAdmin) {

            if (!Session.isStudent()
                    || studentId == null
                    || studentId.isBlank()) {

                showAlert(
                        Alert.AlertType.WARNING,
                        "Login Required",
                        "Please log in before updating a post."
                );

                return;
            }

            if (!studentId.equals(selected.getPostedBy())) {

                showAlert(
                        Alert.AlertType.WARNING,
                        "Access Denied",
                        "You can only edit your own posts."
                );

                return;
            }
        }

        if (!validateFields()) {
            return;
        }

        String sql;

        if (isAdmin) {

            sql = """
                    UPDATE lost_found_items
                    SET item_name = ?, description = ?, category = ?,
                        location = ?, campus = ?, date_reported = ?, type = ?,
                        status = ?, image_path = ?
                    WHERE id = ?
                    """;

        } else {

            sql = """
                    UPDATE lost_found_items
                    SET item_name = ?, description = ?, category = ?,
                        location = ?, campus = ?, date_reported = ?, type = ?,
                        status = ?, image_path = ?
                    WHERE id = ? AND posted_by = ?
                    """;
        }

        try (Connection conn = DBConnection.getConnection()) {

            if (conn == null) {
                return;
            }

            try (PreparedStatement stmt = conn.prepareStatement(sql)) {

                stmt.setString(1, itemNameField.getText().trim());
                stmt.setString(2, itemText(descriptionField));
                stmt.setString(3, itemText(categoryField));
                stmt.setString(4, itemText(locationField));
                stmt.setString(5, campusComboBox.getValue());

                LocalDate date = dateReportedPicker.getValue();

                if (date == null) {
                    stmt.setNull(6, java.sql.Types.DATE);
                } else {
                    stmt.setDate(6, Date.valueOf(date));
                }

                stmt.setString(7, typeComboBox.getValue());
                stmt.setString(8, statusComboBox.getValue());
                stmt.setString(9, currentImagePath);
                stmt.setInt(10, selected.getId());

                if (!isAdmin) {
                    stmt.setString(11, studentId);
                }

                int rowsUpdated = stmt.executeUpdate();

                if (rowsUpdated == 0) {

                    showAlert(
                            Alert.AlertType.WARNING,
                            "Update Failed",
                            "The post no longer exists or you do not have permission to edit it."
                    );

                    loadItems();
                    loadFeed();
                    return;
                }
            }

            showAlert(
                    Alert.AlertType.INFORMATION,
                    "Success",
                    isAdmin
                            ? "The student's post has been updated successfully!"
                            : "Your post has been updated successfully!"
            );

            clearFields();
            loadItems();
            loadFeed();

        } catch (SQLException e) {

            showAlert(
                    Alert.AlertType.ERROR,
                    "Database Error",
                    e.getMessage()
            );
        }
    }

    // ==========================================
    // DELETE ITEM - ADMIN OR OWNER
    // ==========================================

    @FXML
    private void handleDelete() {

        Item selected = itemsTable.getSelectionModel()
                .getSelectedItem();

        if (selected == null) {

            showAlert(
                    Alert.AlertType.WARNING,
                    "No Selection",
                    "Please select an item to delete."
            );

            return;
        }

        boolean isAdmin = Session.isAdmin();
        String studentId = Session.getStudentId();

        // Students can only delete their own posts.
        // Admins can delete any post.

        if (!isAdmin) {

            if (!Session.isStudent()
                    || studentId == null
                    || studentId.isBlank()) {

                showAlert(
                        Alert.AlertType.WARNING,
                        "Login Required",
                        "Please log in before deleting a post."
                );

                return;
            }

            if (!studentId.equals(selected.getPostedBy())) {

                showAlert(
                        Alert.AlertType.WARNING,
                        "Access Denied",
                        "You can only delete your own posts."
                );

                return;
            }
        }

        Alert confirmation = new Alert(
                Alert.AlertType.CONFIRMATION
        );

        confirmation.setTitle("Confirm Delete");

        confirmation.setHeaderText(
                isAdmin
                        ? "Delete this student's post?"
                        : "Delete your post?"
        );

        confirmation.setContentText(
                "Are you sure you want to delete "
                        + selected.getItemName() + "?"
        );

        Optional<ButtonType> result = confirmation.showAndWait();

        if (result.isEmpty() || result.get() != ButtonType.OK) {
            return;
        }

        try (Connection conn = DBConnection.getConnection()) {

            if (conn == null) {
                return;
            }

            conn.setAutoCommit(false);

            try {

                String checkSql;

                if (isAdmin) {

                    checkSql = """
                            SELECT id FROM lost_found_items
                            WHERE id = ?
                            FOR UPDATE
                            """;

                } else {

                    checkSql = """
                            SELECT id FROM lost_found_items
                            WHERE id = ? AND posted_by = ?
                            FOR UPDATE
                            """;
                }

                boolean postExists;

                try (PreparedStatement checkStmt =
                             conn.prepareStatement(checkSql)) {

                    checkStmt.setInt(1, selected.getId());

                    if (!isAdmin) {
                        checkStmt.setString(2, studentId);
                    }

                    try (ResultSet rs = checkStmt.executeQuery()) {
                        postExists = rs.next();
                    }
                }

                if (!postExists) {

                    conn.rollback();

                    showAlert(
                            Alert.AlertType.WARNING,
                            "Access Denied",
                            "The post no longer exists or you do not have permission to delete it."
                    );

                    loadItems();
                    loadFeed();
                    return;
                }

                // Delete comments first.

                String commentSql = """
                        DELETE FROM item_feedback
                        WHERE item_id = ?
                        """;

                try (PreparedStatement commentStmt =
                             conn.prepareStatement(commentSql)) {

                    commentStmt.setInt(1, selected.getId());
                    commentStmt.executeUpdate();
                }

                // Delete the selected post.

                String itemSql;

                if (isAdmin) {

                    itemSql = """
                            DELETE FROM lost_found_items
                            WHERE id = ?
                            """;

                } else {

                    itemSql = """
                            DELETE FROM lost_found_items
                            WHERE id = ? AND posted_by = ?
                            """;
                }

                int rowsDeleted;

                try (PreparedStatement itemStmt =
                             conn.prepareStatement(itemSql)) {

                    itemStmt.setInt(1, selected.getId());

                    if (!isAdmin) {
                        itemStmt.setString(2, studentId);
                    }

                    rowsDeleted = itemStmt.executeUpdate();
                }

                if (rowsDeleted == 0) {

                    conn.rollback();

                    showAlert(
                            Alert.AlertType.WARNING,
                            "Delete Failed",
                            "The post could not be deleted."
                    );

                    loadItems();
                    loadFeed();
                    return;
                }

                conn.commit();

            } catch (SQLException e) {

                conn.rollback();
                throw e;
            }

            showAlert(
                    Alert.AlertType.INFORMATION,
                    "Success",
                    isAdmin
                            ? "The student's post and its comments were deleted successfully!"
                            : "Your post and its comments were deleted successfully!"
            );

            clearFields();
            loadItems();
            loadFeed();

        } catch (SQLException e) {

            showAlert(
                    Alert.AlertType.ERROR,
                    "Database Error",
                    e.getMessage()
            );
        }
    }

    // ==========================================
    // SEARCH AND FILTER
    // ==========================================

    private void applyFilters() {

        if (filteredItems == null) {
            return;
        }

        String keyword = searchField.getText() == null
                ? ""
                : searchField.getText().trim().toLowerCase(Locale.ROOT);

        String selectedFilter = filterComboBox.getValue();

        filteredItems.setPredicate(item -> {

            boolean matchesSearch =
                    safe(item.getItemName()).contains(keyword)
                    || safe(item.getCategory()).contains(keyword)
                    || safe(item.getLocation()).contains(keyword)
                    || safe(item.getCampus()).contains(keyword)
                    || safe(item.getDescription()).contains(keyword);

            boolean matchesFilter =
                    selectedFilter == null
                    || selectedFilter.equals("All")
                    || (item.getType() != null
                    && item.getType().equalsIgnoreCase(selectedFilter));

            return matchesSearch && matchesFilter;
        });
    }

    private String safe(String value) {

        return value == null
                ? ""
                : value.toLowerCase(Locale.ROOT);
    }

    private String itemText(TextField field) {

        return field.getText() == null
                ? ""
                : field.getText().trim();
    }

    // ==========================================
    // STATISTICS
    // ==========================================

    private void updateStatistics() {

        long lost = itemList.stream()
                .filter(item ->
                        "Lost".equalsIgnoreCase(item.getType()))
                .count();

        long found = itemList.stream()
                .filter(item ->
                        "Found".equalsIgnoreCase(item.getType()))
                .count();

        totalItemsLabel.setText(String.valueOf(itemList.size()));
        lostItemsLabel.setText(String.valueOf(lost));
        foundItemsLabel.setText(String.valueOf(found));
    }

    // ==========================================
    // REFRESH
    // ==========================================

    @FXML
    private void handleRefresh() {

        loadItems();
        loadFeed();

        showAlert(
                Alert.AlertType.INFORMATION,
                "Refreshed",
                "Records and student posts have been refreshed."
        );
    }

    // ==========================================
    // CLEAR FORM
    // ==========================================

    @FXML
    private void handleClear() {
        clearFields();
    }

    private void clearFields() {

        itemNameField.clear();
        descriptionField.clear();
        categoryField.clear();
        locationField.clear();
        campusComboBox.setValue(null);

        dateReportedPicker.setValue(null);

        typeComboBox.setValue(null);
        statusComboBox.setValue("Pending");

        selectedImagePath = null;
        currentImagePath = null;

        showNoImage();

        itemsTable.getSelectionModel().clearSelection();
    }

    // ==========================================
    // FILL FORM
    // ==========================================

    private void fillFields(Item item) {

        itemNameField.setText(item.getItemName());
        descriptionField.setText(item.getDescription());
        categoryField.setText(item.getCategory());
        locationField.setText(item.getLocation());
        campusComboBox.setValue(item.getCampus());

        dateReportedPicker.setValue(item.getDateReported());

        typeComboBox.setValue(item.getType());
        statusComboBox.setValue(item.getStatus());

        currentImagePath = item.getImagePath();
        selectedImagePath = item.getImagePath();

        displayImage(item.getImagePath());
    }

    // ==========================================
    // VALIDATION
    // ==========================================

    private boolean validateFields() {

        if (itemNameField.getText() == null
                || itemNameField.getText().trim().isEmpty()) {

            showAlert(
                    Alert.AlertType.WARNING,
                    "Missing Information",
                    "Please enter the item name."
            );

            return false;
        }

        if (campusComboBox.getValue() == null) {

            showAlert(
                    Alert.AlertType.WARNING,
                    "Missing Information",
                    "Please select a campus."
            );

            return false;
        }

        if (typeComboBox.getValue() == null) {

            showAlert(
                    Alert.AlertType.WARNING,
                    "Missing Information",
                    "Please select Lost or Found."
            );

            return false;
        }

        return true;
    }

    // ==========================================
    // STUDENT POST FEED
    // ==========================================

    private void loadFeed() {

        postFeedBox.getChildren().clear();

        String sql = """
                SELECT * FROM lost_found_items
                ORDER BY date_reported DESC, id DESC
                """;

        try (Connection conn = DBConnection.getConnection()) {

            if (conn == null) {
                return;
            }

            try (PreparedStatement stmt = conn.prepareStatement(sql);
                 ResultSet rs = stmt.executeQuery()) {

                int count = 0;

                while (rs.next()) {

                    count++;

                    Date sqlDate = rs.getDate("date_reported");

                    LocalDate date = sqlDate == null
                            ? null
                            : sqlDate.toLocalDate();

                    Item item = new Item(
                            rs.getInt("id"),
                            rs.getString("item_name"),
                            rs.getString("description"),
                            rs.getString("category"),
                            rs.getString("location"),
                            rs.getString("campus"),
                            date,
                            rs.getString("type"),
                            rs.getString("status"),
                            rs.getString("image_path"),
                            rs.getString("posted_by")
                    );

                    createPostCard(item);
                }

                if (count == 0) {

                    Label emptyLabel = new Label(
                            "No student posts yet."
                    );

                    emptyLabel.setStyle(
                            "-fx-text-fill: #777777;"
                    );

                    postFeedBox.getChildren().add(emptyLabel);
                }
            }

        } catch (SQLException e) {

            showAlert(
                    Alert.AlertType.ERROR,
                    "Feed Error",
                    e.getMessage()
            );
        }
    }

    private void createPostCard(Item item) {

        VBox card = new VBox(9);
        card.setMaxWidth(Double.MAX_VALUE);
        card.setFillWidth(true);
        card.setStyle(
                "-fx-background-color: #FFFFFF;" +
                "-fx-background-radius: 12;" +
                "-fx-border-color: #D7E4DD;" +
                "-fx-border-radius: 12;" +
                "-fx-border-width: 1;" +
                "-fx-padding: 16;" +
                "-fx-cursor: hand;"
        );

        Label title = new Label(
                safeDisplay(item.getType()) + ": " + safeDisplay(item.getItemName())
        );
        title.setWrapText(true);
        title.setMaxWidth(Double.MAX_VALUE);
        title.setStyle(
                "-fx-font-size: 17px;" +
                "-fx-font-weight: bold;" +
                "-fx-text-fill: #173D69;"
        );

        Label author = new Label(
                "Posted by: " + maskStudentId(item.getPostedBy())
        );
        author.setStyle(
                "-fx-font-size: 12px;" +
                "-fx-text-fill: #666666;"
        );

        Label description = new Label(
                "Description: " + safeDisplay(item.getDescription())
        );
        description.setWrapText(true);
        description.setMaxWidth(Double.MAX_VALUE);
        description.setStyle(
                "-fx-font-size: 13px;" +
                "-fx-text-fill: #263B30;"
        );

        Label category = new Label(
                "Category: " + safeDisplay(item.getCategory())
        );
        category.setWrapText(true);
        category.setMaxWidth(Double.MAX_VALUE);
        category.setStyle(
                "-fx-font-size: 13px;" +
                "-fx-text-fill: #263B30;"
        );

        Label location = new Label(
                "Location: " + safeDisplay(item.getLocation())
        );
        location.setWrapText(true);
        location.setMaxWidth(Double.MAX_VALUE);
        location.setStyle(
                "-fx-font-size: 13px;" +
                "-fx-text-fill: #263B30;"
        );

        Label campus = new Label(
                "Campus: " + safeDisplay(item.getCampus())
        );
        campus.setWrapText(true);
        campus.setMaxWidth(Double.MAX_VALUE);
        campus.setStyle(
                "-fx-font-size: 13px;" +
                "-fx-text-fill: #263B30;"
        );

        Label date = new Label(
                "Date reported: " +
                (item.getDateReported() == null
                        ? "Not specified"
                        : item.getDateReported().toString())
        );
        date.setStyle(
                "-fx-font-size: 12px;" +
                "-fx-text-fill: #52645A;"
        );

        Label status = new Label(
                "Status: " + safeDisplay(item.getStatus())
        );
        status.setStyle(
                "-fx-font-size: 13px;" +
                "-fx-text-fill: #01796F;" +
                "-fx-font-weight: bold;"
        );

        card.getChildren().addAll(
                title,
                author,
                description,
                category,
                location,
                campus,
                date,
                status
        );

        if (item.getImagePath() != null
                && !item.getImagePath().isBlank()) {

            try {
                Path path = Path.of(item.getImagePath());

                if (!path.isAbsolute()) {
                    path = Path.of(
                            System.getProperty("user.dir")
                    ).resolve(path);
                }

                if (Files.exists(path)) {
                    ImageView postImage = new ImageView(
                            new Image(path.toUri().toString(), true)
                    );

                    postImage.setFitWidth(260);
                    postImage.setFitHeight(170);
                    postImage.setPreserveRatio(true);
                    postImage.setSmooth(true);

                    card.getChildren().add(postImage);
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        Label commentHint = new Label(
                "Select this post to view and add comments"
        );
        commentHint.setWrapText(true);
        commentHint.setStyle(
                "-fx-text-fill: #1E40AF;" +
                "-fx-font-size: 12px;"
        );

        card.getChildren().add(commentHint);

        card.setOnMouseClicked(event -> {
            selectedFeedItem = item;

            selectedPostLabel.setText(
                    safeDisplay(item.getType()) + ": "
                            + safeDisplay(item.getItemName())
            );

            loadComments(item.getId());
        });

        postFeedBox.getChildren().add(card);
    }

    private String safeDisplay(String value) {

        return value == null || value.isBlank()
                ? "Not specified"
                : value;
    }

    // ==========================================
    // COMMENTS - STUDENT ONLY
    // ==========================================

    private void loadComments(int itemId) {

        commentsBox.getChildren().clear();

        String sql = """
                SELECT sender_name, message, date_sent
                FROM item_feedback
                WHERE item_id = ?
                ORDER BY date_sent ASC, id ASC
                """;

        try (Connection conn = DBConnection.getConnection()) {

            if (conn == null) {
                return;
            }

            try (PreparedStatement stmt = conn.prepareStatement(sql)) {

                stmt.setInt(1, itemId);

                try (ResultSet rs = stmt.executeQuery()) {

                    int count = 0;

                    while (rs.next()) {

                        count++;

                        String sender = rs.getString("sender_name");
                        String message = rs.getString("message");
                        String date = rs.getString("date_sent");

                        VBox commentCard = new VBox(4);

                        Label senderLabel = new Label(
                                maskStudentId(sender)
                        );

                        senderLabel.setStyle(
                                "-fx-font-weight: bold;" +
                                "-fx-text-fill: #173D69;"
                        );

                        Label messageLabel = new Label(message);
                        messageLabel.setWrapText(true);
                        messageLabel.setStyle("-fx-text-fill: black;");

                        Label dateLabel = new Label(
                                date == null ? "" : date
                        );

                        dateLabel.setStyle(
                                "-fx-font-size: 10px;" +
                                "-fx-text-fill: #888888;"
                        );

                        commentCard.getChildren().addAll(
                                senderLabel,
                                messageLabel,
                                dateLabel
                        );

                        commentCard.setStyle(
                                "-fx-background-color: #F4F7FB;" +
                                "-fx-background-radius: 8;" +
                                "-fx-padding: 10;"
                        );

                        commentsBox.getChildren().add(commentCard);
                    }

                    if (count == 0) {

                        Label noComments = new Label(
                                "No comments yet. Be the first to comment!"
                        );

                        noComments.setWrapText(true);

                        commentsBox.getChildren().add(noComments);
                    }
                }
            }

        } catch (SQLException e) {

            showAlert(
                    Alert.AlertType.ERROR,
                    "Comment Error",
                    e.getMessage()
            );
        }
    }

    @FXML
    private void handleAddComment() {

        if (!Session.isStudent()) {

            showAlert(
                    Alert.AlertType.WARNING,
                    "Access Denied",
                    "Only student accounts can add comments."
            );

            return;
        }

        if (selectedFeedItem == null) {

            showAlert(
                    Alert.AlertType.WARNING,
                    "No Post Selected",
                    "Please select a post before commenting."
            );

            return;
        }

        String studentId = Session.getStudentId();

        if (studentId == null || studentId.isBlank()) {

            showAlert(
                    Alert.AlertType.WARNING,
                    "Login Required",
                    "Please log in before commenting."
            );

            return;
        }

        String message = commentField.getText() == null
                ? ""
                : commentField.getText().trim();

        if (message.isEmpty()) {

            showAlert(
                    Alert.AlertType.WARNING,
                    "Empty Comment",
                    "Please enter your comment."
            );

            return;
        }

        String sql = """
                INSERT INTO item_feedback
                (item_id, sender_name, message)
                VALUES (?, ?, ?)
                """;

        try (Connection conn = DBConnection.getConnection()) {

            if (conn == null) {
                return;
            }

            try (PreparedStatement stmt = conn.prepareStatement(sql)) {

                stmt.setInt(1, selectedFeedItem.getId());
                stmt.setString(2, studentId);
                stmt.setString(3, message);

                stmt.executeUpdate();
            }

            commentField.clear();
            loadComments(selectedFeedItem.getId());

        } catch (SQLException e) {

            showAlert(
                    Alert.AlertType.ERROR,
                    "Comment Error",
                    e.getMessage()
            );
        }
    }

    // ==========================================
    // MASK STUDENT ID
    // ==========================================

    private String maskStudentId(String studentId) {

        if (studentId == null || studentId.isBlank()) {
            return "Unknown student";
        }

        if (studentId.length() < 2) {
            return "****";
        }

        return studentId.charAt(0)
                + "*".repeat(studentId.length() - 2)
                + studentId.charAt(studentId.length() - 1);
    }

    // ==========================================
    // BACK TO DASHBOARD
    // ==========================================

    @FXML
    private void handleBackToDashboard(ActionEvent event) {

        try {

            FXMLLoader loader = new FXMLLoader(
                    getClass().getResource("/view/dashboard.fxml")
            );

            Parent root = loader.load();

            Node source = (Node) event.getSource();

            Stage stage = (Stage) source.getScene().getWindow();

            Scene scene = new Scene(root);

            stage.setScene(scene);
            stage.setTitle("LSPU-HNN | Dashboard");
            stage.setMaximized(true);
            stage.show();

        } catch (Exception e) {

            e.printStackTrace();

            showAlert(
                    Alert.AlertType.ERROR,
                    "Navigation Error",
                    "Unable to return to the Dashboard."
            );
        }
    }

    // ==========================================
    // ALERT
    // ==========================================

    private void showAlert(
            Alert.AlertType type,
            String title,
            String message) {

        Alert alert = new Alert(type);

        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);

        alert.showAndWait();
    }

    // ==========================================
    // ITEM MODEL
    // ==========================================

    public static class Item {

        private final int id;
        private final String itemName;
        private final String description;
        private final String category;
        private final String location;
        private final String campus;
        private final LocalDate dateReported;
        private final String type;
        private final String status;
        private final String imagePath;
        private final String postedBy;

        public Item(
                int id,
                String itemName,
                String description,
                String category,
                String location,
                String campus,
                LocalDate dateReported,
                String type,
                String status,
                String imagePath,
                String postedBy) {

            this.id = id;
            this.itemName = itemName;
            this.description = description;
            this.category = category;
            this.location = location;
            this.campus = campus;
            this.dateReported = dateReported;
            this.type = type;
            this.status = status;
            this.imagePath = imagePath;
            this.postedBy = postedBy;
        }

        public int getId() {
            return id;
        }

        public String getItemName() {
            return itemName;
        }

        public String getDescription() {
            return description;
        }

        public String getCategory() {
            return category;
        }

        public String getLocation() {
            return location;
        }

        public String getCampus() {
            return campus;
        }

        public LocalDate getDateReported() {
            return dateReported;
        }

        public String getType() {
            return type;
        }

        public String getStatus() {
            return status;
        }

        public String getImagePath() {
            return imagePath;
        }

        public String getPostedBy() {
            return postedBy;
        }
    }
}
