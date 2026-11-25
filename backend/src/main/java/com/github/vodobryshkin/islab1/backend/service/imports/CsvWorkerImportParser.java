package com.github.vodobryshkin.islab1.backend.service.imports;

import com.github.vodobryshkin.islab1.backend.domain.Color;
import com.github.vodobryshkin.islab1.backend.domain.Country;
import com.github.vodobryshkin.islab1.backend.domain.OrganizationType;
import com.github.vodobryshkin.islab1.backend.domain.Position;
import com.github.vodobryshkin.islab1.backend.domain.Status;
import com.github.vodobryshkin.islab1.backend.dto.address.AddressRequest;
import com.github.vodobryshkin.islab1.backend.dto.corrdinates.CoordinatesRequest;
import com.github.vodobryshkin.islab1.backend.dto.location.LocationRequest;
import com.github.vodobryshkin.islab1.backend.dto.organization.OrganizationRequest;
import com.github.vodobryshkin.islab1.backend.dto.person.PersonRequest;
import com.github.vodobryshkin.islab1.backend.dto.worker.WorkerRequest;
import jakarta.enterprise.context.ApplicationScoped;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.PushbackInputStream;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.List;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVRecord;

@ApplicationScoped
public class CsvWorkerImportParser implements WorkerImportParser {

    @Override
    public String format() {
        return "csv";
    }

    @Override
    public List<WorkerRequest> parse(InputStream input) {
        try (Reader reader = reader(input);
             CSVParser parser = CSVFormat.DEFAULT.builder()
                     .setHeader()
                     .setSkipHeaderRecord(true)
                     .setTrim(true)
                     .build()
                     .parse(reader)) {
            List<WorkerRequest> records = new ArrayList<>();
            int row = 1;
            for (CSVRecord csvRecord : parser) {
                row++;
                try {
                    records.add(mapRecord(csvRecord));
                } catch (Exception e) {
                    throw new ImportFailure(ImportFailure.Kind.FORMAT,
                            "Failed to parse CSV row " + row + ".", row, List.of(), e);
                }
            }
            return records;
        } catch (ImportFailure e) {
            throw e;
        } catch (Exception e) {
            throw new ImportFailure(ImportFailure.Kind.FORMAT,
                    "The CSV file could not be read.", null, List.of(), e);
        }
    }

    private Reader reader(InputStream input) throws IOException {
        PushbackInputStream pushback = new PushbackInputStream(input, 3);
        byte[] bom = new byte[3];
        int read = pushback.read(bom, 0, 3);
        if (read < 3 || bom[0] != (byte) 0xEF || bom[1] != (byte) 0xBB || bom[2] != (byte) 0xBF) {
            if (read > 0) {
                pushback.unread(bom, 0, read);
            }
        }
        return new InputStreamReader(pushback, StandardCharsets.UTF_8);
    }

    private WorkerRequest mapRecord(CSVRecord record) {
        WorkerRequest worker = new WorkerRequest();
        worker.setName(get(record, "name"));

        CoordinatesRequest coordinates = new CoordinatesRequest();
        coordinates.setX(parseInteger(get(record, "coordinates_x")));
        coordinates.setY(parseLong(get(record, "coordinates_y")));
        worker.setCoordinates(coordinates);

        worker.setSalary(parseDouble(get(record, "salary")));
        worker.setRating(parseInteger(get(record, "rating")));
        worker.setStartDate(parseZonedDateTime(get(record, "start_date")));
        worker.setPosition(parseEnum(Position.class, get(record, "position")));
        worker.setStatus(parseEnum(Status.class, get(record, "status")));

        String hairColor = get(record, "person_hair_color");
        if (hairColor != null) {
            PersonRequest person = new PersonRequest();
            person.setEyeColor(parseEnum(Color.class, get(record, "person_eye_color")));
            person.setHairColor(parseEnum(Color.class, hairColor));

            LocationRequest personLocation = new LocationRequest();
            personLocation.setX(parseInteger(get(record, "person_location_x")));
            personLocation.setY(parseInteger(get(record, "person_location_y")));
            personLocation.setZ(parseLong(get(record, "person_location_z")));
            person.setLocation(personLocation);

            person.setBirthday(parseZonedDateTime(get(record, "person_birthday")));
            person.setWeight(parseFloat(get(record, "person_weight")));
            person.setNationality(parseEnum(Country.class, get(record, "person_nationality")));
            worker.setPerson(person);
        }

        String orgName = get(record, "organization_full_name");
        if (orgName != null) {
            OrganizationRequest organization = new OrganizationRequest();
            organization.setFullName(orgName);
            organization.setAnnualTurnover(parseFloat(get(record, "organization_annual_turnover")));
            organization.setEmployeesCount(parseLong(get(record, "organization_employees_count")));
            organization.setRating(parseInteger(get(record, "organization_rating")));
            organization.setType(parseEnum(OrganizationType.class, get(record, "organization_type")));

            AddressRequest official = new AddressRequest();
            official.setStreet(get(record, "organization_official_address_street"));
            String officialTownX = get(record, "organization_official_address_town_x");
            if (officialTownX != null) {
                LocationRequest town = new LocationRequest();
                town.setX(parseInteger(officialTownX));
                town.setY(parseInteger(get(record, "organization_official_address_town_y")));
                town.setZ(parseLong(get(record, "organization_official_address_town_z")));
                official.setTown(town);
            }
            organization.setOfficialAddress(official);

            AddressRequest postal = new AddressRequest();
            postal.setStreet(get(record, "organization_postal_address_street"));
            String postalTownX = get(record, "organization_postal_address_town_x");
            if (postalTownX != null) {
                LocationRequest town = new LocationRequest();
                town.setX(parseInteger(postalTownX));
                town.setY(parseInteger(get(record, "organization_postal_address_town_y")));
                town.setZ(parseLong(get(record, "organization_postal_address_town_z")));
                postal.setTown(town);
            }
            organization.setPostalAddress(postal);

            worker.setOrganization(organization);
        }

        return worker;
    }

    private String get(CSVRecord record, String column) {
        try {
            String value = record.get(column);
            return value != null && !value.isEmpty() ? value : null;
        } catch (IllegalArgumentException e) {
            return null;
        }
    }

    private Integer parseInteger(String value) {
        return value == null ? null : Integer.parseInt(value);
    }

    private Long parseLong(String value) {
        return value == null ? null : Long.parseLong(value);
    }

    private Double parseDouble(String value) {
        return value == null ? null : Double.parseDouble(value);
    }

    private Float parseFloat(String value) {
        return value == null ? null : Float.parseFloat(value);
    }

    private ZonedDateTime parseZonedDateTime(String value) {
        return value == null ? null : ZonedDateTime.parse(value);
    }

    private <E extends Enum<E>> E parseEnum(Class<E> enumClass, String value) {
        return value == null ? null : Enum.valueOf(enumClass, value);
    }
}