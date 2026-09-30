package com.Hao.ticketbooking.event;

import com.Hao.ticketbooking.event.dto.CreateEventRequest;
import com.Hao.ticketbooking.event.dto.EventResponse;
import com.Hao.ticketbooking.event.dto.SeatMapResponse;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/events")
public class EventController {

    private final EventService eventService;
    private final SeatMapService seatMapService;

    public EventController(EventService eventService, SeatMapService seatMapService) {
        this.eventService = eventService;
        this.seatMapService = seatMapService;
    }

    @PreAuthorize("hasRole('ADMIN')")
    @PostMapping
    public ResponseEntity<EventResponse> create(@Valid @RequestBody CreateEventRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(eventService.create(request));
    }

    @GetMapping
    public List<EventResponse> listUpcoming() {
        return eventService.listUpcoming();
    }

    @GetMapping("/{id}")
    public EventResponse get(@PathVariable Long id) {
        return eventService.get(id);
    }

    // Public, like the other GETs: covered by GET /api/events/** in SecurityConfig
    @GetMapping("/{id}/seats")
    public SeatMapResponse seatMap(@PathVariable Long id) {
        return seatMapService.getSeatMap(id);
    }
}
