import { useMemo, useRef } from 'react';
import { useFrame } from '@react-three/fiber';
import * as THREE from 'three';

interface Star {
  x: number;
  y: number;
  speed: number;
  size: number;
  color: THREE.Color;
  twinklePhase: number;
  twinkleSpeed: number;
  layer: number;
}

interface Nebula {
  x: number;
  y: number;
  radius: number;
  color: THREE.Color;
  speed: number;
}

interface ShootingStar {
  x: number;
  y: number;
  angle: number;
  speed: number;
  life: number;
  maxLife: number;
  active: boolean;
}

const STAR_COUNT = 400;
const NEBULA_COUNT = 8;

function createStars(): Star[] {
  const stars: Star[] = [];

  for (let i = 0; i < STAR_COUNT; i++) {
    // Three layers: far (slow/small), mid, close (fast/large)
    const layer = Math.random() < 0.5 ? 0 : Math.random() < 0.6 ? 1 : 2;

    let speed: number, size: number;
    if (layer === 0) {
      speed = 5 + Math.random() * 10;
      size = 0.3 + Math.random() * 0.8;
    } else if (layer === 1) {
      speed = 15 + Math.random() * 25;
      size = 0.6 + Math.random() * 1.2;
    } else {
      speed = 30 + Math.random() * 50;
      size = 1.0 + Math.random() * 2.0;
    }

    // Star colors: white-blue, blue, yellow, orange
    let color: THREE.Color;
    const colorRand = Math.random();
    if (colorRand < 0.5) {
      color = new THREE.Color(0.9 + Math.random() * 0.1, 0.9 + Math.random() * 0.1, 1.0);
    } else if (colorRand < 0.75) {
      color = new THREE.Color(0.7 + Math.random() * 0.3, 0.8 + Math.random() * 0.2, 1.0);
    } else if (colorRand < 0.9) {
      color = new THREE.Color(1.0, 1.0, 0.7 + Math.random() * 0.3);
    } else {
      color = new THREE.Color(1.0, 0.7 + Math.random() * 0.2, 0.5 + Math.random() * 0.2);
    }

    stars.push({
      x: (Math.random() - 0.5) * 60,
      y: (Math.random() - 0.5) * 40,
      speed,
      size: size * 0.02,
      color,
      twinklePhase: Math.random() * Math.PI * 2,
      twinkleSpeed: 1 + Math.random() * 3,
      layer,
    });
  }

  return stars;
}

function createNebulas(): Nebula[] {
  const nebulaColors = [
    new THREE.Color(0.2, 0.1, 0.4),   // Purple
    new THREE.Color(0.1, 0.2, 0.4),   // Deep blue
    new THREE.Color(0.3, 0.1, 0.2),   // Magenta
    new THREE.Color(0.1, 0.3, 0.3),   // Teal
    new THREE.Color(0.4, 0.2, 0.1),   // Orange
  ];

  return Array.from({ length: NEBULA_COUNT }, () => ({
    x: (Math.random() - 0.5) * 80,
    y: (Math.random() - 0.5) * 50,
    radius: 3 + Math.random() * 4,
    color: nebulaColors[Math.floor(Math.random() * nebulaColors.length)],
    speed: 3 + Math.random() * 8,
  }));
}

export function Starfield() {
  const starsRef = useRef(createStars());
  const nebulasRef = useRef(createNebulas());
  const shootingStarRef = useRef<ShootingStar>({
    x: 0, y: 0, angle: 0, speed: 0, life: 0, maxLife: 0, active: false
  });
  const nextShootingStarRef = useRef(2 + Math.random() * 4);
  const pointsRef = useRef<THREE.Points>(null);
  const nebulaGroupRef = useRef<THREE.Group>(null);
  const shootingStarMeshRef = useRef<THREE.Group>(null);

  const [starPositions, starColors, starSizes] = useMemo(() => {
    const positions = new Float32Array(STAR_COUNT * 3);
    const colors = new Float32Array(STAR_COUNT * 3);
    const sizes = new Float32Array(STAR_COUNT);

    starsRef.current.forEach((star, i) => {
      positions[i * 3] = star.x;
      positions[i * 3 + 1] = star.y;
      positions[i * 3 + 2] = -15 - star.layer * 2;
      colors[i * 3] = star.color.r;
      colors[i * 3 + 1] = star.color.g;
      colors[i * 3 + 2] = star.color.b;
      sizes[i] = star.size;
    });

    return [positions, colors, sizes];
  }, []);

  useFrame((_, delta) => {
    const stars = starsRef.current;
    const nebulas = nebulasRef.current;
    const shootingStar = shootingStarRef.current;

    // Update stars
    if (pointsRef.current) {
      const positions = pointsRef.current.geometry.attributes.position.array as Float32Array;
      const colors = pointsRef.current.geometry.attributes.color.array as Float32Array;

      for (let i = 0; i < stars.length; i++) {
        const star = stars[i];
        star.x -= star.speed * delta * 0.1;
        star.twinklePhase += star.twinkleSpeed * delta;

        if (star.x < -30) {
          star.x = 30;
          star.y = (Math.random() - 0.5) * 40;
        }

        positions[i * 3] = star.x;
        positions[i * 3 + 1] = star.y;

        // Twinkling effect
        const twinkle = (Math.sin(star.twinklePhase) + 1) / 2 * 0.4 + 0.6;
        colors[i * 3] = star.color.r * twinkle;
        colors[i * 3 + 1] = star.color.g * twinkle;
        colors[i * 3 + 2] = star.color.b * twinkle;
      }

      pointsRef.current.geometry.attributes.position.needsUpdate = true;
      pointsRef.current.geometry.attributes.color.needsUpdate = true;
    }

    // Update nebulas
    if (nebulaGroupRef.current) {
      nebulas.forEach((nebula, i) => {
        nebula.x -= nebula.speed * delta * 0.05;
        if (nebula.x < -45) {
          nebula.x = 45;
          nebula.y = (Math.random() - 0.5) * 50;
        }
        const child = nebulaGroupRef.current!.children[i];
        if (child) {
          child.position.set(nebula.x, nebula.y, -20);
        }
      });
    }

    // Shooting star logic
    nextShootingStarRef.current -= delta;
    if (nextShootingStarRef.current <= 0 && !shootingStar.active) {
      shootingStar.active = true;
      shootingStar.x = 35;
      shootingStar.y = (Math.random() - 0.5) * 30;
      shootingStar.angle = 0.2 + Math.random() * 0.5;
      shootingStar.speed = 300 + Math.random() * 400;
      shootingStar.life = 0;
      shootingStar.maxLife = 0.4 + Math.random() * 0.8;
      nextShootingStarRef.current = 2 + Math.random() * 4;
    }

    if (shootingStar.active && shootingStarMeshRef.current) {
      shootingStar.life += delta;
      shootingStar.x -= Math.cos(shootingStar.angle) * shootingStar.speed * delta * 0.02;
      shootingStar.y -= Math.sin(shootingStar.angle) * shootingStar.speed * delta * 0.02;

      const progress = shootingStar.life / shootingStar.maxLife;
      let alpha = 1;
      if (progress < 0.3) alpha = progress / 0.3;
      else alpha = 1 - (progress - 0.3) / 0.7;

      shootingStarMeshRef.current.position.set(shootingStar.x, shootingStar.y, -5);
      shootingStarMeshRef.current.visible = true;

      // Update trail
      const children = shootingStarMeshRef.current.children;
      for (let i = 0; i < children.length; i++) {
        const mesh = children[i] as THREE.Mesh;
        const mat = mesh.material as THREE.MeshBasicMaterial;
        const tailProgress = i / children.length;
        mat.opacity = alpha * (1 - tailProgress) * 0.8;
        mesh.position.set(
          Math.cos(shootingStar.angle) * tailProgress * 2,
          Math.sin(shootingStar.angle) * tailProgress * 2,
          0
        );
        mesh.scale.setScalar(1 - tailProgress * 0.7);
      }

      if (shootingStar.life >= shootingStar.maxLife) {
        shootingStar.active = false;
        shootingStarMeshRef.current.visible = false;
      }
    }
  });

  const starGeometry = useMemo(() => {
    const geo = new THREE.BufferGeometry();
    geo.setAttribute('position', new THREE.BufferAttribute(starPositions, 3));
    geo.setAttribute('color', new THREE.BufferAttribute(starColors, 3));
    geo.setAttribute('size', new THREE.BufferAttribute(starSizes, 1));
    return geo;
  }, [starPositions, starColors, starSizes]);

  return (
    <group>
      {/* Nebulas */}
      <group ref={nebulaGroupRef}>
        {nebulasRef.current.map((nebula, i) => (
          <mesh key={i} position={[nebula.x, nebula.y, -20]}>
            <circleGeometry args={[nebula.radius, 32]} />
            <meshBasicMaterial
              color={nebula.color}
              transparent
              opacity={0.12}
              depthWrite={false}
            />
          </mesh>
        ))}
      </group>

      {/* Stars */}
      <points ref={pointsRef} geometry={starGeometry}>
        <pointsMaterial
          size={0.08}
          vertexColors
          transparent
          opacity={0.9}
          sizeAttenuation
          depthWrite={false}
        />
      </points>

      {/* Shooting star */}
      <group ref={shootingStarMeshRef} visible={false}>
        {Array.from({ length: 10 }, (_, i) => (
          <mesh key={i}>
            <circleGeometry args={[0.06 - i * 0.005, 8]} />
            <meshBasicMaterial color="#ffffff" transparent opacity={0.8} depthWrite={false} />
          </mesh>
        ))}
      </group>
    </group>
  );
}
