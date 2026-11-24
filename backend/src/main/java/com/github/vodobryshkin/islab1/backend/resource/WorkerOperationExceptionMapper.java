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
                .entity(new ErrorResponse(exception.getMessage()))
                .build();
    }
}
