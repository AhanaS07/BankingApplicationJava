package com.tnf.common_dto.dto.common;

import java.time.LocalDateTime;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

// Standard error body returned by every service's exception handler. Keeps the error shape uniform
// across services (timestamp, status, error, message, path). Uses LocalDateTime to match the
// positional constructor used by the other banking services (e.g. customer-service).
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ErrorResponse {

    private LocalDateTime timestamp;

    private int status;

    private String error;

    private String message;

    private String path;
}
