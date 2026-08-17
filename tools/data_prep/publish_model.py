from __future__ import annotations

import argparse
import json
import urllib.request
from pathlib import Path


def post_json(url: str, payload: dict, token: str | None = None) -> dict:
    request = urllib.request.Request(
        url,
        data=json.dumps(payload).encode("utf-8"),
        headers={
            "Content-Type": "application/json",
            **({"Authorization": f"Bearer {token}"} if token else {}),
        },
        method="POST",
    )
    with urllib.request.urlopen(request, timeout=30) as response:
        return json.load(response)


def main() -> None:
    parser = argparse.ArgumentParser(description="Publish a deidentified versioned model")
    parser.add_argument("--server", default="http://127.0.0.1:8000")
    parser.add_argument("--username", default="doctor")
    parser.add_argument("--password", required=True)
    parser.add_argument("--patient-code", default="P001")
    parser.add_argument("--model", type=Path, default=Path("private_data/p001/model_v1.json"))
    args = parser.parse_args()

    token = post_json(
        f"{args.server.rstrip('/')}/auth/login",
        {"username": args.username, "password": args.password},
    )["access_token"]
    patient_request = urllib.request.Request(
        f"{args.server.rstrip('/')}/patients",
        headers={"Authorization": f"Bearer {token}"},
    )
    with urllib.request.urlopen(patient_request, timeout=30) as response:
        patients = json.load(response)
    patient = next(row for row in patients if row["code"] == args.patient_code)
    model = json.loads(args.model.read_text(encoding="utf-8"))
    result = post_json(
        f"{args.server.rstrip('/')}/models",
        {
            "patient_id": patient["id"],
            "payload": model,
            "metrics": {
                "source": "deidentified P001 pipeline",
                "clinical_validation": False,
            },
            "approve": True,
        },
        token,
    )
    print(json.dumps({"id": result["id"], "version": result["version"]}, ensure_ascii=False))


if __name__ == "__main__":
    main()
