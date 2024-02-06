from fastapi.security import OAuth2PasswordBearer, SecurityScopes

from app.core.config import settings
from app.core.types import get_higher_scope, scopes_hierarchy

oauth2_scheme = OAuth2PasswordBearer(
    tokenUrl=f"{settings.API_V1_PREFIX}/auth/login/",  # TODO: Shouldn't be hardcoded
    scopes={
        "user": "User access",
        "admin": "Admin access",
    },
)


def check_scopes(security_scopes: SecurityScopes, token_scopes: list[str]) -> bool:
    """
    Check if the token scopes are sufficient to access the endpoint.

    :param security_scopes: The security scopes
    :param token_scopes: The token scopes

    :return: Whether the token scopes are sufficient to access the endpoint
    """
    # Get the scopes required to access the endpoint
    required_scopes = security_scopes.scopes
    # Get the higher scope in `token_scopes`
    higher_scope = get_higher_scope(token_scopes)
    # Check if the token scopes are sufficient to access the endpoint
    return all(scope in scopes_hierarchy[higher_scope] for scope in required_scopes)
