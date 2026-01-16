# Task List - TradeWar 3: Galaxy

## Phase 1: Foundation & Main Menu
1. [ ] Define core data models for the Game State (Players, Companies, Map).
2. [ ] Implement a Screen switcher for transitioning between menus and the game.
3. [ ] Create a basic LibGDX Screen for the main menu.
4. [ ] Implement an animated starfield background for the main menu.
5. [ ] Add "Start New Game" and "Quit" buttons to the main menu.

## Phase 2: Stock Market System
6. [ ] Create `Company` and `StockMarket` data model classes.
7. [ ] Implement price simulation logic using a clamped normal distribution (1-4000).
8. [ ] Implement random time intervals for stock price updates (0.5 to 4 seconds).
9. [ ] Create the left-side UI panel for the stock market (balance, companies, prices, holdings).
10. [ ] Implement Buy/Sell buttons with bulk purchase (X) logic and "Buy Max" fallback.
11. [ ] Add functionality to buy an increase of the bulk purchase amount X.
12. [ ] Implement keyboard shortcuts (Space to repeat last action).
13. [ ] Add button validation (disable when no effect).

## Phase 3: War Map & Hexagonal Grid
14. [ ] Implement hexagonal grid system coordinate math and neighbor detection.
15. [ ] Generate the hexagonal map and assign starting countries to 5 factions.
16. [ ] Initialize unit counts for players (10) and neutral countries (5-20).
17. [ ] Render the hex grid with faction-specific colors.
18. [ ] Display unit counts within occupied hexagons.
19. [ ] Implement unit placement logic on owned or adjacent hexes.

## Phase 4: Battle System & Game Logic
20. [ ] Implement periodic battle checks (1-7 seconds per country).
21. [ ] Implement loss calculation logic based on unit counts and adjacency bonuses.
22. [ ] Implement country ownership updates upon unit elimination.
23. [ ] Implement win condition monitoring (one non-neutral player remaining).

## Phase 5: AI Opponents
24. [ ] Implement Stock Market AI (buy low, sell high) following same constraints as player.
25. [ ] Implement Strategic AI for unit placement (expand and defend).

## Phase 6: Polishing & Balancing
26. [ ] Tune normal distribution parameters for stock price volatility.
27. [ ] Refine battle mechanics and AI difficulty levels.
28. [ ] Add sound effects for UI and gameplay events.
29. [ ] Add visual feedback for stock price changes and active battles.
