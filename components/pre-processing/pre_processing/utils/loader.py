from pathlib import Path
from typing import Any

import yaml


def get_project_dir() -> Path:
    """Resolve to the project directory (/components/pre-processing)"""
    return Path(__file__).resolve().parents[2]


def get_config_dir() -> Path:
    """Resolve to the config directory (/config)"""
    return get_project_dir().parents[1] / "config"


def get_kafka_config() -> dict[str, Any]:
    """Parse config file at /config/common/kafka.yml"""
    kafka_config = Path(get_config_dir() / "common" / "kafka.yml")
    return yaml.safe_load(kafka_config.read_text(encoding="utf-8"))["kafka"]


def get_avro_schema(schema_name: str) -> str:
    """Parse config file at /config/common/kafka.yml"""
    # Verify that the schema exists
    if not schema_name.endswith(".avsc"):
        schema_name += ".avsc"
    avro_schema = Path(get_config_dir() / "avro" / f"{schema_name}")
    if not avro_schema.exists():
        raise FileNotFoundError(f"Schema {schema_name} not found")
    return avro_schema.read_text(encoding="utf-8")


def get_data_path() -> str:
    data_path = Path(get_project_dir().parents[1] / "data")
    return str(data_path)
