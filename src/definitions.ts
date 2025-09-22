export interface CarAudioPlugin {
  play(options: { url: string }): Promise<{ url: string }>;
  pause(): Promise<void>;
  resume(): Promise<void>;
  stop(): Promise<void>;
}
