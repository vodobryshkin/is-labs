package com.github.vodobryshkin.islab1.backend.service;

import com.github.vodobryshkin.islab1.backend.domain.ImportOperation;
import com.github.vodobryshkin.islab1.backend.domain.User;
import com.github.vodobryshkin.islab1.backend.dto.common.ListQuery;
import com.github.vodobryshkin.islab1.backend.dto.common.PageResponse;
import com.github.vodobryshkin.islab1.backend.dto.imports.ImportOperationResponse;
import com.github.vodobryshkin.islab1.backend.repository.ImportOperationRepository;
import com.github.vodobryshkin.islab1.backend.repository.UserRepository;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;

/**
 * Права просмотра и транзакционные границы истории импорта.
 */
@ApplicationScoped
public class ImportHistoryService {
    private final ImportOperationRepository repository;
    private final UserRepository users;

    @Inject
    public ImportHistoryService(ImportOperationRepository repository, UserRepository users) {
        this.repository = repository;
        this.users = users;
    }

    protected ImportHistoryService() {
        this(null, null);
    }

    /**
     * Владелец определяется по аутентифицированному имени, а не параметру URL.
     */
    @Transactional
    public PageResponse<ImportOperationResponse> list(String username, ListQuery query) {
        User caller = users.findByUsername(username).orElseThrow();
        Long ownerId = caller.isAdmin() ? null : caller.getId();

        return new PageResponse<>(repository.findPage(ownerId, query).stream()
                .map(ImportOperationResponse::new).toList(),
                query.getPage(), query.getPageSize(), repository.count(ownerId));
    }

    /**
     * Начало сохраняется независимо от будущего отката импортируемых данных.
     */
    @Transactional(Transactional.TxType.REQUIRES_NEW)
    public long start(String username) {
        ImportOperation operation = new ImportOperation(users.findByUsername(username).orElseThrow());
        repository.save(operation);
        return operation.getId();
    }

    /**
     * Вызывается внутри транзакции импорта: статус и данные фиксируются вместе.
     */
    @Transactional(Transactional.TxType.MANDATORY)
    public void succeed(long id, int addedCount) {
        operation(id).succeed(addedCount);
    }

    /**
     * Вызывается после выхода из неуспешной транзакции импорта.
     */
    @Transactional(Transactional.TxType.REQUIRES_NEW)
    public void fail(long id) {
        operation(id).fail();
    }

    private ImportOperation operation(long id) {
        return repository.findForUpdate(id)
                .orElseThrow(() -> new IllegalArgumentException("Import operation does not exist."));
    }
}
