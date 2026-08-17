from __future__ import annotations

from collections import defaultdict

from fastapi import WebSocket


class ConnectionHub:
    def __init__(self) -> None:
        self.channels: dict[str, set[WebSocket]] = defaultdict(set)

    async def connect(self, channel: str, websocket: WebSocket) -> None:
        await websocket.accept()
        self.channels[channel].add(websocket)

    def disconnect(self, channel: str, websocket: WebSocket) -> None:
        self.channels[channel].discard(websocket)
        if not self.channels[channel]:
            self.channels.pop(channel, None)

    async def broadcast(self, channel: str, payload: dict) -> None:
        stale: list[WebSocket] = []
        for websocket in tuple(self.channels.get(channel, ())):
            try:
                await websocket.send_json(payload)
            except Exception:
                stale.append(websocket)
        for websocket in stale:
            self.disconnect(channel, websocket)


hub = ConnectionHub()
