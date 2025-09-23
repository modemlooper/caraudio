export interface PlayOptions {
  url: string;
  title?: string;
  artist?: string;
  artwork?: string;
}

export interface PlaybackStatus {
  status: 'idle' | 'preparing' | 'playing' | 'paused' | 'stopped' | 'error';
  url?: string;
  title?: string;
  artist?: string;
}

export interface CarAudioPlugin {
  play(options: PlayOptions): Promise<PlaybackStatus>;
  pause(): Promise<PlaybackStatus>;
  resume(): Promise<PlaybackStatus>;
  stop(): Promise<PlaybackStatus>;
  getStatus(): Promise<PlaybackStatus>;
  ensureAudibleVolume(): Promise<{ success: boolean }>;
}
