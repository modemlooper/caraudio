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

export interface BrowsableItemOptions {
  parentId?: string;
  mediaId: string;
  title: string;
  subtitle?: string;
}

export interface PlayableItemOptions {
  parentId?: string;
  mediaId: string;
  title: string;
  subtitle?: string;
  description?: string;
  url: string;
  artwork?: string;
}

export interface MediaItemData {
  type: 'browsable' | 'playable';
  parentId?: string;
  mediaId: string;
  title: string;
  subtitle?: string;
  description?: string;
  url?: string;
  artwork?: string;
}

export interface SetMediaItemsOptions {
  items: MediaItemData[];
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
  
  // Media item management methods
  clearMediaItems(): Promise<{ success: boolean }>;
  addBrowsableItem(options: BrowsableItemOptions): Promise<{ success: boolean }>;
  addPlayableItem(options: PlayableItemOptions): Promise<{ success: boolean }>;
  setMediaItems(options: SetMediaItemsOptions): Promise<{ success: boolean; itemsAdded: number }>;
  refreshAndroidAutoUI(): Promise<{ success: boolean }>;
  
  // Event listeners
  addListener(
    eventName: 'androidAutoCommand',
    listenerFunc: (data: AndroidAutoCommandData) => void,
  ): Promise<any>;
  
  removeAllListeners(): Promise<void>;
}
