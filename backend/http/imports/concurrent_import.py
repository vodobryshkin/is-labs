#!/usr/bin/env python3
"""Две загрузки и загрузка против CRUD; запуск через check_import_concurrency.sh."""

import copy
import importlib.util
import json
from pathlib import Path


ROOT = Path(__file__).resolve().parent
SPEC = importlib.util.spec_from_file_location("domain_http_checks",
                                           ROOT.parent / "domain" / "concurrent_rules.py")
HTTP = importlib.util.module_from_spec(SPEC)
SPEC.loader.exec_module(HTTP)


def totals(client):
    return {name: client.call("GET", name)[1]["total_items"]
            for name in ["workers", "coordinates", "organizations", "addresses", "persons", "locations"]}


def main():
    a, b = HTTP.Client("_import_a"), HTTP.Client("_import_b")
    payload = json.loads((ROOT / "files" / "success.json").read_text())
    result = HTTP.race(lambda: a.call("POST", "imports", payload),
                       lambda: b.call("POST", "imports", payload), [201, 409])
    assert totals(a) == {"workers": 2, "coordinates": 2, "organizations": 1,
                         "addresses": 2, "persons": 1, "locations": 3}
    for client, response in zip([a, b], result):
        history = client.call("GET", "imports")[1]
        assert history["total_items"] == 1, history
        entry = history["items"][0]
        if response[0] == 201:
            assert entry["status"] == "SUCCESS" and entry["added_count"] == 2, entry
        else:
            assert entry["status"] == "FAILED" and entry["added_count"] is None, entry
            assert response[1]["record"] == 1, response
    print("PASS: two imports, one complete file committed and separate owner histories")

    before = totals(a)
    data = copy.deepcopy(payload)
    data[0]["organization"] = None
    data[1]["organization"] = HTTP.org(HTTP.PREFIX + ' «Mixed Import»')
    result = HTTP.race(lambda: a.call("POST", "imports", data),
                       lambda: b.call("POST", "organizations",
                                      HTTP.org(HTTP.PREFIX.upper() + ' "mixed import"')), [201, 409])
    imported = int(result[0][0] == 201)
    after = totals(a)
    assert after["organizations"] == before["organizations"] + 1
    assert after["addresses"] == before["addresses"] + 2
    for name in ["workers", "coordinates"]:
        assert after[name] == before[name] + 2 * imported, after
    for name in ["persons", "locations"]:
        assert after[name] == before[name] + imported, after
    entry = a.call("GET", "imports?sort_order=desc")[1]["items"][0]
    if imported:
        assert entry["status"] == "SUCCESS" and entry["added_count"] == 2, entry
    else:
        assert entry["status"] == "FAILED" and entry["added_count"] is None, entry
        assert result[0][1]["record"] == 2, result
    print("PASS: import versus ordinary create, including rollback before conflicting second record")


if __name__ == "__main__":
    main()
