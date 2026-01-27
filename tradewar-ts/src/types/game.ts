export interface HexCoord {
  q: number;
  r: number;
}

export interface Faction {
  id: string;
  name: string;
}

export interface Country {
  coords: HexCoord;
  units: Record<string, number>;
  nextBattleTime: number;
}

export interface Company {
  id: string;
  name: string;
  price: number;
  previousPrice: number;
  nextUpdateTime: number;
}

export interface Player {
  id: string;
  name: string;
  factionId: string;
}

export interface GameState {
  phase: 'lobby' | 'playing' | 'ended';
  countries: Country[];
  companies: Company[];
  factions: Faction[];
  players: Player[];
  unitCost: number;
  winner: Faction | null;
}

export interface LocalPlayerState {
  playerId: string | null;
  factionId: string | null;
  money: number;
  holdings: Record<string, number>;
  bulkAmount: number;
}

export const NEUTRAL_FACTION: Faction = {
  id: 'neutral',
  name: 'Neutral',
};

export const FACTION_COLORS: Record<string, string> = {
  neutral: '#666666',
  player: '#4488ff',
  faction0: '#4488ff',
  faction1: '#ff4444',
  faction2: '#ffaa00',
  faction3: '#44ff88',
  faction4: '#ff44ff',
  faction5: '#44ffff',
};

export function getFactionColor(factionId: string, localFactionId: string | null): string {
  if (factionId === 'neutral') return FACTION_COLORS.neutral;
  if (factionId === localFactionId) return FACTION_COLORS.player;
  return FACTION_COLORS[factionId] || FACTION_COLORS.faction1;
}
