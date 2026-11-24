POST {{base_url}}/imports
Content-Type: application/octet-stream
file,files/success.json;
HTTP 401

GET {{base_url}}/auth/csrf
HTTP 200
[Captures]
csrf: jsonpath "$.csrf_token"
POST {{base_url}}/auth/register
X-CSRF-Token: {{csrf}}
{
    "username": "import_access_user",
    "password": "secret_1"
}
HTTP 201
[Captures]
user_id: jsonpath "$.id"
POST {{base_url}}/auth/login
X-CSRF-Token: {{csrf}}
{
    "username": "import_access_user",
    "password": "secret_1"
}
HTTP 200

GET {{base_url}}/auth/csrf
HTTP 200
[Captures]
csrf: jsonpath "$.csrf_token"
POST {{base_url}}/imports
Content-Type: application/octet-stream
file,files/success.json;
HTTP 403

GET {{base_url}}/imports
HTTP 200
[Asserts]
jsonpath "$.total_items" == 0
POST {{base_url}}/imports
X-CSRF-Token: {{csrf}}
Content-Type: application/octet-stream
file,files/success.json;
HTTP 201

POST {{base_url}}/auth/logout
X-CSRF-Token: {{csrf}}
HTTP 204

GET {{base_url}}/auth/csrf
HTTP 200
[Captures]
csrf: jsonpath "$.csrf_token"
POST {{base_url}}/auth/register
X-CSRF-Token: {{csrf}}
{
    "username": "import_other_user",
    "password": "secret_1"
}
HTTP 201
[Captures]
user_id: jsonpath "$.id"
POST {{base_url}}/auth/login
X-CSRF-Token: {{csrf}}
{
    "username": "import_other_user",
    "password": "secret_1"
}
HTTP 200

GET {{base_url}}/auth/csrf
HTTP 200
[Captures]
csrf: jsonpath "$.csrf_token"
GET {{base_url}}/imports?username=import_access_user&admin=true
HTTP 200
[Asserts]
jsonpath "$.total_items" == 0
