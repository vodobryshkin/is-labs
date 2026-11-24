package com.github.vodobryshkin.islab1.backend.dto.imports;

import com.github.vodobryshkin.islab1.backend.domain.ImportStatus;
import com.github.vodobryshkin.islab1.backend.dto.common.ValidationIssue;
import com.github.vodobryshkin.islab1.backend.service.imports.ImportReport;
import jakarta.json.bind.annotation.JsonbNillable;
import jakarta.json.bind.annotation.JsonbProperty;
import java.util.List;
import lombok.Getter;

/**
 * Результат синхронной загрузки; operation_id позволяет найти попытку в истории.
 */
@Getter
public class ImportResponse {
    @JsonbProperty("operation_id")
    private final long operationId;
    private final ImportStatus status;
    @JsonbNillable
    @JsonbProperty("added_count")
    private final Integer addedCount;
    @JsonbNillable
    private final Integer record;
    private final String message;
    private final List<ValidationIssue> errors;

    public ImportResponse(ImportReport report) {
        operationId = report.operationId();
        addedCount = report.addedCount();
        status = report.failure() == null ? ImportStatus.SUCCESS : ImportStatus.FAILED;
        record = report.failure() == null ? null : report.failure().getRecord();
        message = report.failure() == null ? null : report.failure().getMessage();
        errors = report.failure() == null ? List.of() : report.failure().getErrors();
    }
}
