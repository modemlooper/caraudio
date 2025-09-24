export interface PlayOptions {
  url: string;
  title?: string;
  artist?: string;
  album?: string;
  artwork?: string;
  duration?: number;
}

export interface PlaybackStatus {
  status: 'idle' | 'preparing' | 'playing' | 'paused' | 'stopped' | 'error';
  url?: string;
  title?: string;
  artist?: string;
}

export interface AndroidAutoCommandData {
  action: 'play' | 'pause' | 'stop' | 'skipToNext' | 'skipToPrevious' | 'seekTo';
  position?: number;
}

export interface AndroidAutoStatus {
  enabled: boolean;
  connected: boolean;
}

export interface CarAudioPlugin {
  play(options: PlayOptions): Promise<PlaybackStatus>;
  pause(): Promise<PlaybackStatus>;
  resume(): Promise<PlaybackStatus>;
  stop(): Promise<PlaybackStatus>;
  getStatus(): Promise<PlaybackStatus>;
  ensureAudibleVolume(): Promise<{ success: boolean }>;
  
  // Android Auto methods
  enableAndroidAuto(options: { enabled: boolean }): Promise<AndroidAutoStatus>;
  updateAndroidAutoNowPlaying(options: PlayOptions): Promise<{ success: boolean }>;
  
  // Event listeners
  addListener(
    eventName: 'androidAutoCommand',
    listenerFunc: (data: AndroidAutoCommandData) => void,
  ): Promise<any>;
  
  removeAllListeners(): Promise<void>;
}
