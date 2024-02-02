from typing import Any
from unittest.mock import MagicMock

import pytest
from pydantic import ValidationError, ValidationInfo

from app.core.security import get_password_hash, verify_password
from app.schemas.account import (
    AccountCreate,
    AccountUpdate,
    OwnAccountUpdate,
    validate_password,
)

strong_password = "StrongPassword123!45*"


def test_validate_password():
    # Test valid password
    info = MagicMock(spec=ValidationInfo)
    info.data = {}
    hashed_password = validate_password(strong_password, info)
    assert verify_password(strong_password, hashed_password)

    # Test weak password
    weak_password = "password"
    try:
        validate_password(weak_password, info)
    except ValueError as e:
        assert "Password is too weak" in str(e)
    else:
        assert False, "Expected ValueError"


def test_validate_password_with_hashed_password():
    # Test valid password
    info = MagicMock(spec=ValidationInfo)
    password = get_password_hash(strong_password)
    hashed_password = validate_password(password, info)
    assert verify_password(strong_password, hashed_password)


def test_account_create():
    # Test valid account creation
    account_dict = {
        "username": "johndoe",
        "last_name": "Doe",
        "first_name": "John",
        "password": strong_password,
        "enabled": True,
        "scope": "admin",
    }
    account = AccountCreate(**account_dict)
    assert account.model_dump() == {
        **account_dict,
        "enabled": False,
        "scope": "user",
        "password": account.password,
        "balance": 0,
    }

    # Test invalid account creation
    account_dict = {
        "username": "johndoe",
        "last_name": "Doe",
        "first_name": "John",
        "password": "password",
    }
    try:
        AccountCreate(**account_dict)
    except ValidationError as e:
        assert "Password is too weak" in str(e)
        assert "value_error" in str(e)
    else:
        assert False, "Expected ValidationError"


def test_account_update():
    # Test valid account update
    account_dict = {
        "username": "johndoe",
        "balance": 100,
        "last_name": "Doe",
        "first_name": "John",
    }
    account = AccountUpdate(**account_dict)
    assert account.model_dump() == {
        **account_dict,
        "enabled": None,
        "scope": None,
        "password": None,
    }

    # Test invalid account update
    account_dict: dict[str, Any] = {
        "username": "jd",
        "last_name": "Doe",
        "first_name": "John",
    }
    with pytest.raises(ValidationError) as exc_info:
        AccountUpdate(**account_dict)
    assert "string_too_short" in str(exc_info.value)

    with pytest.raises(ValidationError) as exc_info:
        AccountUpdate(
            password="password",
        )
    assert "Password is too weak" in str(exc_info.value)


def test_own_account_update():
    # Test valid own account update
    account_dict = {
        "username": "johndoe",
        "last_name": "Doe",
        "first_name": "John",
    }
    account = OwnAccountUpdate(**account_dict)
    assert account.model_dump() == {
        **account_dict,
        "password": None,
    }

    # Test invalid own account update
    account_dict = {
        "username": "johndoe",
        "last_name": "Doe",
        "first_name": "John",
        "enabled": True,
    }
    with pytest.raises(ValidationError) as exc_info:
        OwnAccountUpdate(**account_dict)
    assert "extra_forbidden" in str(exc_info.value)
