import { useGameStore } from '@/store/gameStore';
import { calculateBulkUpgradeCost } from '@/game/stock';

const panelStyle: React.CSSProperties = {
  position: 'absolute',
  top: 0,
  left: 0,
  width: '320px',
  height: '100%',
  background: 'rgba(20, 26, 38, 0.92)',
  borderRight: '2px solid rgba(77, 102, 128, 0.5)',
  padding: '16px',
  display: 'flex',
  flexDirection: 'column',
  gap: '12px',
  overflow: 'auto',
  fontFamily: "'Segoe UI', system-ui, sans-serif",
};

const titleStyle: React.CSSProperties = {
  fontSize: '22px',
  fontWeight: 'bold',
  color: '#88aacc',
  textAlign: 'center',
  marginBottom: '8px',
  textShadow: '0 2px 4px rgba(0,0,0,0.5)',
  letterSpacing: '2px',
};

const statsRowStyle: React.CSSProperties = {
  display: 'flex',
  justifyContent: 'space-between',
  padding: '10px 12px',
  background: 'rgba(0, 0, 0, 0.3)',
  borderRadius: '6px',
  border: '1px solid rgba(77, 102, 128, 0.3)',
};

const labelStyle: React.CSSProperties = {
  color: '#8899aa',
  fontSize: '14px',
};

const valueStyle: React.CSSProperties = {
  fontSize: '16px',
  fontWeight: 'bold',
};

const companyRowStyle: React.CSSProperties = {
  display: 'flex',
  flexDirection: 'column',
  gap: '6px',
  padding: '10px 12px',
  background: 'rgba(0, 0, 0, 0.25)',
  borderRadius: '6px',
  border: '1px solid rgba(77, 102, 128, 0.2)',
};

const buttonStyle: React.CSSProperties = {
  padding: '6px 12px',
  background: 'rgba(51, 64, 89, 1)',
  border: '1px solid rgba(85, 119, 153, 0.5)',
  color: '#fff',
  cursor: 'pointer',
  borderRadius: '4px',
  fontSize: '12px',
  fontWeight: 'bold',
  transition: 'all 0.15s',
};

const buttonDisabledStyle: React.CSSProperties = {
  ...buttonStyle,
  background: 'rgba(30, 30, 46, 0.7)',
  color: '#556',
  cursor: 'not-allowed',
};

const separatorStyle: React.CSSProperties = {
  borderTop: '1px solid rgba(77, 102, 128, 0.4)',
  margin: '4px 0',
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
      <div style={titleStyle}>GALACTIC EXCHANGE</div>

      <div style={statsRowStyle}>
        <span style={labelStyle}>Credits</span>
        <span style={{ ...valueStyle, color: '#66ff99' }}>
          {local.money.toLocaleString()}
        </span>
      </div>

      <div style={statsRowStyle}>
        <span style={labelStyle}>Unit Cost</span>
        <span style={{ ...valueStyle, color: '#ffcc80' }}>
          {gameState.unitCost.toLocaleString()}
        </span>
      </div>

      <div style={statsRowStyle}>
        <span style={labelStyle}>Trade Amount</span>
        <span style={{ ...valueStyle, color: '#aabbff' }}>
          {local.bulkAmount}
        </span>
      </div>

      <button
        style={local.money >= upgradeCost ? {
          ...buttonStyle,
          padding: '10px',
          background: 'rgba(51, 85, 51, 1)',
          border: '1px solid rgba(85, 153, 85, 0.5)',
        } : {
          ...buttonDisabledStyle,
          padding: '10px',
        }}
        onClick={upgradeBulk}
        disabled={local.money < upgradeCost}
      >
        UPGRADE (${upgradeCost.toLocaleString()})
      </button>

      <div style={separatorStyle} />

      <div style={{ fontSize: '12px', color: '#667788', display: 'flex', justifyContent: 'space-between', padding: '0 4px' }}>
        <span style={{ flex: 1 }}>COMPANY</span>
        <span style={{ width: '70px', textAlign: 'right' }}>PRICE</span>
        <span style={{ width: '35px', textAlign: 'center' }}>+/-</span>
        <span style={{ width: '50px', textAlign: 'right' }}>OWNED</span>
      </div>

      {gameState.companies.map((company) => {
        const held = local.holdings[company.id] || 0;
        const priceChange = company.price - company.previousPrice;
        const canBuy = local.money >= company.price * local.bulkAmount;
        const canSell = held >= 1;

        // Price color based on relative value
        const priceRatio = company.price / 2000;
        let priceColor = '#ffffaa'; // Yellow (normal)
        if (priceRatio > 1.2) priceColor = '#ff8888'; // Red (expensive)
        else if (priceRatio < 0.8) priceColor = '#88ffaa'; // Green (cheap)

        return (
          <div key={company.id} style={companyRowStyle}>
            <div style={{ display: 'flex', alignItems: 'center', gap: '8px' }}>
              <span style={{ flex: 1, fontWeight: 'bold', color: '#ccd' }}>
                {company.name}
              </span>
              <span style={{ width: '70px', textAlign: 'right', color: priceColor, fontWeight: 'bold' }}>
                {company.price.toLocaleString()}
              </span>
              <span style={{
                width: '35px',
                textAlign: 'center',
                color: priceChange >= 0 ? '#4f8' : '#f44',
                fontWeight: 'bold',
              }}>
                {priceChange >= 0 ? '+' : '-'}
              </span>
              <span style={{
                width: '50px',
                textAlign: 'right',
                color: held > 0 ? '#88ffaa' : '#556677',
                fontWeight: 'bold',
              }}>
                {held}
              </span>
            </div>
            <div style={{ display: 'flex', gap: '8px' }}>
              <button
                style={canBuy ? buttonStyle : buttonDisabledStyle}
                onClick={() => buyStock(company)}
                disabled={!canBuy}
              >
                BUY {local.bulkAmount}
              </button>
              <button
                style={canSell ? buttonStyle : buttonDisabledStyle}
                onClick={() => sellStock(company)}
                disabled={!canSell}
              >
                SELL {Math.min(held, local.bulkAmount)}
              </button>
            </div>
          </div>
        );
      })}
    </div>
  );
}
