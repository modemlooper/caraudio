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


### Interfaces


#### PlaybackStatus

| Prop         | Type                                                                                |
| ------------ | ----------------------------------------------------------------------------------- |
| **`status`** | <code>'error' \| 'idle' \| 'preparing' \| 'playing' \| 'paused' \| 'stopped'</code> |
| **`url`**    | <code>string</code>                                                                 |
| **`title`**  | <code>string</code>                                                                 |
| **`artist`** | <code>string</code>                                                                 |


#### PlayOptions

| Prop          | Type                |
| ------------- | ------------------- |
| **`url`**     | <code>string</code> |
| **`title`**   | <code>string</code> |
| **`artist`**  | <code>string</code> |
| **`artwork`** | <code>string</code> |

</docgen-api>
