
package system.com.lostfound;

import java.io.IOException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;

public class TimelineController {

    @FXML
    private VBox timelineContainer;

    @FXML
    private ComboBox<String> sortComboBox;

    @FXML
    private ComboBox<String> yearFilterComboBox;

    private final BST historyBST = new BST();

    @FXML
    public void initialize() {

        loadHistoricalEvents();

        // SORTING OPTIONS
        sortComboBox.getItems().addAll(
                "Oldest to Newest",
                "Newest to Oldest",
                "Title A - Z",
                "Title Z - A"
        );

        // YEAR FILTER OPTIONS
        yearFilterComboBox.getItems().addAll(
                "All Years",
                "1950 - 1969",
                "1970 - 1989",
                "1990 - 2009",
                "2010 onwards"
        );

        // DEFAULT SELECTIONS
        sortComboBox.setValue("Oldest to Newest");
        yearFilterComboBox.setValue("All Years");

        displayTimeline();
    }

    // LOAD HISTORICAL EVENTS INTO THE BST
    private void loadHistoricalEvents() {

        historyBST.insert(new HistoryEvent(
                1952,
                "Establishment of Baybay Provincial High School",
                "The institution was founded in Siniloan, Laguna, "
                + "providing educational opportunities to students.",
                "/images/history/1952.jpeg"
        ));

        historyBST.insert(new HistoryEvent(
                1957,
                "Conversion into Baybay Agricultural and Vocational School",
                "The school was reorganized to focus on agricultural "
                + "and vocational education.",
                "/images/history/1957.jpeg"
        ));

        historyBST.insert(new HistoryEvent(
                1971,
                "Establishment of Baybay National College of Agriculture and Technology",
                "The institution expanded its educational programs "
                + "in agriculture and technology.",
                "/images/history/1971.jpeg"
        ));

        historyBST.insert(new HistoryEvent(
                1983,
                "Establishment of Laguna State Polytechnic College",
                "On June 10, 1983, Batas Pambansa Blg. 482 established "
                + "Laguna State Polytechnic College.",
                "/images/history/1983.jpeg"
        ));

        historyBST.insert(new HistoryEvent(
                1999,
                "Integration of Santa Cruz and Los Baños Campuses",
                "The integration expanded the institution's "
                + "academic reach and educational services.",
                "/images/history/1999.jpeg"
        ));

        historyBST.insert(new HistoryEvent(
                2000,
                "Integration of San Pablo City Campus",
                "The San Pablo City Campus became part of "
                + "Laguna State Polytechnic College.",
                "/images/history/2000.jpeg"
        ));

        historyBST.insert(new HistoryEvent(
                2007,
                "Conversion into Laguna State Polytechnic University",
                "Republic Act No. 9402 converted Laguna State "
                + "Polytechnic College into Laguna State Polytechnic University.",
                "/images/history/2007.jpeg"
        ));

        historyBST.insert(new HistoryEvent(
                2025,
                "LSPU 73rd Founding Anniversary",
                "LSPU celebrated its 73rd Founding Anniversary "
                + "on December 3, 2025, at the Siniloan Host Campus.",
                "/images/history/2025.jpeg"
        ));
    }

    // HANDLE FILTER CHANGES
    @FXML
    private void handleFiltersChanged(ActionEvent event) {
        displayTimeline();
    }

    // DISPLAY FILTERED AND SORTED EVENTS
    private void displayTimeline() {

        timelineContainer.getChildren().clear();

        // GET ALL EVENTS FROM THE BST
        List<HistoryEvent> events = new ArrayList<>();

        for (HistoryEvent event : historyBST.getInOrder()) {
            events.add(event);
        }

        // APPLY YEAR FILTER
        String selectedYear = yearFilterComboBox.getValue();

        if (selectedYear != null) {

            events.removeIf(event -> {

                int year = event.getYear();

                switch (selectedYear) {

                    case "1950 - 1969":
                        return year < 1950 || year > 1969;

                    case "1970 - 1989":
                        return year < 1970 || year > 1989;

                    case "1990 - 2009":
                        return year < 1990 || year > 2009;

                    case "2010 onwards":
                        return year < 2010;

                    default:
                        return false;
                }
            });
        }

        // APPLY SORTING
        String selectedSort = sortComboBox.getValue();

        if (selectedSort != null) {

            switch (selectedSort) {

                case "Oldest to Newest":
                    events.sort(
                            Comparator.comparingInt(HistoryEvent::getYear)
                    );
                    break;

                case "Newest to Oldest":
                    events.sort(
                            Comparator.comparingInt(HistoryEvent::getYear)
                                    .reversed()
                    );
                    break;

                case "Title A - Z":
                    events.sort(
                            Comparator.comparing(
                                    HistoryEvent::getTitle,
                                    String.CASE_INSENSITIVE_ORDER
                            )
                    );
                    break;

                case "Title Z - A":
                    events.sort(
                            Comparator.comparing(
                                    HistoryEvent::getTitle,
                                    String.CASE_INSENSITIVE_ORDER
                            ).reversed()
                    );
                    break;
            }
        }

        // DISPLAY RESULTS
        if (events.isEmpty()) {

            Label emptyLabel = new Label(
                    "No historical events found for this year range."
            );

            emptyLabel.setStyle(
                    "-fx-font-size: 14px;"
                    + "-fx-text-fill: #66756c;"
                    + "-fx-padding: 20;"
            );

            timelineContainer.getChildren().add(emptyLabel);

            return;
        }

        for (HistoryEvent event : events) {

            HBox eventCard = createEventCard(event);

            timelineContainer.getChildren().add(eventCard);
        }
    }

    // CREATE AN INDIVIDUAL TIMELINE CARD
    private HBox createEventCard(HistoryEvent event) {

        HBox card = new HBox(20);

        card.setAlignment(Pos.CENTER_LEFT);
        card.setPadding(new Insets(18));
        card.setMaxWidth(Double.MAX_VALUE);

        card.setStyle(
                "-fx-background-color: white;"
                + "-fx-background-radius: 12;"
                + "-fx-border-color: #dddddd;"
                + "-fx-border-radius: 12;"
        );

        // HISTORICAL IMAGE
        ImageView imageView = new ImageView();

        imageView.setFitWidth(220);
        imageView.setFitHeight(145);
        imageView.setPreserveRatio(true);
        imageView.setSmooth(true);

        URL imageURL = getClass().getResource(event.getImagePath());

        if (imageURL != null) {

            Image image = new Image(imageURL.toExternalForm());

            imageView.setImage(image);

        } else {

            System.out.println(
                    "Image not found: " + event.getImagePath()
            );
        }

        // EVENT INFORMATION
        VBox information = new VBox(10);

        information.setAlignment(Pos.CENTER_LEFT);
        information.setMaxWidth(Double.MAX_VALUE);

        HBox.setHgrow(information, Priority.ALWAYS);

        Label yearLabel = new Label(
                String.valueOf(event.getYear())
        );

        yearLabel.setStyle(
                "-fx-font-size: 25px;"
                + "-fx-font-weight: bold;"
                + "-fx-text-fill: #1b5e20;"
        );

        Label titleLabel = new Label(event.getTitle());

        titleLabel.setWrapText(true);

        titleLabel.setStyle(
                "-fx-font-size: 17px;"
                + "-fx-font-weight: bold;"
                + "-fx-text-fill: #222222;"
        );

        Label descriptionLabel = new Label(
                event.getDescription()
        );

        descriptionLabel.setWrapText(true);

        descriptionLabel.setStyle(
                "-fx-font-size: 13px;"
                + "-fx-text-fill: #555555;"
        );

        information.getChildren().addAll(
                yearLabel,
                titleLabel,
                descriptionLabel
        );

        card.getChildren().addAll(
                imageView,
                information
        );

        return card;
    }

    // BACK TO DASHBOARD
    @FXML
    private void handleBack(ActionEvent event) {

        try {

            FXMLLoader loader = new FXMLLoader(
                    getClass().getResource("/view/dashboard.fxml")
            );

            Parent root = loader.load();

            Stage stage = (Stage) ((Button) event.getSource())
                    .getScene()
                    .getWindow();

            stage.setScene(new Scene(root));
            stage.setMaximized(true);

        } catch (IOException | NullPointerException e) {

            e.printStackTrace();

            Alert alert = new Alert(Alert.AlertType.ERROR);

            alert.setTitle("Navigation Error");
            alert.setHeaderText("Unable to return to Dashboard");
            alert.setContentText(e.getMessage());

            alert.showAndWait();
        }
    }
}
