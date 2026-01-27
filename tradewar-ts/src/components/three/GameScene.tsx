import { useEffect, useRef } from 'react';
import { Canvas, useThree } from '@react-three/fiber';
import { OrthographicCamera } from '@react-three/drei';
import * as THREE from 'three';
import { HexMap } from './HexMap';
import { Starfield } from './Starfield';
import { GAME } from '@/game/constants';

// Calculate the world bounds of the hex map
const HEX_SIZE = 1;
const MAP_RADIUS = GAME.MAP_RADIUS;
// For flat-top hexes, width spans about 1.5 * size per hex horizontally
// Height spans about sqrt(3) * size per hex vertically
const MAP_WIDTH = (MAP_RADIUS * 2 + 1) * HEX_SIZE * 1.75;
const MAP_HEIGHT = (MAP_RADIUS * 2 + 1) * HEX_SIZE * Math.sqrt(3);
const PADDING = 1.5; // Extra padding around the map

function CameraController() {
  const { camera, size } = useThree();
  const cameraRef = useRef(camera as THREE.OrthographicCamera);

  useEffect(() => {
    cameraRef.current = camera as THREE.OrthographicCamera;
  }, [camera]);

  useEffect(() => {
    const updateZoom = () => {
      const cam = cameraRef.current;
      if (!cam) return;

      // Calculate zoom to fit the map in the viewport
      const worldWidth = MAP_WIDTH + PADDING * 2;
      const worldHeight = MAP_HEIGHT + PADDING * 2;

      // Zoom is pixels per world unit
      const zoomX = size.width / worldWidth;
      const zoomY = size.height / worldHeight;

      // Use the smaller zoom to ensure the entire map fits
      cam.zoom = Math.min(zoomX, zoomY);
      cam.updateProjectionMatrix();
    };

    updateZoom();
  }, [size.width, size.height]);

  return null;
}

export function GameScene() {
  return (
    <Canvas
      style={{ position: 'absolute', top: 0, left: 0, width: '100%', height: '100%' }}
      gl={{ antialias: true, alpha: false }}
    >
      <OrthographicCamera
        makeDefault
        position={[0, 0, 10]}
        zoom={50}
        near={0.1}
        far={100}
      />
      <CameraController />
      <color attach="background" args={['#050508']} />
      <Starfield />
      <HexMap />
    </Canvas>
  );
}
