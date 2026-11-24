package com.github.vodobryshkin.islab1.backend.dto.imports;

import com.github.vodobryshkin.islab1.backend.domain.ImportOperation;
import com.github.vodobryshkin.islab1.backend.domain.ImportStatus;
import jakarta.json.bind.annotation.JsonbNillable;
import jakarta.json.bind.annotation.JsonbProperty;
import lombok.Getter;

/**
 * Публичная запись истории без хеша пароля и внутренних данных пользователя.
 */
@Getter
public class ImportOperationResponse {
    private final Long id;
    private final ImportStatus status;
    private final String username;

    @JsonbNillable
    @JsonbProperty("added_count")
    private final Integer addedCount;

    /**
     * Формирует запись истории внутри транзакции чтения.
     */
    public ImportOperationResponse(ImportOperation operation) {
        id = operation.getId();
        status = operation.getStatus();
        username = operation.getUser().getUsername();
        addedCount = operation.getAddedCount();
    }
}
