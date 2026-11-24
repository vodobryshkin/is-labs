package com.github.vodobryshkin.islab1.backend.service.imports;

import com.github.vodobryshkin.islab1.backend.dto.address.AddressRequest;
import com.github.vodobryshkin.islab1.backend.dto.common.ValidationIssue;
import com.github.vodobryshkin.islab1.backend.dto.location.LocationRequest;
import com.github.vodobryshkin.islab1.backend.dto.organization.OrganizationRequest;
import com.github.vodobryshkin.islab1.backend.dto.worker.WorkerRequest;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validator;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.regex.Pattern;

/**
 * Общая для всех форматов проверка DTO и обязательных полных вложенных данных.
 */
@ApplicationScoped
public class WorkerImportValidator {
    public static final int MAX_RECORDS = 10000;
    private static final Pattern SNAKE_CASE = Pattern.compile("([a-z0-9])([A-Z])");
    private final Validator validator;

    @Inject
    public WorkerImportValidator(Validator validator) {
        this.validator = validator;
    }

    protected WorkerImportValidator() {
        this(null);
    }

    public void validate(List<WorkerRequest> records) {
        if (records.isEmpty()) {
            throw new ImportFailure(ImportFailure.Kind.VALIDATION,
                    "The file must contain at least one worker.", null);
        }
        if (records.size() > MAX_RECORDS) {
            throw new ImportFailure(ImportFailure.Kind.TOO_LARGE,
                    "The file must not contain more than 10000 records.", null);
        }
        for (int i = 0; i < records.size(); i++) {
            WorkerRequest record = records.get(i);
            if (record == null) {
                throw new ImportFailure(ImportFailure.Kind.VALIDATION,
                        "An import record must not be null.", i + 1);
            }
            List<ValidationIssue> issues = new ArrayList<>(validator.validate(record).stream()
                    .map(this::issue).toList());
            references(record, issues);
            issues.sort(Comparator.comparing(ValidationIssue::getField)
                    .thenComparing(ValidationIssue::getCode));
            if (!issues.isEmpty()) {
                throw new ImportFailure(ImportFailure.Kind.VALIDATION,
                        "The import record contains invalid fields.", i + 1, issues, null);
            }
        }
    }

    private ValidationIssue issue(ConstraintViolation<?> violation) {
        String field = SNAKE_CASE.matcher(violation.getPropertyPath().toString()
                .replaceAll("Valid$", "")).replaceAll("$1_$2").toLowerCase(Locale.ROOT);
        String code = switch (violation.getConstraintDescriptor().getAnnotation()
                .annotationType().getSimpleName()) {
            case "NotNull" -> "MUST_NOT_BE_NULL";
            case "NotBlank", "NotEmpty" -> "MUST_NOT_BE_EMPTY";
            case "Positive" -> "MUST_BE_POSITIVE";
            case "Pattern" -> "MUST_MATCH_PATTERN";
            case "AssertTrue" -> "INVALID_COMBINATION";
            default -> "OUT_OF_RANGE";
        };
        return new ValidationIssue(field, code, violation.getMessage());
    }

    private void references(WorkerRequest worker, List<ValidationIssue> issues) {
        reject(worker.getOrganizationId(), "organization_id", issues);
        if (worker.getCoordinates() != null) {
            reject(worker.getCoordinates().getId(), "coordinates.id", issues);
        }
        if (worker.getPerson() != null) {
            reject(worker.getPerson().getId(), "person.id", issues);
            location(worker.getPerson().getLocation(), "person.location", issues);
        }
        OrganizationRequest organization = worker.getOrganization();
        if (organization != null) {
            reject(organization.getOfficialAddressId(), "organization.official_address_id", issues);
            reject(organization.getPostalAddressId(), "organization.postal_address_id", issues);
            address(organization.getOfficialAddress(), "organization.official_address", issues);
            address(organization.getPostalAddress(), "organization.postal_address", issues);
        }
    }

    private void address(AddressRequest address, String path, List<ValidationIssue> issues) {
        if (address != null) {
            location(address.getTown(), path + ".town", issues);
        }
    }

    private void location(LocationRequest location, String path, List<ValidationIssue> issues) {
        if (location != null) {
            reject(location.getId(), path + ".id", issues);
        }
    }

    private void reject(Long id, String field, List<ValidationIssue> issues) {
        if (id != null) {
            issues.add(new ValidationIssue(field, "REFERENCE_NOT_ALLOWED",
                    "Provide nested object fields in this record instead of an ID reference."));
        }
    }
}
