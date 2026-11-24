package com.github.vodobryshkin.islab1.backend.service.imports;

import com.github.vodobryshkin.islab1.backend.service.ImportHistoryService;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.inject.Any;
import jakarta.enterprise.inject.Instance;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import java.util.Locale;

/**
 * Разбор вне транзакции записи; ошибка истории сохраняется после отката записи данных.
 */
@ApplicationScoped
public class WorkerImportService {
    public static final int MAX_FILE_BYTES = 10 * 1024 * 1024;
    private final Instance<WorkerImportParser> parsers;
    private final WorkerImportValidator validator;
    private final WorkerImportTransaction transaction;
    private final ImportHistoryService history;

    @Inject
    public WorkerImportService(@Any Instance<WorkerImportParser> parsers,
            WorkerImportValidator validator, WorkerImportTransaction transaction,
            ImportHistoryService history) {
        this.parsers = parsers;
        this.validator = validator;
        this.transaction = transaction;
        this.history = history;
    }

    protected WorkerImportService() {
        this(null, null, null, null);
    }

    @Transactional(Transactional.TxType.NOT_SUPPORTED)
    public ImportReport importFile(InputStream input, String format, String username) {
        long operationId = history.start(username);
        try {
            WorkerImportParser parser = parser(format);
            byte[] file = read(input);
            var records = parser.parse(new ByteArrayInputStream(file));
            validator.validate(records);
            transaction.importWorkers(operationId, records, username);
            return new ImportReport(operationId, records.size(), null);
        } catch (Exception exception) {
            // JTA может выбросить checked RollbackException из CDI-перехватчика при commit.
            ImportFailure failure = failure(exception);
            history.fail(operationId);
            return new ImportReport(operationId, null, failure);
        }
    }

    private WorkerImportParser parser(String format) {
        String name = format == null ? "json" : format.strip().toLowerCase(Locale.ROOT);
        for (WorkerImportParser parser : parsers) {
            if (parser.format().equals(name)) {
                return parser;
            }
        }
        throw new ImportFailure(ImportFailure.Kind.UNSUPPORTED_FORMAT,
                "Unsupported import format: " + name, null);
    }

    private byte[] read(InputStream input) {
        if (input == null) {
            throw new ImportFailure(ImportFailure.Kind.FORMAT, "The import file is empty.", null);
        }
        try {
            byte[] bytes = input.readNBytes(MAX_FILE_BYTES + 1);
            if (bytes.length > MAX_FILE_BYTES) {
                throw new ImportFailure(ImportFailure.Kind.TOO_LARGE,
                        "The import file must not exceed 10 MiB.", null);
            }
            return bytes;
        } catch (IOException exception) {
            throw new ImportFailure(ImportFailure.Kind.FORMAT,
                    "The import file could not be read.", null, List.of(), exception);
        }
    }

    private ImportFailure failure(Exception exception) {
        for (Throwable cause = exception; cause != null; cause = cause.getCause()) {
            if (cause instanceof ImportFailure failure) {
                return failure;
            }
        }
        return new ImportFailure(ImportFailure.Kind.STORAGE,
                "Failed to save the import. No objects were added.", null, List.of(), exception);
    }
}
