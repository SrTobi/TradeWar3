// Synthesized sound effects using Web Audio API

let audioContext: AudioContext | null = null;

function getAudioContext(): AudioContext {
  if (!audioContext) {
    audioContext = new AudioContext();
  }
  return audioContext;
}

// Resume audio context on user interaction (required by browsers)
export function resumeAudio(): void {
  const ctx = getAudioContext();
  if (ctx.state === 'suspended') {
    ctx.resume();
  }
}

// Play a simple tone
function playTone(
  frequency: number,
  duration: number,
  type: OscillatorType = 'sine',
  volume: number = 0.3,
  attack: number = 0.01,
  decay: number = 0.1
): void {
  const ctx = getAudioContext();
  const oscillator = ctx.createOscillator();
  const gainNode = ctx.createGain();

  oscillator.connect(gainNode);
  gainNode.connect(ctx.destination);

  oscillator.type = type;
  oscillator.frequency.setValueAtTime(frequency, ctx.currentTime);

  // Envelope
  gainNode.gain.setValueAtTime(0, ctx.currentTime);
  gainNode.gain.linearRampToValueAtTime(volume, ctx.currentTime + attack);
  gainNode.gain.linearRampToValueAtTime(volume * 0.7, ctx.currentTime + attack + decay);
  gainNode.gain.linearRampToValueAtTime(0, ctx.currentTime + duration);

  oscillator.start(ctx.currentTime);
  oscillator.stop(ctx.currentTime + duration);
}

// Play noise burst (for explosions)
function playNoise(duration: number, volume: number = 0.3): void {
  const ctx = getAudioContext();
  const bufferSize = ctx.sampleRate * duration;
  const buffer = ctx.createBuffer(1, bufferSize, ctx.sampleRate);
  const data = buffer.getChannelData(0);

  for (let i = 0; i < bufferSize; i++) {
    data[i] = Math.random() * 2 - 1;
  }

  const noise = ctx.createBufferSource();
  noise.buffer = buffer;

  const filter = ctx.createBiquadFilter();
  filter.type = 'lowpass';
  filter.frequency.setValueAtTime(1000, ctx.currentTime);
  filter.frequency.exponentialRampToValueAtTime(100, ctx.currentTime + duration);

  const gainNode = ctx.createGain();
  gainNode.gain.setValueAtTime(volume, ctx.currentTime);
  gainNode.gain.exponentialRampToValueAtTime(0.01, ctx.currentTime + duration);

  noise.connect(filter);
  filter.connect(gainNode);
  gainNode.connect(ctx.destination);

  noise.start(ctx.currentTime);
  noise.stop(ctx.currentTime + duration);
}

// Sound effect functions
export function playClick(): void {
  playTone(800, 0.08, 'square', 0.15, 0.005, 0.02);
}

export function playHover(): void {
  playTone(600, 0.05, 'sine', 0.08, 0.005, 0.01);
}

export function playBuy(): void {
  // Rising tone
  const ctx = getAudioContext();
  const osc = ctx.createOscillator();
  const gain = ctx.createGain();

  osc.connect(gain);
  gain.connect(ctx.destination);

  osc.type = 'sine';
  osc.frequency.setValueAtTime(400, ctx.currentTime);
  osc.frequency.exponentialRampToValueAtTime(800, ctx.currentTime + 0.15);

  gain.gain.setValueAtTime(0.2, ctx.currentTime);
  gain.gain.linearRampToValueAtTime(0, ctx.currentTime + 0.15);

  osc.start(ctx.currentTime);
  osc.stop(ctx.currentTime + 0.15);
}

export function playSell(): void {
  // Falling tone
  const ctx = getAudioContext();
  const osc = ctx.createOscillator();
  const gain = ctx.createGain();

  osc.connect(gain);
  gain.connect(ctx.destination);

  osc.type = 'sine';
  osc.frequency.setValueAtTime(800, ctx.currentTime);
  osc.frequency.exponentialRampToValueAtTime(400, ctx.currentTime + 0.15);

  gain.gain.setValueAtTime(0.2, ctx.currentTime);
  gain.gain.linearRampToValueAtTime(0, ctx.currentTime + 0.15);

  osc.start(ctx.currentTime);
  osc.stop(ctx.currentTime + 0.15);
}

export function playPlaceUnit(): void {
  // Thump sound
  playTone(150, 0.2, 'sine', 0.4, 0.01, 0.05);
  playTone(100, 0.15, 'triangle', 0.3, 0.02, 0.05);
}

export function playBattle(): void {
  // Explosion sound
  playNoise(0.3, 0.4);
  playTone(80, 0.25, 'sawtooth', 0.3, 0.01, 0.1);
}

export function playUpgrade(): void {
  // Ascending arpeggio
  const ctx = getAudioContext();
  const notes = [400, 500, 600, 800];

  notes.forEach((freq, i) => {
    const osc = ctx.createOscillator();
    const gain = ctx.createGain();

    osc.connect(gain);
    gain.connect(ctx.destination);

    osc.type = 'sine';
    osc.frequency.setValueAtTime(freq, ctx.currentTime);

    const startTime = ctx.currentTime + i * 0.08;
    gain.gain.setValueAtTime(0, startTime);
    gain.gain.linearRampToValueAtTime(0.15, startTime + 0.02);
    gain.gain.linearRampToValueAtTime(0, startTime + 0.15);

    osc.start(startTime);
    osc.stop(startTime + 0.15);
  });
}

export function playVictory(): void {
  // Triumphant fanfare
  const ctx = getAudioContext();
  const notes = [523, 659, 784, 1047]; // C5, E5, G5, C6

  notes.forEach((freq, i) => {
    const osc = ctx.createOscillator();
    const gain = ctx.createGain();

    osc.connect(gain);
    gain.connect(ctx.destination);

    osc.type = 'triangle';
    osc.frequency.setValueAtTime(freq, ctx.currentTime);

    const startTime = ctx.currentTime + i * 0.15;
    gain.gain.setValueAtTime(0, startTime);
    gain.gain.linearRampToValueAtTime(0.25, startTime + 0.05);
    gain.gain.linearRampToValueAtTime(0.2, startTime + 0.3);
    gain.gain.linearRampToValueAtTime(0, startTime + 0.5);

    osc.start(startTime);
    osc.stop(startTime + 0.5);
  });
}

export function playDefeat(): void {
  // Sad descending tones
  const ctx = getAudioContext();
  const notes = [400, 350, 300, 250];

  notes.forEach((freq, i) => {
    const osc = ctx.createOscillator();
    const gain = ctx.createGain();

    osc.connect(gain);
    gain.connect(ctx.destination);

    osc.type = 'sine';
    osc.frequency.setValueAtTime(freq, ctx.currentTime);

    const startTime = ctx.currentTime + i * 0.2;
    gain.gain.setValueAtTime(0, startTime);
    gain.gain.linearRampToValueAtTime(0.2, startTime + 0.05);
    gain.gain.linearRampToValueAtTime(0, startTime + 0.4);

    osc.start(startTime);
    osc.stop(startTime + 0.4);
  });
}

export function playError(): void {
  // Buzzer sound
  playTone(200, 0.2, 'square', 0.2, 0.01, 0.05);
  setTimeout(() => playTone(150, 0.2, 'square', 0.2, 0.01, 0.05), 100);
}

export function playGameStart(): void {
  // Whoosh + chime
  const ctx = getAudioContext();

  // Whoosh (filtered noise)
  const bufferSize = ctx.sampleRate * 0.5;
  const buffer = ctx.createBuffer(1, bufferSize, ctx.sampleRate);
  const data = buffer.getChannelData(0);
  for (let i = 0; i < bufferSize; i++) {
    data[i] = Math.random() * 2 - 1;
  }

  const noise = ctx.createBufferSource();
  noise.buffer = buffer;

  const filter = ctx.createBiquadFilter();
  filter.type = 'bandpass';
  filter.frequency.setValueAtTime(500, ctx.currentTime);
  filter.frequency.exponentialRampToValueAtTime(2000, ctx.currentTime + 0.3);
  filter.Q.setValueAtTime(2, ctx.currentTime);

  const gain = ctx.createGain();
  gain.gain.setValueAtTime(0.3, ctx.currentTime);
  gain.gain.linearRampToValueAtTime(0, ctx.currentTime + 0.5);

  noise.connect(filter);
  filter.connect(gain);
  gain.connect(ctx.destination);

  noise.start(ctx.currentTime);
  noise.stop(ctx.currentTime + 0.5);

  // Chime
  setTimeout(() => {
    playTone(880, 0.3, 'sine', 0.2, 0.01, 0.1);
    playTone(1320, 0.3, 'sine', 0.15, 0.01, 0.1);
  }, 200);
}
