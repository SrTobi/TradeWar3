import { useGameStore } from '@/store/gameStore';
import { calculateBulkUpgradeCost } from '@/game/stock';

const panelStyle: React.CSSProperties = {
  position: 'absolute',
  top: 0,
  left: 0,
  width: '320px',
  height: '100%',
  background: 'rgba(10, 10, 30, 0.9)',
  borderRight: '2px solid #334',
  padding: '16px',
  display: 'flex',
  flexDirection: 'column',
  gap: '12px',
  overflow: 'auto',
};

const headerStyle: React.CSSProperties = {
  fontSize: '20px',
  fontWeight: 'bold',
  color: '#4af',
  textAlign: 'center',
  marginBottom: '8px',
};

const statsRowStyle: React.CSSProperties = {
  display: 'flex',
  justifyContent: 'space-between',
  padding: '8px',
  background: 'rgba(0, 0, 0, 0.3)',
  borderRadius: '4px',
};

const companyRowStyle: React.CSSProperties = {
  display: 'flex',
  flexDirection: 'column',
  gap: '4px',
  padding: '8px',
  background: 'rgba(0, 0, 0, 0.3)',
  borderRadius: '4px',
};

const buttonStyle: React.CSSProperties = {
  padding: '4px 8px',
  background: '#335',
  border: '1px solid #557',
  color: '#fff',
  cursor: 'pointer',
  borderRadius: '3px',
  fontSize: '12px',
};

export function StockPanel() {
  const gameState = useGameStore((s) => s.gameState);
  const local = useGameStore((s) => s.local);
  const buyStock = useGameStore((s) => s.buyStock);
  const sellStock = useGameStore((s) => s.sellStock);
  const upgradeBulk = useGameStore((s) => s.upgradeBulk);

  if (!gameState) return null;

  const upgradeCost = calculateBulkUpgradeCost(local.bulkAmount);

  return (
    <div style={panelStyle}>
      <div style={headerStyle}>STOCK MARKET</div>

      <div style={statsRowStyle}>
        <span>Balance:</span>
        <span style={{ color: '#4f8' }}>${local.money.toFixed(0)}</span>
      </div>

      <div style={statsRowStyle}>
        <span>Unit Cost:</span>
        <span style={{ color: '#fa4' }}>${gameState.unitCost}</span>
      </div>

      <div style={statsRowStyle}>
        <span>Trade Bulk:</span>
        <span style={{ color: '#aaf' }}>{local.bulkAmount}</span>
      </div>

      <button
        style={{ ...buttonStyle, padding: '8px' }}
        onClick={upgradeBulk}
        disabled={local.money < upgradeCost}
      >
        Upgrade Bulk (${upgradeCost.toFixed(0)})
      </button>

      <div style={{ borderTop: '1px solid #334', margin: '8px 0' }} />

      {gameState.companies.map((company) => {
        const held = local.holdings[company.id] || 0;
        const priceChange = company.price - company.previousPrice;
        const changeColor = priceChange >= 0 ? '#4f8' : '#f44';

        return (
          <div key={company.id} style={companyRowStyle}>
            <div style={{ display: 'flex', justifyContent: 'space-between' }}>
              <span style={{ fontWeight: 'bold' }}>{company.name}</span>
              <span style={{ color: '#ff8' }}>${company.price.toFixed(0)}</span>
            </div>
            <div style={{ display: 'flex', justifyContent: 'space-between', fontSize: '12px' }}>
              <span style={{ color: changeColor }}>
                {priceChange >= 0 ? '+' : ''}{priceChange.toFixed(0)}
              </span>
              <span style={{ color: held > 0 ? '#4f8' : '#666' }}>
                Held: {held}
              </span>
            </div>
            <div style={{ display: 'flex', gap: '8px', marginTop: '4px' }}>
              <button
                style={buttonStyle}
                onClick={() => buyStock(company)}
                disabled={local.money < company.price * local.bulkAmount}
              >
                Buy {local.bulkAmount}
              </button>
              <button
                style={buttonStyle}
                onClick={() => sellStock(company)}
                disabled={held < 1}
              >
                Sell {Math.min(held, local.bulkAmount)}
              </button>
            </div>
          </div>
        );
      })}
    </div>
  );
}
