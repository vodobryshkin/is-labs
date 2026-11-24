package com.github.vodobryshkin.islab1.backend.service.imports;

import com.github.vodobryshkin.islab1.backend.dto.common.ValidationIssue;
import java.util.List;
import lombok.Getter;

/**
 * Ошибка импорта с номером записи от единицы; техническая причина не попадает в HTTP-ответ.
 */
@Getter
public class ImportFailure extends RuntimeException {
    private static final long serialVersionUID = 1L;

    public enum Kind {
        FORMAT, VALIDATION, CONFLICT, STORAGE, TOO_LARGE, UNSUPPORTED_FORMAT
    }

    private final Kind kind;
    private final Integer record;
    private final List<ValidationIssue> errors;

    public ImportFailure(Kind kind, String message, Integer record) {
        this(kind, message, record, List.of(), null);
    }

    public ImportFailure(Kind kind, String message, Integer record,
            List<ValidationIssue> errors, Throwable cause) {
        super(message, cause);
        this.kind = kind;
        this.record = record;
        this.errors = List.copyOf(errors);
    }
}
