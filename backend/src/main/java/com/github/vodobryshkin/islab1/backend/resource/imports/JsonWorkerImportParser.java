package com.github.vodobryshkin.islab1.backend.resource.imports;

import com.github.vodobryshkin.islab1.backend.dto.worker.WorkerRequest;
import com.github.vodobryshkin.islab1.backend.resource.StrictDeserializer;
import com.github.vodobryshkin.islab1.backend.service.imports.ImportFailure;
import com.github.vodobryshkin.islab1.backend.service.imports.WorkerImportParser;
import com.github.vodobryshkin.islab1.backend.service.imports.WorkerImportValidator;
import jakarta.annotation.PreDestroy;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.json.Json;
import jakarta.json.JsonException;
import jakarta.json.bind.Jsonb;
import jakarta.json.bind.JsonbBuilder;
import jakarta.json.bind.JsonbConfig;
import jakarta.json.bind.JsonbException;
import jakarta.json.stream.JsonParser;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

/**
 * Читает массив по одной записи; типы чисел и строк проверяются так же строго, как в CRUD.
 */
@ApplicationScoped
public class JsonWorkerImportParser implements WorkerImportParser {
    private final Jsonb jsonb = JsonbBuilder.create(new JsonbConfig()
            .withDeserializers(new StrictDeserializer.Text(), new StrictDeserializer.Int32(),
                    new StrictDeserializer.Int64(), new StrictDeserializer.Float32(),
                    new StrictDeserializer.Float64()));

    @Override
    public String format() {
        return "json";
    }

    @Override
    public List<WorkerRequest> parse(InputStream input) {
        List<WorkerRequest> records = new ArrayList<>();
        Integer record = null;
        try (JsonParser parser = Json.createParser(input)) {
            if (!parser.hasNext() || parser.next() != JsonParser.Event.START_ARRAY) {
                throw new ImportFailure(ImportFailure.Kind.FORMAT,
                        "The import file must contain a JSON array.", null);
            }
            while (parser.hasNext()) {
                record = records.size() + 1;
                JsonParser.Event event = parser.next();
                if (event == JsonParser.Event.END_ARRAY) {
                    record = null;
                    if (parser.hasNext()) {
                        throw new ImportFailure(ImportFailure.Kind.FORMAT,
                                "Unexpected content after the JSON array.", null);
                    }
                    return List.copyOf(records);
                }
                if (event != JsonParser.Event.START_OBJECT) {
                    throw new ImportFailure(ImportFailure.Kind.FORMAT,
                            "Each import record must be a JSON object.", record);
                }
                if (records.size() >= WorkerImportValidator.MAX_RECORDS) {
                    throw new ImportFailure(ImportFailure.Kind.TOO_LARGE,
                            "The file must not contain more than 10000 records.", record);
                }
                records.add(jsonb.fromJson(parser.getObject().toString(), WorkerRequest.class));
            }
            throw new ImportFailure(ImportFailure.Kind.FORMAT,
                    "The JSON array is not complete.", record);
        } catch (JsonException | JsonbException exception) {
            throw new ImportFailure(ImportFailure.Kind.FORMAT,
                    "The file contains invalid JSON or a value of the wrong type.",
                    record, List.of(), exception);
        }
    }

    @PreDestroy
    public void close() {
        try {
            jsonb.close();
        } catch (Exception exception) {
            throw new IllegalStateException("Could not close the JSON import parser.", exception);
        }
    }
}
