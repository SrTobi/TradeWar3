# Development Plan - TradeWar 3: Galaxy

## Overview
TradeWar 3: Galaxy is a strategy game where players earn money through a stock market simulation to fund their war machinery. The goal is to dominate a hexagonal map by strategically placing units and defeating three AI opponents. The game features a split-screen interface: the left side handles the stock market, and the right side displays the hexagonal war map.

## Development Phases

### Phase 1: Foundation & Main Menu
- **Setup Project Structure**: Define core data models for the Game State, including Players, Companies, and the Map.
- **Main Menu Implementation**:
    - Create a basic LibGDX `Screen` for the main menu.
    - Implement an animated starfield background.
    - Add "Start New Game" and "Quit" buttons.
- **Screen Management**: Implement a simple screen switcher to transition between the menu and the game.

### Phase 2: Stock Market System
- **Data Model**: Create `Company` and `StockMarket` classes.
- **Price Simulation**: 
    - Implement independent price updates for each stock using a normal distribution (clamped between 1 and 4000).
    - Use random time intervals (0.5 to 4 seconds) for updates.
- **UI Implementation**:
    - Create the left-side panel showing balance, companies, prices, and stock holdings.
    - Implement Buy/Sell buttons with bulk purchase (X) logic.
    - Add "Buy X increase" functionality.
    - Implement keyboard shortcuts (Space to repeat last action).
- **Validation**: Ensure buttons are disabled when actions have no effect (e.g., insufficient funds).

### Phase 3: War Map & Hexagonal Grid
- **Map Generation**: 
    - Implement a hexagonal grid system.
    - Randomly assign starting countries to 4 factions (Player + 3 AI) and the Neutral faction.
    - Initialize unit counts (10 for players, 5-20 for Neutral).
- **Visuals**:
    - Render the hex grid with faction-specific colors (Blue, Red, Yellow, Orange, Gray).
    - Display unit counts within occupied hexagons.
- **Unit Placement**:
    - Implement logic for placing units on owned or adjacent hexes.

### Phase 4: Battle System & Game Logic
- **Battle Engine**:
    - Implement periodic battle checks (1-7 seconds per country).
    - Calculate losses based on unit counts and adjacency bonuses.
- **Occupation Logic**: Update country ownership when a faction's units are wiped out.
- **Win Condition**: Monitor faction status and trigger the end game when only one non-neutral player remains.

### Phase 5: AI Opponents
- **Stock Market AI**: Implement AI logic to buy low and sell high, interacting with the market through the same constraints as the user.
- **Strategic AI**: Implement unit placement logic for AI to expand and defend territory.

### Phase 6: Polishing & Balancing
- Tune the normal distribution parameters for stock price changes.
- Refine battle mechanics and AI difficulty.
- Add sound effects and UI feedback.

## Dependencies & Considerations
- **Framework**: Built using Scala 3 and LibGDX.
- **UI Library**: Consider using `scene2d.ui` for the stock market panel and buttons.
- **Randomness**: Ensure seeds are handled correctly for reproducible/fair game starts.
- **Performance**: Monitor performance of the hexagonal grid rendering and simultaneous battle calculations as the map grows.

## Risks
- **AI Complexity**: Making the AI "smart" enough to be challenging without being unfair.
- **Balance**: The synergy between stock market success and war map dominance needs careful tuning to avoid "snowball" effects that make the game too easy or too hard.
- **Hexagonal Math**: Implementing efficient neighbor detection and rendering for the hex grid.
