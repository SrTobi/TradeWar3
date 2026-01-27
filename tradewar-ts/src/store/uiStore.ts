import { create } from 'zustand';
import type { HexCoord } from '@/types/game';

type Screen = 'menu' | 'lobby' | 'game';

interface BattleParticle {
  id: string;
  coords: HexCoord;
  startTime: number;
}

interface UIStore {
  screen: Screen;
  setScreen: (screen: Screen) => void;
  hoveredHex: HexCoord | null;
  setHoveredHex: (hex: HexCoord | null) => void;
  battleParticles: BattleParticle[];
  addBattleParticle: (coords: HexCoord) => void;
  removeBattleParticle: (id: string) => void;
  playerName: string;
  setPlayerName: (name: string) => void;
  serverAddress: string;
  setServerAddress: (address: string) => void;
  isHost: boolean;
  setIsHost: (isHost: boolean) => void;
}

let particleId = 0;

export const useUIStore = create<UIStore>((set) => ({
  screen: 'menu',
  setScreen: (screen) => set({ screen }),

  hoveredHex: null,
  setHoveredHex: (hex) => set({ hoveredHex: hex }),

  battleParticles: [],
  addBattleParticle: (coords) => {
    const id = `particle-${particleId++}`;
    set((s) => ({
      battleParticles: [...s.battleParticles, { id, coords, startTime: Date.now() }],
    }));
    setTimeout(() => {
      set((s) => ({
        battleParticles: s.battleParticles.filter((p) => p.id !== id),
      }));
    }, 1000);
  },

  playerName: '',
  setPlayerName: (name) => set({ playerName: name }),

  serverAddress: 'localhost',
  setServerAddress: (address) => set({ serverAddress: address }),

  isHost: false,
  setIsHost: (isHost) => set({ isHost }),

  removeBattleParticle: (id) =>
    set((s) => ({
      battleParticles: s.battleParticles.filter((p) => p.id !== id),
    })),
}));
