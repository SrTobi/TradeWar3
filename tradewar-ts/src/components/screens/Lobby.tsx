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
  background: 'linear-gradient(180deg, #080810 0%, #101828 50%, #0a1020 100%)',
  fontFamily: "'Segoe UI', system-ui, sans-serif",
};

const titleStyle: React.CSSProperties = {
  fontSize: '52px',
  fontWeight: 'bold',
  color: '#88bbee',
  marginBottom: '8px',
  textShadow: '0 0 30px rgba(136, 187, 238, 0.5), 0 4px 8px rgba(0,0,0,0.8)',
  letterSpacing: '4px',
};

const subtitleStyle: React.CSSProperties = {
  fontSize: '18px',
  color: '#667788',
  marginBottom: '32px',
  fontStyle: 'italic',
};

const playerContainerStyle: React.CSSProperties = {
  background: 'rgba(26, 31, 46, 0.85)',
  borderRadius: '12px',
  border: '2px solid rgba(77, 102, 128, 0.4)',
  padding: '20px',
  minWidth: '400px',
  minHeight: '150px',
  marginBottom: '32px',
};

const playerRowStyle: React.CSSProperties = {
  display: 'flex',
  alignItems: 'center',
  gap: '12px',
  padding: '10px 16px',
  borderRadius: '6px',
  marginBottom: '8px',
};

const colorBoxStyle: React.CSSProperties = {
  width: '24px',
  height: '24px',
  borderRadius: '4px',
  border: '2px solid rgba(255,255,255,0.3)',
};

const buttonStyle: React.CSSProperties = {
  padding: '16px 40px',
  fontSize: '18px',
  fontWeight: 'bold',
  background: 'linear-gradient(180deg, rgba(51, 85, 51, 1) 0%, rgba(35, 65, 35, 1) 100%)',
  border: '2px solid rgba(85, 153, 85, 0.6)',
  borderRadius: '8px',
  color: '#fff',
  cursor: 'pointer',
  transition: 'all 0.2s',
  textShadow: '0 2px 4px rgba(0,0,0,0.5)',
  letterSpacing: '2px',
};

const retreatButtonStyle: React.CSSProperties = {
  ...buttonStyle,
  padding: '12px 28px',
  fontSize: '14px',
  background: 'linear-gradient(180deg, rgba(85, 51, 51, 1) 0%, rgba(65, 35, 35, 1) 100%)',
  border: '2px solid rgba(153, 85, 85, 0.6)',
  marginTop: '16px',
};

const waitingStyle: React.CSSProperties = {
  color: '#778899',
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
      <h1 style={titleStyle}>BATTLE STATIONS</h1>
      <p style={subtitleStyle}>Waiting for commanders...</p>

      <div style={playerContainerStyle}>
        <p style={{ color: '#667788', marginBottom: '12px', fontSize: '14px', textAlign: 'center' }}>
          COMMANDERS READY ({players.length})
        </p>
        {players.map((player) => {
          const color = getFactionColor(player.factionId, local.factionId);
          const isLocal = player.factionId === local.factionId;
          return (
            <div
              key={player.id}
              style={{
                ...playerRowStyle,
                background: isLocal ? 'rgba(68, 136, 255, 0.2)' : 'rgba(0, 0, 0, 0.2)',
                border: isLocal ? '1px solid rgba(68, 136, 255, 0.5)' : '1px solid transparent',
              }}
            >
              <div style={{ ...colorBoxStyle, background: color }} />
              <span style={{ color: '#dde', fontWeight: 'bold', flex: 1 }}>
                {player.name}
              </span>
              {isLocal && (
                <span style={{ color: '#88aaff', fontSize: '12px' }}>(YOU)</span>
              )}
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

      <button style={retreatButtonStyle} onClick={handleRetreat}>
        RETREAT
      </button>
    </div>
  );
}
