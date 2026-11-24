package system.com.lostfound;

import java.io.IOException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

import org.mindrot.jbcrypt.BCrypt;

import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.ComboBox;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import javafx.stage.Stage;

public class LoginController {

    @FXML
    private ComboBox<String> accountTypeComboBox;

    @FXML
    private TextField nameField;

    @FXML
    private PasswordField passwordField;

    @FXML
    public void initialize() {

        accountTypeComboBox.getItems().setAll(
                "Student",
                "Admin"
        );

        accountTypeComboBox.setValue("Student");

        updateLoginFields();
    }

    @FXML
    private void handleAccountTypeChange() {
        updateLoginFields();
    }

    private void updateLoginFields() {

        if (accountTypeComboBox == null) {
            return;
        }

        boolean adminSelected =
                "Admin".equals(accountTypeComboBox.getValue());

        if (adminSelected) {
            nameField.setPromptText("Enter Admin Username");
            passwordField.setVisible(true);
            passwordField.setManaged(true);
        } else {
            nameField.setPromptText("Enter your School ID");
            passwordField.clear();
            passwordField.setVisible(false);
            passwordField.setManaged(false);
        }
    }

    private boolean isValidSchoolId(String schoolId) {
        return schoolId.matches("\\d{4}-\\d{4}");
    }

    @FXML
    public void handleLogin(ActionEvent event) throws IOException {

        String accountType = accountTypeComboBox.getValue();

        if ("Admin".equals(accountType)) {
            handleAdminLogin(event);
        } else {
            handleStudentLogin(event);
        }
    }

    // ==========================================
    // STUDENT LOGIN
    // ==========================================

    private void handleStudentLogin(ActionEvent event)
            throws IOException {

        String schoolId = nameField.getText().trim();

        if (schoolId.isEmpty()) {
            showAlert(
                    Alert.AlertType.WARNING,
                    "Login Required",
                    "Please enter your School ID."
            );
            return;
        }

        if (!isValidSchoolId(schoolId)) {
            showAlert(
                    Alert.AlertType.ERROR,
                    "Invalid School ID",
                    "Please enter a valid School ID.\n\n"
                    + "Format: 0225-1019\n"
                    + "Required: 4 digits - 4 digits"
            );
            return;
        }

        Session.setStudentId(schoolId);

        recordLogin(schoolId, "STUDENT");

        openDashboard(event);
    }

    // ==========================================
    // ADMIN LOGIN
    // ==========================================

    private void handleAdminLogin(ActionEvent event)
            throws IOException {

        String username = nameField.getText().trim();
        String password = passwordField.getText();

        if (username.isEmpty() || password.isEmpty()) {
            showAlert(
                    Alert.AlertType.WARNING,
                    "Login Required",
                    "Please enter your admin username and password."
            );
            return;
        }

        String sql =
                "SELECT password_hash FROM admins WHERE username = ?";

        try (Connection connection = DBConnection.getConnection()) {

            if (connection == null) {
                showAlert(
                        Alert.AlertType.ERROR,
                        "Database Error",
                        "Unable to connect to the database."
                );
                return;
            }

            try (PreparedStatement statement =
                    connection.prepareStatement(sql)) {

                statement.setString(1, username);

                try (ResultSet result = statement.executeQuery()) {

                    if (result.next()) {

                        String storedHash =
                                result.getString("password_hash");

                        if (BCrypt.checkpw(password, storedHash)) {

                            Session.setAdmin(username);

                            recordLogin(username, "ADMIN");

                            passwordField.clear();

                            openDashboard(event);
                            return;
                        }
                    }
                }
            }

            showAlert(
                    Alert.AlertType.ERROR,
                    "Login Failed",
                    "Incorrect admin username or password."
            );

        } catch (SQLException e) {

            e.printStackTrace();

            showAlert(
                    Alert.AlertType.ERROR,
                    "Database Error",
                    "An error occurred while verifying your admin account."
            );
        }
    }

    // ==========================================
    // SAVE LOGIN HISTORY
    // ==========================================

    private void recordLogin(
            String accountIdentifier,
            String accountType
    ) {

        String sql =
                "INSERT INTO login_logs " +
                "(account_identifier, account_type) " +
                "VALUES (?, ?)";

        try (Connection connection = DBConnection.getConnection()) {

            if (connection == null) {
                System.err.println(
                        "Login history was not saved: database unavailable."
                );
                return;
            }

            try (PreparedStatement statement =
                         connection.prepareStatement(sql)) {

                statement.setString(1, accountIdentifier);
                statement.setString(2, accountType);

                statement.executeUpdate();

                System.out.println(
                        "Login history saved for: " +
                        accountIdentifier
                );
            }

        } catch (SQLException e) {

            System.err.println(
                    "Unable to save login history: " + e.getMessage()
            );

            e.printStackTrace();
        }
    }

    // ==========================================
    // OPEN DASHBOARD
    // ==========================================

    private void openDashboard(ActionEvent event)
            throws IOException {

        FXMLLoader loader = new FXMLLoader(
                getClass().getResource("/view/dashboard.fxml")
        );

        Parent dashboard = loader.load();

        Stage stage = (Stage) ((Node) event.getSource())
                .getScene().getWindow();

        Scene scene = new Scene(dashboard);

        stage.setScene(scene);
        stage.setTitle("LSPU-HNN | Dashboard");
        stage.setMaximized(true);
        stage.show();
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
}