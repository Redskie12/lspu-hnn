
package system.com.lostfound;

import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.animation.PauseTransition;
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

import java.io.IOException;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

public class TraversalController {

    // ==========================================
    // FXML COMPONENTS
    // ==========================================

    @FXML private WebView mapView;
    @FXML private Label mapStatusLabel;

    @FXML private Label resultLabel;
    @FXML private Label statusLabel;

    @FXML private ImageView campusImage;
    @FXML private StackPane campusImageContainer;

    @FXML private Label currentCampusLabel;
    @FXML private Label campusLocationLabel;
    @FXML private Label campusDescriptionLabel;
    @FXML private Label campusProgramsLabel;

    @FXML private Label distanceLabel;
    @FXML private Label totalDistanceLabel;

    // ==========================================
    // TRAVERSAL VARIABLES
    // ==========================================

    private final CampusGraph campusGraph = new CampusGraph();

    private Timeline animation;
    private WebEngine mapEngine;
    private PauseTransition mapResizePause;

    private int currentStep = 0;
    private int totalDistance = 0;

    private String previousCampus = null;
    private String pendingCampus = "Siniloan";

    private List<String> traversalOrder = new ArrayList<>();

    private final List<String> visitedCampuses =
            new ArrayList<>();

    private boolean mapReady = false;

    private final MapBridge mapBridge = new MapBridge();

    // ==========================================
    // CAMPUS COORDINATES
    // ==========================================

    private static final double SINILOAN_LAT = 14.413847;
    private static final double SINILOAN_LNG = 121.448183;

    private static final double SANTA_CRUZ_LAT = 14.262670;
    private static final double SANTA_CRUZ_LNG = 121.397520;

    private static final double LOS_BANOS_LAT = 14.187296;
    private static final double LOS_BANOS_LNG = 121.231148;

    private static final double SAN_PABLO_LAT = 14.082800;
    private static final double SAN_PABLO_LNG = 121.312500;

    // ==========================================
    // INITIALIZE
    // ==========================================

    @FXML
    public void initialize() {

        // RESPONSIVE CAMPUS IMAGE

        campusImage.setPreserveRatio(true);
        campusImage.setSmooth(true);

        if (campusImageContainer != null) {

            campusImage.fitWidthProperty().bind(
                    campusImageContainer.widthProperty().subtract(20)
            );
        }

        showCampusDetails("Siniloan");

        statusLabel.setText(
                "Ready to start traversal."
        );

        resultLabel.setText(
                "Traversal order will appear here."
        );

        distanceLabel.setText(
                "Distance from previous campus: 0 km"
        );

        totalDistanceLabel.setText(
                "Total distance: 0 km"
        );

        setupMapResizeListener();

        loadInteractiveMap();
    }

    // ==========================================
    // MAP RESIZE LISTENER
    // ==========================================

    private void setupMapResizeListener() {

        mapResizePause = new PauseTransition(
                Duration.millis(300)
        );

        mapResizePause.setOnFinished(event -> {

            if (mapReady && mapEngine != null) {

                try {

                    mapEngine.executeScript(
                            "refreshMapSize();"
                    );

                } catch (Exception e) {

                    System.out.println(
                            "Unable to refresh map size."
                    );
                }
            }
        });

        mapView.widthProperty().addListener(
                (observable, oldValue, newValue) -> {

                    if (mapResizePause != null) {
                        mapResizePause.playFromStart();
                    }
                }
        );

        mapView.heightProperty().addListener(
                (observable, oldValue, newValue) -> {

                    if (mapResizePause != null) {
                        mapResizePause.playFromStart();
                    }
                }
        );
    }

    // ==========================================
    // LOAD INTERACTIVE MAP
    // ==========================================

    private void loadInteractiveMap() {

        mapEngine = mapView.getEngine();

        mapEngine.setJavaScriptEnabled(true);

        mapStatusLabel.setText(
                "Loading interactive campus map..."
        );

        mapEngine.getLoadWorker().stateProperty().addListener(
                (observable, oldState, newState) -> {

                    if (newState == Worker.State.SUCCEEDED) {

                        try {

                            JSObject window = (JSObject)
                                    mapEngine.executeScript("window");

                            window.setMember(
                                    "javaBridge",
                                    mapBridge
                            );

                            Object ready = mapEngine.executeScript(
                                    "typeof L !== 'undefined' && " +
                                    "typeof campusMarkers !== 'undefined' && " +
                                    "typeof setTraversalState === 'function' && " +
                                    "typeof addTraversalSegment === 'function'"
                            );

                            mapReady = Boolean.TRUE.equals(ready);

                            if (mapReady) {

                                mapStatusLabel.setText(
                                        "Interactive campus map ready."
                                );

                                updateMapTraversalState();

                                moveMapToCampus(
                                        pendingCampus,
                                        false
                                );

                                mapEngine.executeScript(
                                        "refreshMapSize();"
                                );

                            } else {

                                mapStatusLabel.setText(
                                        "Map is loading. Check your internet connection."
                                );
                            }

                        } catch (Exception e) {

                            mapStatusLabel.setText(
                                    "Unable to initialize map."
                            );

                            e.printStackTrace();
                        }
                    }

                    if (newState == Worker.State.FAILED) {

                        mapReady = false;

                        mapStatusLabel.setText(
                                "Unable to load map. Check your internet connection."
                        );
                    }
                }
        );

        mapEngine.loadContent(createMapHTML());
    }

    // ==========================================
    // MAP HTML
    // ==========================================

    private String createMapHTML() {

        String html = """
                <!DOCTYPE html>
                <html>

                <head>

                    <meta charset="UTF-8">

                    <meta name="viewport"
                          content="width=device-width, initial-scale=1.0">

                    <title>LSPU Campus Traversal</title>

                    <link rel="stylesheet"
                          href="https://unpkg.com/leaflet@1.8.0/dist/leaflet.css"/>

                    <script src="https://unpkg.com/leaflet@1.8.0/dist/leaflet.js"></script>

                    <style>

                        html, body {
                            height: 100%;
                            width: 100%;
                            margin: 0;
                            padding: 0;
                            overflow: hidden;
                            font-family: Arial, sans-serif;
                        }

                        #map {
                            height: 100%;
                            width: 100%;
                            background: #E2E8F0;
                        }

                        .campus-marker {
                            width: 22px;
                            height: 22px;
                            border-radius: 50%;
                            border: 3px solid white;
                            background: #94A3B8;
                            box-shadow: 0 2px 8px rgba(0,0,0,0.4);
                            transition: all 0.25s ease;
                        }

                        .campus-marker.current {
                            background: #0F766E;
                            width: 28px;
                            height: 28px;
                            border: 4px solid #CCFBF1;
                            box-shadow: 0 0 0 6px rgba(15,118,110,0.25);
                        }

                        .campus-marker.visited {
                            background: #5EEAD4;
                            border: 3px solid #0F766E;
                        }

                        .campus-name-label {
                            background: rgba(255,255,255,0.96);
                            border: 1px solid #0F766E;
                            border-radius: 6px;
                            color: #134E4A;
                            font-weight: bold;
                            font-size: 12px;
                            padding: 4px 7px;
                            box-shadow: 0 2px 6px rgba(0,0,0,0.2);
                            white-space: nowrap;
                        }

                        .leaflet-tooltip.campus-name-label:before {
                            display: none;
                        }

                        .leaflet-popup-content {
                            font-size: 13px;
                            font-weight: bold;
                        }

                        .distance-label {
                            background: white;
                            border: 1px solid #0F766E;
                            border-radius: 5px;
                            padding: 3px 6px;
                            color: #0F766E;
                            font-weight: bold;
                            font-size: 11px;
                            box-shadow: 0 1px 4px rgba(0,0,0,0.2);
                        }

                    </style>

                </head>

                <body>

                    <div id="map"></div>

                    <script>

                        L.Browser.any3d = false;

                        const campuses = {

                            "Siniloan": {
                                lat: __SINILOAN_LAT__,
                                lng: __SINILOAN_LNG__
                            },

                            "Santa Cruz": {
                                lat: __SANTA_CRUZ_LAT__,
                                lng: __SANTA_CRUZ_LNG__
                            },

                            "Los Baños": {
                                lat: __LOS_BANOS_LAT__,
                                lng: __LOS_BANOS_LNG__
                            },

                            "San Pablo": {
                                lat: __SAN_PABLO_LAT__,
                                lng: __SAN_PABLO_LNG__
                            }

                        };

                        const map = L.map("map", {

                            zoomControl: true,
                            scrollWheelZoom: true,
                            doubleClickZoom: true,
                            dragging: true,

                            zoomAnimation: false,
                            fadeAnimation: false,
                            markerZoomAnimation: false,

                            preferCanvas: true

                        }).setView(
                            [14.25, 121.35],
                            10
                        );

                        let usingFallbackTiles = false;

                        let tileLayer = L.tileLayer(
                            "https://{s}.tile.openstreetmap.org/{z}/{x}/{y}.png",
                            {
                                maxZoom: 19,
                                attribution: "&copy; OpenStreetMap contributors",
                                updateWhenIdle: true,
                                keepBuffer: 3
                            }
                        ).addTo(map);

                        tileLayer.on("tileerror", function() {

                            if (usingFallbackTiles) return;

                            usingFallbackTiles = true;

                            map.removeLayer(tileLayer);

                            tileLayer = L.tileLayer(
                                "https://{s}.basemaps.cartocdn.com/light_all/{z}/{x}/{y}{r}.png",
                                {
                                    maxZoom: 19,
                                    attribution:
                                        "&copy; OpenStreetMap contributors &copy; CARTO",
                                    updateWhenIdle: true,
                                    keepBuffer: 3
                                }
                            ).addTo(map);

                            if (window.javaBridge) {

                                window.javaBridge.updateMapStatus(
                                    "Using backup map tiles."
                                );

                            }

                        });

                        const campusMarkers = {};

                        const campusLines = [];

                        let traversalPathPoints = [];

                        const traversalPath = L.polyline(
                            [],
                            {
                                color: "#F59E0B",
                                weight: 5,
                                opacity: 0.95,
                                lineCap: "round",
                                lineJoin: "round"
                            }
                        ).addTo(map);

                        function createIcon(type) {

                            return L.divIcon({

                                className: "",

                                html:
                                    '<div class="campus-marker ' +
                                    type +
                                    '"></div>',

                                iconSize: [36, 36],

                                iconAnchor: [18, 18]

                            });
                        }

                        Object.keys(campuses).forEach(function(name) {

                            const campus = campuses[name];

                            const marker = L.marker(
                                [campus.lat, campus.lng],
                                {
                                    icon: createIcon("")
                                }
                            ).addTo(map);

                            marker.bindPopup(
                                "<b>LSPU " + name + " Campus</b>"
                            );

                            marker.bindTooltip(
                                name + " Campus",
                                {
                                    permanent: true,
                                    direction: "right",
                                    offset: [12, 0],
                                    className: "campus-name-label"
                                }
                            );

                            marker.on("click", function() {

                                if (window.javaBridge) {

                                    window.javaBridge.selectCampusFromMap(
                                        name
                                    );

                                }

                            });

                            campusMarkers[name] = marker;

                        });

                        const connections = [

                            ["Siniloan", "Santa Cruz", 27],
                            ["Siniloan", "Los Baños", 75],
                            ["Siniloan", "San Pablo", 63],
                            ["Santa Cruz", "Los Baños", 26],
                            ["Santa Cruz", "San Pablo", 40],
                            ["Los Baños", "San Pablo", 24]

                        ];

                        connections.forEach(function(connection) {

                            const first = campuses[connection[0]];

                            const second = campuses[connection[1]];

                            const line = L.polyline(
                                [
                                    [first.lat, first.lng],
                                    [second.lat, second.lng]
                                ],
                                {
                                    color: "#0F766E",
                                    weight: 2,
                                    opacity: 0.65,
                                    dashArray: "6, 7"
                                }
                            ).addTo(map);

                            line.bindTooltip(
                                connection[0] + " - " +
                                connection[1] + ": " +
                                connection[2] + " km"
                            );

                            campusLines.push(line);

                        });

                        function setTraversalState(current, visited) {

                            Object.keys(campusMarkers).forEach(function(name) {

                                let markerType = "";

                                if (name === current) {

                                    markerType = "current";

                                } else if (visited.includes(name)) {

                                    markerType = "visited";

                                }

                                campusMarkers[name].setIcon(
                                    createIcon(markerType)
                                );

                            });

                        }

                        function addTraversalSegment(fromName, toName) {

                            const fromCampus = campuses[fromName];

                            const toCampus = campuses[toName];

                            if (!fromCampus || !toCampus) return;

                            if (traversalPathPoints.length === 0) {

                                traversalPathPoints.push([
                                    fromCampus.lat,
                                    fromCampus.lng
                                ]);

                            }

                            traversalPathPoints.push([
                                toCampus.lat,
                                toCampus.lng
                            ]);

                            traversalPath.setLatLngs(
                                traversalPathPoints
                            );

                        }

                        function flyToCampus(name) {

                            const campus = campuses[name];

                            if (!campus) return;

                            map.flyTo(
                                [campus.lat, campus.lng],
                                12,
                                {
                                    animate: true,
                                    duration: 1.2
                                }
                            );

                            campusMarkers[name].openPopup();

                        }

                        function setMapView(name) {

                            const campus = campuses[name];

                            if (!campus) return;

                            map.setView(
                                [campus.lat, campus.lng],
                                12
                            );

                            campusMarkers[name].openPopup();

                        }

                        function refreshMapSize() {

                            setTimeout(function() {

                                map.invalidateSize(false);

                                if (tileLayer) {
                                    tileLayer.redraw();
                                }

                            }, 150);

                        }

                        function resetCampusMap() {

                            Object.keys(campusMarkers).forEach(function(name) {

                                campusMarkers[name].setIcon(
                                    createIcon("")
                                );

                                campusMarkers[name].closePopup();

                            });

                            traversalPathPoints = [];

                            traversalPath.setLatLngs([]);

                            map.flyTo(
                                [14.25, 121.35],
                                10,
                                {
                                    animate: true,
                                    duration: 1
                                }
                            );

                            refreshMapSize();

                        }

                        setTimeout(function() {

                            map.invalidateSize(false);

                        }, 500);

                        setTimeout(function() {

                            map.invalidateSize(false);

                            if (tileLayer) {
                                tileLayer.redraw();
                            }

                        }, 1500);

                    </script>

                </body>

                </html>
                """;

        // ==========================================
        // REPLACE CAMPUS COORDINATES
        // ==========================================

        html = html.replace(
                "__SINILOAN_LAT__",
                String.valueOf(SINILOAN_LAT)
        );

        html = html.replace(
                "__SINILOAN_LNG__",
                String.valueOf(SINILOAN_LNG)
        );

        html = html.replace(
                "__SANTA_CRUZ_LAT__",
                String.valueOf(SANTA_CRUZ_LAT)
        );

        html = html.replace(
                "__SANTA_CRUZ_LNG__",
                String.valueOf(SANTA_CRUZ_LNG)
        );

        html = html.replace(
                "__LOS_BANOS_LAT__",
                String.valueOf(LOS_BANOS_LAT)
        );

        html = html.replace(
                "__LOS_BANOS_LNG__",
                String.valueOf(LOS_BANOS_LNG)
        );

        html = html.replace(
                "__SAN_PABLO_LAT__",
                String.valueOf(SAN_PABLO_LAT)
        );

        html = html.replace(
                "__SAN_PABLO_LNG__",
                String.valueOf(SAN_PABLO_LNG)
        );

        return html;
    }

    // ==========================================
    // JAVA BRIDGE
    // ==========================================

    public class MapBridge {

        public void selectCampusFromMap(String campus) {

            Platform.runLater(() -> {

                showCampusDetails(campus);

                pendingCampus = campus;

                updateMapTraversalState();

            });
        }

        public void updateMapStatus(String message) {

            Platform.runLater(() ->
                    mapStatusLabel.setText(message)
            );
        }
    }

    // ==========================================
    // UPDATE MAP TRAVERSAL STATE
    // ==========================================

    private void updateMapTraversalState() {

        if (!mapReady) return;

        String current = pendingCampus;

        String script =
                "setTraversalState('" +
                current.replace("'", "\\'") +
                "', " +
                toJavaScriptArray(visitedCampuses) +
                ");";

        mapEngine.executeScript(script);
    }

    // ==========================================
    // CONVERT JAVA LIST TO JAVASCRIPT ARRAY
    // ==========================================

    private String toJavaScriptArray(List<String> values) {

        StringBuilder result = new StringBuilder("[");

        for (int i = 0; i < values.size(); i++) {

            result.append("'")
                    .append(values.get(i).replace("'", "\\'"))
                    .append("'");

            if (i < values.size() - 1) {

                result.append(",");

            }
        }

        result.append("]");

        return result.toString();
    }

    // ==========================================
    // MOVE MAP TO CAMPUS
    // ==========================================

    private void moveMapToCampus(
            String campus,
            boolean animate
    ) {

        pendingCampus = campus;

        if (!mapReady) return;

        String safeCampus = campus.replace("'", "\\'");

        if (animate) {

            mapEngine.executeScript(
                    "flyToCampus('" + safeCampus + "');"
            );

        } else {

            mapEngine.executeScript(
                    "setMapView('" + safeCampus + "');"
            );

        }
    }

    // ==========================================
    // BFS BUTTON
    // ==========================================

    @FXML
    private void handleBFS(ActionEvent event) {

        startTraversal("BFS");
    }

    // ==========================================
    // DFS BUTTON
    // ==========================================

    @FXML
    private void handleDFS(ActionEvent event) {

        startTraversal("DFS");
    }

    // ==========================================
    // START TRAVERSAL
    // ==========================================

    private void startTraversal(String algorithm) {

        if (animation != null) {

            animation.stop();

        }

        currentStep = 0;

        totalDistance = 0;

        previousCampus = null;

        visitedCampuses.clear();

        distanceLabel.setText(
                "Distance from previous campus: 0 km"
        );

        totalDistanceLabel.setText(
                "Total distance: 0 km"
        );

        if (mapReady) {

            mapEngine.executeScript("resetCampusMap();");

        }

        if (algorithm.equals("BFS")) {

            traversalOrder = campusGraph.bfs("Siniloan");

            statusLabel.setText(
                    "Running Breadth-First Search..."
            );

        } else {

            traversalOrder = campusGraph.dfs("Siniloan");

            statusLabel.setText(
                    "Running Depth-First Search..."
            );

        }

        if (traversalOrder == null || traversalOrder.isEmpty()) {

            statusLabel.setText(
                    "No campuses found in the traversal."
            );

            resultLabel.setText(
                    "No traversal results."
            );

            return;
        }

        resultLabel.setText(
                "Starting from Siniloan..."
        );

        pendingCampus = "Siniloan";

        showCampusDetails("Siniloan");

        moveMapToCampus(
                "Siniloan",
                false
        );

        updateMapTraversalState();

        animation = new Timeline(
                new KeyFrame(
                        Duration.seconds(1.5),
                        e -> animateNextCampus()
                )
        );

        animation.setCycleCount(
                traversalOrder.size()
        );

        animation.play();
    }

    // ==========================================
    // TRAVERSAL ANIMATION
    // ==========================================

    private void animateNextCampus() {

        if (currentStep >= traversalOrder.size()) {

            return;

        }

        String campus = traversalOrder.get(currentStep);

        int distance = 0;

        if (previousCampus != null) {

            distance = campusGraph.getDistance(
                    previousCampus,
                    campus
            );

            if (distance >= 0) {

                totalDistance += distance;

            }

            if (mapReady) {

                String safePrevious =
                        previousCampus.replace("'", "\\'");

                String safeCurrent =
                        campus.replace("'", "\\'");

                mapEngine.executeScript(
                        "addTraversalSegment('" +
                        safePrevious +
                        "','" +
                        safeCurrent +
                        "');"
                );
            }
        }

        if (!visitedCampuses.contains(campus)) {

            visitedCampuses.add(campus);

        }

        pendingCampus = campus;

        showCampusDetails(campus);

        moveMapToCampus(
                campus,
                true
        );

        updateMapTraversalState();

        distanceLabel.setText(
                "Distance from previous campus: " +
                distance + " km"
        );

        totalDistanceLabel.setText(
                "Total distance traveled: " +
                totalDistance + " km"
        );

        StringBuilder result = new StringBuilder();

        for (int i = 0; i <= currentStep; i++) {

            result.append(
                    traversalOrder.get(i)
            );

            if (i < currentStep) {

                result.append(" → ");

            }
        }

        resultLabel.setText(
                result.toString()
        );

        statusLabel.setText(
                "Visiting " + campus +
                " (" + (currentStep + 1) +
                " of " + traversalOrder.size() + ")"
        );

        previousCampus = campus;

        currentStep++;

        if (currentStep == traversalOrder.size()) {

            statusLabel.setText(
                    "Traversal completed! Total distance: " +
                    totalDistance + " km"
            );

        }
    }

    // ==========================================
    // DISPLAY CAMPUS DETAILS
    // ==========================================

    private void showCampusDetails(String campus) {

        String location;
        String description;
        String programs;
        String imagePath;

        switch (campus) {

            case "Siniloan":

                location = "Siniloan, Laguna";

                description =
                        "Siniloan is the host campus and the birthplace " +
                        "of LSPU. It traces its origins to Baybay " +
                        "Provincial High School, established in 1952.";

                programs =
                        "Agriculture, Education, Engineering, " +
                        "Information Technology, and other programs.";

                imagePath = "/images/campuses/siniloan.jpeg";

                break;

            case "Santa Cruz":

                location = "Santa Cruz, Laguna";

                description =
                        "The Santa Cruz Campus originated from the " +
                        "Laguna College of Arts and Trades. It was " +
                        "integrated into the institution in 1999.";

                programs =
                        "Engineering, Industrial Technology, " +
                        "and other programs.";

                imagePath = "/images/campuses/sta-cruz.jpeg";

                break;

            case "San Pablo":

                location = "San Pablo City, Laguna";

                description =
                        "The San Pablo City Campus originated from " +
                        "San Pablo City National School of Arts and " +
                        "Trades. It was integrated in 2000.";

                programs =
                        "Teacher Education, Engineering, " +
                        "Industrial Technology, and other programs.";

                imagePath = "/images/campuses/san-pablo.jpeg";

                break;

            case "Los Baños":

                location = "Los Baños, Laguna";

                description =
                        "The Los Baños Campus traces its origins to " +
                        "the Los Baños College of Fisheries. It was " +
                        "integrated into the institution in 1999.";

                programs =
                        "Fisheries and other related academic programs.";

                imagePath = "/images/campuses/los-banos.jpeg";

                break;

            default:

                System.out.println(
                        "Unknown campus: " + campus
                );

                return;
        }

        currentCampusLabel.setText(campus);

        campusLocationLabel.setText(location);

        campusDescriptionLabel.setText(description);

        campusProgramsLabel.setText(programs);

        loadCurrentCampusImage(imagePath);
    }

    // ==========================================
    // LOAD CAMPUS IMAGE
    // ==========================================

    private void loadCurrentCampusImage(String imagePath) {

        try {

            URL imageURL = getClass().getResource(imagePath);

            if (imageURL == null) {

                campusImage.setImage(null);

                System.out.println(
                        "Current campus image not found: " +
                        imagePath
                );

                return;
            }

            Image image = new Image(
                    imageURL.toExternalForm()
            );

            campusImage.setImage(image);

            // KEEP THE ORIGINAL IMAGE PROPORTIONS
            campusImage.setPreserveRatio(true);

            campusImage.setSmooth(true);

        } catch (Exception e) {

            campusImage.setImage(null);

            System.out.println(
                    "Unable to load current campus image: " +
                    imagePath
            );

            e.printStackTrace();
        }
    }

    // ==========================================
    // RESET BUTTON
    // ==========================================

    @FXML
    private void handleReset(ActionEvent event) {

        if (animation != null) {

            animation.stop();

        }

        currentStep = 0;

        totalDistance = 0;

        previousCampus = null;

        visitedCampuses.clear();

        pendingCampus = "Siniloan";

        if (mapReady) {

            mapEngine.executeScript(
                    "resetCampusMap();"
            );

            updateMapTraversalState();

        }

        showCampusDetails("Siniloan");

        moveMapToCampus(
                "Siniloan",
                false
        );

        resultLabel.setText(
                "Traversal order will appear here."
        );

        statusLabel.setText(
                "Ready to start traversal."
        );

        distanceLabel.setText(
                "Distance from previous campus: 0 km"
        );

        totalDistanceLabel.setText(
                "Total distance: 0 km"
        );
    }

    // ==========================================
    // BACK TO DASHBOARD
    // ==========================================

    @FXML
    private void handleBack(ActionEvent event) {

        if (animation != null) {

            animation.stop();

        }

        if (mapResizePause != null) {

            mapResizePause.stop();

        }

        mapReady = false;

        if (mapEngine != null) {

            mapEngine.load("about:blank");

        }

        try {

            Parent root = FXMLLoader.load(
                    getClass().getResource(
                            "/view/dashboard.fxml"
                    )
            );

            Stage stage = (Stage) ((Node) event.getSource())
                    .getScene()
                    .getWindow();

            stage.setScene(
                    new Scene(root)
            );

            stage.setTitle(
                    "LSPU-HNN Dashboard"
            );

            stage.setMaximized(true);

            stage.show();

        } catch (IOException e) {

            e.printStackTrace();

        }
    }
}
