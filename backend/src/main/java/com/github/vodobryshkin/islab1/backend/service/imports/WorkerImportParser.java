package com.github.vodobryshkin.islab1.backend.service.imports;

import com.github.vodobryshkin.islab1.backend.dto.worker.WorkerRequest;
import java.io.InputStream;
import java.util.List;

/**
 * Новый формат добавляется отдельной CDI-реализацией, без изменения сохранения и истории.
 */
public interface WorkerImportParser {
    String format();

    List<WorkerRequest> parse(InputStream input);
}
