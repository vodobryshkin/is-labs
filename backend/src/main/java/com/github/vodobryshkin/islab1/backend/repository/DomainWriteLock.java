package com.github.vodobryshkin.islab1.backend.repository;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.transaction.Transactional;

/**
 * Последовательно выполняет проверку и изменение работников/организаций в PostgreSQL.
 * Блокировка действует до конца транзакции и общая для всех экземпляров приложения.
 */
@ApplicationScoped
public class DomainWriteLock {
    @PersistenceContext(unitName = "is-lab1")
    private EntityManager entityManager;

    /**
     * Вызывается перед чтением изменяемых объектов, всегда в одном порядке таблиц.
     */
    @Transactional(Transactional.TxType.MANDATORY)
    public void acquire() {
        String isolation = (String) entityManager.createNativeQuery("SHOW transaction_isolation")
                .getSingleResult();
        if (!"read committed".equals(isolation)) {
            throw new IllegalStateException("Domain writes require READ COMMITTED isolation.");
        }
        entityManager.createNativeQuery(
                "LOCK TABLE organizations, workers IN SHARE ROW EXCLUSIVE MODE").executeUpdate();
    }
}
