package controllers.gestiondetemps;

import javafx.collections.ListChangeListener;
import javafx.collections.ObservableList;
import javafx.concurrent.Worker;
import javafx.fxml.FXML;
import javafx.scene.web.WebEngine;
import javafx.scene.web.WebView;
import models.Event;
import netscape.javascript.JSObject;
import org.json.JSONArray;
import org.json.JSONObject;
import services.EventStore;

import java.net.URL;
import java.time.LocalDateTime;

public class CalendarViewController {

    @FXML
    private WebView calendarWebView;

    private WebEngine webEngine;
    private final ObservableList<Event> events = EventStore.getInstance().getEvents();
    private boolean isCalendarReady = false;

    @FXML
    private void initialize() {
        webEngine = calendarWebView.getEngine();
        
        // Enable JavaScript
        webEngine.setJavaScriptEnabled(true);
        
        // Load the calendar HTML
        URL calendarUrl = getClass().getResource("/Gestion de temps/calendar.html");
        if (calendarUrl != null) {
            webEngine.load(calendarUrl.toExternalForm());
        } else {
            System.err.println("Calendar HTML not found!");
        }
        
        // Wait for page to load
        webEngine.getLoadWorker().stateProperty().addListener((obs, oldState, newState) -> {
            if (newState == Worker.State.SUCCEEDED) {
                isCalendarReady = true;
                
                // Inject Java connector
                JSObject window = (JSObject) webEngine.executeScript("window");
                window.setMember("javaConnector", new JavaConnector());
                
                // Load initial events
                loadEventsToCalendar();
            }
        });
        
        // Listen for event changes
        events.addListener((ListChangeListener<Event>) change -> {
            if (isCalendarReady) {
                loadEventsToCalendar();
            }
        });
    }

    private void loadEventsToCalendar() {
        if (!isCalendarReady) return;
        
        try {
            JSONArray eventsArray = new JSONArray();
            
            for (Event event : events) {
                JSONObject eventObj = new JSONObject();
                eventObj.put("id", event.getId());
                eventObj.put("title", event.getTitle());
                
                // Format dates with precise times
                if (event.getDate() != null) {
                    String dateStr = event.getDate().toString(); // Format: YYYY-MM-DD
                    
                    if (event.getStart_time() != null) {
                        // Convert Timestamp to time string (HH:mm:ss)
                        String startTime = event.getStart_time().toLocalDateTime().toLocalTime().toString();
                        eventObj.put("start", dateStr + "T" + startTime);
                        
                        if (event.getEnd_time() != null) {
                            // Use end_time if available
                            String endTime = event.getEnd_time().toLocalDateTime().toLocalTime().toString();
                            eventObj.put("end", dateStr + "T" + endTime);
                        } else if (event.getDuration() > 0) {
                            // Calculate end time from start_time + duration
                            LocalDateTime startDateTime = event.getStart_time().toLocalDateTime();
                            LocalDateTime endDateTime = startDateTime.plusMinutes(event.getDuration());
                            String endTime = endDateTime.toLocalTime().toString();
                            eventObj.put("end", dateStr + "T" + endTime);
                        }
                    } else {
                        // No start time - treat as all-day event
                        eventObj.put("start", dateStr);
                        eventObj.put("allDay", true);
                    }
                }
                
                // Add event type for color coding
                eventObj.put("type", event.getType() != null ? event.getType() : "autre");
                
                // Add description
                if (event.getDescription() != null && !event.getDescription().isEmpty()) {
                    eventObj.put("description", event.getDescription());
                }
                
                eventsArray.put(eventObj);
            }
            
            String eventsJson = eventsArray.toString();
            webEngine.executeScript("loadEvents('" + escapeJson(eventsJson) + "')");
            
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private String escapeJson(String json) {
        return json.replace("'", "\\'").replace("\n", "\\n").replace("\r", "\\r");
    }

    // Inner class for JavaScript to Java communication
    public class JavaConnector {
        public void onEventClick(String eventId) {
            try {
                int id = Integer.parseInt(eventId);
                Event event = events.stream()
                    .filter(e -> e.getId() == id)
                    .findFirst()
                    .orElse(null);
                
                if (event != null) {
                    // Show event details or edit dialog
                    System.out.println("Event clicked: " + event.getTitle());
                    // You can add a dialog here to show/edit event details
                }
            } catch (NumberFormatException e) {
                e.printStackTrace();
            }
        }
    }
}
