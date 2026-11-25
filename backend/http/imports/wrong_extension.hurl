GET {{base_url}}/auth/csrf
HTTP 200
[Captures]
csrf: jsonpath "$.csrf_token"

POST {{base_url}}/auth/register
X-CSRF-Token: {{csrf}}
{
    "username": "wrong_ext_user",
    "password": "secret_1"
}
HTTP 201

POST {{base_url}}/auth/login
X-CSRF-Token: {{csrf}}
{
    "username": "wrong_ext_user",
    "password": "secret_1"
}
HTTP 200

GET {{base_url}}/auth/csrf
HTTP 200
[Captures]
csrf: jsonpath "$.csrf_token"

POST {{base_url}}/imports?format=json
X-CSRF-Token: {{csrf}}
Content-Type: application/octet-stream
file, data/json_payload.txt;
HTTP 201
[Asserts]
jsonpath "$.status" == "SUCCESS"
jsonpath "$.added_count" == 1

POST {{base_url}}/imports?format=csv
X-CSRF-Token: {{csrf}}
Content-Type: application/octet-stream
file, data/csv_payload.json;
HTTP 201
[Asserts]
jsonpath "$.status" == "SUCCESS"
jsonpath "$.added_count" == 1

GET {{base_url}}/workers
HTTP 200
[Captures]
workers_ok: jsonpath "$.total_items"

POST {{base_url}}/imports?format=json
X-CSRF-Token: {{csrf}}
Content-Type: application/octet-stream
file, data/csv_payload.json;
HTTP 400
[Asserts]
jsonpath "$.status" == "FAILED"

POST {{base_url}}/imports?format=csv
X-CSRF-Token: {{csrf}}
Content-Type: application/octet-stream
file, data/json_payload.txt;
HTTP 400
[Asserts]
jsonpath "$.status" == "FAILED"

POST {{base_url}}/imports?format=xml
X-CSRF-Token: {{csrf}}
Content-Type: application/octet-stream
file, data/json_payload.txt;
HTTP 400
[Asserts]
jsonpath "$.status" == "FAILED"
jsonpath "$.message" contains "Unsupported import format"

GET {{base_url}}/workers
HTTP 200
[Asserts]
jsonpath "$.total_items" == {{workers_ok}}

GET {{base_url}}/imports
HTTP 200
[Asserts]
jsonpath "$.total_items" == 5

POST {{base_url}}/imports
X-CSRF-Token: {{csrf}}
Content-Type: application/octet-stream
file, data/json_payload.dat;
HTTP 200
[Asserts]
jsonpath "$.status" == "SUCCESS"
jsonpath "$.added_count" == 1

GET {{base_url}}/workers
HTTP 200
[Asserts]
jsonpath "$.total_items" > {{workers_ok}}