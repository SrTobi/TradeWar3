TradeWar 3 - Galaxy
===================

TradeWar 3 is the next game in the TradeWar game series.
The game is about making money by buying and selling stocks and then
investing the profits into the war machinery to defeat your enemies.


## The main menu

The game has a main menu where the user can select to
- start a new game
- quit the game

The background is a starfield that is slightly animated.

When the user selects to start a new game, the game starts immediately.

## The game screen

The game screen is devided into two parts:
- the left side is the stock market
- the right side is the war map

### The stock market

At the top of the stock market is the amount of money in € the user has.
Below the stock market shows a list of 5 company names of which the user can buy stocks.
The 5 company names are randomly selected from a list of company names.
Next to each company name is the current price of the stock and how many stocks the user has.
Next to that are two buttons to buy and sell the stock.
Every time one of the buttons is pressed, the amount of stocks is increased or decreased by X
(except if the user has no stocks left to sell or not enough money to buy the stock).
After a button is pressed, it stays selected. If the user presses space, it is as if the button was pressed again.
Under the stock market there is another buy option to buy an increase of X by one.
This helps to bulk buy more stocks.
If the user has not enough money to buy X stocks, a press on the buy button will buy as many
stocks as the user can afford.
If a button would have no effect, it is disabled.

The stock prices will always be between 1 and 4000.
Each stock changes its price independently after a random time interval between 0.5 and 4 seconds.
The new price is randomly selected with a normal distribution around the current price.
The correct values for the distribution have to be determined.
The closer the price is to one of the extreme values, the less likeley it is.

### The war map

The war map shows a hexagonal map with a grid of hexagons.
Each hexagon represents a country.
The hexagons are colored based on the country's occupation.
The user itself is blue.
There are always 3 opponents with red, yellow and orange color.
Additionally, there is a neutral player with gray color.
In the beginning every faction has one country randomly assigned to them.
Except for the neutral player, it owns the remaining countries.
Every country has a number of units per faction.
They are shown as number within the hexagon (but only if the number is greater than 0).
Every player starts with 10 units in their start country.
The neutral player starts with a random number of units between 5 and 20 in each country.

Now factions can position units on the map, but only if the country belongs to them or
if they own an adjacent country (owning here means there is no enemy unit in the hexagon).

If a country has units from different factions they start waring.
For each country every few seconds (randomly between 1 and 7 seconds) a battle appears.
The more units a faction has the more the enemies will lose.
Also, players get bonus strength for each surrounding country they own.
If one faction has no units left, the war in that country ends and the country belongs now
to the player with the remaining units.

### Winning

The game ends when only one player has countries left (except for the neutral player).

### AI

The 3 opponents are implemented via an AI. They have to interact with the stock market
exactly like the user. They will see the same prices and can buy and sell stocks.
(The user is not shown the stocks of the other players).

It will then also interact with the war map and try to smartly position units.