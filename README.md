# overview of this projects structure with all its class definitions

#### this file contains a semi-comprehensive & detailed list about each class and its purpose w/ additional relevant information depending on the class. please keep in mind this text file might not always be up to date immediately so please excuse some inconsistencies as the mod gets updated

###### _(this project was written & compiled in IntelliJ IDEA w/ the Minecraft development plugin for Minecraft version 26.2 on Fabric w/ java 25)_

## list of included classes & their paths

##### current path: src/main/java/dev/ralsei/, classes are organized by their purposes & included features. they're further subdivided into their own area with unique subpackages:

- entity > entity.dialogue
- client > client.render > client.gui
- item
- network
- sound
- chat

##### please maintain this group structuring when adding new classes

#### additionally, to see networking payloads & their classes, check the following file path;

- networking payloads     >           src/main/java/dev/ralsei/network

## important information regarding the overall project and its structure

##### this mod was designed to use the recent Fabric network API & render pipeline changes for Minecraft. the mod entities visuals & networking backend were split into `extractRenderState`/`submit` (see RalseiEntityRenderer), and `StreamCodec`/`PayloadTypeRegistry` as opposed to the outdated packet-byte-buffer style

##### server/client split: only RalseiEntity, PlayerLock, RalseiChatListener, and the payload registration/receivers in Deltarune run on the server. everything under client/ (including the renderer & dialogue screen) is client-only with no server impact & zero server tick cost

##### all graphics for the entity were taken from the game "Deltarune" by Toby Fox: https://deltarune.com/

##### the lullaby is a custom-made, altered version of the lullaby song from "Deltarune" using a fanmade soundfont recreation for the game

## core / initialization

### Deltarune

- main mod initializer; registers entity types, items, creative tabs, sounds & all networking payloads on startup
- registers the server-side receiver for `DialogueClosedPayload` (the one serverbound packet in the mod)

### DeltaruneClient

- client-side initializer; registers the entity renderer, the right-click interaction handler for the mod entity (talk/follow-toggle/boredom-interrupt), and every clientbound payload receiver
- this is where the right-click gets routed to either opening the dialogue screen (client-side) or `beginTalking`/`requestFollowToggle`/`interruptBoredomTask` (server operation)

### DeltaruneDataGenerator

- default Fabric data generator entry point; currently empty placeholder, carried over from the Fabric mod template at https://fabricmc.net/develop/template/

## entity

### RalseiEntity

- main NPC entity for the mod; a `PathfinderMob` handling talk/follow state (via `PlayerLock`), has included boredom tasks (sleep/sing), 4-directional grid movement & facing, custom ai goals (rescue when stuck, aerial-follow for elytra flight, escape/flee when near death)
- deliberately deviates from normal mob properties to act more as a decorative, neutral NPC(total damage immunity except for player attacks), see `isInvulnerableTo`/`fireImmune`

### PlayerLock

- small reusable "ownership slot" utility—tracks which server player currently owns an interaction (talking to/being followed by Ralsei), with acquire/release/validate semantics
- used twice per Ralsei instance; once for the talkLock, once for the followLock

### ModEntityTypes

- registers the `RalseiEntity` entity type & its default attributes (health, movement speed, follow range, jump strength etc.)

### ModEntityTypeIds

- holds the `ResourceKey` for the previously specified entity type, kept separate from ModEntityTypes so the key can be referenced without triggering full registration

## entity.dialogue

### DialogueMessage

- record describing a single line of dialogue: text, typing speed, portrait, text animation, size, alignment, and an optional sprite "talk override" (wave/giggle/shy/etc.)
- has several telescoping constructors purely for default values, see notes in class for additional information

### DialoguePool

- static lookup table + data & logic for all dialogue: random dialogue lines, wake-up dialogues & keyword-triggered chat responses (`ChatTrigger`)
- `matchChatKey`/`randomForChatKey` split the "which response fits this message" decision (server-side, needs the actual words) from "which random variant is shown" (client-side, only needs the matched key), the server will never transmit the actual response text

### ChatTrigger

- record pairing a response key + keyword group(s) (supporting AND-within-group, OR-across-groups) with a pool of possible `DialogueMessage[]` responses
- used by `DialoguePool` to score chat messages against known keyword triggers

### TextAlignment / TextAnimation / TextSize / TypingSpeed

- small enums driving dialogue screen presentation: left/centered text, still/shake/wave/scared per-letter animation, small/normal/big text scale, and slow/normal/fast typing speed (ticks-per-character)

## client.render

### RalseiEntityRenderer

- custom sprite-billboard renderer; extracts a `RalseiVisualState` (idle/walk/sprint/fly/hurt/dead/scared/etc.) each frame based on entity state, then draws a single flat quad with the right texture/frame, either 4-directionally snapped (via entity yaw) or camera-billboarded (for non-directional sprites like idle)
- purposefully deviates from vanilla's `Display`/`DisplayRenderer` billboard math & kept the position-based camera-facing approach since it doesn't behave correctly otherwise with vanilla's rotation-based `CENTER` constraint used in the first method

### RalseiEntityRenderState

- plain per frame state snapshot passed from `extractRenderState` to `submit` (position, yaw, chosen visual state, animation frame)

### RalseiVisualState

- enum of every sprite/animation that can be displayed with frame dimensions, count, loop flag & frame timing baked in per state

### Direction4

- 4-way (front/back/left/right) directional enum used for sprite-facing; note the FRONT/LEFT/RIGHT/BACK angle ranges are intentionally set up the way they are, don't change without checking in-game

## client.gui

### RalseiDialogueScreen

- the dialogue box screen: portrait, wrapped/typed-out text with per-character animation (still/shake/wave/scared), talking sound per revealed character & open/close fade-in/out transitions
- entirely client-side; zero server cost regardless of how much text/animation logic lives here

## chat

### RalseiChatListener

- listens to server chat messages, checks if any nearby Ralsei should respond (keyword match + range check) & gives a response without ever broadcasting it to any other players

## item

### ModItems

- registers the spawn egg item

### ModItemIds

- holds the `ResourceKey` for the spawn egg, same separation pattern as `ModEntityTypeIds`

### ModCreativeTabs

- creates the dedicated creative inventory tab & adds the spawn egg

## network

##### all payloads are minimal `record`s implementing `CustomPacketPayload` with a `StreamCodec` — see `Deltarune`/`DeltaruneClient` for where each is registered & handled

### DialogueClosedPayload

- **serverbound.** client > server confirmation for dialogue closure that triggers `RalseiEntity.endTalking`

### DialogueForceClosePayload

- **clientbound.** server > client: force-close the dialogue screen if one is open (used when a talk/follow request is denied)

### DialogueOpenFollowConfirmPayload

- **clientbound.** server > client: open the dialogue screen with the follow-start or follow-stop message set

### DialogueOpenChatResponsePayload

- **clientbound.** server > client: open the dialogue screen with a response, carrying just the matched key + any boredom-interruption tag (never the actual response text)

### DialogueOpenDyingPayload

- **clientbound.** server > client: open the dialogue screen with associated dialogue

### StopLullabyPayload

- **clientbound.** server > client: stop the idle task sound client-side (sent when boredom task is interrupted)

## sound

### ModSounds

- registers the only two custom sound events included in this mod: the talking sound & Ralsei's lullaby