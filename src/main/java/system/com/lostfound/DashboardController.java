
package system.com.lostfound;

import java.io.IOException;
import java.net.URL;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.format.DateTimeFormatter;
import java.util.Collections;
import java.util.Map;
import java.util.Set;
import java.util.WeakHashMap;

import javafx.animation.FadeTransition;
import javafx.animation.Interpolator;
import javafx.animation.ParallelTransition;
import javafx.animation.ScaleTransition;
import javafx.animation.TranslateTransition;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.geometry.Pos;
import javafx.geometry.Side;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.chart.BarChart;
import javafx.scene.chart.PieChart;
import javafx.scene.chart.XYChart;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonType;
import javafx.scene.control.ContextMenu;
import javafx.scene.control.CustomMenuItem;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.SeparatorMenuItem;
import javafx.scene.control.Slider;
import javafx.scene.image.ImageView;
import javafx.scene.input.MouseEvent;
import javafx.scene.input.ScrollEvent;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.media.Media;
import javafx.scene.media.MediaPlayer;
import javafx.scene.media.MediaView;
import javafx.scene.paint.Color;
import javafx.scene.shape.Circle;
import javafx.stage.Stage;
import javafx.util.Duration;

public class DashboardController {

    // ==========================================
    // FXML COMPONENTS
    // ==========================================

    @FXML private VBox sidebar;
    @FXML private VBox navigationButtons;
    @FXML private StackPane contentArea;
    @FXML private ScrollPane dashboardScrollPane;
    @FXML private VBox dashboardContent;

    @FXML private StackPane welcomeBanner;
    @FXML private ImageView campusImageView;

    @FXML private Label lostCountLabel;
    @FXML private Label foundCountLabel;

    @FXML private BarChart<String, Number> historicalChart;
    @FXML private PieChart statusChart;

    @FXML private VBox recentActivityBox;

    @FXML private VBox loginHistorySection;
    @FXML private VBox loginHistoryBox;

    @FXML private Button profileButton;

    @FXML private Button dashboardNavButton;
    @FXML private Button historyNavButton;
    @FXML private Button campusNavButton;
    @FXML private Button traversalNavButton;
    @FXML private Button timelineNavButton;
    @FXML private Button lostFoundNavButton;

    @FXML private MediaView videoView;
    @FXML private Button playButton;
    @FXML private Label videoStatusLabel;

    @FXML private Slider videoProgressSlider;
    @FXML private Label videoCurrentTimeLabel;
    @FXML private Label videoDurationLabel;
    @FXML private Button rewindButton;
    @FXML private Button fastForwardButton;

    // ==========================================
    // VARIABLES
    // ==========================================

    private Node dashboardView;
    private MediaPlayer mediaPlayer;
    private ContextMenu profileMenu;

    private boolean videoInitialized = false;
    private boolean updatingVideoSlider = false;

    private Button activeNavigationButton;

    private final Set<Button> animatedButtons =
            Collections.newSetFromMap(new WeakHashMap<>());

    private final Map<Button, ScaleTransition> buttonAnimations =
            new WeakHashMap<>();

    private final Map<Button, String> originalButtonStyles =
            new WeakHashMap<>();

    private final Set<Button> hoverButtons =
            Collections.newSetFromMap(new WeakHashMap<>());

    // ==========================================
    // INITIALIZE
    // ==========================================

    @FXML
    public void initialize() {

        if (contentArea != null &&
                !contentArea.getChildren().isEmpty()) {
            dashboardView = contentArea.getChildren().get(0);
        }

        configureVerticalScrolling();
        configureWelcomeBanner();

        setupProfileButton();
        setupProfileMenu();
        setupLoginHistoryVisibility();

        setupCampusImageFade();
        setupHistoricalChart();
        setupVideo();

        loadDashboardData();

        updateActiveButton(dashboardNavButton);

        registerButtonAnimations(sidebar);
        registerButtonAnimations(contentArea);
        registerButtonAnimations(profileButton);

        applyHoverEffects(sidebar);
        applyHoverEffects(contentArea);
        applyHoverEffect(profileButton);
    }

    // ==========================================
    // PAGE TRANSITION ANIMATION
    // ==========================================

    private void animatePageTransition(Node page) {

        if (page == null) {
            return;
        }

        page.setOpacity(0);
        page.setTranslateY(20);

        FadeTransition fade =
                new FadeTransition(Duration.millis(450), page);

        fade.setFromValue(0);
        fade.setToValue(1);
        fade.setInterpolator(Interpolator.EASE_OUT);

        TranslateTransition slide =
                new TranslateTransition(Duration.millis(450), page);

        slide.setFromY(20);
        slide.setToY(0);
        slide.setInterpolator(Interpolator.EASE_OUT);

        new ParallelTransition(fade, slide).play();
    }

    // ==========================================
    // LOGIN HISTORY VISIBILITY
    // ==========================================

    private void setupLoginHistoryVisibility() {

        if (loginHistorySection == null) {
            return;
        }

        boolean admin = Session.isAdmin();

        loginHistorySection.setVisible(admin);
        loginHistorySection.setManaged(admin);

        if (admin) {
            loadLoginHistory();
        }
    }

    // ==========================================
    // LOAD ALL STUDENT LOGIN HISTORY
    // ==========================================

    private void loadLoginHistory() {

        if (!Session.isAdmin() || loginHistoryBox == null) {
            return;
        }

        loginHistoryBox.getChildren().clear();

        // Show ALL student accounts that have logged in.
        // Each student appears only once.
        // Show the latest login date and time.
        // Administrator accounts are excluded.
        // No LIMIT is used.

        String sql =
                "SELECT TRIM(account_identifier) AS account_identifier, " +
                "MAX(login_time) AS login_time " +
                "FROM login_logs " +
                "WHERE UPPER(TRIM(account_type)) = 'STUDENT' " +
                "AND account_identifier IS NOT NULL " +
                "AND TRIM(account_identifier) <> '' " +
                "GROUP BY TRIM(account_identifier) " +
                "ORDER BY MAX(login_time) DESC";

        try (Connection connection = DBConnection.getConnection()) {

            if (connection == null) {
                showLoginHistoryMessage(
                        "Unable to connect to the database."
                );
                return;
            }

            try (PreparedStatement statement =
                         connection.prepareStatement(sql);
                 ResultSet result = statement.executeQuery()) {

                boolean hasRecords = false;

                DateTimeFormatter formatter =
                        DateTimeFormatter.ofPattern(
                                "MMM dd, yyyy  |  hh:mm a"
                        );

                while (result.next()) {

                    hasRecords = true;

                    String identifier =
                            result.getString("account_identifier");

                    Timestamp loginTime =
                            result.getTimestamp("login_time");

                    if (identifier == null || identifier.trim().isEmpty()) {
                        identifier = "Unknown Account";
                    }

                    String formattedDate = "Date unavailable";

                    if (loginTime != null) {
                        formattedDate = loginTime.toLocalDateTime()
                                .format(formatter);
                    }

                    Label accountLabel = new Label(identifier);

                    accountLabel.setStyle(
                            "-fx-font-size: 14px;" +
                            "-fx-font-weight: bold;" +
                            "-fx-text-fill: #244B45;"
                    );

                    Label typeLabel = new Label("STUDENT");

                    typeLabel.setStyle(
                            "-fx-font-size: 10px;" +
                            "-fx-font-weight: bold;" +
                            "-fx-text-fill: #00796F;" +
                            "-fx-background-color: #DCEBDD;" +
                            "-fx-background-radius: 20;" +
                            "-fx-padding: 4 9;"
                    );

                    Label dateLabel = new Label(formattedDate);

                    dateLabel.setStyle(
                            "-fx-font-size: 11px;" +
                            "-fx-text-fill: #81918D;"
                    );

                    VBox accountDetails =
                            new VBox(5, accountLabel, dateLabel);

                    HBox loginRow =
                            new HBox(12, accountDetails, typeLabel);

                    loginRow.setAlignment(Pos.CENTER_LEFT);
                    loginRow.setSpacing(12);
                    loginRow.setMaxWidth(Double.MAX_VALUE);

                    HBox.setHgrow(
                            accountDetails,
                            Priority.ALWAYS
                    );

                    loginRow.setStyle(
                            "-fx-background-color: #F2F6FA;" +
                            "-fx-background-radius: 8;" +
                            "-fx-padding: 12;"
                    );

                    loginHistoryBox.getChildren().add(loginRow);
                }

                if (!hasRecords) {
                    showLoginHistoryMessage(
                            "No student login records available yet."
                    );
                }
            }

        } catch (SQLException e) {

            System.err.println(
                    "Error loading student login history: " + e.getMessage()
            );

            e.printStackTrace();

            showLoginHistoryMessage(
                    "Unable to load student login history. Check the login_logs table."
            );
        }
    }

    private void showLoginHistoryMessage(String message) {

        if (loginHistoryBox == null) {
            return;
        }

        loginHistoryBox.getChildren().clear();

        Label messageLabel = new Label(message);
        messageLabel.setWrapText(true);

        messageLabel.setStyle(
                "-fx-font-size: 13px;" +
                "-fx-text-fill: #71817E;" +
                "-fx-padding: 8;"
        );

        loginHistoryBox.getChildren().add(messageLabel);
    }

    // ==========================================
    // VERTICAL SCROLLING
    // ==========================================

    private void configureVerticalScrolling() {

        if (dashboardScrollPane == null) {
            return;
        }

        dashboardScrollPane.setFitToWidth(true);
        dashboardScrollPane.setFitToHeight(false);

        dashboardScrollPane.setHbarPolicy(
                ScrollPane.ScrollBarPolicy.NEVER
        );

        dashboardScrollPane.setVbarPolicy(
                ScrollPane.ScrollBarPolicy.AS_NEEDED
        );

        dashboardScrollPane.setPannable(false);
        dashboardScrollPane.setMinWidth(0);
        dashboardScrollPane.setMaxWidth(Double.MAX_VALUE);

        if (dashboardContent != null) {
            dashboardContent.setMinWidth(0);
            dashboardContent.setMaxWidth(Double.MAX_VALUE);
        }

        dashboardScrollPane.addEventFilter(
                ScrollEvent.SCROLL,
                event -> {
                    if (Math.abs(event.getDeltaX()) >
                            Math.abs(event.getDeltaY())) {
                        event.consume();
                        dashboardScrollPane.setHvalue(0);
                    }
                }
        );

        dashboardScrollPane.hvalueProperty().addListener(
                (observable, oldValue, newValue) -> {
                    if (newValue.doubleValue() != 0) {
                        dashboardScrollPane.setHvalue(0);
                    }
                }
        );

        Platform.runLater(() -> {
            dashboardScrollPane.setHbarPolicy(
                    ScrollPane.ScrollBarPolicy.NEVER
            );
            dashboardScrollPane.setPannable(false);
            dashboardScrollPane.setFitToWidth(true);
            dashboardScrollPane.setHvalue(0);
        });
    }

    // ==========================================
    // WELCOME BANNER
    // ==========================================

    private void configureWelcomeBanner() {

        if (welcomeBanner == null || campusImageView == null) {
            return;
        }

        welcomeBanner.setMinHeight(250);
        welcomeBanner.setPrefHeight(250);
        welcomeBanner.setMaxHeight(250);
        welcomeBanner.setMaxWidth(Double.MAX_VALUE);

        campusImageView.fitWidthProperty().bind(
                welcomeBanner.widthProperty()
        );

        campusImageView.fitHeightProperty().bind(
                welcomeBanner.heightProperty()
        );

        campusImageView.setPreserveRatio(false);
        campusImageView.setSmooth(true);
    }

    // ==========================================
    // BUTTON ANIMATIONS
    // ==========================================

    private void registerButtonAnimations(Node node) {

        if (node == null) {
            return;
        }

        if (node instanceof Button button &&
                animatedButtons.add(button)) {

            button.addEventHandler(
                    MouseEvent.MOUSE_PRESSED,
                    event -> animateButton(button, 0.96, 80)
            );

            button.addEventHandler(
                    MouseEvent.MOUSE_RELEASED,
                    event -> {
                        if (button.isHover()) {
                            animateButton(button, 1.04, 140);
                        } else {
                            animateButton(button, 1.0, 140);
                        }
                    }
            );

            button.addEventHandler(
                    MouseEvent.MOUSE_EXITED,
                    event -> animateButton(button, 1.0, 140)
            );
        }

        if (node instanceof Parent parent) {
            for (Node child : parent.getChildrenUnmodifiable()) {
                registerButtonAnimations(child);
            }
        }
    }

    private void animateButton(
            Button button,
            double scale,
            double duration
    ) {

        ScaleTransition previous =
                buttonAnimations.get(button);

        if (previous != null) {
            previous.stop();
        }

        ScaleTransition animation =
                new ScaleTransition(
                        Duration.millis(duration),
                        button
                );

        animation.setToX(scale);
        animation.setToY(scale);
        animation.setInterpolator(Interpolator.EASE_OUT);

        buttonAnimations.put(button, animation);

        animation.setOnFinished(event -> {
            if (buttonAnimations.get(button) == animation) {
                buttonAnimations.remove(button);
            }
        });

        animation.playFromStart();
    }

    // ==========================================
    // COLORED HOVER EFFECTS
    // ==========================================

    private void applyHoverEffects(Node node) {

        if (node == null) {
            return;
        }

        if (node instanceof Button button) {
            applyHoverEffect(button);
        }

        if (node instanceof Parent parent) {
            for (Node child : parent.getChildrenUnmodifiable()) {
                applyHoverEffects(child);
            }
        }
    }

    private void applyHoverEffect(Button button) {

        if (button == null || !hoverButtons.add(button)) {
            return;
        }

        button.addEventHandler(
                MouseEvent.MOUSE_ENTERED,
                event -> {

                    if (!isNavigationButton(button)) {
                        originalButtonStyles.put(
                                button,
                                button.getStyle() == null
                                        ? ""
                                        : button.getStyle()
                        );
                    }

                    button.setStyle(getHoverStyle(button));

                    animateButton(button, 1.04, 160);
                }
        );

        button.addEventHandler(
                MouseEvent.MOUSE_EXITED,
                event -> {

                    if (isNavigationButton(button)) {

                        restoreNavigationButton(button);

                    } else {

                        String originalStyle =
                                originalButtonStyles.get(button);

                        if (originalStyle != null) {
                            button.setStyle(originalStyle);
                        }
                    }

                    animateButton(button, 1.0, 160);
                }
        );
    }

    // ==========================================
    // NAVIGATION BUTTON CHECK
    // ==========================================

    private boolean isNavigationButton(Button button) {

        return button == dashboardNavButton ||
               button == historyNavButton ||
               button == campusNavButton ||
               button == traversalNavButton ||
               button == timelineNavButton ||
               button == lostFoundNavButton;
    }

    // ==========================================
    // RESTORE NAVIGATION BUTTON STYLE
    // ==========================================

    private void restoreNavigationButton(Button button) {

        if (button == null) {
            return;
        }

        if (button == activeNavigationButton) {

            button.setStyle(
                    "-fx-background-color: #00796F;" +
                    "-fx-text-fill: white;" +
                    "-fx-font-size: 14px;" +
                    "-fx-font-weight: bold;" +
                    "-fx-padding: 13 15;" +
                    "-fx-background-radius: 8;" +
                    "-fx-cursor: hand;"
            );

        } else {

            button.setStyle(
                    "-fx-background-color: transparent;" +
                    "-fx-text-fill: #294B47;" +
                    "-fx-font-size: 14px;" +
                    "-fx-padding: 13 15;" +
                    "-fx-background-radius: 8;" +
                    "-fx-cursor: hand;"
            );
        }
    }

    // ==========================================
    // GET HOVER COLORS
    // ==========================================

    private String getHoverStyle(Button button) {

        String originalStyle = button.getStyle();

        if (originalStyle == null) {
            originalStyle = "";
        }

        String hoverBackground;
        String hoverText;

        if (button.getText() != null &&
                button.getText().toLowerCase().contains("logout")) {

            hoverBackground = "#FADBD5";
            hoverText = "#B63E32";
        }

        else if (isNavigationButton(button)) {

            if (button == activeNavigationButton) {
                hoverBackground = "#009688";
                hoverText = "#FFFFFF";
            } else {
                hoverBackground = "#C3E5DC";
                hoverText = "#00695C";
            }
        }

        else if (originalStyle.contains("#D97757")) {

            hoverBackground = "#E58B6D";
            hoverText = "#FFFFFF";
        }

        else if (originalStyle.contains("#DCEBDD") ||
                originalStyle.contains("#FFF0ED")) {

            if (originalStyle.contains("#FFF0ED")) {
                hoverBackground = "#FADBD5";
                hoverText = "#B63E32";
            } else {
                hoverBackground = "#C3E5D6";
                hoverText = "#00695C";
            }
        }

        else if (originalStyle.contains("#00796F") ||
                originalStyle.contains("#008577") ||
                originalStyle.contains("#00695C")) {

            hoverBackground = "#00A58A";
            hoverText = "#FFFFFF";
        }

        else {

            hoverBackground = "#DCEFEB";
            hoverText = "#00695C";
        }

        String updatedStyle = originalStyle;

        if (updatedStyle.matches(
                "(?s).*?-fx-background-color\\s*:.*")) {

            updatedStyle = updatedStyle.replaceFirst(
                    "-fx-background-color\\s*:[^;]*;",
                    "-fx-background-color: " + hoverBackground + ";"
            );

        } else {

            updatedStyle +=
                    "-fx-background-color: " + hoverBackground + ";";
        }

        if (updatedStyle.matches(
                "(?s).*?-fx-text-fill\\s*:.*")) {

            updatedStyle = updatedStyle.replaceFirst(
                    "-fx-text-fill\\s*:[^;]*;",
                    "-fx-text-fill: " + hoverText + ";"
            );

        } else {

            updatedStyle +=
                    "-fx-text-fill: " + hoverText + ";";
        }

        return updatedStyle;
    }

    // ==========================================
    // NAVIGATION BUTTONS
    // ==========================================

    private Button[] getNavigationButtons() {

        return new Button[] {
                dashboardNavButton,
                historyNavButton,
                campusNavButton,
                traversalNavButton,
                timelineNavButton,
                lostFoundNavButton
        };
    }

    // ==========================================
    // ACTIVE NAVIGATION BUTTON
    // ==========================================

    private void updateActiveButton(Button activeButton) {

        activeNavigationButton = activeButton;

        for (Button button : getNavigationButtons()) {

            if (button == null) {
                continue;
            }

            restoreNavigationButton(button);
        }
    }

    // ==========================================
    // PROFILE BUTTON
    // ==========================================

    private void setupProfileButton() {

        if (profileButton == null) {
            return;
        }

        profileButton.setText("👤");

        profileButton.setStyle(
                "-fx-background-color: #DCEBDD;" +
                "-fx-text-fill: #285C35;" +
                "-fx-font-size: 22px;" +
                "-fx-background-radius: 50em;" +
                "-fx-border-color: white;" +
                "-fx-border-width: 2;" +
                "-fx-border-radius: 50em;" +
                "-fx-padding: 0;" +
                "-fx-cursor: hand;"
        );
    }

    // ==========================================
    // ACCOUNT INFORMATION
    // ==========================================

    private String getAccountName() {

        if (Session.isAdmin()) {
            return "Administrator";
        }

        if (Session.isStudent()) {
            return "Student";
        }

        return "User";
    }

    private String getAccountRole() {

        if (Session.isAdmin()) {
            return "ADMIN";
        }

        if (Session.isStudent()) {
            return "STUDENT";
        }

        return "USER";
    }

    private String getAccountIdentifier() {

        if (Session.isAdmin()) {
            return Session.getAdminUsername() != null
                    ? Session.getAdminUsername()
                    : "Admin username unavailable";
        }

        if (Session.isStudent()) {
            return Session.getStudentId() != null
                    ? Session.getStudentId()
                    : "School ID unavailable";
        }

        return "No account information";
    }

    // ==========================================
    // PROFILE MENU
    // ==========================================

    private void setupProfileMenu() {

        profileMenu = new ContextMenu();

        profileMenu.setStyle(
                "-fx-background-color: white;" +
                "-fx-background-radius: 12;" +
                "-fx-padding: 8;"
        );

        Circle avatarCircle = new Circle(23);
        avatarCircle.setFill(Color.web("#DCEBDD"));

        Label avatarLabel = new Label("👤");
        avatarLabel.setStyle("-fx-font-size: 22px;");

        StackPane avatar =
                new StackPane(avatarCircle, avatarLabel);

        avatar.setPrefSize(48, 48);

        Label accountName =
                new Label(getAccountName());

        accountName.setStyle(
                "-fx-font-size: 15px;" +
                "-fx-font-weight: bold;" +
                "-fx-text-fill: #244B45;"
        );

        Label accountRole =
                new Label(getAccountRole());

        accountRole.setStyle(
                "-fx-font-size: 11px;" +
                "-fx-font-weight: bold;" +
                "-fx-text-fill: #00796F;"
        );

        VBox accountDetails =
                new VBox(5, accountName, accountRole);

        accountDetails.setAlignment(Pos.CENTER_LEFT);

        HBox profileHeader =
                new HBox(12, avatar, accountDetails);

        profileHeader.setAlignment(Pos.CENTER_LEFT);
        profileHeader.setStyle("-fx-padding: 12;");

        CustomMenuItem profileItem =
                new CustomMenuItem(profileHeader);

        profileItem.setHideOnClick(false);

        Label accountInfo =
                new Label("ACCOUNT INFORMATION");

        accountInfo.setStyle(
                "-fx-font-size: 10px;" +
                "-fx-font-weight: bold;" +
                "-fx-text-fill: #81918D;" +
                "-fx-padding: 10 12 5 12;"
        );

        Label identifierLabel =
                new Label(getAccountIdentifier());

        identifierLabel.setWrapText(true);

        identifierLabel.setStyle(
                "-fx-font-size: 13px;" +
                "-fx-font-weight: bold;" +
                "-fx-text-fill: #294B47;" +
                "-fx-padding: 0 12 12 12;"
        );

        VBox accountInformation =
                new VBox(5, accountInfo, identifierLabel);

        CustomMenuItem accountInfoItem =
                new CustomMenuItem(accountInformation);

        accountInfoItem.setHideOnClick(false);

        Button logoutButton = new Button("↪   Logout");

        logoutButton.setMaxWidth(Double.MAX_VALUE);
        logoutButton.setPrefHeight(40);

        logoutButton.setStyle(
                "-fx-background-color: #FFF0ED;" +
                "-fx-text-fill: #C65343;" +
                "-fx-font-size: 13px;" +
                "-fx-font-weight: bold;" +
                "-fx-background-radius: 8;" +
                "-fx-cursor: hand;" +
                "-fx-alignment: CENTER_LEFT;" +
                "-fx-padding: 0 15;"
        );

        applyHoverEffect(logoutButton);

        logoutButton.setOnAction(event -> {
            profileMenu.hide();
            confirmLogout(profileButton);
        });

        CustomMenuItem logoutItem =
                new CustomMenuItem(logoutButton);

        logoutItem.setHideOnClick(true);

        profileMenu.getItems().setAll(
                profileItem,
                new SeparatorMenuItem(),
                accountInfoItem,
                new SeparatorMenuItem(),
                logoutItem
        );
    }

    @FXML
    private void handleProfileMenu(ActionEvent event) {

        if (profileMenu == null) {
            setupProfileMenu();
        }

        if (profileMenu.isShowing()) {
            profileMenu.hide();
        } else {
            setupProfileMenu();

            if (profileButton != null &&
                    profileButton.getScene() != null) {

                profileMenu.show(
                        profileButton,
                        Side.BOTTOM,
                        0,
                        5
                );
            }
        }
    }

    // ==========================================
    // LOGOUT
    // ==========================================

    @FXML
    private void handleLogout(ActionEvent event) {
        confirmLogout((Node) event.getSource());
    }

    private void confirmLogout(Node source) {

        Alert alert =
                new Alert(Alert.AlertType.CONFIRMATION);

        alert.setTitle("Logout Confirmation");
        alert.setHeaderText(
                "Are you sure you want to logout?"
        );

        alert.setContentText(
                "You will be returned to the login page."
        );

        alert.showAndWait().ifPresent(response -> {

            if (response == ButtonType.OK) {

                try {

                    URL loginURL =
                            getClass().getResource("/view/login.fxml");

                    if (loginURL == null) {
                        throw new IOException(
                                "login.fxml was not found."
                        );
                    }

                    Parent loginRoot =
                            FXMLLoader.load(loginURL);

                    if (mediaPlayer != null) {
                        mediaPlayer.stop();
                        mediaPlayer.dispose();
                        mediaPlayer = null;
                    }

                    if (profileMenu != null) {
                        profileMenu.hide();
                    }

                    Session.clearSession();

                    Stage stage =
                            (Stage) source.getScene().getWindow();

                    Scene scene = new Scene(loginRoot);

                    stage.setScene(scene);
                    stage.setTitle("LSPU-HNN Login");
                    stage.centerOnScreen();

                } catch (IOException e) {

                    showError("Logout Error", e.getMessage());
                    e.printStackTrace();
                }
            }
        });
    }

    // ==========================================
    // CAMPUS IMAGE FADE
    // ==========================================

    private void setupCampusImageFade() {

        if (campusImageView == null) {
            return;
        }

        campusImageView.setOpacity(0);

        FadeTransition fade =
                new FadeTransition(
                        Duration.seconds(2),
                        campusImageView
                );

        fade.setFromValue(0);
        fade.setToValue(1);
        fade.play();
    }

    // ==========================================
    // HISTORICAL BAR CHART
    // ==========================================

    private void setupHistoricalChart() {

        if (historicalChart == null) {
            return;
        }

        historicalChart.setTitle(null);
        historicalChart.setAnimated(true);
        historicalChart.setLegendVisible(false);

        XYChart.Series<String, Number> series =
                new XYChart.Series<>();

        series.getData().add(new XYChart.Data<>("1950s", 2));
        series.getData().add(new XYChart.Data<>("1960s", 3));
        series.getData().add(new XYChart.Data<>("1970s", 4));
        series.getData().add(new XYChart.Data<>("1980s", 5));
        series.getData().add(new XYChart.Data<>("1990s", 6));
        series.getData().add(new XYChart.Data<>("2000s", 8));

        historicalChart.getData().clear();
        historicalChart.getData().add(series);

        Platform.runLater(() -> {

            historicalChart.lookupAll(".axis-label").forEach(node ->
                    node.setStyle(
                            "-fx-text-fill: #294B47;" +
                            "-fx-font-size: 12px;"
                    )
            );

            historicalChart.lookupAll(".axis-tick-label").forEach(node ->
                    node.setStyle(
                            "-fx-text-fill: #526B68;" +
                            "-fx-font-size: 11px;"
                    )
            );
        });
    }

    // ==========================================
    // DATABASE DASHBOARD DATA
    // ==========================================

    private void loadDashboardData() {

        loadLostAndFoundCounts();
        loadStatusChart();
        loadRecentActivities();
    }

    private void loadLostAndFoundCounts() {

        String sql =
                "SELECT COUNT(*) FROM lost_found_items WHERE type = ?";

        try (Connection connection =
                     DBConnection.getConnection()) {

            if (connection == null) {
                return;
            }

            try (PreparedStatement statement =
                         connection.prepareStatement(sql)) {

                statement.setString(1, "lost");

                try (ResultSet result =
                             statement.executeQuery()) {

                    if (result.next() && lostCountLabel != null) {
                        lostCountLabel.setText(
                                String.valueOf(result.getInt(1))
                        );
                    }
                }
            }

            try (PreparedStatement statement =
                         connection.prepareStatement(sql)) {

                statement.setString(1, "found");

                try (ResultSet result =
                             statement.executeQuery()) {

                    if (result.next() && foundCountLabel != null) {
                        foundCountLabel.setText(
                                String.valueOf(result.getInt(1))
                        );
                    }
                }
            }

        } catch (SQLException e) {

            System.err.println(
                    "Error loading item counts: " + e.getMessage()
            );

            e.printStackTrace();
        }
    }

    // ==========================================
    // LOST AND FOUND PIE CHART
    // ==========================================

    private void loadStatusChart() {

        if (statusChart == null) {
            return;
        }

        ObservableList<PieChart.Data> chartData =
                FXCollections.observableArrayList();

        String sql =
                "SELECT status, COUNT(*) AS total " +
                "FROM lost_found_items GROUP BY status";

        try (Connection connection =
                     DBConnection.getConnection()) {

            if (connection == null) {
                return;
            }

            try (PreparedStatement statement =
                         connection.prepareStatement(sql);
                 ResultSet result = statement.executeQuery()) {

                while (result.next()) {

                    String status = result.getString("status");
                    int total = result.getInt("total");

                    if (status == null ||
                            status.trim().isEmpty()) {
                        status = "Unknown";
                    }

                    chartData.add(
                            new PieChart.Data(status, total)
                    );
                }
            }

            statusChart.setData(chartData);
            statusChart.setTitle(null);
            statusChart.setLabelsVisible(true);
            statusChart.setLegendVisible(true);

            Platform.runLater(() -> {

                statusChart.lookupAll(".chart-pie-label").forEach(node ->
                        node.setStyle(
                                "-fx-fill: #294B47;" +
                                "-fx-font-size: 12px;"
                        )
                );

                statusChart.lookupAll(".chart-legend-item-label").forEach(node ->
                        node.setStyle(
                                "-fx-text-fill: #526B68;" +
                                "-fx-font-size: 12px;"
                        )
                );
            });

        } catch (SQLException e) {

            System.err.println(
                    "Error loading status chart: " + e.getMessage()
            );

            e.printStackTrace();
        }
    }

    // ==========================================
    // RECENT ACTIVITIES
    // ==========================================

    private void loadRecentActivities() {

        if (recentActivityBox == null) {
            return;
        }

        recentActivityBox.getChildren().clear();

        String sql =
                "SELECT item_name, type, status, date_reported " +
                "FROM lost_found_items " +
                "ORDER BY date_reported DESC, id DESC LIMIT 5";

        try (Connection connection =
                     DBConnection.getConnection()) {

            if (connection == null) {
                showNoRecentActivities();
                return;
            }

            try (PreparedStatement statement =
                         connection.prepareStatement(sql);
                 ResultSet result = statement.executeQuery()) {

                boolean hasActivities = false;

                while (result.next()) {

                    hasActivities = true;

                    String itemName =
                            result.getString("item_name");

                    String type =
                            result.getString("type");

                    String status =
                            result.getString("status");

                    Timestamp date =
                            result.getTimestamp("date_reported");

                    if (itemName == null) {
                        itemName = "Unnamed Item";
                    }

                    if (type == null) {
                        type = "Unknown";
                    }

                    if (status == null) {
                        status = "Unknown";
                    }

                    Label itemLabel = new Label(
                            itemName + "  |  " +
                            type.toUpperCase() + "  |  " +
                            status
                    );

                    itemLabel.setWrapText(true);

                    itemLabel.setStyle(
                            "-fx-font-size: 13px;" +
                            "-fx-text-fill: #294B47;" +
                            "-fx-padding: 10;" +
                            "-fx-background-color: #F2F6FA;" +
                            "-fx-background-radius: 7;"
                    );

                    Label dateLabel = new Label(
                            date != null ? date.toString() : ""
                    );

                    dateLabel.setStyle(
                            "-fx-font-size: 10px;" +
                            "-fx-text-fill: #81918D;" +
                            "-fx-padding: 0 10 8 10;"
                    );

                    VBox activity =
                            new VBox(3, itemLabel, dateLabel);

                    recentActivityBox.getChildren().add(activity);
                }

                if (!hasActivities) {
                    showNoRecentActivities();
                }
            }

        } catch (SQLException e) {

            System.err.println(
                    "Error loading recent activities: " + e.getMessage()
            );

            e.printStackTrace();
            showNoRecentActivities();
        }
    }

    private void showNoRecentActivities() {

        if (recentActivityBox == null) {
            return;
        }

        recentActivityBox.getChildren().clear();

        Label message =
                new Label("No recent activities available.");

        message.setStyle(
                "-fx-font-size: 13px;" +
                "-fx-text-fill: #71817E;"
        );

        recentActivityBox.getChildren().add(message);
    }

    // ==========================================
    // VIDEO SETUP
    // ==========================================

    private void setupVideo() {

        if (videoView == null) {
            return;
        }

        if (videoProgressSlider != null) {
            videoProgressSlider.setMin(0);
            videoProgressSlider.setMax(1);
            videoProgressSlider.setValue(0);
            videoProgressSlider.setDisable(true);

            videoProgressSlider.setOnMouseReleased(event ->
                    seekToSliderPosition()
            );

            videoProgressSlider.setOnKeyReleased(event ->
                    seekToSliderPosition()
            );

            videoProgressSlider.valueChangingProperty().addListener(
                    (observable, wasChanging, isChanging) -> {
                        if (wasChanging && !isChanging) {
                            seekToSliderPosition();
                        }
                    }
            );

            videoProgressSlider.valueProperty().addListener(
                    (observable, oldValue, newValue) -> {
                        if (videoCurrentTimeLabel != null &&
                                videoProgressSlider.isValueChanging()) {
                            videoCurrentTimeLabel.setText(
                                    formatVideoTime(
                                            Duration.seconds(newValue.doubleValue())
                                    )
                            );
                        }
                    }
            );
        }

        try {

            URL videoURL =
                    getClass().getResource(
                            "/videos/lspu-introduction.mp4"
                    );

            if (videoURL == null) {

                if (videoStatusLabel != null) {
                    videoStatusLabel.setText(
                            "Video file not found."
                    );
                }

                return;
            }

            Media media =
                    new Media(videoURL.toExternalForm());

            mediaPlayer = new MediaPlayer(media);
            videoView.setMediaPlayer(mediaPlayer);

            mediaPlayer.setOnReady(() -> {

                videoInitialized = true;

                Duration totalDuration =
                        mediaPlayer.getTotalDuration();

                if (totalDuration != null &&
                        !totalDuration.isUnknown() &&
                        !totalDuration.isIndefinite()) {

                    if (videoProgressSlider != null) {
                        videoProgressSlider.setMin(0);
                        videoProgressSlider.setMax(
                                totalDuration.toSeconds()
                        );
                        videoProgressSlider.setValue(0);
                        videoProgressSlider.setDisable(false);
                    }

                    if (videoDurationLabel != null) {
                        videoDurationLabel.setText(
                                formatVideoTime(totalDuration)
                        );
                    }
                }

                if (videoCurrentTimeLabel != null) {
                    videoCurrentTimeLabel.setText("00:00");
                }

                if (videoStatusLabel != null) {
                    videoStatusLabel.setText(
                            "Video ready to play."
                    );
                }

                setupVideoProgressListener();
            });

            mediaPlayer.setOnEndOfMedia(() -> {

                if (videoStatusLabel != null) {
                    videoStatusLabel.setText("Video finished.");
                }

                if (playButton != null) {
                    playButton.setText("▶ Play");
                }

                if (videoProgressSlider != null &&
                        mediaPlayer.getTotalDuration() != null) {
                    videoProgressSlider.setValue(
                            mediaPlayer.getTotalDuration().toSeconds()
                    );
                }
            });

            mediaPlayer.setOnError(() -> {

                if (videoStatusLabel != null) {
                    videoStatusLabel.setText(
                            "Unable to play video."
                    );
                }

                if (videoProgressSlider != null) {
                    videoProgressSlider.setDisable(true);
                }

                if (mediaPlayer != null) {
                    System.err.println(mediaPlayer.getError());
                }
            });

        } catch (Exception e) {

            System.err.println(
                    "Video setup error: " + e.getMessage()
            );

            e.printStackTrace();

            if (videoStatusLabel != null) {
                videoStatusLabel.setText(
                        "Video could not be loaded."
                );
            }
        }
    }

    // ==========================================
    // VIDEO PROGRESS LISTENER
    // ==========================================

    private void setupVideoProgressListener() {

        if (mediaPlayer == null) {
            return;
        }

        mediaPlayer.currentTimeProperty().addListener(
                (observable, oldTime, newTime) -> {

                    if (videoProgressSlider == null ||
                            videoProgressSlider.isValueChanging()) {
                        return;
                    }

                    updatingVideoSlider = true;

                    videoProgressSlider.setValue(
                            newTime.toSeconds()
                    );

                    updatingVideoSlider = false;

                    if (videoCurrentTimeLabel != null) {
                        videoCurrentTimeLabel.setText(
                                formatVideoTime(newTime)
                        );
                    }
                }
        );
    }

    // ==========================================
    // SEEK VIDEO
    // ==========================================

    private void seekToSliderPosition() {

        if (mediaPlayer == null ||
                !videoInitialized ||
                videoProgressSlider == null ||
                updatingVideoSlider) {
            return;
        }

        mediaPlayer.seek(
                Duration.seconds(videoProgressSlider.getValue())
        );
    }

    // ==========================================
    // FORMAT VIDEO TIME
    // ==========================================

    private String formatVideoTime(Duration duration) {

        if (duration == null ||
                duration.isUnknown() ||
                duration.isIndefinite()) {
            return "00:00";
        }

        long totalSeconds =
                Math.max(0, (long) duration.toSeconds());

        long hours = totalSeconds / 3600;
        long minutes = (totalSeconds % 3600) / 60;
        long seconds = totalSeconds % 60;

        if (hours > 0) {
            return String.format(
                    "%02d:%02d:%02d",
                    hours,
                    minutes,
                    seconds
            );
        }

        return String.format(
                "%02d:%02d",
                minutes,
                seconds
        );
    }

    // ==========================================
    // REWIND AND FAST-FORWARD
    // ==========================================

    private void seekVideoBy(double seconds) {

        if (mediaPlayer == null || !videoInitialized) {
            return;
        }

        Duration totalDuration =
                mediaPlayer.getTotalDuration();

        if (totalDuration == null ||
                totalDuration.isUnknown() ||
                totalDuration.isIndefinite()) {
            return;
        }

        double currentSeconds =
                mediaPlayer.getCurrentTime().toSeconds();

        double newSeconds =
                currentSeconds + seconds;

        newSeconds = Math.max(
                0,
                Math.min(newSeconds, totalDuration.toSeconds())
        );

        mediaPlayer.seek(Duration.seconds(newSeconds));

        if (videoProgressSlider != null) {
            videoProgressSlider.setValue(newSeconds);
        }

        if (videoCurrentTimeLabel != null) {
            videoCurrentTimeLabel.setText(
                    formatVideoTime(Duration.seconds(newSeconds))
            );
        }
    }

    @FXML
    private void handleRewindVideo(ActionEvent event) {
        seekVideoBy(-10);
    }

    @FXML
    private void handleFastForwardVideo(ActionEvent event) {
        seekVideoBy(10);
    }

    // ==========================================
    // VIDEO PLAY / PAUSE
    // ==========================================

    @FXML
    private void handlePlayPause(ActionEvent event) {

        if (mediaPlayer == null || !videoInitialized) {

            if (videoStatusLabel != null) {
                videoStatusLabel.setText("Video is not ready.");
            }

            return;
        }

        if (mediaPlayer.getStatus() ==
                MediaPlayer.Status.PLAYING) {

            mediaPlayer.pause();

            if (playButton != null) {
                playButton.setText("▶ Play");
            }

            if (videoStatusLabel != null) {
                videoStatusLabel.setText("Video paused.");
            }

        } else {

            mediaPlayer.play();

            if (playButton != null) {
                playButton.setText("Ⅱ Pause");
            }

            if (videoStatusLabel != null) {
                videoStatusLabel.setText("Playing video...");
            }
        }
    }

    // ==========================================
    // VIDEO STOP
    // ==========================================

    @FXML
    private void handleStopVideo(ActionEvent event) {

        if (mediaPlayer == null) {
            return;
        }

        mediaPlayer.stop();

        if (playButton != null) {
            playButton.setText("▶ Play");
        }

        if (videoStatusLabel != null) {
            videoStatusLabel.setText("Video stopped.");
        }
    }

    // ==========================================
    // VIDEO REPLAY
    // ==========================================

    @FXML
    private void handleReplayVideo(ActionEvent event) {

        if (mediaPlayer == null || !videoInitialized) {
            return;
        }

        mediaPlayer.seek(Duration.ZERO);
        mediaPlayer.play();

        if (playButton != null) {
            playButton.setText("Ⅱ Pause");
        }

        if (videoStatusLabel != null) {
            videoStatusLabel.setText("Replaying video...");
        }
    }

    // ==========================================
    // DASHBOARD NAVIGATION
    // ==========================================

    @FXML
    private void handleDashboard(ActionEvent event) {

        updateActiveButton(dashboardNavButton);

        if (dashboardView != null) {

            contentArea.getChildren().setAll(dashboardView);

            configureVerticalScrolling();
            configureWelcomeBanner();

            registerButtonAnimations(dashboardView);
            applyHoverEffects(dashboardView);

            animatePageTransition(dashboardView);

        } else {

            showError(
                    "Dashboard Error",
                    "The dashboard content could not be restored."
            );
        }
    }

    // ==========================================
    // HISTORY NAVIGATION
    // ==========================================

    @FXML
    private void handleHistory(ActionEvent event) {
        updateActiveButton(historyNavButton);
        openPage("/view/history.fxml");
    }

    // ==========================================
    // CAMPUS NAVIGATION
    // ==========================================

    @FXML
    private void handleCampus(ActionEvent event) {
        updateActiveButton(campusNavButton);
        openPage("/view/campus.fxml");
    }

    // ==========================================
    // TRAVERSAL NAVIGATION
    // ==========================================

    @FXML
    private void handleTraversal(ActionEvent event) {
        updateActiveButton(traversalNavButton);
        openPage("/view/traversal.fxml");
    }

    // ==========================================
    // TIMELINE NAVIGATION
    // ==========================================

    @FXML
    private void handleTimeline(ActionEvent event) {
        updateActiveButton(timelineNavButton);
        openPage("/view/timeline.fxml");
    }

    // ==========================================
    // LOST AND FOUND NAVIGATION
    // ==========================================

    @FXML
    private void handleLostFound(ActionEvent event) {
        updateActiveButton(lostFoundNavButton);
        openPage("/view/lostfound.fxml");
    }

    // ==========================================
    // LOAD OTHER PAGES
    // ==========================================

    private void openPage(String fxmlPath) {

        try {

            URL pageURL =
                    getClass().getResource(fxmlPath);

            if (pageURL == null) {

                showError(
                        "Page Not Found",
                        "Cannot find: " + fxmlPath
                );

                return;
            }

            Parent page =
                    FXMLLoader.load(pageURL);

            contentArea.getChildren().setAll(page);

            registerButtonAnimations(page);
            applyHoverEffects(page);

            animatePageTransition(page);

            if (mediaPlayer != null &&
                    mediaPlayer.getStatus() ==
                            MediaPlayer.Status.PLAYING) {

                mediaPlayer.pause();

                if (playButton != null) {
                    playButton.setText("▶ Play");
                }
            }

        } catch (IOException e) {

            showError(
                    "Navigation Error",
                    "Unable to open " + fxmlPath +
                    "\n" + e.getMessage()
            );

            e.printStackTrace();
        }
    }

    // ==========================================
    // ERROR ALERT
    // ==========================================

    private void showError(String title, String message) {

        Alert alert =
                new Alert(Alert.AlertType.ERROR);

        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);

        alert.showAndWait();
    }
}
