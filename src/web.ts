import { WebPlugin } from '@capacitor/core';

import type { CarAudioPlugin, PlayOptions, PlaybackStatus } from './definitions';

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
}
