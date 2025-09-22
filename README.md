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

</docgen-index>

<docgen-api>
<!--Update the source file JSDoc comments and rerun docgen to update the docs below-->

### play(...)

```typescript
play(options: { url: string; }) => Promise<{ url: string; }>
```

| Param         | Type                          |
| ------------- | ----------------------------- |
| **`options`** | <code>{ url: string; }</code> |

**Returns:** <code>Promise&lt;{ url: string; }&gt;</code>

--------------------


### pause()

```typescript
pause() => Promise<void>
```

--------------------


### resume()

```typescript
resume() => Promise<void>
```

--------------------


### stop()

```typescript
stop() => Promise<void>
```

--------------------

</docgen-api>
