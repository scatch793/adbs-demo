from __future__ import annotations

from functools import lru_cache
from pathlib import Path

from pydantic import Field
from pydantic_settings import BaseSettings, SettingsConfigDict


class Settings(BaseSettings):
    model_config = SettingsConfigDict(
        env_file=".env",
        env_prefix="OMNIDAPT_",
        case_sensitive=False,
        extra="ignore",
    )

    app_name: str = "Ominidapt PD Research Server"
    environment: str = "development"
    database_url: str = "sqlite:///./data/omnidapt.db"
    secret_key: str = Field(
        default="development-only-change-me-please-32-bytes",
        min_length=32,
    )
    access_token_minutes: int = 15
    refresh_token_days: int = 14
    storage_root: Path = Path("./data/objects")
    minio_endpoint: str | None = None
    minio_access_key: str | None = None
    minio_secret_key: str | None = None
    minio_secure: bool = False
    minio_bucket: str = "omnidapt"
    redis_url: str | None = None
    bootstrap_admin_username: str = "admin"
    bootstrap_admin_password: str = "Admin-ChangeMe-2026"
    bootstrap_doctor_username: str = "doctor"
    bootstrap_doctor_password: str = "Doctor-ChangeMe-2026"
    bootstrap_patient_username: str = "patient"
    bootstrap_patient_password: str = "Patient-ChangeMe-2026"
    cors_origins: str = "*"

    @property
    def cors_origin_list(self) -> list[str]:
        if self.cors_origins.strip() == "*":
            return ["*"]
        return [value.strip() for value in self.cors_origins.split(",") if value.strip()]


@lru_cache
def get_settings() -> Settings:
    return Settings()
