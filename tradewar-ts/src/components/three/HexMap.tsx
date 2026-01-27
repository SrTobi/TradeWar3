import { useGameStore } from '@/store/gameStore';
import { gameClient } from '@/network/client';
import { Hex } from './Hex';
import type { HexCoord } from '@/types/game';

const HEX_SIZE = 1;

export function HexMap() {
  const gameState = useGameStore((s) => s.gameState);
  const localFactionId = useGameStore((s) => s.local.factionId);
  const spendMoney = useGameStore((s) => s.spendMoney);

  if (!gameState) return null;

  const handleHexClick = (coords: HexCoord) => {
    if (!localFactionId) return;
    if (gameState.phase !== 'playing') return;

    if (spendMoney(gameState.unitCost)) {
      gameClient.send({ type: 'placeUnits', coords });
    }
  };

  return (
    <group>
      {gameState.countries.map((country) => (
        <Hex
          key={`${country.coords.q},${country.coords.r}`}
          country={country}
          size={HEX_SIZE}
          onClick={() => handleHexClick(country.coords)}
        />
      ))}
    </group>
  );
}
