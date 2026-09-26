# overview of the project structure with class definitions

#### this file contains a semi-comprehensive & detailed list about each class and its purpose w/ additional relevant information depending on the class. please keep in mind this text file might not always be up to date immediately so please excuse some inconsistencies as the mod gets updated

###### _(this project was written & compiled in IntelliJ IDEA w/ the Minecraft development plugin, targeting minecraft 26.2 on fabric w/ java 25)_

## list of included classes & their paths

##### current path: src/main/java/dev/ralsei/, classes are organized by feature area into subpackages (entity, entity.dialogue, client, client.render, client.gui, item, network, sound, chat) rather than flat & alphabetical; please maintain this grouping when adding new classes

#### to see networking payload classes, check the following file path instead of the root package;

- networking payloads     >           src/main/java/dev/ralsei/network

## important information regarding the overall project and its structure

##### this mod is built against a fairly recent minecraft/fabric api revision — the render pipeline now splits `extractRenderState`/`submit` (see RalseiEntityRenderer), and networking goes through `StreamCodec`/`PayloadTypeRegistry` rather than the older packet-byte-buffer style. keep this in mind when comparing against older tutorials or outdated source

##### server vs. client split: only RalseiEntity, PlayerLock, RalseiChatListener, and the payload registration/receivers in Deltarune run on the server. everything under client/ (including the renderer and dialogue screen) is client-only and has zero server tick cost

## core / initialization

### Deltarune

- main mod initializer; registers entity types, items, creative tabs, sounds, and all networking payloads on startup
- also registers the server-side receiver for `DialogueClosedPayload` (the one serverbound packet in the mod)

### DeltaruneClient

- client-side initializer; registers the entity renderer, the right-click interaction handler for Ralsei (talk / follow-toggle / boredom-interrupt), and every clientbound payload receiver
- this is where a right-click on Ralsei gets routed to either opening the dialogue screen (client) or `beginTalking`/`requestFollowToggle`/`interruptBoredomTask` (server)

### DeltaruneDataGenerator

- default fabric data generator entry point; currently empty, placeholder for future generated data (loot tables, recipes, etc.)

## entity

### RalseiEntity

- the main NPC entity; a `PathfinderMob` handling talk/follow state (via `PlayerLock`), boredom tasks (sleep/sing), 4-directional grid movement & facing, three custom flight goals (rescue, aerial-follow, escape/flee), and the near-death "flee" sequence
- deliberately deviates from normal combat stats (immune to explosions & fire, only takes damage from direct player attacks) per lore — see `isInvulnerableTo`/`fireImmune`
- ###### _(constants/fields renamed to a compact, lowercase style deliberately — see comments for what's safe to touch; MIN_FACING_DISTANCE_SQ-equivalent is intentionally matched to vanilla LookControl's epsilon, please don't change)_

### PlayerLock

- small reusable "ownership slot" utility — tracks which single player currently owns an interaction (talking to / being followed by Ralsei), with acquire/release/validate semantics
- used twice per Ralsei instance: once for the talk lock, once for the follow lock

### ModEntityTypes

- registers the `RalseiEntity` entity type & its default attributes (health, movement speed, follow range, jump strength)

### ModEntityTypeIds

- holds the `ResourceKey` for the Ralsei entity type, kept separate from ModEntityTypes so the key can be referenced without triggering full registration

## entity.dialogue

### DialogueMessage

- record describing a single line of dialogue: text, typing speed, portrait, text animation, size, alignment, and an optional sprite "talk override" (wave/giggle/shy/etc.)
- has several telescoping constructors purely for default values — left as-is, see optimization notes

### DialoguePool

- static data + lookup logic for all of Ralsei's dialogue: greetings, wake-up lines, caught-singing lines, dying lines, follow start/stop lines, and keyword-triggered chat responses (`ChatTrigger`)
- `matchChatKey`/`randomForChatKey` split the "which response fits this message" decision (server-side, needs the actual words) from "which random variant do we show" (client-side, only needs the matched key) — the server never has to transmit the actual response text

### ChatTrigger

- record pairing a response key + keyword group(s) (supporting AND-within-group, OR-across-groups) with a pool of possible `DialogueMessage[]` responses
- used by `DialoguePool` to score chat messages against known keyword triggers

### TextAlignment / TextAnimation / TextSize / TypingSpeed

- small enums driving dialogue-screen presentation: left/centered text, still/shake/wave/scared per-letter animation, small/normal/big text scale, and slow/normal/fast typing speed (ticks-per-character)

## client.render

### RalseiEntityRenderer

- custom sprite-billboard renderer; extracts a `RalseiVisualState` (idle/walk/sprint/fly/hurt/dead/scared/etc.) each frame based on entity state, then draws a single flat quad with the right texture/frame, either 4-directionally snapped (via entity yaw) or camera-billboarded (for non-directional sprites like idle)
- looked at vanilla's `Display`/`DisplayRenderer` billboard math for comparison — kept the position-based camera-facing approach since it behaves correctly even when the viewer looks away without moving, unlike vanilla's rotation-based `CENTER` constraint

### RalseiEntityRenderState

- plain per-frame state snapshot passed from `extractRenderState` to `submit` (position, yaw, chosen visual state, animation frame)

### RalseiVisualState

- enum of every sprite/animation Ralsei can display, with frame dimensions, frame count, loop flag, and frame timing baked in per-state

### Direction4

- 4-way (front/back/left/right) directional enum used for sprite-facing; note the FRONT/LEFT/RIGHT/BACK angle ranges are intentionally set up the way they are, don't change without checking in-game

## client.gui

### RalseiDialogueScreen

- the dialogue box screen: portrait, wrapped/typed-out text with per-character animation (still/shake/wave/scared), sound-blip on each revealed character, and open/close slide-fade transitions
- entirely client-side; zero server cost regardless of how much text/animation logic lives here

## chat

### RalseiChatListener

- listens to server chat messages, checks if any nearby Ralsei should respond (keyword match + range check), and kicks off a dialogue response without ever broadcasting Ralsei's response text to other players

## item

### ModItems

- registers the Ralsei spawn egg

### ModItemIds

- holds the `ResourceKey` for the spawn egg, same separation pattern as `ModEntityTypeIds`

### ModCreativeTabs

- creates the "deltarune" creative inventory tab and adds the spawn egg to it (future NPC items go here too)

## network

##### all payloads are minimal `record`s implementing `CustomPacketPayload` with a `StreamCodec` — see `Deltarune`/`DeltaruneClient` for where each is registered & handled

### DialogueClosedPayload

- **serverbound.** client → server: "I closed the dialogue box for this entity." Triggers `RalseiEntity.endTalking`

### DialogueForceClosePayload

- **clientbound.** server → client: force-close the dialogue screen if one is open (used when a talk/follow request is denied)

### DialogueOpenFollowConfirmPayload

- **clientbound.** server → client: open the dialogue screen with the follow-start or follow-stop message set

### DialogueOpenChatResponsePayload

- **clientbound.** server → client: open the dialogue screen with a chat-triggered response, carrying just the matched key + any boredom-interruption tag (never the actual response text)

### DialogueOpenDyingPayload

- **clientbound.** server → client: open the dialogue screen with the near-death "dying" dialogue line

### StopLullabyPayload

- **clientbound.** server → client: stop the lullaby sound client-side (sent when the singing boredom task is interrupted)

## sound

### ModSounds

- registers the two custom sound events: the dialogue typing blip and Ralsei's lullaby
