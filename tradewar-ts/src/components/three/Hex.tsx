import { useRef, useMemo, useEffect } from 'react';
import { useFrame } from '@react-three/fiber';
import * as THREE from 'three';
import type { Country } from '@/types/game';
import { getFactionColor } from '@/types/game';
import { getCountryOwner } from '@/game/battle';
import { hexToPixel } from '@/game/hex';
import { useUIStore } from '@/store/uiStore';
import { useGameStore } from '@/store/gameStore';
import { Text } from '@react-three/drei';

interface HexProps {
  country: Country;
  size: number;
  onClick: () => void;
}

export function Hex({ country, size, onClick }: HexProps) {
  const meshRef = useRef<THREE.Mesh>(null);
  const glowRef = useRef<THREE.Mesh>(null);
  const mainMatRef = useRef<THREE.MeshBasicMaterial>(null);
  const glowMatRef = useRef<THREE.MeshBasicMaterial>(null);

  const hoveredHex = useUIStore((s) => s.hoveredHex);
  const setHoveredHex = useUIStore((s) => s.setHoveredHex);
  const localFactionId = useGameStore((s) => s.local.factionId);

  const owner = getCountryOwner(country);
  const isHovered = hoveredHex?.q === country.coords.q && hoveredHex?.r === country.coords.r;
  const isNeutral = owner === 'neutral';

  const position = useMemo(() => {
    const { x, y } = hexToPixel(country.coords, size);
    return [x, y, 0] as [number, number, number];
  }, [country.coords.q, country.coords.r, size]);

  const color = getFactionColor(owner, localFactionId);

  const hexShape = useMemo(() => {
    const shape = new THREE.Shape();
    // Flat-top hex orientation to match hexToPixel coordinates
    for (let i = 0; i < 6; i++) {
      const angle = (Math.PI / 3) * i;
      const x = (size * 0.95) * Math.cos(angle);
      const y = (size * 0.95) * Math.sin(angle);
      if (i === 0) shape.moveTo(x, y);
      else shape.lineTo(x, y);
    }
    shape.closePath();
    return shape;
  }, [size]);

  useEffect(() => {
    if (mainMatRef.current) {
      mainMatRef.current.color.set(color);
    }
    if (glowMatRef.current) {
      glowMatRef.current.color.set(color);
    }
  }, [color]);

  useEffect(() => {
    if (mainMatRef.current) {
      mainMatRef.current.opacity = isHovered ? 0.9 : 0.7;
    }
  }, [isHovered]);

  useFrame(() => {
    if (glowRef.current && !isNeutral) {
      const scale = 1 + Math.sin(Date.now() * 0.003) * 0.05;
      glowRef.current.scale.setScalar(scale);
    }
  });

  const unitText = Object.entries(country.units)
    .filter(([, n]) => n > 0)
    .map(([, n]) => n)
    .join('/');

  return (
    <group position={position}>
      {!isNeutral && (
        <mesh ref={glowRef} position={[0, 0, -0.1]}>
          <shapeGeometry args={[hexShape]} />
          <meshBasicMaterial ref={glowMatRef} color={color} transparent opacity={0.3} />
        </mesh>
      )}
      <mesh
        ref={meshRef}
        onClick={onClick}
        onPointerEnter={() => setHoveredHex(country.coords)}
        onPointerLeave={() => setHoveredHex(null)}
      >
        <shapeGeometry args={[hexShape]} />
        <meshBasicMaterial
          ref={mainMatRef}
          color={color}
          transparent
          opacity={isHovered ? 0.9 : 0.7}
        />
      </mesh>
      <lineSegments position={[0, 0, 0.01]}>
        <edgesGeometry args={[new THREE.ShapeGeometry(hexShape)]} />
        <lineBasicMaterial color={isHovered ? '#ffffff' : '#333333'} />
      </lineSegments>
      {unitText && (
        <Text
          position={[0, 0, 0.1]}
          fontSize={size * 0.4}
          color="#ffffff"
          anchorX="center"
          anchorY="middle"
          outlineWidth={0.02}
          outlineColor="#000000"
        >
          {unitText}
        </Text>
      )}
    </group>
  );
}
