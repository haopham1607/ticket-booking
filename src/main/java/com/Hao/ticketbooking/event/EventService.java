package com.Hao.ticketbooking.event;

import com.Hao.ticketbooking.event.dto.CreateEventRequest;
import com.Hao.ticketbooking.event.dto.EventResponse;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@Transactional(readOnly = true)   // default for every method; create() overrides it
public class EventService {

    private final EventRepository eventRepository;
    private final SeatRepository seatRepository;

    public EventService(EventRepository eventRepository, SeatRepository seatRepository) {
        this.eventRepository = eventRepository;
        this.seatRepository = seatRepository;
    }

    // One transaction: if generating the seats fails, the event insert is rolled back too,
    // so there is never an event without seats
    @Transactional
    public EventResponse create(CreateEventRequest request) {
        Event event = eventRepository.save(new Event(
                request.name().trim(),
                request.venue().trim(),
                request.startsAt(),
                request.priceCents()
        ));

        int seatsCreated = seatRepository.generateSeats(event.getId(), request.rows(), request.seatsPerRow());

        return EventResponse.from(event, seatsCreated);
    }

    public List<EventResponse> listUpcoming() {
        List<Event> events = eventRepository.findByStartsAtAfterOrderByStartsAtAsc(Instant.now());
        if (events.isEmpty()) {
            return List.of();
        }

        // Two queries in total, however many events: the events, then all their seat counts
        List<Long> ids = events.stream().map(Event::getId).toList();
        Map<Long, Long> seatCounts = seatRepository.countByEventIds(ids).stream()
                .collect(Collectors.toMap(
                        SeatRepository.EventSeatCount::getEventId,
                        SeatRepository.EventSeatCount::getSeatCount));

        return events.stream()
                .map(event -> EventResponse.from(event, seatCounts.getOrDefault(event.getId(), 0L)))
                .toList();
    }

    public EventResponse get(Long id) {
        Event event = eventRepository.findById(id)
                .orElseThrow(() -> new EventNotFoundException(id));
        return EventResponse.from(event, seatRepository.countByEventId(id));
    }
}
