import { useState } from 'react';
import { useUIStore } from '@/store/uiStore';
import { useGameStore } from '@/store/gameStore';
import { gameClient } from '@/network/client';
import { GAME } from '@/game/constants';

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
  fontSize: '72px',
  fontWeight: 'bold',
  color: '#88bbee',
  marginBottom: '0',
  textShadow: '0 0 30px rgba(136, 187, 238, 0.5), 0 4px 8px rgba(0,0,0,0.8)',
  letterSpacing: '8px',
};

const subtitleStyle: React.CSSProperties = {
  fontSize: '42px',
  fontWeight: 'bold',
  color: '#ddaa44',
  marginBottom: '50px',
  textShadow: '0 0 20px rgba(221, 170, 68, 0.4), 0 3px 6px rgba(0,0,0,0.6)',
  letterSpacing: '12px',
};

const formStyle: React.CSSProperties = {
  display: 'flex',
  flexDirection: 'column',
  gap: '16px',
  width: '320px',
};

const inputStyle: React.CSSProperties = {
  padding: '14px 18px',
  fontSize: '16px',
  background: 'rgba(30, 38, 56, 0.9)',
  border: '2px solid rgba(77, 102, 128, 0.5)',
  borderRadius: '8px',
  color: '#e0e8f0',
  outline: 'none',
  fontFamily: 'inherit',
};

const buttonStyle: React.CSSProperties = {
  padding: '16px 28px',
  fontSize: '18px',
  fontWeight: 'bold',
  background: 'linear-gradient(180deg, rgba(51, 64, 89, 1) 0%, rgba(35, 45, 65, 1) 100%)',
  border: '2px solid rgba(85, 119, 153, 0.6)',
  borderRadius: '8px',
  color: '#fff',
  cursor: 'pointer',
  transition: 'all 0.2s',
  textShadow: '0 2px 4px rgba(0,0,0,0.5)',
  letterSpacing: '2px',
};

const hostButtonStyle: React.CSSProperties = {
  ...buttonStyle,
  background: 'linear-gradient(180deg, rgba(51, 85, 51, 1) 0%, rgba(35, 65, 35, 1) 100%)',
  border: '2px solid rgba(85, 153, 85, 0.6)',
};

const errorStyle: React.CSSProperties = {
  color: '#ff6666',
  fontSize: '14px',
  textAlign: 'center',
  textShadow: '0 1px 2px rgba(0,0,0,0.5)',
};

export function MainMenu() {
  const { playerName, setPlayerName, serverAddress, setServerAddress, setScreen, setIsHost } = useUIStore();
  const { setLocalPlayer, setGameState } = useGameStore();
  const [error, setError] = useState('');
  const [connecting, setConnecting] = useState(false);

  const handleConnect = async (isHost: boolean) => {
    if (!playerName.trim()) {
      setError('Please enter your commander name');
      return;
    }

    setConnecting(true);
    setError('');
    setIsHost(isHost);

    try {
      const address = isHost ? 'localhost' : serverAddress;
      await gameClient.connect(address, GAME.SERVER_PORT);

      gameClient.onMessage((msg) => {
        switch (msg.type) {
          case 'joinResponse':
            setLocalPlayer(msg.playerId, msg.factionId);
            break;
          case 'lobbyUpdate':
            setGameState({
              phase: 'lobby',
              countries: [],
              companies: [],
              factions: [],
              players: msg.players,
              unitCost: 0,
              winner: null,
            });
            break;
          case 'gameStarted':
            setScreen('game');
            break;
          case 'gameState':
            setGameState(msg.state);
            if (msg.state.phase === 'playing') {
              setScreen('game');
            }
            break;
        }
      });

      gameClient.send({ type: 'join', playerName: playerName.trim() });
      setScreen('lobby');
    } catch (e) {
      setError(isHost ? 'Failed to connect. Is the server running?' : 'Failed to connect to server');
    } finally {
      setConnecting(false);
    }
  };

  return (
    <div style={containerStyle}>
      <h1 style={titleStyle}>TRADEWAR</h1>
      <p style={subtitleStyle}>GALAXY</p>

      <div style={formStyle}>
        <input
          style={inputStyle}
          placeholder="Commander Name"
          value={playerName}
          onChange={(e) => setPlayerName(e.target.value)}
        />

        <input
          style={inputStyle}
          placeholder="Server Address"
          value={serverAddress}
          onChange={(e) => setServerAddress(e.target.value)}
        />

        {error && <p style={errorStyle}>{error}</p>}

        <button
          style={hostButtonStyle}
          onClick={() => handleConnect(true)}
          disabled={connecting}
        >
          {connecting ? 'CONNECTING...' : 'HOST GAME'}
        </button>

        <button
          style={buttonStyle}
          onClick={() => handleConnect(false)}
          disabled={connecting}
        >
          {connecting ? 'CONNECTING...' : 'JOIN GAME'}
        </button>
      </div>

      <p style={{ marginTop: '50px', color: '#445566', fontSize: '13px' }}>
        Start server: pnpm server
      </p>
    </div>
  );
}
