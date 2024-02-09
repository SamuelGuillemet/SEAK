from redis.asyncio import Redis

from app.schemas.balance import Balance


class BalanceCRUD:
    def build_balance_key(self, username: str) -> str:
        return f"{username}:balance"

    async def get(self, client: Redis, username: str) -> Balance:
        balance_value = await client.get(self.build_balance_key(username))

        if balance_value is None:
            return Balance(balance=None)

        return Balance(balance=float(balance_value))

    async def set(self, client: Redis, username: str, balance: float) -> Balance:
        await client.set(self.build_balance_key(username), balance)

        return await self.get(client, username)

    async def delete(self, client: Redis, username: str) -> int:
        return await client.delete(self.build_balance_key(username))


crud_balance = BalanceCRUD()
