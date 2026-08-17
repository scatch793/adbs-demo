from __future__ import annotations

import io
from pathlib import Path

from minio import Minio
from minio.error import S3Error

from ..config import get_settings


class ObjectStorage:
    def __init__(self) -> None:
        self.settings = get_settings()
        self.settings.storage_root.mkdir(parents=True, exist_ok=True)
        self.client: Minio | None = None
        if (
            self.settings.minio_endpoint
            and self.settings.minio_access_key
            and self.settings.minio_secret_key
        ):
            self.client = Minio(
                self.settings.minio_endpoint,
                access_key=self.settings.minio_access_key,
                secret_key=self.settings.minio_secret_key,
                secure=self.settings.minio_secure,
            )

    def ensure_bucket(self) -> None:
        if self.client is None:
            return
        if not self.client.bucket_exists(self.settings.minio_bucket):
            self.client.make_bucket(self.settings.minio_bucket)

    def put_bytes(self, key: str, content: bytes, content_type: str) -> str:
        if self.client is not None:
            self.ensure_bucket()
            self.client.put_object(
                self.settings.minio_bucket,
                key,
                io.BytesIO(content),
                length=len(content),
                content_type=content_type,
            )
        else:
            path = self.settings.storage_root / key
            path.parent.mkdir(parents=True, exist_ok=True)
            path.write_bytes(content)
        return key

    def get_bytes(self, key: str) -> bytes:
        if self.client is not None:
            response = self.client.get_object(self.settings.minio_bucket, key)
            try:
                return response.read()
            finally:
                response.close()
                response.release_conn()
        path = self.settings.storage_root / key
        if not path.exists():
            raise FileNotFoundError(key)
        return path.read_bytes()

    def exists(self, key: str) -> bool:
        if self.client is not None:
            try:
                self.client.stat_object(self.settings.minio_bucket, key)
                return True
            except S3Error:
                return False
        return (self.settings.storage_root / key).exists()


storage = ObjectStorage()
