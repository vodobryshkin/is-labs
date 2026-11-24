package com.github.vodobryshkin.islab1.backend.service;

import com.github.vodobryshkin.islab1.backend.domain.Organization;
import com.github.vodobryshkin.islab1.backend.domain.Position;
import com.github.vodobryshkin.islab1.backend.domain.Status;
import com.github.vodobryshkin.islab1.backend.repository.DomainWriteLock;
import com.github.vodobryshkin.islab1.backend.repository.OrganizationRepository;
import com.github.vodobryshkin.islab1.backend.repository.WorkerRepository;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import java.util.Objects;

/**
 * Новые ограничения уникальности реализованы в Java, без уникальных индексов и триггеров.
 */
@ApplicationScoped
public class DomainRules {
    private final DomainWriteLock lock;
    private final OrganizationRepository organizations;
    private final WorkerRepository workers;

    @Inject
    public DomainRules(DomainWriteLock lock, OrganizationRepository organizations,
            WorkerRepository workers) {
        this.lock = lock;
        this.organizations = organizations;
        this.workers = workers;
    }

    protected DomainRules() {
        this(null, null, null);
    }

    public void beginWrite() {
        lock.acquire();
    }

    /**
     * Исключает редактируемую организацию из сравнения с существующими названиями.
     */
    public boolean organizationNameTaken(String name, Long excludedId) {
        String normalized = OrganizationNames.normalize(name);
        return organizations.findNames().stream()
                .filter(row -> !Objects.equals(row[0], excludedId))
                .anyMatch(row -> normalized.equals(OrganizationNames.normalize((String) row[1])));
    }

    /**
     * Новый объект организации ещё не имеет работников; без организации правило не действует.
     */
    public boolean directorTaken(Organization organization, Position position, Status status,
            Integer excludedWorkerId) {
        return organization != null && organization.getId() != null
                && position == Position.DIRECTOR && status == Status.HIRED
                && workers.hasOtherHiredDirector(organization.getId(), excludedWorkerId);
    }
}
