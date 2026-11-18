package com.ejr.managekitchen.common.exception;

import java.time.LocalDateTime;
import java.util.List;

/** Format unique des réponses d'erreur de toute l'API. */
public record ApiError(LocalDateTime timestamp, int status, String error, String message, String path,
                       List<String> details) {

    public static ApiError of(int status, String error, String message, String path) {
        return new ApiError(LocalDateTime.now(), status, error, message, path, List.of());
    }
}
