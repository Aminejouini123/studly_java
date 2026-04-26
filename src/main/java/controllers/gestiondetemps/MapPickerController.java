package controllers.gestiondetemps;

import javafx.application.Platform;
import javafx.concurrent.Worker;
import javafx.fxml.FXML;
import javafx.scene.web.WebEngine;
import javafx.scene.web.WebView;
import javafx.stage.Stage;
import netscape.javascript.JSObject;

public class MapPickerController {

    @FXML
    private WebView mapWebView;

    private LocationSelectionCallback callback;

    private static final String MAP_HTML = 
        "<!DOCTYPE html>" +
        "<html>" +
        "<head>" +
        "    <meta charset='utf-8'/>" +
        "    <meta name='viewport' content='initial-scale=1,maximum-scale=1,user-scalable=no'/>" +
        "    <title>Carte Interactive</title>" +
        "    <link rel='stylesheet' href='https://unpkg.com/leaflet@1.9.4/dist/leaflet.css'/>" +
        "    <style>" +
        "        * { margin: 0; padding: 0; box-sizing: border-box; }" +
        "        html, body { width: 100%; height: 100%; overflow: hidden; }" +
        "        #map { width: 100vw; height: 100vh; position: absolute; top: 0; left: 0; right: 0; bottom: 0; }" +
        "        .search-container {" +
        "            position: absolute; top: 15px; left: 15px; z-index: 1000;" +
        "            background: white; padding: 15px; border-radius: 10px;" +
        "            box-shadow: 0 2px 15px rgba(0,0,0,0.3); width: 380px;" +
        "        }" +
        "        .search-input {" +
        "            width: 100%; padding: 12px; border: 2px solid #e0e0e0;" +
        "            border-radius: 8px; font-size: 14px; outline: none;" +
        "        }" +
        "        .search-input:focus { border-color: #667eea; }" +
        "        .search-results {" +
        "            margin-top: 10px; max-height: 300px; overflow-y: auto;" +
        "            background: white; border-radius: 8px; display: none;" +
        "        }" +
        "        .search-item {" +
        "            padding: 12px; cursor: pointer; border-bottom: 1px solid #f0f0f0;" +
        "            font-size: 13px; transition: all 0.2s;" +
        "        }" +
        "        .search-item:hover { background: #f0f8ff; }" +
        "        .search-item:last-child { border-bottom: none; }" +
        "        .location-panel {" +
        "            position: absolute; bottom: 20px; left: 50%; transform: translateX(-50%);" +
        "            z-index: 1000; background: white; padding: 25px 35px;" +
        "            border-radius: 15px; box-shadow: 0 5px 25px rgba(0,0,0,0.3);" +
        "            display: none; text-align: center; min-width: 450px; max-width: 600px;" +
        "        }" +
        "        .location-panel h3 { " +
        "            margin-bottom: 10px; color: #333; font-size: 17px; font-weight: 600;" +
        "            word-wrap: break-word;" +
        "        }" +
        "        .location-panel p { " +
        "            margin-bottom: 18px; color: #666; font-size: 13px;" +
        "            font-family: monospace;" +
        "        }" +
        "        .confirm-btn {" +
        "            background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);" +
        "            color: white; border: none; padding: 14px 35px; border-radius: 10px;" +
        "            cursor: pointer; font-size: 15px; width: 100%; font-weight: 600;" +
        "            transition: all 0.3s; box-shadow: 0 4px 15px rgba(102, 126, 234, 0.4);" +
        "        }" +
        "        .confirm-btn:hover { " +
        "            transform: translateY(-2px); " +
        "            box-shadow: 0 6px 20px rgba(102, 126, 234, 0.6);" +
        "        }" +
        "        .loading { text-align: center; padding: 15px; color: #666; font-size: 13px; }" +
        "    </style>" +
        "</head>" +
        "<body>" +
        "    <div class='search-container'>" +
        "        <input type='text' class='search-input' id='searchInput' " +
        "               placeholder='Rechercher un lieu (Tunis, Carthage, etc.)...'/>" +
        "        <div class='search-results' id='searchResults'></div>" +
        "    </div>" +
        "    <div id='map'></div>" +
        "    <div class='location-panel' id='locationPanel'>" +
        "        <h3 id='locationName'>Lieu selectionne</h3>" +
        "        <p id='locationCoords'></p>" +
        "        <button class='confirm-btn' onclick='confirmLocation()'>Confirmer ce lieu</button>" +
        "    </div>" +
        "    " +
        "    <script src='https://unpkg.com/leaflet@1.9.4/dist/leaflet.js'></script>" +
        "    <script>" +
        "        var map, marker, selectedLat, selectedLng, selectedName;" +
        "        var mapInitialized = false;" +
        "        " +
        "        function initMap() {" +
        "            if (mapInitialized) return;" +
        "            " +
        "            try {" +
        "                map = L.map('map', {" +
        "                    center: [36.8065, 10.1815]," +
        "                    zoom: 13," +
        "                    zoomControl: true," +
        "                    attributionControl: true" +
        "                });" +
        "                " +
        "                L.tileLayer('https://{s}.tile.openstreetmap.org/{z}/{x}/{y}.png', {" +
        "                    attribution: '&copy; OpenStreetMap'," +
        "                    maxZoom: 19," +
        "                    tileSize: 256" +
        "                }).addTo(map);" +
        "                " +
        "                mapInitialized = true;" +
        "                " +
        "                setTimeout(function() { map.invalidateSize(); }, 200);" +
        "                setTimeout(function() { map.invalidateSize(); }, 500);" +
        "                setTimeout(function() { map.invalidateSize(); }, 1000);" +
        "                " +
        "                map.on('click', function(e) {" +
        "                    selectedLat = e.latlng.lat;" +
        "                    selectedLng = e.latlng.lng;" +
        "                    " +
        "                    if (marker) { map.removeLayer(marker); }" +
        "                    marker = L.marker([selectedLat, selectedLng]).addTo(map);" +
        "                    " +
        "                    reverseGeocode(selectedLat, selectedLng);" +
        "                });" +
        "            } catch(e) {" +
        "                console.error('Map init error:', e);" +
        "            }" +
        "        }" +
        "        " +
        "        function reverseGeocode(lat, lng) {" +
        "            var url = 'https://nominatim.openstreetmap.org/reverse?format=json&lat=' + lat + '&lon=' + lng;" +
        "            " +
        "            fetch(url, { headers: { 'User-Agent': 'StudyApp/1.0' } })" +
        "            .then(function(response) { return response.json(); })" +
        "            .then(function(data) {" +
        "                selectedName = data.display_name || (lat.toFixed(4) + ', ' + lng.toFixed(4));" +
        "                showLocationPanel(selectedName, lat, lng);" +
        "            })" +
        "            .catch(function(error) {" +
        "                selectedName = lat.toFixed(4) + ', ' + lng.toFixed(4);" +
        "                showLocationPanel(selectedName, lat, lng);" +
        "            });" +
        "        }" +
        "        " +
        "        function showLocationPanel(name, lat, lng) {" +
        "            document.getElementById('locationName').textContent = name;" +
        "            document.getElementById('locationCoords').textContent = " +
        "                'Latitude: ' + lat.toFixed(6) + ' | Longitude: ' + lng.toFixed(6);" +
        "            document.getElementById('locationPanel').style.display = 'block';" +
        "        }" +
        "        " +
        "        var searchTimeout;" +
        "        document.getElementById('searchInput').addEventListener('input', function(e) {" +
        "            clearTimeout(searchTimeout);" +
        "            var query = e.target.value.trim();" +
        "            " +
        "            if (query.length < 3) {" +
        "                document.getElementById('searchResults').style.display = 'none';" +
        "                return;" +
        "            }" +
        "            " +
        "            searchTimeout = setTimeout(function() { geocodeSearch(query); }, 700);" +
        "        });" +
        "        " +
        "        function geocodeSearch(query) {" +
        "            var resultsDiv = document.getElementById('searchResults');" +
        "            resultsDiv.innerHTML = '<div class=\"loading\">Recherche en cours...</div>';" +
        "            resultsDiv.style.display = 'block';" +
        "            " +
        "            var url = 'https://nominatim.openstreetmap.org/search?format=json&q=' + " +
        "                      encodeURIComponent(query) + '&limit=5&addressdetails=1';" +
        "            " +
        "            fetch(url, { headers: { 'User-Agent': 'StudyApp/1.0' } })" +
        "            .then(function(response) { return response.json(); })" +
        "            .then(function(data) {" +
        "                resultsDiv.innerHTML = '';" +
        "                " +
        "                if (data && data.length > 0) {" +
        "                    data.forEach(function(item) {" +
        "                        var div = document.createElement('div');" +
        "                        div.className = 'search-item';" +
        "                        div.textContent = item.display_name;" +
        "                        div.onclick = function() { selectPlace(item); };" +
        "                        resultsDiv.appendChild(div);" +
        "                    });" +
        "                    resultsDiv.style.display = 'block';" +
        "                } else {" +
        "                    resultsDiv.innerHTML = '<div class=\"loading\">Aucun resultat trouve</div>';" +
        "                }" +
        "            })" +
        "            .catch(function(error) {" +
        "                resultsDiv.innerHTML = '<div class=\"loading\">Erreur de recherche</div>';" +
        "            });" +
        "        }" +
        "        " +
        "        function selectPlace(item) {" +
        "            selectedLat = parseFloat(item.lat);" +
        "            selectedLng = parseFloat(item.lon);" +
        "            selectedName = item.display_name;" +
        "            " +
        "            if (map) {" +
        "                map.setView([selectedLat, selectedLng], 15);" +
        "                " +
        "                if (marker) { map.removeLayer(marker); }" +
        "                marker = L.marker([selectedLat, selectedLng]).addTo(map);" +
        "            }" +
        "            " +
        "            showLocationPanel(selectedName, selectedLat, selectedLng);" +
        "            " +
        "            document.getElementById('searchResults').style.display = 'none';" +
        "            document.getElementById('searchInput').value = '';" +
        "        }" +
        "        " +
        "        function confirmLocation() {" +
        "            if (typeof javaConnector !== 'undefined' && selectedLat && selectedLng) {" +
        "                javaConnector.setLocation(selectedName, selectedLat, selectedLng);" +
        "            }" +
        "        }" +
        "        " +
        "        if (document.readyState === 'loading') {" +
        "            document.addEventListener('DOMContentLoaded', initMap);" +
        "        } else {" +
        "            initMap();" +
        "        }" +
        "    </script>" +
        "</body>" +
        "</html>";

    @FXML
    private void initialize() {
        mapWebView.setPrefSize(900, 600);
        mapWebView.setMinSize(900, 600);

        WebEngine engine = mapWebView.getEngine();
        engine.setJavaScriptEnabled(true);

        engine.getLoadWorker().stateProperty().addListener((obs, oldState, newState) -> {
            if (newState == Worker.State.SUCCEEDED) {
                JSObject window = (JSObject) engine.executeScript("window");
                window.setMember("javaConnector", new JavaConnector());
            }
        });

        engine.loadContent(MAP_HTML);
    }

    public void setCallback(LocationSelectionCallback callback) {
        this.callback = callback;
    }

    public class JavaConnector {
        public void setLocation(String name, double lat, double lng) {
            Platform.runLater(() -> {
                if (callback != null) {
                    callback.onLocationSelected(name, lat, lng);
                }
                Stage stage = (Stage) mapWebView.getScene().getWindow();
                if (stage != null) {
                    stage.close();
                }
            });
        }
    }

    @FunctionalInterface
    public interface LocationSelectionCallback {
        void onLocationSelected(String name, double lat, double lng);
    }
}
