import { WebSocketServer, WebSocket } from 'ws';
import type { GameState, Player, Country, Company, Faction, HexCoord } from '../src/types/game';
import type { ClientMessage, ServerMessage } from '../src/network/messages';
import { generateHexGrid } from '../src/game/hex';
import { processBattle, calculateUnitCost, canPlaceUnits, checkWinner } from '../src/game/battle';
import { createCompanies, updateStockPrice } from '../src/game/stock';
import { GAME } from '../src/game/constants';

interface ConnectedClient {
  ws: WebSocket;
  player: Player | null;
}

class GameServer {
  private wss: WebSocketServer;
  private clients: Map<WebSocket, ConnectedClient> = new Map();
  private gameState: GameState | null = null;
  private gameLoop: NodeJS.Timeout | null = null;
  private factionCounter = 0;

  constructor(port: number) {
    this.wss = new WebSocketServer({ port });
    console.log(`Game server started on port ${port}`);

    this.wss.on('connection', (ws) => {
      console.log('Client connected');
      this.clients.set(ws, { ws, player: null });

      ws.on('message', (data) => {
        const msg = JSON.parse(data.toString()) as ClientMessage;
        this.handleMessage(ws, msg);
      });

      ws.on('close', () => {
        const client = this.clients.get(ws);
        if (client?.player) {
          console.log(`Player ${client.player.name} disconnected`);
        }
        this.clients.delete(ws);
        this.broadcastLobbyUpdate();
      });
    });
  }

  private handleMessage(ws: WebSocket, msg: ClientMessage): void {
    const client = this.clients.get(ws);
    if (!client) return;

    switch (msg.type) {
      case 'join':
        this.handleJoin(client, msg.playerName);
        break;
      case 'startGame':
        this.handleStartGame();
        break;
      case 'placeUnits':
        this.handlePlaceUnits(client, msg.coords);
        break;
    }
  }

  private handleJoin(client: ConnectedClient, playerName: string): void {
    const factionId = `faction${this.factionCounter++}`;
    const playerId = `player${Date.now()}-${Math.random().toString(36).slice(2)}`;

    client.player = {
      id: playerId,
      name: playerName,
      factionId,
    };

    const response: ServerMessage = {
      type: 'joinResponse',
      playerId,
      factionId,
    };
    client.ws.send(JSON.stringify(response));

    this.broadcastLobbyUpdate();
  }

  private handleStartGame(): void {
    if (this.gameState?.phase === 'playing') return;

    const players = this.getPlayers();
    if (players.length === 0) return;

    const factions: Faction[] = [
      { id: 'neutral', name: 'Neutral' },
      ...players.map((p) => ({ id: p.factionId, name: p.name })),
    ];

    // Starting positions on opposite edges of the map
    const startingPositions: HexCoord[] = [
      { q: GAME.MAP_RADIUS, r: 0 },
      { q: -GAME.MAP_RADIUS, r: 0 },
      { q: 0, r: GAME.MAP_RADIUS },
      { q: 0, r: -GAME.MAP_RADIUS },
      { q: GAME.MAP_RADIUS, r: -GAME.MAP_RADIUS },
      { q: -GAME.MAP_RADIUS, r: GAME.MAP_RADIUS },
    ];

    const hexCoords = generateHexGrid(GAME.MAP_RADIUS);
    const countries: Country[] = hexCoords.map((coords) => {
      // Check if this is a starting position for a player
      const playerIndex = startingPositions.findIndex(
        (pos) => pos.q === coords.q && pos.r === coords.r
      );

      if (playerIndex !== -1 && playerIndex < players.length) {
        // This is a player's starting territory
        return {
          coords,
          units: { [players[playerIndex].factionId]: 10 },
          nextBattleTime: Date.now() + Math.random() * GAME.BATTLE_MAX_INTERVAL,
        };
      }

      // Regular neutral territory
      return {
        coords,
        units: {
          neutral: GAME.NEUTRAL_UNITS_MIN + Math.floor(Math.random() * (GAME.NEUTRAL_UNITS_MAX - GAME.NEUTRAL_UNITS_MIN)),
        },
        nextBattleTime: Date.now() + Math.random() * GAME.BATTLE_MAX_INTERVAL,
      };
    });

    const companies = createCompanies(GAME.STOCK_COUNT);

    this.gameState = {
      phase: 'playing',
      countries,
      companies,
      factions,
      players,
      unitCost: calculateUnitCost(countries),
      winner: null,
    };

    this.broadcast({ type: 'gameStarted' });
    this.startGameLoop();
  }

  private handlePlaceUnits(client: ConnectedClient, coords: HexCoord): void {
    if (!this.gameState || this.gameState.phase !== 'playing') return;
    if (!client.player) return;

    const factionId = client.player.factionId;
    const countryIndex = this.gameState.countries.findIndex(
      (c: Country) => c.coords.q === coords.q && c.coords.r === coords.r
    );

    if (countryIndex === -1) return;

    const country = this.gameState.countries[countryIndex];
    if (!canPlaceUnits(country, this.gameState.countries, factionId)) return;

    const newUnits = { ...country.units };
    newUnits[factionId] = (newUnits[factionId] || 0) + 1;

    this.gameState.countries[countryIndex] = {
      ...country,
      units: newUnits,
    };

    this.gameState.unitCost = calculateUnitCost(this.gameState.countries);
  }

  private startGameLoop(): void {
    this.gameLoop = setInterval(() => {
      if (!this.gameState || this.gameState.phase !== 'playing') return;

      const now = Date.now();

      // Process battles
      this.gameState.countries = this.gameState.countries.map((country: Country) => {
        if (now >= country.nextBattleTime) {
          return processBattle(country, this.gameState!.countries, now);
        }
        return country;
      });

      // Update stock prices
      this.gameState.companies = this.gameState.companies.map((company: Company) => {
        if (now >= company.nextUpdateTime) {
          return updateStockPrice(company, now);
        }
        return company;
      });

      // Update unit cost
      this.gameState.unitCost = calculateUnitCost(this.gameState.countries);

      // Check for winner
      const winner = checkWinner(this.gameState.countries, this.gameState.factions);
      if (winner) {
        this.gameState.winner = winner;
        this.gameState.phase = 'ended';
        if (this.gameLoop) {
          clearInterval(this.gameLoop);
          this.gameLoop = null;
        }
      }

      this.broadcast({ type: 'gameState', state: this.gameState });
    }, GAME.SERVER_TICK_RATE);
  }

  private getPlayers(): Player[] {
    return Array.from(this.clients.values())
      .map((c) => c.player)
      .filter((p): p is Player => p !== null);
  }

  private broadcastLobbyUpdate(): void {
    this.broadcast({ type: 'lobbyUpdate', players: this.getPlayers() });
  }

  private broadcast(msg: ServerMessage): void {
    const data = JSON.stringify(msg);
    for (const client of this.clients.values()) {
      if (client.ws.readyState === WebSocket.OPEN) {
        client.ws.send(data);
      }
    }
  }
}

const port = parseInt(process.argv[2]) || GAME.SERVER_PORT;
new GameServer(port);
