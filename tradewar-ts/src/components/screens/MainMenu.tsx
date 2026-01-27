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
  background: 'linear-gradient(180deg, #0a0a1a 0%, #1a1a3a 100%)',
};

const titleStyle: React.CSSProperties = {
  fontSize: '48px',
  fontWeight: 'bold',
  color: '#4af',
  marginBottom: '8px',
  textShadow: '0 0 20px rgba(68, 170, 255, 0.5)',
};

const subtitleStyle: React.CSSProperties = {
  fontSize: '18px',
  color: '#888',
  marginBottom: '40px',
};

const formStyle: React.CSSProperties = {
  display: 'flex',
  flexDirection: 'column',
  gap: '16px',
  width: '300px',
};

const inputStyle: React.CSSProperties = {
  padding: '12px 16px',
  fontSize: '16px',
  background: '#1a1a2a',
  border: '2px solid #334',
  borderRadius: '6px',
  color: '#fff',
  outline: 'none',
};

const buttonStyle: React.CSSProperties = {
  padding: '14px 24px',
  fontSize: '16px',
  fontWeight: 'bold',
  background: '#335',
  border: '2px solid #557',
  borderRadius: '6px',
  color: '#fff',
  cursor: 'pointer',
  transition: 'all 0.2s',
};

const errorStyle: React.CSSProperties = {
  color: '#f44',
  fontSize: '14px',
  textAlign: 'center',
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
      <h1 style={titleStyle}>TRADEWAR GALAXY</h1>
      <p style={subtitleStyle}>Conquer. Trade. Dominate.</p>

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
          style={{ ...buttonStyle, background: '#353' }}
          onClick={() => handleConnect(true)}
          disabled={connecting}
        >
          {connecting ? 'Connecting...' : 'HOST GAME'}
        </button>

        <button
          style={buttonStyle}
          onClick={() => handleConnect(false)}
          disabled={connecting}
        >
          {connecting ? 'Connecting...' : 'JOIN GAME'}
        </button>
      </div>

      <p style={{ marginTop: '40px', color: '#555', fontSize: '12px' }}>
        Start the server with: pnpm server
      </p>
    </div>
  );
}
