import { useRef, useEffect } from 'react';
import { useFrame } from '@react-three/fiber';
import * as THREE from 'three';
import type { Country, HexCoord } from '@/types/game';
import { hexToPixel } from '@/game/hex';

interface Particle {
  x: number;
  y: number;
  vx: number;
  vy: number;
  life: number;
  maxLife: number;
  color: THREE.Color;
  size: number;
}

interface Ripple {
  x: number;
  y: number;
  life: number;
  maxLife: number;
}

interface ParticlesProps {
  countries: Country[];
  size: number;
}

const MAX_PARTICLES = 300;
const PARTICLES_PER_BATTLE = 15;

export function Particles({ countries, size }: ParticlesProps) {
  const particlesRef = useRef<Particle[]>([]);
  const ripplesRef = useRef<Ripple[]>([]);
  const prevUnitsRef = useRef<Map<string, Record<string, number>>>(new Map());
  const meshRef = useRef<THREE.InstancedMesh>(null);
  const rippleGroupRef = useRef<THREE.Group>(null);

  const tempMatrix = useRef(new THREE.Matrix4());
  const tempColor = useRef(new THREE.Color());

  // Detect battles by comparing unit counts
  useEffect(() => {
    const prevUnits = prevUnitsRef.current;

    for (const country of countries) {
      const key = `${country.coords.q},${country.coords.r}`;
      const prev = prevUnits.get(key);

      if (prev) {
        // Check if any faction lost units (battle occurred)
        let battleOccurred = false;
        for (const [factionId, count] of Object.entries(country.units)) {
          const prevCount = prev[factionId] || 0;
          if (count < prevCount) {
            battleOccurred = true;
            break;
          }
        }

        if (battleOccurred && particlesRef.current.length < MAX_PARTICLES) {
          const pos = hexToPixel(country.coords, size);
          emitBattleParticles(pos.x, pos.y, particlesRef.current);
        }
      }

      prevUnits.set(key, { ...country.units });
    }
  }, [countries, size]);

  function emitBattleParticles(x: number, y: number, particles: Particle[]) {
    for (let i = 0; i < PARTICLES_PER_BATTLE; i++) {
      const angle = Math.random() * Math.PI * 2;
      const speed = 50 + Math.random() * 100;

      particles.push({
        x,
        y,
        vx: Math.cos(angle) * speed * 0.02,
        vy: Math.sin(angle) * speed * 0.02,
        life: 0,
        maxLife: 0.3 + Math.random() * 0.5,
        color: new THREE.Color().setHSL(Math.random() * 0.1 + 0.05, 1, 0.6), // Orange-red
        size: 0.03 + Math.random() * 0.06,
      });
    }
  }

  useFrame((_, delta) => {
    const particles = particlesRef.current;
    const ripples = ripplesRef.current;

    // Update particles
    for (let i = particles.length - 1; i >= 0; i--) {
      const p = particles[i];
      p.life += delta;

      if (p.life >= p.maxLife) {
        particles.splice(i, 1);
        continue;
      }

      // Physics
      p.vy -= 1.0 * delta; // Gravity
      p.vx *= 0.98; // Drag
      p.x += p.vx;
      p.y += p.vy;
    }

    // Update ripples
    for (let i = ripples.length - 1; i >= 0; i--) {
      const r = ripples[i];
      r.life += delta;
      if (r.life >= r.maxLife) {
        ripples.splice(i, 1);
      }
    }

    // Update instanced mesh
    if (meshRef.current) {
      for (let i = 0; i < MAX_PARTICLES; i++) {
        if (i < particles.length) {
          const p = particles[i];
          const progress = p.life / p.maxLife;
          const alpha = 1 - progress;
          const scale = p.size * (1 - progress * 0.5);

          tempMatrix.current.makeScale(scale, scale, scale);
          tempMatrix.current.setPosition(p.x, p.y, 0.2);
          meshRef.current.setMatrixAt(i, tempMatrix.current);

          tempColor.current.copy(p.color).multiplyScalar(alpha);
          meshRef.current.setColorAt(i, tempColor.current);
        } else {
          // Hide unused instances
          tempMatrix.current.makeScale(0, 0, 0);
          meshRef.current.setMatrixAt(i, tempMatrix.current);
        }
      }
      meshRef.current.instanceMatrix.needsUpdate = true;
      if (meshRef.current.instanceColor) {
        meshRef.current.instanceColor.needsUpdate = true;
      }
    }

    // Update ripple meshes
    if (rippleGroupRef.current) {
      rippleGroupRef.current.children.forEach((child, i) => {
        if (i < ripples.length) {
          const r = ripples[i];
          const progress = r.life / r.maxLife;
          const radius = size * (0.5 + progress * 1.5);
          const alpha = (1 - progress) * 0.8;

          child.position.set(r.x, r.y, 0.15);
          child.scale.setScalar(radius);
          (child as THREE.Mesh).material = new THREE.MeshBasicMaterial({
            color: '#ffffff',
            transparent: true,
            opacity: alpha,
            side: THREE.DoubleSide,
          });
          child.visible = true;
        } else {
          child.visible = false;
        }
      });
    }
  });

  return (
    <group>
      {/* Battle particles */}
      <instancedMesh ref={meshRef} args={[undefined, undefined, MAX_PARTICLES]}>
        <circleGeometry args={[1, 8]} />
        <meshBasicMaterial transparent depthWrite={false} />
      </instancedMesh>

      {/* Click ripples */}
      <group ref={rippleGroupRef}>
        {Array.from({ length: 10 }, (_, i) => (
          <mesh key={i} visible={false}>
            <ringGeometry args={[0.9, 1, 32]} />
            <meshBasicMaterial transparent opacity={0} side={THREE.DoubleSide} />
          </mesh>
        ))}
      </group>
    </group>
  );
}

// Export function to trigger ripple from outside
export function createRippleEmitter() {
  const ripples: Ripple[] = [];

  return {
    emit: (coords: HexCoord, size: number) => {
      const pos = hexToPixel(coords, size);
      ripples.push({
        x: pos.x,
        y: pos.y,
        life: 0,
        maxLife: 0.4,
      });
    },
    ripples,
  };
}
