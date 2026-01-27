import { useRef, useMemo } from 'react';
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

function lighten(color: THREE.Color, amount: number): THREE.Color {
  return new THREE.Color(
    Math.min(1, color.r + amount),
    Math.min(1, color.g + amount),
    Math.min(1, color.b + amount)
  );
}

function darken(color: THREE.Color, amount: number): THREE.Color {
  return new THREE.Color(
    Math.max(0, color.r - amount),
    Math.max(0, color.g - amount),
    Math.max(0, color.b - amount)
  );
}

export function Hex({ country, size, onClick }: HexProps) {
  const groupRef = useRef<THREE.Group>(null);
  const glowRingsRef = useRef<THREE.Group>(null);
  const innerHighlightRef = useRef<THREE.LineLoop>(null);
  const pulseTimeRef = useRef(Math.random() * Math.PI * 2);

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

  const baseColor = useMemo(
    () => new THREE.Color(getFactionColor(owner, localFactionId)),
    [owner, localFactionId]
  );

  // Hex vertices for flat-top orientation
  const hexVertices = useMemo(() => {
    const verts: THREE.Vector2[] = [];
    for (let i = 0; i < 6; i++) {
      const angle = (Math.PI / 3) * i;
      verts.push(new THREE.Vector2(
        size * 0.95 * Math.cos(angle),
        size * 0.95 * Math.sin(angle)
      ));
    }
    return verts;
  }, [size]);

  // Gradient hex shape (6 triangular wedges with center-to-edge gradient)
  const hexGeometry = useMemo(() => {
    const geometry = new THREE.BufferGeometry();
    const positions: number[] = [];
    const colors: number[] = [];

    const hoverBoost = 0;
    const centerColor = lighten(baseColor, 0.15 + hoverBoost);
    const edgeColor = darken(baseColor, 0.1);

    for (let i = 0; i < 6; i++) {
      const v1 = hexVertices[i];
      const v2 = hexVertices[(i + 1) % 6];

      // Triangle: center, v1, v2
      positions.push(0, 0, 0);
      positions.push(v1.x, v1.y, 0);
      positions.push(v2.x, v2.y, 0);

      // Colors: center bright, edges darker
      colors.push(centerColor.r, centerColor.g, centerColor.b);
      colors.push(edgeColor.r, edgeColor.g, edgeColor.b);
      colors.push(edgeColor.r, edgeColor.g, edgeColor.b);
    }

    geometry.setAttribute('position', new THREE.Float32BufferAttribute(positions, 3));
    geometry.setAttribute('color', new THREE.Float32BufferAttribute(colors, 3));
    return geometry;
  }, [hexVertices, baseColor]);

  // Inner highlight ring (70% size)
  const innerRingGeometry = useMemo(() => {
    const points: THREE.Vector3[] = [];
    for (let i = 0; i <= 6; i++) {
      const angle = (Math.PI / 3) * (i % 6);
      points.push(new THREE.Vector3(
        size * 0.65 * Math.cos(angle),
        size * 0.65 * Math.sin(angle),
        0.02
      ));
    }
    return new THREE.BufferGeometry().setFromPoints(points);
  }, [size]);

  // Border geometry
  const borderGeometry = useMemo(() => {
    const points: THREE.Vector3[] = [];
    for (let i = 0; i <= 6; i++) {
      const angle = (Math.PI / 3) * (i % 6);
      points.push(new THREE.Vector3(
        size * 0.95 * Math.cos(angle),
        size * 0.95 * Math.sin(angle),
        0.01
      ));
    }
    return new THREE.BufferGeometry().setFromPoints(points);
  }, [size]);

  // Glow ring geometries (3 concentric rings)
  const glowRingGeometries = useMemo(() => {
    return [0, 1, 2].map((ring) => {
      const ringSize = size * (1.0 + ring * 0.08);
      const shape = new THREE.Shape();
      for (let i = 0; i < 6; i++) {
        const angle = (Math.PI / 3) * i;
        const x = ringSize * Math.cos(angle);
        const y = ringSize * Math.sin(angle);
        if (i === 0) shape.moveTo(x, y);
        else shape.lineTo(x, y);
      }
      shape.closePath();
      return new THREE.ShapeGeometry(shape);
    });
  }, [size]);

  useFrame((_, delta) => {
    pulseTimeRef.current += delta;
    const pulseTime = pulseTimeRef.current;

    // Update glow rings with pulsing
    if (glowRingsRef.current && !isNeutral) {
      const glowPulse = (Math.sin(pulseTime * 2) + 1) / 2 * 0.15 + 0.1;
      glowRingsRef.current.children.forEach((child, i) => {
        const mesh = child as THREE.Mesh;
        const mat = mesh.material as THREE.MeshBasicMaterial;
        mat.opacity = glowPulse * (1 - i * 0.3);
      });
    }

    // Update inner highlight
    if (innerHighlightRef.current) {
      const mat = innerHighlightRef.current.material as THREE.LineBasicMaterial;
      const highlightColor = lighten(baseColor, 0.3);
      mat.color = highlightColor;
      mat.opacity = isNeutral ? 0.1 : 0.3;
    }
  });

  const unitEntries = Object.entries(country.units).filter(([, n]) => n > 0);
  const borderColor = isHovered ? new THREE.Color('#ffffff') : darken(baseColor, 0.3);

  return (
    <group ref={groupRef} position={position}>
      {/* Glow rings (behind main hex) */}
      {!isNeutral && (
        <group ref={glowRingsRef} position={[0, 0, -0.1]}>
          {glowRingGeometries.map((geo, i) => (
            <mesh key={i} geometry={geo}>
              <meshBasicMaterial
                color={baseColor}
                transparent
                opacity={0.15 * (1 - i * 0.3)}
                depthWrite={false}
              />
            </mesh>
          ))}
        </group>
      )}

      {/* Main hex with gradient */}
      <mesh
        geometry={hexGeometry}
        onClick={onClick}
        onPointerEnter={() => setHoveredHex(country.coords)}
        onPointerLeave={() => setHoveredHex(null)}
      >
        <meshBasicMaterial
          vertexColors
          transparent
          opacity={isHovered ? 0.95 : 0.85}
        />
      </mesh>

      {/* Border */}
      <lineLoop geometry={borderGeometry}>
        <lineBasicMaterial
          color={borderColor}
          linewidth={isHovered ? 3 : 2}
        />
      </lineLoop>

      {/* Inner highlight */}
      <lineLoop ref={innerHighlightRef} geometry={innerRingGeometry}>
        <lineBasicMaterial
          color={lighten(baseColor, 0.3)}
          transparent
          opacity={0.3}
        />
      </lineLoop>

      {/* Unit count badges */}
      {unitEntries.length > 0 && (
        <group position={[0, 0, 0.1]}>
          {unitEntries.map(([factionId, count], idx) => {
            const badgeColor = new THREE.Color(getFactionColor(factionId, localFactionId));
            const yOffset = (idx - (unitEntries.length - 1) / 2) * size * 0.35;
            return (
              <group key={factionId} position={[0, yOffset, 0]}>
                {/* Badge background shadow */}
                <mesh position={[0.02, -0.02, -0.01]}>
                  <planeGeometry args={[size * 0.6, size * 0.28]} />
                  <meshBasicMaterial color="#000000" transparent opacity={0.3} />
                </mesh>
                {/* Badge background */}
                <mesh>
                  <planeGeometry args={[size * 0.6, size * 0.28]} />
                  <meshBasicMaterial color={badgeColor} transparent opacity={0.9} />
                </mesh>
                {/* Unit count text */}
                <Text
                  position={[0, 0, 0.01]}
                  fontSize={size * 0.22}
                  color="#ffffff"
                  anchorX="center"
                  anchorY="middle"
                  outlineWidth={0.01}
                  outlineColor="#000000"
                >
                  {count}
                </Text>
              </group>
            );
          })}
        </group>
      )}
    </group>
  );
}
