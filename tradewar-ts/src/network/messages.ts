import type { GameState, Player, HexCoord } from '@/types/game';

export type ClientMessage =
  | { type: 'join'; playerName: string }
  | { type: 'startGame' }
  | { type: 'placeUnits'; coords: HexCoord };

export type ServerMessage =
  | { type: 'joinResponse'; playerId: string; factionId: string }
  | { type: 'lobbyUpdate'; players: Player[] }
  | { type: 'gameStarted' }
  | { type: 'gameState'; state: GameState }
  | { type: 'error'; message: string };

export function serialize(msg: ClientMessage | ServerMessage): string {
  return JSON.stringify(msg);
}

export function deserialize<T>(data: string): T {
  return JSON.parse(data) as T;
}
