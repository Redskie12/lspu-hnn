
package system.com.lostfound;

import javafx.animation.PauseTransition;
import javafx.animation.FadeTransition;
import javafx.application.Platform;
import javafx.concurrent.Worker;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Label;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.StackPane;
import javafx.scene.web.WebEngine;
import javafx.scene.web.WebView;
import javafx.stage.Stage;
import javafx.util.Duration;

import netscape.javascript.JSObject;

import java.net.URL;

public class CampusController {

    @FXML private WebView mapView;
    @FXML private ImageView campusImage;
    @FXML private StackPane campusImageContainer;

    @FXML private Label campusNameLabel;
    @FXML private Label locationLabel;
    @FXML private Label informationLabel;
    @FXML private Label programsLabel;
    @FXML private Label mapStatusLabel;

    private WebEngine mapEngine;
    private boolean mapReady = false;

    private final MapBridge mapBridge = new MapBridge();

    private static final double IMAGE_PADDING = 20;

    // Campus coordinates
    private static final double SINILOAN_LAT = 14.413847;
    private static final double SINILOAN_LNG = 121.448183;

    private static final double SANTA_CRUZ_LAT = 14.26267;
    private static final double SANTA_CRUZ_LNG = 121.39752;

    private static final double LOS_BANOS_LAT = 14.187296;
    private static final double LOS_BANOS_LNG = 121.231148;

    private static final double SAN_PABLO_LAT = 14.0828;
    private static final double SAN_PABLO_LNG = 121.3125;

    @FXML
    public void initialize() {

        // ==========================================
        // RESPONSIVE CAMPUS IMAGE
        // ==========================================

        if (campusImage != null && campusImageContainer != null) {

            campusImage.setPreserveRatio(true);
            campusImage.setSmooth(true);

            campusImage.setFitWidth(0);
            campusImage.setFitHeight(0);

            campusImage.fitWidthProperty().bind(
                    campusImageContainer.widthProperty()
                            .subtract(IMAGE_PADDING)
            );

            campusImage.fitHeightProperty().bind(
                    campusImageContainer.heightProperty()
                            .subtract(IMAGE_PADDING)
            );
        }

        // ==========================================
        // MAP INITIALIZATION
        // ==========================================

        mapEngine = mapView.getEngine();

        mapView.widthProperty().addListener(
                (obs, oldValue, newValue) -> refreshMapSize()
        );

        mapView.heightProperty().addListener(
                (obs, oldValue, newValue) -> refreshMapSize()
        );

        showCampusDetails("Siniloan");
        loadInteractiveMap();
    }

    // ==========================================
    // LOAD INTERACTIVE OPENSTREETMAP
    // ==========================================

    private void loadInteractiveMap() {

        String html = """
                <!DOCTYPE html>
                <html>
                <head>
                    <meta charset="UTF-8">
                    <meta name="viewport"
                          content="width=device-width, initial-scale=1.0">

                    <link rel="stylesheet"
                          href="https://unpkg.com/leaflet@1.8.0/dist/leaflet.css"/>

                    <script src="https://unpkg.com/leaflet@1.8.0/dist/leaflet.js"></script>

                    <style>
                        html, body {
                            width: 100%;
                            height: 100%;
                            margin: 0;
                            padding: 0;
                            overflow: hidden;
                            font-family: Arial, sans-serif;
                        }

                        #map {
                            position: absolute;
                            inset: 0;
                            width: 100%;
                            height: 100%;
                            background: #E2E8F0;
                            animation: mapFadeIn 700ms ease-out;
                        }

                        @keyframes mapFadeIn {
                            from { opacity: 0.35; }
                            to   { opacity: 1; }
                        }

                        .campus-marker {
                            width: 18px;
                            height: 18px;
                            border-radius: 50%;
                            background: #01796F;
                            border: 3px solid #ffffff;
                            box-shadow: 0 2px 8px rgba(0,0,0,0.35);
                            position: relative;
                        }

                        .campus-marker::after {
                            content: "";
                            position: absolute;
                            left: -7px;
                            top: -7px;
                            width: 26px;
                            height: 26px;
                            border: 2px solid rgba(1,121,111,0.55);
                            border-radius: 50%;
                            animation: markerPulse 1.8s ease-out infinite;
                        }

                        .campus-name-label {
                            background: #ffffff !important;
                            border: 1px solid #01796F !important;
                            border-radius: 6px !important;
                            color: #134E4A !important;
                            font-size: 12px !important;
                            font-weight: bold !important;
                            padding: 4px 7px !important;
                            box-shadow: 0 2px 7px rgba(0,0,0,0.22) !important;
                            white-space: nowrap !important;
                        }

                        .campus-name-label::before {
                            display: none !important;
                        }

                        @keyframes markerPulse {
                            0%   { transform: scale(0.55); opacity: 0.9; }
                            100% { transform: scale(1.55); opacity: 0; }
                        }

                        .route-animated {
                            stroke-dasharray: 12 9;
                            animation: routeFlow 2.2s linear infinite;
                        }

                        @keyframes routeFlow {
                            to { stroke-dashoffset: -42; }
                        }

                        .campus-popup {
                            font-family: Arial, sans-serif;
                            text-align: center;
                        }

                        .campus-popup b {
                            color: #0F766E;
                            font-size: 14px;
                        }

                        .leaflet-popup-content-wrapper {
                            border-radius: 10px;
                        }
                    </style>
                </head>

                <body>
                    <div id="map"></div>

                    <script>
                        if (typeof L === 'undefined') {
                            document.getElementById('map').innerHTML =
                                '<p style="padding:20px;color:#475569;">' +
                                'Unable to load Leaflet. Check your internet connection.' +
                                '</p>';
                            throw new Error('Leaflet library unavailable');
                        }

                        L.Browser.any3d = false;

                        const map = L.map('map', {
                            zoomControl: true,
                            scrollWheelZoom: true,
                            doubleClickZoom: true,
                            dragging: true,
                            zoomAnimation: false,
                            fadeAnimation: false,
                            markerZoomAnimation: false,
                            preferCanvas: false
                        });

                        let usingFallbackTiles = false;

                        let tileLayer = L.tileLayer(
                            'https://{s}.tile.openstreetmap.org/{z}/{x}/{y}.png',
                            {
                                maxZoom: 19,
                                attribution: '&copy; OpenStreetMap contributors',
                                updateWhenIdle: true,
                                keepBuffer: 3
                            }
                        ).addTo(map);

                        tileLayer.on('tileerror', function() {
                            if (usingFallbackTiles) {
                                return;
                            }

                            usingFallbackTiles = true;
                            map.removeLayer(tileLayer);

                            tileLayer = L.tileLayer(
                                'https://{s}.basemaps.cartocdn.com/light_all/{z}/{x}/{y}{r}.png',
                                {
                                    maxZoom: 19,
                                    attribution: '&copy; OpenStreetMap contributors &copy; CARTO',
                                    updateWhenIdle: true,
                                    keepBuffer: 3
                                }
                            ).addTo(map);

                            if (window.javaBridge) {
                                window.javaBridge.updateMapStatus(
                                    'Using alternate map tiles because OpenStreetMap tiles did not load.'
                                );
                            }
                        });

                        const campuses = [
                            {
                                name: 'Siniloan',
                                lat: __SINILOAN_LAT__,
                                lng: __SINILOAN_LNG__
                            },
                            {
                                name: 'Santa Cruz',
                                lat: __SANTA_CRUZ_LAT__,
                                lng: __SANTA_CRUZ_LNG__
                            },
                            {
                                name: 'Los Baños',
                                lat: __LOS_BANOS_LAT__,
                                lng: __LOS_BANOS_LNG__
                            },
                            {
                                name: 'San Pablo',
                                lat: __SAN_PABLO_LAT__,
                                lng: __SAN_PABLO_LNG__
                            }
                        ];

                        const markers = {};

                        campuses.forEach(function(campus) {

                            const campusIcon = L.divIcon({
                                className: '',
                                html: '<div class="campus-marker"></div>',
                                iconSize: [24, 24],
                                iconAnchor: [12, 12]
                            });

                            const marker = L.marker(
                                [campus.lat, campus.lng],
                                { icon: campusIcon }
                            ).addTo(map);

                            marker.bindPopup(
                                '<div class="campus-popup">' +
                                '<b>LSPU ' + campus.name + ' Campus</b><br>' +
                                'Click to view campus details' +
                                '</div>'
                            );

                            marker.bindTooltip(
                                campus.name + ' Campus',
                                {
                                    permanent: true,
                                    direction: 'top',
                                    offset: [0, -13],
                                    opacity: 1,
                                    className: 'campus-name-label'
                                }
                            );

                            marker.on('click', function() {

                                map.flyTo(
                                    [campus.lat, campus.lng],
                                    16,
                                    {
                                        animate: true,
                                        duration: 1.2
                                    }
                                );

                                if (window.javaBridge) {
                                    window.javaBridge.selectCampusFromMap(
                                        campus.name
                                    );
                                }
                            });

                            markers[campus.name] = marker;
                        });

                        const connectionPoints = campuses.map(
                            function(campus) {
                                return [campus.lat, campus.lng];
                            }
                        );

                        const routeCoordinates = campuses.map(function(campus) {
                            return campus.lng + ',' + campus.lat;
                        }).join(';');

                        const routeUrl =
                            'https://router.project-osrm.org/route/v1/driving/' +
                            routeCoordinates +
                            '?overview=full&geometries=geojson&steps=false';

                        fetch(routeUrl)
                            .then(function(response) {
                                if (!response.ok) {
                                    throw new Error('Routing service unavailable');
                                }
                                return response.json();
                            })
                            .then(function(data) {
                                if (!data.routes || data.routes.length === 0) {
                                    throw new Error('No road route returned');
                                }

                                const roadRoute = L.geoJSON(
                                    data.routes[0].geometry,
                                    {
                                        style: {
                                            color: '#01796F',
                                            weight: 5,
                                            opacity: 0.9,
                                            className: 'route-animated'
                                        }
                                    }
                                ).addTo(map);

                                roadRoute.bindPopup(
                                    '<b>LSPU Campus Road Route</b><br>' +
                                    'Siniloan → Santa Cruz → Los Baños → San Pablo'
                                );

                                if (window.javaBridge) {
                                    window.javaBridge.updateMapStatus(
                                        'Map loaded. Real driving route displayed through all four campuses.'
                                    );
                                }
                            })
                            .catch(function(error) {
                                console.log('OSRM route error:', error);

                                if (window.javaBridge) {
                                    window.javaBridge.updateMapStatus(
                                        'Campus markers loaded, but the road route is unavailable. Check your internet connection.'
                                    );
                                }
                            });

                        const campusBounds = L.latLngBounds(
                            connectionPoints
                        );

                        map.fitBounds(
                            campusBounds,
                            {
                                padding: [35, 35],
                                maxZoom: 11
                            }
                        );

                        window.moveToCampus = function(name) {

                            const campus = campuses.find(
                                function(item) {
                                    return item.name === name;
                                }
                            );

                            if (!campus) {
                                return;
                            }

                            map.flyTo(
                                [campus.lat, campus.lng],
                                16,
                                {
                                    animate: true,
                                    duration: 1.25,
                                    easeLinearity: 0.25
                                }
                            );

                            if (markers[name]) {
                                markers[name].openPopup();
                            }
                        };

                        window.refreshMapSize = function() {
                            requestAnimationFrame(function() {
                                map.invalidateSize({
                                    animate: false,
                                    pan: false
                                });
                            });
                        };

                        window.mapReady = true;

                        setTimeout(function() {
                            map.invalidateSize(false);
                            tileLayer.redraw();
                        }, 500);

                        setTimeout(function() {
                            map.invalidateSize(false);
                            tileLayer.redraw();
                        }, 1500);
                    </script>
                </body>
                </html>
                """;

        html = html
                .replace("__SINILOAN_LAT__", String.valueOf(SINILOAN_LAT))
                .replace("__SINILOAN_LNG__", String.valueOf(SINILOAN_LNG))
                .replace("__SANTA_CRUZ_LAT__", String.valueOf(SANTA_CRUZ_LAT))
                .replace("__SANTA_CRUZ_LNG__", String.valueOf(SANTA_CRUZ_LNG))
                .replace("__LOS_BANOS_LAT__", String.valueOf(LOS_BANOS_LAT))
                .replace("__LOS_BANOS_LNG__", String.valueOf(LOS_BANOS_LNG))
                .replace("__SAN_PABLO_LAT__", String.valueOf(SAN_PABLO_LAT))
                .replace("__SAN_PABLO_LNG__", String.valueOf(SAN_PABLO_LNG));

        mapEngine.getLoadWorker().stateProperty().addListener(
                (observable, oldState, newState) -> {

                    if (newState == Worker.State.SUCCEEDED) {
                        try {
                            Object result = mapEngine.executeScript(
                                    "typeof L !== 'undefined' && window.mapReady === true"
                            );

                            if (!Boolean.TRUE.equals(result)) {
                                mapReady = false;
                                mapStatusLabel.setText(
                                        "Map library did not load. Check your internet connection."
                                );
                                return;
                            }

                            JSObject window = (JSObject)
                                    mapEngine.executeScript("window");

                            window.setMember("javaBridge", mapBridge);

                            mapReady = true;

                            mapView.setOpacity(0);

                            FadeTransition mapFade = new FadeTransition(
                                    Duration.millis(650), mapView);

                            mapFade.setFromValue(0);
                            mapFade.setToValue(1);
                            mapFade.play();

                            mapStatusLabel.setText(
                                    "Interactive campus map loaded. Select a marker or campus button."
                            );

                            refreshMapSize();

                        } catch (Exception e) {
                            mapReady = false;

                            mapStatusLabel.setText(
                                    "Unable to initialize the campus map."
                            );

                            e.printStackTrace();
                        }
                    }

                    if (newState == Worker.State.FAILED) {
                        mapReady = false;

                        mapStatusLabel.setText(
                                "Map failed to load. Check your internet connection."
                        );
                    }
                }
        );

        mapEngine.loadContent(html);
    }

    // ==========================================
    // REFRESH MAP SIZE
    // ==========================================

    private void refreshMapSize() {

        if (!mapReady || mapEngine == null) {
            return;
        }

        Platform.runLater(() -> {

            PauseTransition delay =
                    new PauseTransition(Duration.millis(300));

            delay.setOnFinished(event -> {

                if (!mapReady) {
                    return;
                }

                try {
                    mapEngine.executeScript(
                            "if (window.refreshMapSize) {" +
                            "window.refreshMapSize();" +
                            "}"
                    );
                } catch (Exception e) {
                    System.out.println("Map refresh pending.");
                }
            });

            delay.play();
        });
    }

    // ==========================================
    // JAVA BRIDGE
    // ==========================================

    public class MapBridge {

        public void selectCampusFromMap(String campus) {
            Platform.runLater(() ->
                    showCampusDetails(campus)
            );
        }

        public void updateMapStatus(String message) {
            Platform.runLater(() ->
                    mapStatusLabel.setText(message)
            );
        }
    }

    // ==========================================
    // CAMPUS DETAILS
    // ==========================================

    private void showCampusDetails(String campus) {

        String name;
        String location;
        String information;
        String programs;
        String imagePath;

        switch (campus) {

            case "Siniloan":
                name = "Siniloan Host Campus";
                location = "L. de Leon Street, Siniloan, Laguna";

                information =
                        "The Siniloan Host Campus is the birthplace of "
                        + "Laguna State Polytechnic University. Its history "
                        + "dates back to 1952, when it was established as "
                        + "Baybay Provincial High School. In 1957, it was "
                        + "converted into Baybay Agricultural and Vocational "
                        + "School and later became Baybay National College "
                        + "of Agriculture and Technology in 1971. On June 10, "
                        + "1983, it became Laguna State Polytechnic College.";

                programs =
                        "Agriculture, Education, Engineering, Information "
                        + "Technology, and other academic programs.";

                imagePath = "/images/campuses/siniloan.jpeg";
                break;

            case "Santa Cruz":
                name = "Santa Cruz Campus";
                location = "Barangay Bubukal, Santa Cruz, Laguna";

                information =
                        "The Santa Cruz Campus originated from the Laguna "
                        + "College of Arts and Trades. In January 1999, it "
                        + "was integrated into Laguna State Polytechnic "
                        + "College. It serves as the university's main "
                        + "campus and contributes to technical and "
                        + "professional education in Laguna.";

                programs =
                        "Engineering, Industrial Technology, and other "
                        + "academic programs.";

                imagePath = "/images/campuses/sta-cruz.jpeg";
                break;

            case "Los Baños":
                name = "Los Baños Campus";
                location = "Mayondon, Los Baños, Laguna";

                information =
                        "The Los Baños Campus traces its origins to the "
                        + "Los Baños College of Fisheries. In 1999, it was "
                        + "integrated into Laguna State Polytechnic College "
                        + "through the government's higher education "
                        + "integration program. It continues to contribute "
                        + "to fisheries education and related disciplines.";

                programs =
                        "Fisheries and related academic disciplines.";

                imagePath = "/images/campuses/los-banos.jpeg";
                break;

            case "San Pablo":
                name = "San Pablo City Campus";
                location = "Barangay Del Remedio, San Pablo City, Laguna";

                information =
                        "The San Pablo City Campus originated from the "
                        + "San Pablo City National School of Arts and Trades. "
                        + "In 2000, it was integrated into Laguna State "
                        + "Polytechnic College. It expanded the institution's "
                        + "educational reach in teacher education, "
                        + "engineering, and industrial technology.";

                programs =
                        "Teacher Education, Engineering, Industrial "
                        + "Technology, and other academic programs.";

                imagePath = "/images/campuses/san-pablo.jpeg";
                break;

            default:
                return;
        }

        campusNameLabel.setText(name);
        locationLabel.setText(location);
        informationLabel.setText(information);
        programsLabel.setText(programs);

        loadCampusImage(imagePath);
        fadeInCampusDetails();
    }

    // ==========================================
    // CAMPUS DETAILS FADE-IN
    // ==========================================

    private void fadeInCampusDetails() {

        fadeIn(campusNameLabel);
        fadeIn(locationLabel);
        fadeIn(informationLabel);
        fadeIn(programsLabel);
        fadeIn(campusImage);
    }

    private void fadeIn(Node node) {

        if (node == null) {
            return;
        }

        FadeTransition fade = new FadeTransition(
                Duration.millis(400), node);

        fade.setFromValue(0.25);
        fade.setToValue(1.0);
        fade.play();
    }

    // ==========================================
    // LOAD CAMPUS IMAGE
    // ==========================================

    private void loadCampusImage(String imagePath) {

        try {
            URL imageURL = getClass().getResource(imagePath);

            if (imageURL != null) {

                Image image = new Image(
                        imageURL.toExternalForm(),
                        true
                );

                campusImage.setImage(image);

            } else {
                campusImage.setImage(null);

                System.out.println(
                        "Campus image not found: " + imagePath
                );
            }

        } catch (Exception e) {

            campusImage.setImage(null);

            System.out.println(
                    "Unable to load campus image: " + imagePath
            );

            e.printStackTrace();
        }
    }

    // ==========================================
    // MOVE MAP TO SELECTED CAMPUS
    // ==========================================

    private void moveMapToCampus(String campus) {

        if (!mapReady || mapEngine == null) {
            return;
        }

        String safeCampus = campus
                .replace("\\", "\\\\")
                .replace("'", "\\'");

        try {
            mapEngine.executeScript(
                    "window.moveToCampus('" + safeCampus + "');"
            );

        } catch (Exception e) {

            System.out.println(
                    "Unable to move map to " + campus
            );

            e.printStackTrace();
        }
    }

    // ==========================================
    // CAMPUS BUTTONS
    // ==========================================

    @FXML
    private void selectSiniloan() {
        showCampusDetails("Siniloan");
        moveMapToCampus("Siniloan");
    }

    @FXML
    private void selectSantaCruz() {
        showCampusDetails("Santa Cruz");
        moveMapToCampus("Santa Cruz");
    }

    @FXML
    private void selectLosBanos() {
        showCampusDetails("Los Baños");
        moveMapToCampus("Los Baños");
    }

    @FXML
    private void selectSanPablo() {
        showCampusDetails("San Pablo");
        moveMapToCampus("San Pablo");
    }

    // ==========================================
    // BACK TO DASHBOARD
    // ==========================================

    @FXML
    private void handleBack(ActionEvent event) {

        try {
            FXMLLoader loader = new FXMLLoader(
                    getClass().getResource("/view/dashboard.fxml")
            );

            Parent root = loader.load();

            Stage stage = (Stage) ((Node) event.getSource())
                    .getScene()
                    .getWindow();

            stage.setScene(new Scene(root));
            stage.setTitle("LSPU-HNN Dashboard");
            stage.setMaximized(true);
            stage.show();

        } catch (Exception e) {

            System.out.println("Error returning to dashboard:");
            e.printStackTrace();
        }
    }
}
