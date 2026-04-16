package services;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import models.Event;

import java.sql.Date;
import java.sql.SQLException;
import java.time.LocalDate;
import java.util.Comparator;
import java.util.List;

public class EventStore {
    private static EventStore instance;

    private final ObservableList<Event> events = FXCollections.observableArrayList();
    private final EventService eventService;
    private int nextLocalId = 1;

    private EventStore() {
        // Initialize eventService and load persisted events off the FX thread
        EventService service = null;
        try {
            service = new EventService();
        } catch (Exception ignored) {
            service = null;
        }
        this.eventService = service;
        final EventService svc = service;

        if (svc != null) {
            javafx.concurrent.Task<List<Event>> loader = new javafx.concurrent.Task<>() {
                @Override
                protected List<Event> call() throws Exception {
                    return svc.recuperer();
                }
            };

            loader.setOnSucceeded(ev -> {
                List<Event> persistedEvents = loader.getValue();
                if (persistedEvents != null && !persistedEvents.isEmpty()) {
                    events.addAll(persistedEvents);
                    nextLocalId = persistedEvents.stream()
                            .map(Event::getId)
                            .max(Comparator.naturalOrder())
                            .orElse(0) + 1;
                } else {
                    seedSampleData();
                }
            });

            loader.setOnFailed(ev -> seedSampleData());

            Thread t = new Thread(loader, "eventstore-loader");
            t.setDaemon(true);
            t.start();
        } else {
            seedSampleData();
        }
    }

    public static EventStore getInstance() {
        if (instance == null) {
            instance = new EventStore();
        }
        return instance;
    }

    public ObservableList<Event> getEvents() {
        return events;
    }

    public void addEvent(Event event) {
        if (event.getId() <= 0) {
            event.setId(nextLocalId++);
        }

        persistAdd(event);
        events.add(event);
    }

    public void updateEvent(Event updatedEvent) {
        persistUpdate(updatedEvent);

        for (int i = 0; i < events.size(); i++) {
            if (events.get(i).getId() == updatedEvent.getId()) {
                events.set(i, updatedEvent);
                return;
            }
        }

        // If the event is missing from the in-memory list, keep it visible instead of dropping it.
        events.add(updatedEvent);
    }

    public void deleteEvent(Event event) {
        persistDelete(event);
        events.removeIf(existing -> existing.getId() == event.getId());
    }

    private void persistAdd(Event event) {
        if (eventService == null) {
            return;
        }

        try {
            eventService.ajouter(event);
        } catch (SQLException | RuntimeException ignored) {
        }
    }

    private void persistUpdate(Event event) {
        if (eventService == null) {
            return;
        }

        try {
            eventService.modifier(event);
        } catch (SQLException | RuntimeException ignored) {
        }
    }

    private void persistDelete(Event event) {
        if (eventService == null) {
            return;
        }

        try {
            eventService.supprimer(event.getId());
        } catch (SQLException | RuntimeException ignored) {
        }
    }

    private void seedSampleData() {
        if (!events.isEmpty()) {
            return;
        }

        Event sciences = new Event();
        sciences.setId(nextLocalId++);
        sciences.setTitle("sciences");
        sciences.setDescription("jaime sciences");
        sciences.setDate(Date.valueOf(LocalDate.of(2026, 2, 26)));
        sciences.setDuration(210);
        sciences.setPriority("Moyenne");
        sciences.setStatus("En cours");
        sciences.setReminder_minutes(40);
        sciences.setType("Etude");
        sciences.setLocation("Salle 2");

        events.add(sciences);
    }
}
