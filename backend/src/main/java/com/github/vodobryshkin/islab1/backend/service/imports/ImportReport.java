package com.github.vodobryshkin.islab1.backend.service.imports;

/**
 * Результат после завершения транзакции данных и сохранения итогового статуса истории.
 */
public record ImportReport(long operationId, Integer addedCount, ImportFailure failure) {
}
