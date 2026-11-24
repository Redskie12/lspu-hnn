
package system.com.lostfound;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;

import java.sql.Connection;

public class Main extends Application {

    @Override
    public void start(Stage stage) throws Exception {

        // Test MySQL Database Connection
        try (Connection connection = DBConnection.getConnection()) {

            if (connection != null) {
                System.out.println("LSPU-HNN Database is Connected!");
            }

        } catch (Exception e) {
            System.out.println("Unable to connect to database.");
            e.printStackTrace();
        }

        // Load Login Page
        Parent root = FXMLLoader.load(
            getClass().getResource("/view/login.fxml")
        );

        // Set window dimensions
        Scene scene = new Scene(root, 1200, 750);

        stage.setTitle("LSPU-HNN | Login");
        stage.setScene(scene);

        stage.setWidth(1200);
        stage.setHeight(750);

        // Prevent resizing
        stage.setResizable(false);

        // Open window in the center
        stage.centerOnScreen();
        stage.show();
    }

    public static void main(String[] args) {
        launch(args);
    }
}
