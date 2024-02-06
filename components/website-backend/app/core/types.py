from enum import Enum


class SecurityScopes(str, Enum):
    USER = "user"
    ADMIN = "admin"
    SERVICE = "service"


scopes_hierarchy: dict[str, list[str]] = {
    SecurityScopes.USER.value: [SecurityScopes.USER.value],
    SecurityScopes.ADMIN.value: [
        SecurityScopes.USER.value,
        SecurityScopes.ADMIN.value,
    ],
}


def get_higher_scope(scopes: list[str]) -> str:
    """
    Get the higher scope in the given list of scopes,
    ie the scope with the highest number of subscopes in the list.

    :param scopes: The list of scopes
    :return: The higher scope in the given list of scopes
    """
    return max(scopes, key=lambda scope: len(scopes_hierarchy[scope]))
