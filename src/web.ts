import { WebPlugin } from '@capacitor/core';

import type { 
  CarAudioPlugin, 
  PlayOptions, 
  PlaybackStatus, 
  AndroidAutoStatus,
  BrowsableItemOptions,
  PlayableItemOptions,
  SetMediaItemsOptions
} from './definitions';

export class CarAudioWeb extends WebPlugin implements CarAudioPlugin {
  async play(options: PlayOptions): Promise<PlaybackStatus> {
    console.warn('CarAudio.play is not implemented for web.', options);
    return Promise.reject('Not implemented for web.');
  }
  async pause(): Promise<PlaybackStatus> {
    console.warn('CarAudio.pause is not implemented for web.');
    return Promise.reject('Not implemented for web.');
  }
  async resume(): Promise<PlaybackStatus> {
    console.warn('CarAudio.resume is not implemented for web.');
    return Promise.reject('Not implemented for web.');
  }
  async stop(): Promise<PlaybackStatus> {
    console.warn('CarAudio.stop is not implemented for web.');
    return Promise.reject('Not implemented for web.');
  }
  async getStatus(): Promise<PlaybackStatus> {
    console.warn('CarAudio.getStatus is not implemented for web.');
    return Promise.reject('Not implemented for web.');
  }
  async ensureAudibleVolume(): Promise<{ success: boolean }> {
    console.warn('CarAudio.ensureAudibleVolume is not implemented for web.');
    return Promise.reject('Not implemented for web.');
  }

  // Android Auto methods (not available on web)
  async enableAndroidAuto(_options: { enabled: boolean }): Promise<AndroidAutoStatus> {
    console.warn('CarAudio.enableAndroidAuto is not available on web platform.');
    return Promise.resolve({ enabled: false, connected: false });
  }

  async updateAndroidAutoNowPlaying(options: PlayOptions): Promise<{ success: boolean }> {
    console.warn('CarAudio.updateAndroidAutoNowPlaying is not available on web platform.', options);
    return Promise.resolve({ success: false });
  }

  // Media item management methods (not available on web)
  async clearMediaItems(): Promise<{ success: boolean }> {
    console.warn('CarAudio.clearMediaItems is not available on web platform.');
    return Promise.resolve({ success: false });
  }

  async addBrowsableItem(options: BrowsableItemOptions): Promise<{ success: boolean }> {
    console.warn('CarAudio.addBrowsableItem is not available on web platform.', options);
    return Promise.resolve({ success: false });
  }

  async addPlayableItem(options: PlayableItemOptions): Promise<{ success: boolean }> {
    console.warn('CarAudio.addPlayableItem is not available on web platform.', options);
    return Promise.resolve({ success: false });
  }

  async setMediaItems(options: SetMediaItemsOptions): Promise<{ success: boolean; itemsAdded: number }> {
    console.warn('CarAudio.setMediaItems is not available on web platform.', options);
    return Promise.resolve({ success: false, itemsAdded: 0 });
  }

  async refreshAndroidAutoUI(): Promise<{ success: boolean }> {
    console.warn('CarAudio.refreshAndroidAutoUI is not available on web platform.');
    return Promise.resolve({ success: false });
  }
}
