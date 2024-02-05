from unittest.mock import patch

from interface.login_window import LoginWindow
from interface.main_interface import MainInterface


def test_valid_login():
    with patch.object(MainInterface, "__init__", return_value=None):
        login_window = LoginWindow()
        login_window.username_entry.insert(0, "user1")
        login_window.password_entry.insert(0, "password")
        result = login_window.validate_login()
        assert result is True, "The login should have worked"
