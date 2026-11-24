package com.github.vodobryshkin.islab1.backend.resource;

import com.github.vodobryshkin.islab1.backend.dto.common.ErrorResponse;
import com.github.vodobryshkin.islab1.backend.service.WorkerOperationException;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.ext.ExceptionMapper;
import jakarta.ws.rs.ext.Provider;

/**
 * Преобразует бизнес-отказ в прежний HTTP-ответ после выхода из транзакционного сервиса.
 */
@Provider
public class WorkerOperationExceptionMapper implements ExceptionMapper<WorkerOperationException> {
    @Override
    public Response toResponse(WorkerOperationException exception) {
        int status = switch (exception.getOutcome()) {
            case NOT_FOUND, REFERENCE_NOT_FOUND -> 404;
            case CONFLICT, IN_USE -> 409;
            default -> throw new IllegalArgumentException("Expected a worker operation failure.");
        };

        return Response.status(status)
                .type(MediaType.APPLICATION_JSON_TYPE)
                .entity(new ErrorResponse(message(exception)))
                .build();
    }

    private String message(WorkerOperationException exception) {
        return switch (exception.getOutcome()) {
            case NOT_FOUND -> "Worker with id=" + exception.getSubjectId() + " does not exist.";
            case REFERENCE_NOT_FOUND -> missing(exception.getSubject(), exception.getSubjectId());
            case IN_USE -> "Worker with id=" + exception.getSubjectId()
                    + " is referenced by a person and cannot be deleted.";
            case CONFLICT -> "person".equals(exception.getSubject())
                    ? "Person with id=" + exception.getSubjectId()
                            + " is already assigned to another worker."
                    : "Another organization with this full_name already exists.";
            default -> throw new IllegalArgumentException("Expected a worker operation failure.");
        };
    }

    private String missing(String subject, long id) {
        String template = switch (subject) {
            case "address" -> "Address with id=%d does not exist.";
            case "coordinates" -> "Coordinates with id=%d do not exist.";
            case "organization" -> "Organization with id=%d does not exist.";
            case "person" -> "Person with id=%d does not exist.";
            case "location" -> "Location with id=%d does not exist.";
            default -> throw new IllegalArgumentException("Unsupported reference: " + subject);
        };

        return template.formatted(id);
    }
}
