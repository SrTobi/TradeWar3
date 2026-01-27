// Sound effects using pre-generated MP3 files

// Audio cache to avoid reloading
const audioCache = new Map<string, HTMLAudioElement>();

// Preload all sounds
const SOUNDS = {
  click: '/sfx/click.mp3',
  buy: '/sfx/buy.mp3',
  sell: '/sfx/sell.mp3',
  place: '/sfx/place.mp3',
  battle: '/sfx/battle.mp3',
  upgrade: '/sfx/upgrade.mp3',
  victory: '/sfx/victory.mp3',
  defeat: '/sfx/defeat.mp3',
  gamestart: '/sfx/gamestart.mp3',
  error: '/sfx/error.mp3',
} as const;

type SoundName = keyof typeof SOUNDS;

// Preload sounds on module load
function preloadSounds(): void {
  for (const [name, path] of Object.entries(SOUNDS)) {
    const audio = new Audio(path);
    audio.preload = 'auto';
    audioCache.set(name, audio);
  }
}

// Initialize preloading
preloadSounds();

// Play a sound by name
function playSound(name: SoundName, volume: number = 0.5): void {
  try {
    // Create a new audio instance for overlapping sounds
    const audio = new Audio(SOUNDS[name]);
    audio.volume = volume;
    audio.play().catch(() => {
      // Ignore autoplay errors - user hasn't interacted yet
    });
  } catch {
    // Ignore errors
  }
}

// Resume audio context on user interaction (for browsers that require it)
export function resumeAudio(): void {
  // HTML5 Audio doesn't need explicit resume like Web Audio API
  // But we can trigger a silent play to unlock audio on iOS
  const audio = audioCache.get('click');
  if (audio) {
    audio.volume = 0;
    audio.play().catch(() => {}).finally(() => {
      audio.volume = 0.5;
    });
  }
}

// Sound effect functions
export function playClick(): void {
  playSound('click', 0.4);
}

export function playHover(): void {
  playSound('click', 0.2);
}

export function playBuy(): void {
  playSound('buy', 0.5);
}

export function playSell(): void {
  playSound('sell', 0.5);
}

export function playPlaceUnit(): void {
  playSound('place', 0.6);
}

export function playBattle(): void {
  playSound('battle', 0.5);
}

export function playUpgrade(): void {
  playSound('upgrade', 0.5);
}

export function playVictory(): void {
  playSound('victory', 0.6);
}

export function playDefeat(): void {
  playSound('defeat', 0.5);
}

export function playError(): void {
  playSound('error', 0.4);
}

export function playGameStart(): void {
  playSound('gamestart', 0.5);
}
