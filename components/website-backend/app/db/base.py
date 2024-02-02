# Import all the models so that Alembic can see the models and generate the migration scripts accordingly.
from app.db.base_class import Base  # noqa
from app.models.account import Account  # noqa
