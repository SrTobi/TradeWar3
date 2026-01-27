import { Canvas } from '@react-three/fiber';
import { OrthographicCamera } from '@react-three/drei';
import { HexMap } from './HexMap';
import { Starfield } from './Starfield';

export function GameScene() {
  return (
    <Canvas
      style={{ position: 'absolute', top: 0, left: 0, width: '100%', height: '100%' }}
    >
      <OrthographicCamera
        makeDefault
        position={[0, 0, 10]}
        zoom={50}
      />
      <ambientLight intensity={1} />
      <Starfield />
      <HexMap />
    </Canvas>
  );
}
