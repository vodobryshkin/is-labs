package com.github.vodobryshkin.islab1.backend.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.SequenceGenerator;
import jakarta.persistence.Table;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
import java.util.Objects;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * История одной попытки импорта. Завершённая операция больше не меняет состояние.
 */
@Entity
@Table(name = "import_operations")
@Getter
@NoArgsConstructor
public class ImportOperation {
    @Id
    @SequenceGenerator(name = "import_operations_seq",
            sequenceName = "import_operations_id_seq", allocationSize = 1)
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "import_operations_seq")
    private Long id;

    @ManyToOne(optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    @NotNull
    private User user;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    @NotNull
    private ImportStatus status;

    @Column(name = "added_count")
    @PositiveOrZero
    private Integer addedCount;

    /**
     * Создаёт незавершённую операцию для вошедшего пользователя.
     */
    public ImportOperation(User user) {
        this.user = Objects.requireNonNull(user);
        status = ImportStatus.IN_PROGRESS;
    }

    /**
     * Фиксирует число добавленных работников при успехе.
     */
    public void succeed(int count) {
        requireInProgress();
        if (count < 0) {
            throw new IllegalArgumentException("Added count must not be negative.");
        }
        addedCount = count;
        status = ImportStatus.SUCCESS;
    }

    /**
     * Завершает операцию ошибкой без числа добавленных работников.
     */
    public void fail() {
        requireInProgress();
        status = ImportStatus.FAILED;
    }

    private void requireInProgress() {
        if (status != ImportStatus.IN_PROGRESS) {
            throw new IllegalStateException("Import operation has already finished.");
        }
    }
}
