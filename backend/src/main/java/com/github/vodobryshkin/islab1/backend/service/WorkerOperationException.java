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
        super("Worker operation refused: " + refusal.getOutcome()
                + ", subject=" + refusal.getSubject() + ", id=" + refusal.getSubjectId());
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
}
