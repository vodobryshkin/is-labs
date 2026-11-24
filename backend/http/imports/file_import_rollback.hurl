GET {{base_url}}/auth/csrf
HTTP 200
[Captures]
csrf: jsonpath "$.csrf_token"
POST {{base_url}}/auth/register
X-CSRF-Token: {{csrf}}
{
    "username": "import_rollback_user",
    "password": "secret_1"
}
HTTP 201
[Captures]
user_id: jsonpath "$.id"
POST {{base_url}}/auth/login
X-CSRF-Token: {{csrf}}
{
    "username": "import_rollback_user",
    "password": "secret_1"
}
HTTP 200

GET {{base_url}}/auth/csrf
HTTP 200
[Captures]
csrf: jsonpath "$.csrf_token"
POST {{base_url}}/imports
X-CSRF-Token: {{csrf}}
Content-Type: application/octet-stream
file,files/duplicate-name.json;
HTTP 409
[Captures]
operation_id: jsonpath "$.operation_id"
[Asserts]
jsonpath "$.status" == "FAILED"
jsonpath "$.record" == 2
jsonpath "$.added_count" == null
jsonpath "$.message" == "Another organization with this full_name already exists."
GET {{base_url}}/workers
HTTP 200
[Asserts]
jsonpath "$.total_items" == 0
GET {{base_url}}/coordinates
HTTP 200
[Asserts]
jsonpath "$.total_items" == 0
GET {{base_url}}/organizations
HTTP 200
[Asserts]
jsonpath "$.total_items" == 0
GET {{base_url}}/addresses
HTTP 200
[Asserts]
jsonpath "$.total_items" == 0
GET {{base_url}}/persons
HTTP 200
[Asserts]
jsonpath "$.total_items" == 0
GET {{base_url}}/locations
HTTP 200
[Asserts]
jsonpath "$.total_items" == 0
GET {{base_url}}/imports
HTTP 200
[Asserts]
jsonpath "$.total_items" == 1
jsonpath "$.items[0].id" == {{operation_id}}
jsonpath "$.items[0].status" == "FAILED"
jsonpath "$.items[0].added_count" == null
POST {{base_url}}/imports
X-CSRF-Token: {{csrf}}
Content-Type: application/octet-stream
file,files/success.json;
HTTP 201
[Asserts]
jsonpath "$.added_count" == 2
GET {{base_url}}/workers
HTTP 200
[Asserts]
jsonpath "$.total_items" == 2
GET {{base_url}}/coordinates
HTTP 200
[Asserts]
jsonpath "$.total_items" == 2
GET {{base_url}}/organizations
HTTP 200
[Asserts]
jsonpath "$.total_items" == 1
GET {{base_url}}/addresses
HTTP 200
[Asserts]
jsonpath "$.total_items" == 2
GET {{base_url}}/persons
HTTP 200
[Asserts]
jsonpath "$.total_items" == 1
GET {{base_url}}/locations
HTTP 200
[Asserts]
jsonpath "$.total_items" == 3
GET {{base_url}}/imports
HTTP 200
[Asserts]
jsonpath "$.total_items" == 2
jsonpath "$.items[0].status" == "FAILED"
jsonpath "$.items[1].status" == "SUCCESS"
