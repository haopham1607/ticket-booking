package com.Hao.ticketbooking.event.dto;

import java.util.List;

public record RowResponse(String row, List<SeatResponse> seats) {
}
