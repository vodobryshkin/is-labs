# Каждый сценарий запускается на отдельной очищенной тестовой БД.

GET {{base_url}}/auth/csrf
HTTP 200
[Captures]
csrf: jsonpath "$.csrf_token"
POST {{base_url}}/auth/register
X-CSRF-Token: {{csrf}}
{
    "username": "rollback_refusal_user",
    "password": "secret_1"
}
HTTP 201

POST {{base_url}}/auth/login
X-CSRF-Token: {{csrf}}
{
    "username": "rollback_refusal_user",
    "password": "secret_1"
}
HTTP 200

GET {{base_url}}/auth/csrf
HTTP 200
[Captures]
csrf: jsonpath "$.csrf_token"
# Успешное создание остаётся рабочим.
POST {{base_url}}/workers
X-CSRF-Token: {{csrf}}
{
    "name": "Rollback Worker",
    "coordinates": {
        "x": 1,
        "y": 1
    },
    "salary": 100,
    "rating": 1,
    "start_date": "2026-10-01T09:00:00Z",
    "position": null,
    "status": null,
    "person": {
        "eye_color": null,
        "hair_color": "BLUE",
        "location": {
            "x": 1,
            "y": 1,
            "z": 1
        },
        "birthday": "2000-01-01T00:00:00Z",
        "weight": 70,
        "nationality": "SPAIN"
    }
}
HTTP 201
[Captures]
id: jsonpath "$.id"
GET {{base_url}}/workers/{{id}}
HTTP 200
[Captures]
person_id: jsonpath "$.person.id"
coordinates_id: jsonpath "$.coordinates.id"
# Занятая персона: не должны появиться координаты, организация, адреса и города.
POST {{base_url}}/workers
X-CSRF-Token: {{csrf}}
{
    "name": "Refused Worker",
    "coordinates": {
        "x": 1,
        "y": 1
    },
    "salary": 100,
    "rating": 1,
    "start_date": "2026-10-01T09:00:00Z",
    "position": null,
    "status": null,
    "person": {
        "id": {{person_id}}
    },
    "organization": {
        "full_name": "Should Not Exist",
        "annual_turnover": 10,
        "employees_count": 1,
        "rating": 1,
        "type": null,
        "official_address": {
            "street": "Official",
            "town": {
                "x": 3,
                "y": 3,
                "z": 3
            }
        },
        "postal_address": {
            "street": "Postal",
            "town": {
                "x": 2,
                "y": 2,
                "z": 2
            }
        }
    }
}
HTTP 409
[Asserts]
header "Content-Type" contains "application/json"
jsonpath "$.message" == "Person with id={{person_id}} is already assigned to another worker."
GET {{base_url}}/workers
HTTP 200
[Asserts]
jsonpath "$.total_items" == 1
GET {{base_url}}/coordinates
HTTP 200
[Asserts]
jsonpath "$.total_items" == 1
GET {{base_url}}/persons
HTTP 200
[Asserts]
jsonpath "$.total_items" == 1
GET {{base_url}}/locations
HTTP 200
[Asserts]
jsonpath "$.total_items" == 1
GET {{base_url}}/addresses
HTTP 200
[Asserts]
jsonpath "$.total_items" == 0
GET {{base_url}}/organizations
HTTP 200
[Asserts]
jsonpath "$.total_items" == 0
# Неуспешное обновление сохраняет прежние поля и привязки.
PUT {{base_url}}/workers/{{id}}
X-CSRF-Token: {{csrf}}
{
    "name": "Should Not Change",
    "coordinates": {
        "x": 9,
        "y": 9
    },
    "salary": 999,
    "rating": 1,
    "start_date": "2026-10-01T09:00:00Z",
    "position": null,
    "status": null,
    "person": null,
    "organization_id": 2147483647
}
HTTP 404
[Asserts]
jsonpath "$.message" == "Organization with id=2147483647 does not exist."
GET {{base_url}}/workers/{{id}}
HTTP 200
[Asserts]
jsonpath "$.name" == "Rollback Worker"
jsonpath "$.salary" == 100
jsonpath "$.coordinates.id" == {{coordinates_id}}
jsonpath "$.person.id" == {{person_id}}
jsonpath "$.organization" == null
jsonpath "$.status" == null
POST {{base_url}}/workers/{{id}}/hire
X-CSRF-Token: {{csrf}}
{
    "organization_id": 2147483647
}
HTTP 404
[Asserts]
jsonpath "$.message" == "Organization with id=2147483647 does not exist."
DELETE {{base_url}}/workers/{{id}}
X-CSRF-Token: {{csrf}}
HTTP 409
[Asserts]
jsonpath "$.message" == "Worker with id={{id}} is referenced by a person and cannot be deleted."
GET {{base_url}}/workers/{{id}}
HTTP 200
[Asserts]
jsonpath "$.organization" == null
jsonpath "$.status" == null
GET {{base_url}}/workers
HTTP 200
[Asserts]
jsonpath "$.total_items" == 1
GET {{base_url}}/coordinates
HTTP 200
[Asserts]
jsonpath "$.total_items" == 1
GET {{base_url}}/persons
HTTP 200
[Asserts]
jsonpath "$.total_items" == 1
GET {{base_url}}/locations
HTTP 200
[Asserts]
jsonpath "$.total_items" == 1
GET {{base_url}}/addresses
HTTP 200
[Asserts]
jsonpath "$.total_items" == 0
GET {{base_url}}/organizations
HTTP 200
[Asserts]
jsonpath "$.total_items" == 0
# Успешные update/delete и ответы для отсутствующего работника.
PUT {{base_url}}/workers/{{id}}
X-CSRF-Token: {{csrf}}
{
    "name": "Rollback Worker",
    "coordinates": {
        "x": 1,
        "y": 1
    },
    "salary": 100,
    "rating": 1,
    "start_date": "2026-10-01T09:00:00Z",
    "position": null,
    "status": null,
    "person": null
}
HTTP 204

DELETE {{base_url}}/workers/{{id}}
X-CSRF-Token: {{csrf}}
HTTP 204

DELETE {{base_url}}/workers/{{id}}
X-CSRF-Token: {{csrf}}
HTTP 404
[Asserts]
jsonpath "$.message" == "Worker with id={{id}} does not exist."
PUT {{base_url}}/workers/{{id}}
X-CSRF-Token: {{csrf}}
{
    "name": "Rollback Worker",
    "coordinates": {
        "x": 1,
        "y": 1
    },
    "salary": 100,
    "rating": 1,
    "start_date": "2026-10-01T09:00:00Z",
    "position": null,
    "status": null,
    "person": null
}
HTTP 404

POST {{base_url}}/workers/{{id}}/hire
X-CSRF-Token: {{csrf}}
{
    "organization_id": 2147483647
}
HTTP 404

