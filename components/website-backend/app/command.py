import argparse
import asyncio
import logging
import sys

from app.commands.add_user import add_n_users
from app.commands.delete_user import delete_users
from app.commands.dump_db import dump_db
from app.commands.execute_sql import execute_sql_command
from app.commands.init_db import init_db
from app.commands.load_db import load_db
from app.commands.migrate_db import migrate_db
from app.commands.open_api import open_api
from app.commands.reset_db import reset_db
from app.db.pre_start import pre_start
from app.dependencies import get_db, get_redis
from app.utils.logger import setup_logs

logger = logging.getLogger("app.command")

parser = argparse.ArgumentParser(
    description=r"""
    Commands to manage the application.
    Run `python app/command.py <command> --help` for more information on a command.
    """,
    prog="python app/command.py",
    allow_abbrev=False,
    exit_on_error=True,
)
subparsers = parser.add_subparsers(help="sub-command help")

reset_parser = subparsers.add_parser(
    "reset",
    help="Reset database",
)

init_parser = subparsers.add_parser(
    "init",
    help="Initialize database",
)
init_parser.add_argument(
    "-y",
    "--yes",
    action="store_true",
    help="Bypass prompt",
    default=False,
)
init_parser.add_argument(
    "--bypass-revision",
    action="store_true",
    help="Bypass revision",
    default=False,
)

migrate_parser = subparsers.add_parser(
    "migrate",
    help="Migrate database",
)
migrate_parser.add_argument(
    "--bypass-revision",
    action="store_true",
    help="Bypass revision",
    default=False,
)
migrate_parser.add_argument(
    "-f",
    "--force",
    action="store_true",
    help="Force migration",
    default=False,
)

open_api_parser = subparsers.add_parser(
    "openapi",
    help="Generate OpenAPI schema",
)
open_api_parser.add_argument(
    "-o",
    "--output",
    type=str,
    help="Output file",
    default="openapi.json",
)

dump_db_parser = subparsers.add_parser(
    "dump",
    help="Dump database",
)
dump_db_parser.add_argument(
    "-o",
    "--output",
    type=str,
    help="Output file",
    default="dump.json",
)

load_db_parser = subparsers.add_parser(
    "load",
    help="Load database",
)
load_db_parser.add_argument(
    "-i",
    "--input",
    type=str,
    help="Input file",
    default="dump.json",
)

execute_parser = subparsers.add_parser(
    "execute",
    help="Execute SQL command",
)
execute_parser.add_argument(
    "command",
    type=str,
    help="SQL command",
)

add_user_parser = subparsers.add_parser(
    "add-users",
    help="Add n users",
)
add_user_parser.add_argument(
    "-n",
    type=int,
    help="Number of users",
)
add_user_parser.add_argument(
    "-f",
    "--file",
    type=str,
    help="File to save users",
    default="users.txt",
)
add_user_parser.add_argument(
    "--default-balance",
    type=int,
    help="Default balance",
    default=10000,
)

delete_user_parser = subparsers.add_parser(
    "delete-users",
    help="Delete users",
)
delete_user_parser.add_argument(
    "-f",
    "--file",
    type=str,
    help="File with users",
    default="users.txt",
)


PROMPT_MESSAGE = (
    "Are you sure you want to reset the database, this will delete all data? [y/N] "
)


async def setup_database():  # pragma: no cover
    get_db.setup()
    get_redis.setup()
    await pre_start()


async def main(command: str) -> None:
    args = parser.parse_args()

    logger.info(f"Running command: {command}")

    # OpenAPI schema generation is a special case
    # because it doesn't require a database connection
    if command == "openapi":
        open_api(args.output)
        return

    await setup_database()

    match command:
        case "reset":
            await reset_db()
        case "init":
            if args.yes or input(PROMPT_MESSAGE).lower() == "y":
                await reset_db()
                await migrate_db()
                await init_db()
            else:
                logger.info("Aborted")
        case "migrate":
            await migrate_db()
        case "dump":
            await dump_db(args.output)
        case "load":
            await load_db(args.input)
        case "execute":
            await execute_sql_command(args.command)
        case "add-users":
            await add_n_users(args.n, args.file, args.default_balance)
        case "delete-users":
            await delete_users(args.file)


if __name__ == "__main__":  # pragma: no cover
    if len(sys.argv) == 1:
        parser.print_help()
        sys.exit(1)

    setup_logs("app", level=logging.INFO)
    asyncio.run(main(sys.argv[1]))
