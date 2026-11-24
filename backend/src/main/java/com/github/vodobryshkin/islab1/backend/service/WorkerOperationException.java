package com.github.vodobryshkin.islab1.backend.service;

/**
 * Бизнес-отказ операции с работником. Выходит из транзакционного метода,
 * чтобы CDI пометил общую транзакцию для отката, включая предыдущие записи импорта.
 */
public class WorkerOperationException extends RuntimeException {
    private static final long serialVersionUID = 1L;

    private final Outcome outcome;
    private final String subject;
    private final Long subjectId;

    /**
     * Сохраняет причину отказа без зависимости от HTTP.
     *
     * @param refusal результат отклонённой операции
     */
    public WorkerOperationException(Change refusal) {
        super(message(refusal));
        outcome = refusal.getOutcome();
        subject = refusal.getSubject();
        subjectId = refusal.getSubjectId();
    }

    public Outcome getOutcome() {
        return outcome;
    }

    public String getSubject() {
        return subject;
    }

    public Long getSubjectId() {
        return subjectId;
    }

    private static String message(Change exception) {
        return switch (exception.getOutcome()) {
            case NOT_FOUND -> "Worker with id=" + exception.getSubjectId() + " does not exist.";
            case REFERENCE_NOT_FOUND -> missing(exception.getSubject(), exception.getSubjectId());
            case IN_USE -> "Worker with id=" + exception.getSubjectId()
                    + " is referenced by a person and cannot be deleted.";
            case CONFLICT -> conflict(exception);
            default -> throw new IllegalArgumentException("Expected a worker operation failure.");
        };
    }

    private static String conflict(Change exception) {
        return switch (String.valueOf(exception.getSubject())) {
            case "director" -> "Organization with id=" + exception.getSubjectId()
                    + " already has a hired director.";
            case "person" -> "Person with id=" + exception.getSubjectId()
                    + " is already assigned to another worker.";
            default -> "Another organization with this full_name already exists.";
        };
    }

    private static String missing(String subject, long id) {
        String template = switch (subject) {
            case "address" -> "Address with id=%d does not exist.";
            case "coordinates" -> "Coordinates with id=%d do not exist.";
            case "organization" -> "Organization with id=%d does not exist.";
            case "person" -> "Person with id=%d does not exist.";
            case "location" -> "Location with id=%d does not exist.";
            default -> throw new IllegalArgumentException("Unsupported reference: " + subject);
        };

        return template.formatted(id);
    }
}
