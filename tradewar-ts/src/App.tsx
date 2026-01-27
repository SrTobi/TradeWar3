import { useUIStore } from '@/store/uiStore';
import { MainMenu } from '@/components/screens/MainMenu';
import { Lobby } from '@/components/screens/Lobby';
import { Game } from '@/components/screens/Game';

export function App() {
  const screen = useUIStore((s) => s.screen);

  switch (screen) {
    case 'menu':
      return <MainMenu />;
    case 'lobby':
      return <Lobby />;
    case 'game':
      return <Game />;
  }
}
