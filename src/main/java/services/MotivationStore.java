package services;

import models.Event;
import models.Motivation;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

public class MotivationStore {
    private static MotivationStore instance;

    private final List<Motivation> motivations = new ArrayList<>();
    private final MotivationService motivationService;
    private int nextLocalId = 1;

    private MotivationStore() {
        MotivationService service = null;
        try {
            service = new MotivationService();
            List<Motivation> persisted = service.recuperer();
            if (persisted != null && !persisted.isEmpty()) {
                motivations.addAll(persisted);
                nextLocalId = persisted.stream()
                        .map(Motivation::getId)
                        .max(Comparator.naturalOrder())
                        .orElse(0) + 1;
            }
        } catch (Exception ignored) {
        }
        this.motivationService = service;
    }

    public static MotivationStore getInstance() {
        if (instance == null) {
            instance = new MotivationStore();
        }
        return instance;
    }

    public Motivation getForEvent(Event event) {
        if (event == null || event.getMotivation_id() <= 0) {
            return null;
        }

        return motivations.stream()
                .filter(motivation -> motivation.getId() == event.getMotivation_id())
                .findFirst()
                .orElse(null);
    }

    public Motivation saveForEvent(Event event, Motivation motivation) {
        if (event == null || motivation == null) {
            return null;
        }

        if (event.getMotivation_id() > 0) {
            motivation.setId(event.getMotivation_id());
        } else if (motivation.getId() <= 0) {
            motivation.setId(nextLocalId++);
        }

        motivation.setUser_id(event.getUser_id());
        persistSave(motivation, event.getMotivation_id() > 0);

        boolean updated = false;
        for (int i = 0; i < motivations.size(); i++) {
            if (motivations.get(i).getId() == motivation.getId()) {
                motivations.set(i, motivation);
                updated = true;
                break;
            }
        }
        if (!updated) {
            motivations.add(motivation);
        }

        Event updatedEvent = copyEvent(event);
        updatedEvent.setMotivation_id(motivation.getId());
        EventStore.getInstance().updateEvent(updatedEvent);
        return motivation;
    }

    public void deleteForEvent(Event event) {
        if (event == null || event.getMotivation_id() <= 0) {
            return;
        }

        int motivationId = event.getMotivation_id();
        persistDelete(motivationId);
        motivations.removeIf(motivation -> motivation.getId() == motivationId);

        Event updatedEvent = copyEvent(event);
        updatedEvent.setMotivation_id(0);
        EventStore.getInstance().updateEvent(updatedEvent);
    }

    private void persistSave(Motivation motivation, boolean update) {
        if (motivationService == null) {
            return;
        }

        try {
            if (update) {
                motivationService.modifier(motivation);
            } else {
                motivationService.ajouter(motivation);
            }
        } catch (SQLException | RuntimeException ignored) {
        }
    }

    private void persistDelete(int motivationId) {
        if (motivationService == null) {
            return;
        }

        try {
            motivationService.supprimer(motivationId);
        } catch (SQLException | RuntimeException ignored) {
        }
    }

    private Event copyEvent(Event source) {
        Event copy = new Event();
        copy.setId(source.getId());
        copy.setTitle(source.getTitle());
        copy.setDescription(source.getDescription());
        copy.setType(source.getType());
        copy.setDuration(source.getDuration());
        copy.setLocation(source.getLocation());
        copy.setStatus(source.getStatus());
        copy.setPriority(source.getPriority());
        copy.setDifficulty(source.getDifficulty());
        copy.setDate(source.getDate());
        copy.setStart_time(source.getStart_time());
        copy.setEnd_time(source.getEnd_time());
        copy.setColor(source.getColor());
        copy.setCategory(source.getCategory());
        copy.setNotes(source.getNotes());
        copy.setAll_day(source.getAll_day());
        copy.setReminder_minutes(source.getReminder_minutes());
        copy.setGoogle_event_id(source.getGoogle_event_id());
        copy.setMotivation_id(source.getMotivation_id());
        copy.setUser_id(source.getUser_id());
        return copy;
    }
}
