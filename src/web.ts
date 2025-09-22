import { WebPlugin } from '@capacitor/core';

import type { CarAudioPlugin } from './definitions';

export class CarAudioWeb extends WebPlugin implements CarAudioPlugin {
  async play(options: { url: string }): Promise<{ url: string }> {
    console.warn('CarAudio.play is not implemented for web.', options);
    return Promise.reject('Not implemented for web.');
  }
  async pause(): Promise<void> {
    console.warn('CarAudio.pause is not implemented for web.');
    return Promise.reject('Not implemented for web.');
  }
  async resume(): Promise<void> {
    console.warn('CarAudio.resume is not implemented for web.');
    return Promise.reject('Not implemented for web.');
  }
  async stop(): Promise<void> {
    console.warn('CarAudio.stop is not implemented for web.');
    return Promise.reject('Not implemented for web.');
  }
}
