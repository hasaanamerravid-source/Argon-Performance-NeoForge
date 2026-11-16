# Argon

Lite jigsaw layout and frame cadence for Minecraft 26.x on NeoForge. Speeds up jigsaw structure layout and paces frames when Max Framerate is below 260. Does not change vanilla structure size or seeds.

NeoForge. Java 25.

## Links

- Homepage: https://github.com/hasaanamerravid-source/Argon-Performance-NeoForge
- Issues: https://github.com/hasaanamerravid-source/Argon-Performance-NeoForge/issues
- Fabric and Quilt: https://github.com/hasaanamerravid-source/Argon-Performance-Fabric

## Layout

Argon speeds up jigsaw layout (villages, ancient cities, trail ruins, and modded jigsaw structures) without changing what vanilla places.

It does not cap depth, piece count, or stronghold, mineshaft, or fortress chains. Those 0.1.x limits cut structures short and could leave a stronghold without a portal room. They are gone. `config/argon.json` is no longer read.

Vanilla's free space is a `VoxelShape` that every candidate piece is tested against. Argon carries a box octree in its place: the structure region is the tree boundary and every accepted piece is appended to the tree, so the test is the same question answered by boxes instead of voxels.

- Piece overlap uses a box octree. A candidate is tested against nearby pieces on the 0.25-deflated box. The accept or reject answer matches vanilla.
- Piece bounds are kept as six doubles in one flat array per leaf, not as `AABB` objects, so a scan walks contiguous memory. The duplicate check compares those doubles instead of boxing them through `AABB.equals`.
- A child node is only created once some piece actually reaches it. Empty children cost no object and no boundary test per query. Pieces small against a large structure box are the common case, and most children there stay empty.
- Subdivision splits only the axes that are at least two blocks wide, so a flat or rod-shaped region still gets a tree instead of a single leaf that every query scans linearly.
- Inserting a piece and testing one allocate nothing. The `AABB.of` copy and the 0.25 deflate are computed straight off the piece `BoundingBox`.
- The layout counters are `LongAdder`, not `AtomicLong`, because they are bumped once per candidate piece and worldgen runs that loop across chunk threads.
- Jigsaw name, target, facing, and joint use one orientation lookup instead of two, because both live in `ORIENTATION`. The name is checked first, and a null child name still matches anything, the way vanilla reads it.

What Argon does not do to the layout:

- Pool-weight deduplication, template palette replacement, and processor bounds pruning are not included. Those can change layout or break mods that edit template lists.
- A rigid child is not skipped. An earlier build skipped one when the parent connector faced outside the structure box or into a placed piece, on the claim that vanilla would reject it too. The connector is only recorded when it sits inside the parent piece, and the parent piece is already in the octree, so the guard was true for every recorded connector and it dropped rigid children vanilla places. Those candidates run vanilla.
- Duplicate pool elements are not deduplicated. In 26.3 the candidate list is iterated but never `contains`-checked, so there was nothing to deduplicate, and dropping a repeat would change which pieces are tried and therefore the random sequence.
- A child attached inside its own parent piece is left to vanilla. That path tests against a per-piece shape the octree does not model, and pieces placed that way are always inside the parent box, which the octree already has.

## Frame cadence

When Max Framerate is below 260, Argon replaces `FramerateLimiter.limitDisplayFPS`. It keeps a frame clock, parks until the last stretch, then spins until the slot. The interval is cached until the cap changes, and the clock advances to the target time so it does not drift. A cap change or a stall longer than one frame resets the clock so it does not try to catch up.

The stretch that is parked and the stretch that is spun is not fixed. Park granularity belongs to the kernel and the scheduler, so a window that is too small means the park lands late and the frame goes out late, and one that is too large burns a core on the same deadline. The window starts at 1 ms, grows by however late a park came back, and gives half of any unused stretch back. It stays between 20 µs and 1 ms, and resets when the cap changes.

260 and above, and non-positive values, stay vanilla. Vanilla only calls the frame limiter while the cap is below 260, so the limiter mixin never sees an unlimited frame rate; the debug line reads the live limit so it reports the frame rate that is actually being applied. This does not raise FPS. Do not combine VSync with an FPS cap. Do not install another limiter mixin beside Argon.

## Debug line

The F3 left column shows one `Argon-NeoForge (version)` line. Colors stay aqua for the name and green for the version. The line is client-only. Extra copies from 26.3's per-batch `extractLines` calls are dropped.

26.3 calls `extractLines` more than once per frame and only the left-side batch can use the line, so the text is built when a brand slot is actually filled rather than on every call. The layout counters usually sit still across frames, so the text is also cached until one of them moves. The text and the counts it was built from are held together, so a line can never disagree with its own numbers.

## Sides

Layout mixins run wherever chunks generate, including a dedicated server. Frame cadence and the F3 line are client-only.

## Install

Drop `argon-0.3.0.jar` into the mods folder of a NeoForge instance for Minecraft 26.3. Java 25 is required. NeoForge 26.3.0 or newer is required.

No config file. Max Framerate in video settings is the only switch.

## Build

JDK 25 on `PATH` or `JAVA_HOME`. Gradle 9.8.0 via the wrapper.

```sh
chmod +x gradlew
./gradlew build
```

Jar: `build/libs/argon-0.3.0.jar`

## Tests

`./gradlew test` runs the unit tests. The interesting one is `MixinInjectionTest`: every mixin target, call site and captured local in this mod is written as a string, so javac cannot see a stale one and a wrong target is a crash the first time a jigsaw structure places a child, long after the build said everything was fine. That test reads the compiled mixin classes and the real Minecraft class files and checks that each target exists, that each call site is in the bytecode as often as the injector demands, and that every captured local name resolves to exactly one variable.

`JigsawMatchTest` answers the same attach question as vanilla's `JigsawBlock.canAttach` over the whole cross product of parent and child orientations, joint types and name matches, so the faster matcher cannot drift away from the rule it replaces.

`BoxOctreeTest` compares `hasRoomFor` with vanilla's own `Shapes.join` and `Shapes.joinIsNotEmpty` free-space algebra, candidate for candidate.
