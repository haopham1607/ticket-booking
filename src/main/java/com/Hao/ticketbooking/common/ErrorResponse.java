package com.Hao.ticketbooking.common;

/**
 * The JSON body returned for every error, e.g.
 * { "error": "NOT_FOUND", "message": "Not Found" }
 */
public record ErrorResponse(String error, String message) {
}
