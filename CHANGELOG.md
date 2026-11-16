# Changelog

## 0.3.0

NeoForge port of Argon 0.3.0. Structure layout and seeds are unchanged. This version is edge-case correctness, thin-region layout performance, and improvements in reliability.

### NeoForge 

- The mod entry point is a `@Mod` class and the metadata is `META-INF/neoforge.mods.toml`. The loader name in the log line and in the F3 line reads `NeoForge`.
- The mixin targets are written the way NeoForge needs them. Every target is a string with `remap = false`, because NeoForge runs the game under the official Mojang mappings and there is no refmap to remap against.
- The build uses ModDevGradle 2.0.148 against NeoForge 26.3.0.103, with the Gradle configuration cache on.
- The decompile and recompile pipeline is deliberately left on. The layout mixins capture vanilla locals by name, and the local variable table only carries those names in a jar that was produced by recompiling the decompiled sources. The pipeline that skips recompilation hands back a jar whose locals are still the obfuscated ones, so every `@Local(name = ...)` capture would fail to resolve at runtime.
- The centre-piece capture in `JigsawPlacementMixin` is keyed on the local name `box`. The earlier NeoForge build took the first `BoundingBox` argument instead, which is the centre piece itself and not the box the target reads, so the capture was of the wrong variable.
- `neoforge.mods.toml` uses `iconFile` rather than the deprecated `logoFile`, and requires NeoForge 26.3.0 or newer.
- The mod id, the mod name, the licence, the version and both version ranges come from the Gradle properties, so the metadata and the build cannot drift apart.
- The unit tests run inside the loader, because the game registries a jigsaw block state comes from are only populated once the loader is up. The injection test moved out of the `argon.mixin` package, which the loader owns and will not load a test class from.

### Layout

- `BoxOctree` widens a `BoundingBox` before adding one to its maximum coordinates. At `Integer.MAX_VALUE` the old int addition wrapped negative, so a piece at the positive coordinate limit was silently left out of the tree and an overlapping candidate was accepted. The 0.25-deflated query box uses the same widening.
- A node now subdivides along whichever axes are at least two blocks wide instead of refusing to split unless all three were. A flat or rod-shaped structure region used to keep every piece in one leaf and scan them linearly.
- The root starts at depth zero, so `MAXIMUM_DEPTH = 5` now allows five subdivision levels instead of four.
- The active child count is sized to the number of axes that actually split, up to eight, so a one- or two-axis split does not walk empty child slots.
- Removed the unused `ArgonProfiler.TOTAL_LAYOUT_TIME_NS` counter.

### Frame Cadence

- `FrameCadence.waitUntil` clears the thread's interrupt flag while it parks and restores it before returning. `LockSupport.parkNanos` returns immediately while the flag is set, so an interrupted render thread used to busy-spin through the park window; the interrupt is still delivered after the wait.
- The F3 line reads the live frame limit instead of trusting the pacing flag. Vanilla only calls the frame limiter while the cap is below 260, so the limiter mixin never sees an unlimited frame rate and the pacing state used to stay set from the last limited frame. The line now reports the frame rate that is actually being applied.

### Debug

- `TrojanVoxelShape.getCoords` now returns the coordinates of its filled 1x1x1 placeholder. It used to return an empty list for a non-empty discrete shape, so any accidental vanilla shape query on the carrier would index an empty coordinate list instead of seeing a small valid shape.

### Tests

- Added a differential test that compares `BoxOctree.hasRoomFor` with the real vanilla `Shapes.join` and `Shapes.joinIsNotEmpty` free-space algebra, candidate for candidate.
- Added a differential test that compares Argon's attach rule with vanilla's `JigsawBlock.canAttach` over the whole cross product of parent and child orientations, joint types and name matches.
- Added regression tests for a piece at `Integer.MAX_VALUE`, for subdivision of a region one block wide on an axis, for the carrier's placeholder coordinates, and for interrupt preservation in frame pacing.

## 0.2.4

Every layout change in this entry was checked against the vanilla source of `JigsawPlacement`, `JigsawBlock`, `BooleanOp`, and `Shapes`, and the octree is verified candidate-for-candidate against vanilla's own free-space algebra.

- `JigsawBlock.canAttach` now keeps vanilla's `target.name() == null` guard. A null child name means the connector matches anything. Vanilla templates never produce one because `JigsawBlockInfo.parse` defaults the name, but a mod that builds a `JigsawBlockInfo` directly does, and the name-first matcher used to reject those.
- Removed the rigid-child skip. It skipped a child when the parent connector faced outside the structure box or into a placed piece, on the claim that vanilla would reject it. The connector is only recorded when it sits inside the parent piece, and the parent piece is already in the octree, so the guard was true for every recorded connector and it dropped rigid children that vanilla places. Those candidates run vanilla now.
- Removed the tried-element identity list. In 26.3 the wrapped `Lists.newArrayList` is the candidate piece list, which is iterated but never `contains`-checked, so the identity map was written on every add and read never. Deduplicating it was rejected because dropping a repeated candidate changes which pieces are tried and therefore the random sequence.
- The octree query is named for the answer it gives. `hasRoomFor` is the accept case, and the overlap wrap stands in for the reject side of `joinIsNotEmpty`, so both agree with vanilla's `!joinIsNotEmpty(free, deflated, ONLY_SECOND)`.
- `JigsawPlacement$Placer.tryPlacingChildren` takes `PoolElementStructurePiece`, `MutableObject`, depth, the rigid flag, `LevelHeightAccessor`, `RandomState`, `PoolAliasLookup`, and `LiquidSettings`. The overlap and piece-recording wraps target that descriptor and capture the candidate piece `BoundingBox` as a local.
- A child attached inside its own parent piece runs against vanilla's per-piece `sourceFree` shape, which the octree does not model. Those calls are left alone, and pieces placed that way are always inside the parent box, which is in the octree, so the carrier path stays correct without them.

### Layout

- Piece overlap uses a box octree. A candidate is tested against nearby pieces on the 0.25-deflated box, and the accept or reject answer matches vanilla AABB checks.
- Piece bounds are six doubles in one flat array per leaf, not `AABB` objects, so a scan walks contiguous memory and never dereferences a box. The duplicate check compares those doubles instead of boxing them through `AABB.equals`.
- An octant is only created once some piece reaches it. Empty octants cost no object and no boundary test per query, which is the common case when pieces are small against a large structure box.
- Inserting a piece and testing one allocate nothing. The `AABB.of` copy and the 0.25 deflate are computed straight off the piece `BoundingBox`.
- Octree nodes do not split when a half-extent is under 2 blocks.
- Accepted pieces are recorded on `Shapes.joinUnoptimized` (`require = 1`), the 26.3 accept path, and the carrier is returned so the octree keeps the same contents the `VoxelShape` would have carried.
- `JigsawBlock.canAttach` uses Argon's name-first matcher. Vanilla reads the front and the top out of two properties per block; both live in `ORIENTATION`, so one lookup per block yields both.
- Layout counters are `LongAdder`, not `AtomicLong`, because they are bumped once per candidate piece and worldgen runs that loop across chunk threads.

### Frame Cadence

- The frame limiter is replaced for caps below 260 and left alone at 260 and above, so an unlimited frame rate is not paced.
- The park and spin split tracks the scheduler instead of being fixed, and stays between 20 µs and 1 ms.
- The frame clock advances to the target time so it does not drift, and a cap change or a long stall resets it so it does not try to catch up.

### Debug

- The F3 line is built only when a brand slot is actually filled, and the text is cached until a counter moves.
