package com.github.vodobryshkin.islab1.backend.service.imports;

import com.github.vodobryshkin.islab1.backend.dto.worker.WorkerRequest;
import com.github.vodobryshkin.islab1.backend.service.ImportHistoryService;
import com.github.vodobryshkin.islab1.backend.service.Outcome;
import com.github.vodobryshkin.islab1.backend.service.WorkerOperationException;
import com.github.vodobryshkin.islab1.backend.service.WorkerService;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import java.util.List;

/**
 * Весь файл и успешный статус истории фиксируются одной транзакцией.
 */
@ApplicationScoped
public class WorkerImportTransaction {
    private final WorkerService workers;
    private final ImportHistoryService history;

    @Inject
    public WorkerImportTransaction(WorkerService workers, ImportHistoryService history) {
        this.workers = workers;
        this.history = history;
    }

    protected WorkerImportTransaction() {
        this(null, null);
    }

    @Transactional(Transactional.TxType.REQUIRES_NEW)
    public void importWorkers(long operationId, List<WorkerRequest> records, String username) {
        for (int i = 0; i < records.size(); i++) {
            try {
                workers.create(records.get(i).toEntity(), username);
            } catch (WorkerOperationException exception) {
                ImportFailure.Kind kind = exception.getOutcome() == Outcome.CONFLICT
                        ? ImportFailure.Kind.CONFLICT : ImportFailure.Kind.VALIDATION;
                throw new ImportFailure(kind, exception.getMessage(), i + 1, List.of(), exception);
            } catch (RuntimeException exception) {
                throw new ImportFailure(ImportFailure.Kind.STORAGE,
                        "Failed to save the import. No objects were added.",
                        i + 1, List.of(), exception);
            }
        }
        history.succeed(operationId, records.size());
    }
}
