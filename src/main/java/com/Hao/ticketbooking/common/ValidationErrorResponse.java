package com.Hao.ticketbooking.common;

import java.util.Map;

/**
 * The JSON body for a 400 caused by invalid request fields, e.g.
 * { "error": "VALIDATION_FAILED", "message": "...", "fields": { "email": "must be a well-formed email address" } }
 */
public record ValidationErrorResponse(String error, String message, Map<String, String> fields) {
}
