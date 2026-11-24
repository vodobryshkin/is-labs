GET {{base_url}}/auth/csrf
HTTP 200
[Captures]
csrf: jsonpath "$.csrf_token"
POST {{base_url}}/auth/register
X-CSRF-Token: {{csrf}}
{
    "username": "hired_director_user",
    "password": "secret_1"
}
HTTP 201

POST {{base_url}}/auth/login
X-CSRF-Token: {{csrf}}
{
    "username": "hired_director_user",
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
    "full_name": "First Director Company",
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
    "full_name": "Second Director Company",
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
POST {{base_url}}/workers
X-CSRF-Token: {{csrf}}
{
    "name": "First Director",
    "coordinates": {
        "x": 1,
        "y": 1
    },
    "salary": 100,
    "rating": 1,
    "start_date": "2026-10-01T09:00:00Z",
    "position": "DIRECTOR",
    "status": "HIRED",
    "person": null,
    "organization_id": {{organization_id}}
}
HTTP 201
[Captures]
director_id: jsonpath "$.id"
POST {{base_url}}/workers
X-CSRF-Token: {{csrf}}
{
    "name": "Second Director",
    "coordinates": {
        "x": 1,
        "y": 1
    },
    "salary": 100,
    "rating": 1,
    "start_date": "2026-10-01T09:00:00Z",
    "position": "DIRECTOR",
    "status": "HIRED",
    "person": null,
    "organization_id": {{organization_id}}
}
HTTP 409
[Asserts]
jsonpath "$.message" == "Organization with id={{organization_id}} already has a hired director."
GET {{base_url}}/workers
HTTP 200
[Asserts]
jsonpath "$.total_items" == 1
GET {{base_url}}/coordinates
HTTP 200
[Asserts]
jsonpath "$.total_items" == 1
POST {{base_url}}/workers
X-CSRF-Token: {{csrf}}
{
    "name": "Candidate",
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
    "organization_id": {{organization_id}}
}
HTTP 201
[Captures]
candidate_id: jsonpath "$.id"
GET {{base_url}}/workers/{{candidate_id}}
HTTP 200
[Captures]
candidate_coordinates_id: jsonpath "$.coordinates.id"
POST {{base_url}}/workers/{{candidate_id}}/hire
X-CSRF-Token: {{csrf}}
{
    "organization_id": {{organization_id}}
}
HTTP 409

PUT {{base_url}}/workers/{{candidate_id}}
X-CSRF-Token: {{csrf}}
{
    "name": "Changed Candidate",
    "coordinates": {
        "x": 1,
        "y": 1
    },
    "salary": 100,
    "rating": 1,
    "start_date": "2026-10-01T09:00:00Z",
    "position": "DIRECTOR",
    "status": "HIRED",
    "person": null,
    "organization_id": {{organization_id}}
}
HTTP 409

GET {{base_url}}/workers/{{candidate_id}}
HTTP 200
[Asserts]
jsonpath "$.name" == "Candidate"
jsonpath "$.status" == "REGULAR"
jsonpath "$.coordinates.id" == {{candidate_coordinates_id}}
POST {{base_url}}/workers/{{director_id}}/hire
X-CSRF-Token: {{csrf}}
{
    "organization_id": {{organization_id}}
}
HTTP 204

PUT {{base_url}}/workers/{{director_id}}
X-CSRF-Token: {{csrf}}
{
    "name": "Renamed Director",
    "coordinates": {
        "x": 1,
        "y": 1
    },
    "salary": 100,
    "rating": 1,
    "start_date": "2026-10-01T09:00:00Z",
    "position": "DIRECTOR",
    "status": "HIRED",
    "person": null,
    "organization_id": {{organization_id}}
}
HTTP 204

POST {{base_url}}/workers
X-CSRF-Token: {{csrf}}
{
    "name": "Other Organization Director",
    "coordinates": {
        "x": 1,
        "y": 1
    },
    "salary": 100,
    "rating": 1,
    "start_date": "2026-10-01T09:00:00Z",
    "position": "DIRECTOR",
    "status": "HIRED",
    "person": null,
    "organization_id": {{other_id}}
}
HTTP 201

POST {{base_url}}/workers
X-CSRF-Token: {{csrf}}
{
    "name": "No Organization Director",
    "coordinates": {
        "x": 1,
        "y": 1
    },
    "salary": 100,
    "rating": 1,
    "start_date": "2026-10-01T09:00:00Z",
    "position": "DIRECTOR",
    "status": "HIRED",
    "person": null
}
HTTP 201

POST {{base_url}}/workers
X-CSRF-Token: {{csrf}}
{
    "name": "Hired Developer",
    "coordinates": {
        "x": 1,
        "y": 1
    },
    "salary": 100,
    "rating": 1,
    "start_date": "2026-10-01T09:00:00Z",
    "position": "LEAD_DEVELOPER",
    "status": "HIRED",
    "person": null,
    "organization_id": {{organization_id}}
}
HTTP 201

POST {{base_url}}/workers
X-CSRF-Token: {{csrf}}
{
    "name": "Director Without Status",
    "coordinates": {
        "x": 1,
        "y": 1
    },
    "salary": 100,
    "rating": 1,
    "start_date": "2026-10-01T09:00:00Z",
    "position": "DIRECTOR",
    "status": null,
    "person": null,
    "organization_id": {{organization_id}}
}
HTTP 201

PUT {{base_url}}/workers/{{director_id}}
X-CSRF-Token: {{csrf}}
{
    "name": "Former Director",
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
    "organization_id": {{organization_id}}
}
HTTP 204

POST {{base_url}}/workers/{{candidate_id}}/hire
X-CSRF-Token: {{csrf}}
{
    "organization_id": {{organization_id}}
}
HTTP 204

POST {{base_url}}/workers/{{candidate_id}}/hire
X-CSRF-Token: {{csrf}}
{
    "organization_id": {{other_id}}
}
HTTP 409

GET {{base_url}}/workers/{{candidate_id}}
HTTP 200
[Asserts]
jsonpath "$.organization.id" == {{organization_id}}
jsonpath "$.status" == "HIRED"
