GET {{base_url}}/auth/csrf
HTTP 200
[Captures]
csrf: jsonpath "$.csrf_token"
POST {{base_url}}/auth/register
X-CSRF-Token: {{csrf}}
{
    "username": "import_validation_user",
    "password": "secret_1"
}
HTTP 201
[Captures]
user_id: jsonpath "$.id"
POST {{base_url}}/auth/login
X-CSRF-Token: {{csrf}}
{
    "username": "import_validation_user",
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
file,files/invalid-fields.json;
HTTP 422
[Asserts]
jsonpath "$.status" == "FAILED"
jsonpath "$.added_count" == null
jsonpath "$.record" == 2
jsonpath "$.errors[0].field" == "salary"
POST {{base_url}}/imports
X-CSRF-Token: {{csrf}}
Content-Type: application/octet-stream
file,files/id-reference.json;
HTTP 422
[Asserts]
jsonpath "$.status" == "FAILED"
jsonpath "$.added_count" == null
jsonpath "$.record" == 2
POST {{base_url}}/imports
X-CSRF-Token: {{csrf}}
Content-Type: application/octet-stream
file,files/malformed.json;
HTTP 400
[Asserts]
jsonpath "$.status" == "FAILED"
jsonpath "$.added_count" == null
jsonpath "$.record" == 2
POST {{base_url}}/imports
X-CSRF-Token: {{csrf}}
Content-Type: application/octet-stream
file,files/empty.json;
HTTP 422
[Asserts]
jsonpath "$.status" == "FAILED"
jsonpath "$.added_count" == null
jsonpath "$.record" == null
POST {{base_url}}/imports?format=csv
X-CSRF-Token: {{csrf}}
Content-Type: application/octet-stream
file,files/success.json;
HTTP 415
[Asserts]
jsonpath "$.status" == "FAILED"
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
jsonpath "$.total_items" == 5
jsonpath "$.items[0].status" == "FAILED"
jsonpath "$.items[4].status" == "FAILED"
