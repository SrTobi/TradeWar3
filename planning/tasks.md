# Task List - TradeWar 3: Galaxy

## Phase 1: Foundation & Main Menu
1. [x] Define core data models for the Game State (Players, Companies, Map).
2. [x] Implement a Screen switcher for transitioning between menus and the game.
3. [x] Create a basic LibGDX Screen for the main menu.
4. [x] Implement an animated starfield background for the main menu.
5. [x] Add "Start New Game" and "Quit" buttons to the main menu.

## Phase 2: Stock Market System
6. [x] Create `Company` and `StockMarket` data model classes.
7. [x] Implement price simulation logic using a clamped normal distribution (1-4000).
8. [x] Implement random time intervals for stock price updates (0.5 to 4 seconds).
9. [x] Create the left-side UI panel for the stock market (balance, companies, prices, holdings).
10. [x] Implement Buy/Sell buttons with bulk purchase (X) logic and "Buy Max" fallback.
11. [x] Add functionality to buy an increase of the bulk purchase amount X.
12. [x] Implement keyboard shortcuts (Space to repeat last action).
13. [x] Add button validation (disable when no effect).

## Phase 3: War Map & Hexagonal Grid
14. [x] Implement hexagonal grid system coordinate math and neighbor detection.
15. [x] Generate the hexagonal map and assign starting countries to 5 factions.
16. [x] Initialize unit counts for players (10) and neutral countries (5-20).
17. [x] Render the hex grid with faction-specific colors.
18. [x] Display unit counts within occupied hexagons.
19. [x] Implement unit placement logic on owned or adjacent hexes.

## Phase 4: Battle System & Game Logic
20. [x] Implement periodic battle checks (1-7 seconds per country).
21. [x] Implement loss calculation logic based on unit counts and adjacency bonuses.
22. [x] Implement country ownership updates upon unit elimination.
23. [x] Implement win condition monitoring (one non-neutral player remaining).

## Phase 5: Multiplayer
24. [x] Implement basic networking (Gdx.net) for synchronizing game state.
25. [x] Implement host/join logic in the main menu.
26. [x] Synchronize stock market updates across all clients.
27. [x] Synchronize unit placement and battle outcomes.
28. [x] Handle player disconnection and game state recovery.

## Phase 6: Polishing & Balancing
29. [ ] Tune normal distribution parameters for stock price volatility.
30. [ ] Refine battle mechanics and ensure network stability.
31. [ ] Add sound effects for UI and gameplay events.
32. [ ] Add visual feedback for stock price changes and active battles.
