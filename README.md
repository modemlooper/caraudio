# caraudio

Play audio through carplay

## Install

```bash
npm install caraudio
npx cap sync
```

## API

<docgen-index>

* [`play(...)`](#play)
* [`pause()`](#pause)
* [`resume()`](#resume)
* [`stop()`](#stop)
* [`getStatus()`](#getstatus)
* [`ensureAudibleVolume()`](#ensureaudiblevolume)
* [`enableAndroidAuto(...)`](#enableandroidauto)
* [`updateAndroidAutoNowPlaying(...)`](#updateandroidautonowplaying)
* [`addListener('androidAutoCommand', ...)`](#addlistenerandroidautocommand-)
* [`removeAllListeners()`](#removealllisteners)
* [Interfaces](#interfaces)

</docgen-index>

<docgen-api>
<!--Update the source file JSDoc comments and rerun docgen to update the docs below-->

### play(...)

```typescript
play(options: PlayOptions) => Promise<PlaybackStatus>
```

| Param         | Type                                                |
| ------------- | --------------------------------------------------- |
| **`options`** | <code><a href="#playoptions">PlayOptions</a></code> |

**Returns:** <code>Promise&lt;<a href="#playbackstatus">PlaybackStatus</a>&gt;</code>

--------------------


### pause()

```typescript
pause() => Promise<PlaybackStatus>
```

**Returns:** <code>Promise&lt;<a href="#playbackstatus">PlaybackStatus</a>&gt;</code>

--------------------


### resume()

```typescript
resume() => Promise<PlaybackStatus>
```

**Returns:** <code>Promise&lt;<a href="#playbackstatus">PlaybackStatus</a>&gt;</code>

--------------------


### stop()

```typescript
stop() => Promise<PlaybackStatus>
```

**Returns:** <code>Promise&lt;<a href="#playbackstatus">PlaybackStatus</a>&gt;</code>

--------------------


### getStatus()

```typescript
getStatus() => Promise<PlaybackStatus>
```

**Returns:** <code>Promise&lt;<a href="#playbackstatus">PlaybackStatus</a>&gt;</code>

--------------------


### ensureAudibleVolume()

```typescript
ensureAudibleVolume() => Promise<{ success: boolean; }>
```

**Returns:** <code>Promise&lt;{ success: boolean; }&gt;</code>

--------------------


### enableAndroidAuto(...)

```typescript
enableAndroidAuto(options: { enabled: boolean; }) => Promise<AndroidAutoStatus>
```

| Param         | Type                               |
| ------------- | ---------------------------------- |
| **`options`** | <code>{ enabled: boolean; }</code> |

**Returns:** <code>Promise&lt;<a href="#androidautostatus">AndroidAutoStatus</a>&gt;</code>

--------------------


### updateAndroidAutoNowPlaying(...)

```typescript
updateAndroidAutoNowPlaying(options: PlayOptions) => Promise<{ success: boolean; }>
```

| Param         | Type                                                |
| ------------- | --------------------------------------------------- |
| **`options`** | <code><a href="#playoptions">PlayOptions</a></code> |

**Returns:** <code>Promise&lt;{ success: boolean; }&gt;</code>

--------------------


### addListener('androidAutoCommand', ...)

```typescript
addListener(eventName: 'androidAutoCommand', listenerFunc: (data: AndroidAutoCommandData) => void) => Promise<any>
```

| Param              | Type                                                                                         |
| ------------------ | -------------------------------------------------------------------------------------------- |
| **`eventName`**    | <code>'androidAutoCommand'</code>                                                            |
| **`listenerFunc`** | <code>(data: <a href="#androidautocommanddata">AndroidAutoCommandData</a>) =&gt; void</code> |

**Returns:** <code>Promise&lt;any&gt;</code>

--------------------


### removeAllListeners()

```typescript
removeAllListeners() => Promise<void>
```

--------------------


### Interfaces


#### PlaybackStatus

| Prop         | Type                                                                                |
| ------------ | ----------------------------------------------------------------------------------- |
| **`status`** | <code>'error' \| 'idle' \| 'preparing' \| 'playing' \| 'paused' \| 'stopped'</code> |
| **`url`**    | <code>string</code>                                                                 |
| **`title`**  | <code>string</code>                                                                 |
| **`artist`** | <code>string</code>                                                                 |


#### PlayOptions

| Prop           | Type                |
| -------------- | ------------------- |
| **`url`**      | <code>string</code> |
| **`title`**    | <code>string</code> |
| **`artist`**   | <code>string</code> |
| **`album`**    | <code>string</code> |
| **`artwork`**  | <code>string</code> |
| **`duration`** | <code>number</code> |


#### AndroidAutoStatus

| Prop            | Type                 |
| --------------- | -------------------- |
| **`enabled`**   | <code>boolean</code> |
| **`connected`** | <code>boolean</code> |


#### AndroidAutoCommandData

| Prop           | Type                                                                                     |
| -------------- | ---------------------------------------------------------------------------------------- |
| **`action`**   | <code>'play' \| 'pause' \| 'stop' \| 'skipToNext' \| 'skipToPrevious' \| 'seekTo'</code> |
| **`position`** | <code>number</code>                                                                      |

</docgen-api>
