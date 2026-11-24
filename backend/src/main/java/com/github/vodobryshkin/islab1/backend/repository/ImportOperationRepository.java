package com.github.vodobryshkin.islab1.backend.repository;

import com.github.vodobryshkin.islab1.backend.domain.ImportOperation;
import com.github.vodobryshkin.islab1.backend.dto.common.ListQuery;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.persistence.EntityManager;
import jakarta.persistence.LockModeType;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.TypedQuery;
import java.util.List;
import java.util.Optional;

/**
 * Хранилище истории. Фильтр владельца выбирает сервис по текущему пользователю.
 */
@ApplicationScoped
public class ImportOperationRepository {
    @PersistenceContext(unitName = "is-lab1")
    private EntityManager entityManager;

    public void save(ImportOperation operation) {
        entityManager.persist(operation);
        entityManager.flush();
    }

    public Optional<ImportOperation> findForUpdate(long id) {
        return Optional.ofNullable(entityManager.find(
                ImportOperation.class, id, LockModeType.PESSIMISTIC_WRITE));
    }

    public List<ImportOperation> findPage(Long ownerId, ListQuery query) {
        String order = query.isAscending() ? "ASC" : "DESC";
        TypedQuery<ImportOperation> request = entityManager.createQuery(
                "SELECT i FROM ImportOperation i" + ownerFilter(ownerId)
                        + " ORDER BY i.id " + order, ImportOperation.class);
        if (ownerId != null) {
            request.setParameter("ownerId", ownerId);
        }
        long offset = (long) (query.getPage() - 1) * query.getPageSize();

        return request.setFirstResult((int) Math.min(offset, Integer.MAX_VALUE))
                .setMaxResults(query.getPageSize())
                .getResultList();
    }

    public long count(Long ownerId) {
        TypedQuery<Long> request = entityManager.createQuery(
                "SELECT COUNT(i) FROM ImportOperation i" + ownerFilter(ownerId), Long.class);
        if (ownerId != null) {
            request.setParameter("ownerId", ownerId);
        }
        return request.getSingleResult();
    }

    private String ownerFilter(Long ownerId) {
        return ownerId == null ? "" : " WHERE i.user.id = :ownerId";
    }
}
