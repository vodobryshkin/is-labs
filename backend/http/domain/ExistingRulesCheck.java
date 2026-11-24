import com.github.vodobryshkin.islab1.backend.service.OrganizationNames;
import java.sql.Connection;
import java.sql.DriverManager;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Проверка существующих данных без изменения строк. Использует нормализатор приложения.
 */
public class ExistingRulesCheck {
    public static void main(String[] args) throws Exception {
        String url = required("IS_LAB1_DB_URL");
        String user = required("IS_LAB1_DB_USER");
        String password = System.getenv("IS_LAB1_DB_PASSWORD");
        int conflicts = 0;
        try (Connection connection = DriverManager.getConnection(url, user, password)) {
            connection.setReadOnly(true);
            connection.setTransactionIsolation(Connection.TRANSACTION_REPEATABLE_READ);
            connection.setAutoCommit(false);
            Map<String, List<Long>> names = new LinkedHashMap<>();
            try (var statement = connection.createStatement();
                    var rows = statement.executeQuery(
                            "SELECT organization_id, full_name FROM organizations ORDER BY organization_id")) {
                while (rows.next()) {
                    names.computeIfAbsent(OrganizationNames.normalize(rows.getString(2)),
                            key -> new ArrayList<>()).add(rows.getLong(1));
                }
            }
            for (var entry : names.entrySet()) {
                if (entry.getValue().size() > 1) {
                    conflicts++;
                    System.out.println("Duplicate organization name: " + entry.getKey()
                            + "; ids=" + entry.getValue());
                }
            }
            try (var statement = connection.createStatement();
                    var rows = statement.executeQuery("SELECT organization_id, COUNT(*) FROM workers"
                            + " WHERE organization_id IS NOT NULL AND position = 'DIRECTOR' AND status = 'HIRED'"
                            + " GROUP BY organization_id HAVING COUNT(*) > 1 ORDER BY organization_id")) {
                while (rows.next()) {
                    conflicts++;
                    System.out.println("Multiple hired directors: organization_id=" + rows.getLong(1)
                            + "; count=" + rows.getLong(2));
                }
            }
            connection.rollback();
        }
        System.out.println("Conflicting groups: " + conflicts);
        if (conflicts > 0) {
            System.exit(1);
        }
    }

    private static String required(String name) {
        String value = System.getenv(name);
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException("Set " + name);
        }
        return value;
    }
}
