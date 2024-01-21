# import pytest
# from tkinter import Tk
# from interface.main_interface import MainInterface

# @pytest.fixture
# def main_interface_instance():
#     return MainInterface("user1")

# def test_refresh_updates_scrolled_text(main_interface_instance):
#     main_interface_instance.symbol_entry = "AAPL"
#     main_interface_instance.refresh_main_interface()
#     lines = main_interface_instance.message_text.get("1.0", "end-1c").split('\n')
#     last_line = lines[-2] if lines else ""
#     assert "Snapshot for symbols AAPL" in last_line, "Expected message not found in ScrolledText"
