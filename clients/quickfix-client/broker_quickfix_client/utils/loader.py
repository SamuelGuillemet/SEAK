from configparser import ConfigParser
from io import TextIOWrapper
from pathlib import Path


def get_project_dir() -> Path:
    return Path(__file__).resolve().parents[2]


def get_config_dir() -> Path:
    return get_project_dir() / "config"


def get_file_as_stream(file_path: Path) -> TextIOWrapper:
    """Get the file as a stream"""
    return open(file_path, "r", encoding="utf-8")


def load_cfg(file_path: Path):
    """Load a config file in the form of a ini file"""
    config = ConfigParser()
    config.read_file(get_file_as_stream(file_path))
    return config
