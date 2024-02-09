from fastapi.datastructures import URL, Headers

from app.core.utils.backend.alert_backend import alert_backend


def test_alert_to_terminal(caplog):
    exception = ValueError("Test error")
    method = "GET"
    url = URL("https://example.com")
    headers = Headers({"content-type": "application/json"})
    body = b'{"test": "data"}'

    alert = alert_backend()
    alert(exception, method, url, headers, body)

    assert "An exception has been raised!" in caplog.text
    assert f"{method} {url}" in caplog.text
    assert "- **content-type**: application/json" in caplog.text
    assert "test" in caplog.text
    assert "Test error" in caplog.text
