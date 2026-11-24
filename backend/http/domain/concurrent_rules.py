#!/usr/bin/env python3
"""Проверка двух независимых HTTP-сессий. Запуск: check_concurrency.sh."""

import copy
import json
import os
import secrets
import threading
from concurrent.futures import ThreadPoolExecutor
from http.cookiejar import CookieJar
from urllib.error import HTTPError
from urllib.request import HTTPCookieProcessor, Request, build_opener


BASE = os.environ.get("IS_LAB1_BASE_URL", "http://localhost:8080/api/v1").rstrip("/")
PREFIX = "stage03_" + secrets.token_hex(4)


class Client:
    def __init__(self, suffix):
        self.http = build_opener(HTTPCookieProcessor(CookieJar()))
        self.csrf = self.call("GET", "auth/csrf")[1]["csrf_token"]
        credentials = {"username": PREFIX + suffix, "password": "secret_1"}
        assert self.call("POST", "auth/register", credentials)[0] == 201
        assert self.call("POST", "auth/login", credentials)[0] == 200
        self.csrf = self.call("GET", "auth/csrf")[1]["csrf_token"]

    def call(self, method, path, data=None):
        headers = {"Content-Type": "application/json"}
        if method != "GET":
            headers["X-CSRF-Token"] = self.csrf
        request = Request(BASE + "/" + path, method=method, headers=headers,
                          data=None if data is None else json.dumps(data).encode())
        try:
            response = self.http.open(request, timeout=30)
        except HTTPError as error:
            response = error
        with response:
            body = response.read().decode()
            try:
                value = json.loads(body) if body else None
            except json.JSONDecodeError:
                value = {"message": body[:300]}
            return response.status, value


def org(name):
    return {"full_name": name, "annual_turnover": 100, "employees_count": 10,
            "rating": 1, "type": None,
            "official_address": {"street": "Official", "town": None},
            "postal_address": {"street": "Postal", "town": None}}


def worker(name, organization_id=None, status="HIRED"):
    return {"name": name, "coordinates": {"x": 1, "y": 1}, "salary": 100,
            "rating": 1, "start_date": "2026-10-01T09:00:00Z", "position": "DIRECTOR",
            "status": status, "person": None, "organization_id": organization_id}


def created(client, path, data):
    status, body = client.call("POST", path, data)
    assert status == 201, (path, status, body)
    return body["id"]


def race(left, right, expected):
    gate = threading.Barrier(2)

    def run(action):
        gate.wait(timeout=10)
        return action()

    with ThreadPoolExecutor(max_workers=2) as executor:
        a = executor.submit(run, left)
        b = executor.submit(run, right)
        result = [a.result(timeout=40), b.result(timeout=40)]
    assert sorted(item[0] for item in result) == sorted(expected), result
    return result


def main():
    a, b = Client("_a"), Client("_b")
    race(lambda: a.call("POST", "organizations", org(PREFIX + ' «Name»')),
         lambda: b.call("POST", "organizations", org('  ' + PREFIX.upper() + '  "name"  ')),
         [201, 409])
    print("PASS: concurrent normalized organization create")

    organization_id = created(a, "organizations", org(PREFIX + " Directors"))
    race(lambda: a.call("POST", "workers", worker("Director A", organization_id)),
         lambda: b.call("POST", "workers", worker("Director B", organization_id)), [201, 409])
    assert a.call("GET", "workers")[1]["total_items"] == 1
    print("PASS: concurrent director create, one persisted worker")

    hire_org = created(a, "organizations", org(PREFIX + " Hire"))
    x = created(a, "workers", worker("Candidate A", hire_org, "REGULAR"))
    y = created(b, "workers", worker("Candidate B", hire_org, "REGULAR"))
    race(lambda: a.call("POST", f"workers/{x}/hire", {"organization_id": hire_org}),
         lambda: b.call("POST", f"workers/{y}/hire", {"organization_id": hire_org}), [204, 409])
    statuses = [a.call("GET", f"workers/{key}")[1]["status"] for key in [x, y]]
    assert sorted(statuses) == ["HIRED", "REGULAR"], statuses
    print("PASS: concurrent hire, loser remains REGULAR")

    update_org = created(a, "organizations", org(PREFIX + " Update"))
    x = created(a, "workers", worker("Update A", update_org, "REGULAR"))
    y = created(b, "workers", worker("Update B", update_org, "REGULAR"))
    race(lambda: a.call("PUT", f"workers/{x}", worker("Update A", update_org)),
         lambda: b.call("PUT", f"workers/{y}", worker("Update B", update_org)), [204, 409])
    print("PASS: concurrent update to hired director")

    before_orgs = a.call("GET", "organizations")[1]["total_items"]
    before_workers = a.call("GET", "workers")[1]["total_items"]
    before_coordinates = a.call("GET", "coordinates")[1]["total_items"]
    nested = worker("Nested Worker", status="REGULAR")
    nested.pop("organization_id")
    nested["organization"] = org(PREFIX + ' «Mixed»')
    result = race(lambda: a.call("POST", "organizations", org(PREFIX.upper() + ' "mixed"')),
                  lambda: b.call("POST", "workers", copy.deepcopy(nested)), [201, 409])
    nested_won = int(result[1][0] == 201)
    assert a.call("GET", "organizations")[1]["total_items"] == before_orgs + 1
    assert a.call("GET", "workers")[1]["total_items"] == before_workers + nested_won
    assert a.call("GET", "coordinates")[1]["total_items"] == before_coordinates + nested_won
    print("PASS: standalone organization versus nested creation, no partial worker")

    disposable = created(a, "workers", worker("Disposable", status="REGULAR"))
    gate = threading.Barrier(2)

    def update():
        gate.wait(timeout=10)
        return a.call("PUT", f"workers/{disposable}", worker("Changed", status="REGULAR"))

    def delete():
        gate.wait(timeout=10)
        return b.call("DELETE", f"workers/{disposable}")

    with ThreadPoolExecutor(max_workers=2) as executor:
        first, second = executor.submit(update), executor.submit(delete)
        updated, deleted = first.result(timeout=40), second.result(timeout=40)
    assert updated[0] in [204, 404] and deleted[0] == 204, (updated, deleted)
    assert a.call("GET", f"workers/{disposable}")[0] == 404
    print("PASS: update versus delete")


if __name__ == "__main__":
    main()
