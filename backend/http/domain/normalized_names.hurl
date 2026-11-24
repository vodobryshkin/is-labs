GET {{base_url}}/auth/csrf
HTTP 200
[Captures]
csrf: jsonpath "$.csrf_token"
POST {{base_url}}/auth/register
X-CSRF-Token: {{csrf}}
{
    "username": "normalized_names_user",
    "password": "secret_1"
}
HTTP 201

POST {{base_url}}/auth/login
X-CSRF-Token: {{csrf}}
{
    "username": "normalized_names_user",
    "password": "secret_1"
}
HTTP 200

GET {{base_url}}/auth/csrf
HTTP 200
[Captures]
csrf: jsonpath "$.csrf_token"
POST {{base_url}}/organizations
X-CSRF-Token: {{csrf}}
{
    "full_name": "ООО «Вектор»",
    "annual_turnover": 100,
    "employees_count": 10,
    "rating": 1,
    "type": null,
    "official_address": {
        "street": "Official",
        "town": null
    },
    "postal_address": {
        "street": "Postal",
        "town": null
    }
}
HTTP 201
[Captures]
organization_id: jsonpath "$.id"
POST {{base_url}}/organizations
X-CSRF-Token: {{csrf}}
{
    "full_name": "  ооо   \"вектор\"  ",
    "annual_turnover": 100,
    "employees_count": 10,
    "rating": 1,
    "type": null,
    "official_address": {
        "street": "Official",
        "town": null
    },
    "postal_address": {
        "street": "Postal",
        "town": null
    }
}
HTTP 409
[Asserts]
jsonpath "$.message" == "Another organization with this full_name already exists."
GET {{base_url}}/organizations
HTTP 200
[Asserts]
jsonpath "$.total_items" == 1
GET {{base_url}}/addresses
HTTP 200
[Asserts]
jsonpath "$.total_items" == 2
GET {{base_url}}/organizations/{{organization_id}}
HTTP 200
[Asserts]
jsonpath "$.full_name" == "ООО «Вектор»"
PUT {{base_url}}/organizations/{{organization_id}}
X-CSRF-Token: {{csrf}}
{
    "full_name": " ооо \"вектор\" ",
    "annual_turnover": 100,
    "employees_count": 10,
    "rating": 1,
    "type": null,
    "official_address": {
        "street": "Official",
        "town": null
    },
    "postal_address": {
        "street": "Postal",
        "town": null
    }
}
HTTP 204

POST {{base_url}}/organizations
X-CSRF-Token: {{csrf}}
{
    "full_name": "ООО «Другой»",
    "annual_turnover": 100,
    "employees_count": 10,
    "rating": 1,
    "type": null,
    "official_address": {
        "street": "Official",
        "town": null
    },
    "postal_address": {
        "street": "Postal",
        "town": null
    }
}
HTTP 201
[Captures]
other_id: jsonpath "$.id"
PUT {{base_url}}/organizations/{{other_id}}
X-CSRF-Token: {{csrf}}
{
    "full_name": "ООО  «ВЕКТОР»",
    "annual_turnover": 100,
    "employees_count": 10,
    "rating": 1,
    "type": null,
    "official_address": {
        "street": "Official",
        "town": null
    },
    "postal_address": {
        "street": "Postal",
        "town": null
    }
}
HTTP 409

GET {{base_url}}/organizations/{{other_id}}
HTTP 200
[Asserts]
jsonpath "$.full_name" == "ООО «Другой»"
POST {{base_url}}/workers
X-CSRF-Token: {{csrf}}
{
    "name": "Nested Conflict",
    "coordinates": {
        "x": 1,
        "y": 1
    },
    "salary": 100,
    "rating": 1,
    "start_date": "2026-10-01T09:00:00Z",
    "position": "DIRECTOR",
    "status": "REGULAR",
    "person": null,
    "organization": {
        "full_name": " ООО «ВЕКТОР» ",
        "annual_turnover": 100,
        "employees_count": 10,
        "rating": 1,
        "type": null,
        "official_address": {
            "street": "Official",
            "town": null
        },
        "postal_address": {
            "street": "Postal",
            "town": null
        }
    }
}
HTTP 409

GET {{base_url}}/organizations
HTTP 200
[Asserts]
jsonpath "$.total_items" == 2
GET {{base_url}}/addresses
HTTP 200
[Asserts]
jsonpath "$.total_items" == 6
GET {{base_url}}/workers
HTTP 200
[Asserts]
jsonpath "$.total_items" == 0
GET {{base_url}}/coordinates
HTTP 200
[Asserts]
jsonpath "$.total_items" == 0
GET {{base_url}}/locations
HTTP 200
[Asserts]
jsonpath "$.total_items" == 0
GET {{base_url}}/persons
HTTP 200
[Asserts]
jsonpath "$.total_items" == 0
