package controllers.gestiondetemps;

import javafx.animation.PauseTransition;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.concurrent.Task;
import javafx.scene.control.ContextMenu;
import javafx.scene.control.CustomMenuItem;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.util.Duration;
import org.json.JSONArray;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

public class LocationAutocompleteHelper {

    private static final String SEARCH_ENDPOINT =
            "https://nominatim.openstreetmap.org/search?format=jsonv2&limit=5&q=";

    private final PauseTransition debounce = new PauseTransition(Duration.millis(350));
    private final ContextMenu suggestionsPopup = new ContextMenu();

    public void bind(TextField locationField) {
        locationField.textProperty().addListener((obs, oldValue, newValue) -> {
            String query = newValue == null ? "" : newValue.trim();
            if (query.length() < 3) {
                suggestionsPopup.hide();
                return;
            }

            debounce.stop();
            debounce.setOnFinished(event -> loadSuggestions(locationField, query));
            debounce.playFromStart();
        });

        locationField.focusedProperty().addListener((obs, hadFocus, hasFocus) -> {
            if (!hasFocus) {
                suggestionsPopup.hide();
            }
        });
    }

    private void loadSuggestions(TextField locationField, String query) {
        Task<List<String>> task = new Task<>() {
            @Override
            protected List<String> call() {
                return fetchSuggestions(query);
            }
        };

        task.setOnSucceeded(event -> showSuggestions(locationField, task.getValue()));
        task.setOnFailed(event -> suggestionsPopup.hide());

        Thread thread = new Thread(task, "location-autocomplete");
        thread.setDaemon(true);
        thread.start();
    }

    private void showSuggestions(TextField locationField, List<String> suggestions) {
        if (suggestions == null || suggestions.isEmpty() || !locationField.isFocused()) {
            suggestionsPopup.hide();
            return;
        }

        List<CustomMenuItem> items = new ArrayList<>();
        for (String suggestion : suggestions) {
            Label label = new Label(suggestion);
            label.setWrapText(true);
            label.setMaxWidth(420);

            CustomMenuItem item = new CustomMenuItem(label, true);
            item.setOnAction(event -> {
                locationField.setText(suggestion);
                locationField.positionCaret(suggestion.length());
                suggestionsPopup.hide();
            });
            items.add(item);
        }

        suggestionsPopup.getItems().setAll(FXCollections.observableArrayList(items));
        if (!suggestionsPopup.isShowing()) {
            suggestionsPopup.show(locationField, javafx.geometry.Side.BOTTOM, 0, 0);
        }
    }

    private List<String> fetchSuggestions(String query) {
        List<String> results = new ArrayList<>();

        try {
            String encoded = URLEncoder.encode(query, StandardCharsets.UTF_8);
            URL url = new URL(SEARCH_ENDPOINT + encoded);
            HttpURLConnection connection = (HttpURLConnection) url.openConnection();
            connection.setRequestMethod("GET");
            connection.setRequestProperty("User-Agent", "StudlyJavaFX/1.0");
            connection.setRequestProperty("Accept-Language", "fr,en");
            connection.setConnectTimeout(4000);
            connection.setReadTimeout(4000);

            if (connection.getResponseCode() != 200) {
                return results;
            }

            StringBuilder body = new StringBuilder();
            try (BufferedReader reader = new BufferedReader(
                    new InputStreamReader(connection.getInputStream(), StandardCharsets.UTF_8))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    body.append(line);
                }
            }

            JSONArray array = new JSONArray(body.toString());
            for (int index = 0; index < array.length(); index++) {
                JSONObject object = array.getJSONObject(index);
                String displayName = object.optString("display_name", "").trim();
                if (!displayName.isEmpty()) {
                    results.add(displayName);
                }
            }
        } catch (Exception ignored) {
            Platform.runLater(suggestionsPopup::hide);
        }

        return results;
    }
}
