import { useGameStore } from '@/store/gameStore';
import { useUIStore } from '@/store/uiStore';
import { gameClient } from '@/network/client';
import { getFactionColor } from '@/types/game';

const containerStyle: React.CSSProperties = {
  display: 'flex',
  flexDirection: 'column',
  alignItems: 'center',
  justifyContent: 'center',
  height: '100%',
  background: 'linear-gradient(180deg, #0a0a1a 0%, #1a1a3a 100%)',
};

const titleStyle: React.CSSProperties = {
  fontSize: '36px',
  fontWeight: 'bold',
  color: '#4af',
  marginBottom: '32px',
};

const playerListStyle: React.CSSProperties = {
  display: 'flex',
  flexDirection: 'column',
  gap: '8px',
  marginBottom: '32px',
  minWidth: '300px',
};

const playerStyle: React.CSSProperties = {
  padding: '12px 20px',
  borderRadius: '6px',
  fontSize: '16px',
  fontWeight: 'bold',
  textAlign: 'center',
};

const buttonStyle: React.CSSProperties = {
  padding: '14px 32px',
  fontSize: '18px',
  fontWeight: 'bold',
  background: '#353',
  border: '2px solid #575',
  borderRadius: '6px',
  color: '#fff',
  cursor: 'pointer',
};

const waitingStyle: React.CSSProperties = {
  color: '#888',
  fontSize: '16px',
  fontStyle: 'italic',
};

export function Lobby() {
  const gameState = useGameStore((s) => s.gameState);
  const local = useGameStore((s) => s.local);
  const isHost = useUIStore((s) => s.isHost);
  const setScreen = useUIStore((s) => s.setScreen);
  const reset = useGameStore((s) => s.reset);

  const handleStartGame = () => {
    gameClient.send({ type: 'startGame' });
  };

  const handleRetreat = () => {
    gameClient.disconnect();
    reset();
    setScreen('menu');
  };

  const players = gameState?.players || [];

  return (
    <div style={containerStyle}>
      <h1 style={titleStyle}>BATTLE LOBBY</h1>

      <div style={playerListStyle}>
        <p style={{ color: '#666', marginBottom: '8px', textAlign: 'center' }}>
          Commanders Ready ({players.length})
        </p>
        {players.map((player) => {
          const color = getFactionColor(player.factionId, local.factionId);
          const isLocal = player.factionId === local.factionId;
          return (
            <div
              key={player.id}
              style={{
                ...playerStyle,
                background: color,
                border: isLocal ? '2px solid white' : '2px solid transparent',
              }}
            >
              {player.name} {isLocal ? '(You)' : ''}
            </div>
          );
        })}
      </div>

      {isHost ? (
        <button
          style={buttonStyle}
          onClick={handleStartGame}
          disabled={players.length < 1}
        >
          LAUNCH BATTLE
        </button>
      ) : (
        <p style={waitingStyle}>Awaiting host command...</p>
      )}

      <button
        style={{
          ...buttonStyle,
          background: '#533',
          borderColor: '#755',
          marginTop: '16px',
        }}
        onClick={handleRetreat}
      >
        RETREAT
      </button>
    </div>
  );
}
