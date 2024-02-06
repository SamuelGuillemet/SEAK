from redis.asyncio import Redis

from app.schemas.stock import AccountStock, Stock


class StockCRUD:
    def build_stock_key(self, username: str, symbol: str) -> str:
        return f"{username}:{symbol}"

    async def get(self, client: Redis, username: str, symbol: str) -> Stock:
        stock_value = await client.get(self.build_stock_key(username, symbol))

        if stock_value is None:
            return Stock(symbol=symbol, quantity=0)

        return Stock(symbol=symbol, quantity=int(stock_value))

    async def set(
        self, client: Redis, username: str, symbol: str, quantity: int
    ) -> Stock:
        await client.set(self.build_stock_key(username, symbol), quantity)

        return await self.get(client, username, symbol)

    async def get_all(self, client: Redis, username: str) -> AccountStock:
        keys: list[str] = await client.keys(f"{username}:[A-Z]*")
        if not keys:
            return AccountStock(stocks=[])

        stocks: list[str] = await client.mget(*keys)

        return AccountStock(
            stocks=[
                Stock(symbol=stock_key.split(":")[1], quantity=int(stock_value))
                for stock_key, stock_value in zip(keys, stocks)
                if int(stock_value) > 0
            ]
        )


crud_stock = StockCRUD()
