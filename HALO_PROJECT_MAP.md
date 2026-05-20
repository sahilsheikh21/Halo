# Halo Project Map

Last updated: 2026-05-15

## 1. What this repo is

Halo is a single-module Android app (`:app`) built with:

- Kotlin
- Jetpack Compose
- Hilt
- Room
- Kotlin Coroutines / Flow
- Matrix Rust SDK (`org.matrix.rustcomponents:sdk-android`)

There are no additional app modules. Everything runtime-relevant is inside `app/`.

## 2. High-level runtime flow

### App startup

1. `HaloApplication` enables Hilt.
2. `MainActivity` starts the app UI.
3. `MainActivity` seeds local mock/sample data on first launch via `MockDataSeeder`.
4. `MainActivity` starts `SyncEventProcessor`.
5. `HaloApp` observes `MatrixClientManager.sessionState`.
6. When logged in, `SlidingSyncManager.startSync()` starts Matrix sync.
7. Sync events are pushed into Room.
8. Repositories expose Room-backed `Flow`s.
9. ViewModels collect repository flows.
10. Compose screens render from ViewModels.

## 3. Source map by responsibility

### Entry points

- `app/src/main/java/com/halo/HaloApplication.kt`
- `app/src/main/java/com/halo/MainActivity.kt`

### Dependency injection

- `app/src/main/java/com/halo/di/AppModule.kt`
- `app/src/main/java/com/halo/di/DatabaseModule.kt`
- `app/src/main/java/com/halo/di/MatrixModule.kt`

### Local storage

- Database: `app/src/main/java/com/halo/data/local/HaloDatabase.kt`
- DAOs: `app/src/main/java/com/halo/data/local/dao/`
- Entities: `app/src/main/java/com/halo/data/local/entity/`
- Room relation model: `app/src/main/java/com/halo/data/local/pojo/PostWithAuthor.kt`

### Matrix / network layer

- Session/auth: `app/src/main/java/com/halo/data/matrix/MatrixClientManager.kt`
- Media upload and MXC resolution: `app/src/main/java/com/halo/data/matrix/MediaManager.kt`
- Sync lifecycle: `app/src/main/java/com/halo/data/matrix/SlidingSyncManager.kt`
- Timeline processing: `app/src/main/java/com/halo/data/matrix/SyncEventProcessor.kt`
- Custom event payload models: `app/src/main/java/com/halo/data/matrix/events/`

### Repositories

- Feed: `app/src/main/java/com/halo/data/repository/FeedRepository.kt`
- Stories: `app/src/main/java/com/halo/data/repository/StoryRepository.kt`
- Users: `app/src/main/java/com/halo/data/repository/UserRepository.kt`
- Chat: `app/src/main/java/com/halo/data/repository/ChatRepository.kt`

### UI

- Navigation: `app/src/main/java/com/halo/ui/navigation/`
- Common utilities: `app/src/main/java/com/halo/ui/common/`
- Reusable components: `app/src/main/java/com/halo/ui/components/`
- Screens: `app/src/main/java/com/halo/ui/screens/`
- Theme: `app/src/main/java/com/halo/ui/theme/`

### Domain models

- `app/src/main/java/com/halo/domain/model/`

## 4. What is connected end-to-end

These paths are wired and active in the running app:

### Auth/session path

`SplashScreen` / `LoginScreen` / `RegisterScreen`
→ `AuthViewModel`
→ `MatrixClientManager`
→ `SessionState`
→ `HaloApp`
→ `SlidingSyncManager`

### Feed path

`HomeScreen`
→ `HomeViewModel`
→ `FeedRepository`
→ `PostDao`
→ `posts` table

Author metadata joins through:

`PostDao.getFeedPosts()`
→ `PostWithAuthor`
→ `users` table

### Story path

`HomeScreen` story bar and `StoryViewerScreen`
→ `StoryViewModel` / `HomeViewModel`
→ `StoryRepository`
→ `StoryDao`
→ `stories` table

### Messages path

`MessageListScreen`
→ `MessageViewModel`
→ `ChatRepository.refreshChatRooms()`
→ Matrix rooms
→ `chat_rooms` table

`ChatScreen`
→ `ChatViewModel`
→ `ChatRepository`
→ `MessageDao`
→ `messages` table

### Profile path

`ProfileScreen`
→ `ProfileViewModel`
→ `UserRepository` + `FeedRepository` + `ChatRepository`

This path supports:

- loading cached/fetched user profiles
- showing posts by author
- follow/unfollow state
- starting direct messages

### Sync ingestion path

`SlidingSyncManager`
→ Matrix `SyncService`
→ `SyncEventProcessor`
→ `ChatRepository` / `PostDao` / `StoryDao` / `UserDao`
→ Room
→ repository flows
→ UI

## 5. What is only partially connected

These pieces exist and are reachable, but the full feature is not complete:

### Create flow

`CreateScreen`
→ `CreateViewModel`
→ `MediaManager`
→ `FeedRepository.publishPost()` or `StoryRepository.publishStory()`

This is connected, but depends on:

- a valid logged-in Matrix session
- media upload success
- at least one usable non-DM non-space room

It is not a guaranteed complete “social publish” system yet.

### Explore search

`ExploreScreen`
→ `ExploreViewModel`
→ `UserRepository.searchUsersReal()`

This is live, but strongly depends on homeserver behavior and current Matrix session state.

### DM creation

`ProfileScreen`
→ `ProfileViewModel.startDM()`
→ `ChatRepository.createDirectMessage()`

This is connected, but correctness depends on Matrix SDK room membership APIs and server state.

## 6. What is present in the repo but not really connected

### Mock-only or local-only UI pieces

- `app/src/main/java/com/halo/ui/components/CommentSheet.kt`
  - uses in-memory mock comments only
  - not backed by repository, Room, or Matrix events

- `app/src/main/java/com/halo/ui/screens/activity/ActivityViewModel.kt`
  - returns empty state
  - no repository or Matrix-backed activity feed exists yet

### Custom Matrix event models with no active read/write loop

These files define a model, but the full feature loop is not implemented:

- `app/src/main/java/com/halo/data/matrix/events/HaloComment.kt`
- `app/src/main/java/com/halo/data/matrix/events/HaloReaction.kt`
- `app/src/main/java/com/halo/data/matrix/events/HaloProfile.kt`

Current status:

- `HaloPost` and `HaloStory` are used
- `HaloComment`, `HaloReaction`, and `HaloProfile` are mostly schema/placeholders

### Static mock data package

- `app/src/main/java/com/halo/data/mock/MockData.kt`

This appears to be leftover UI-dev data. It is not the main active data source anymore.

### Empty scratch area

- `app/src/main/java/com/halo/scratch/`

Directory exists but currently has no files.

## 7. Repo areas that are not part of the app runtime

These are useful for debugging or history, but they are not runtime code paths:

- `.gradle/` and `build/`: generated build artifacts
- `.idea/`: IDE metadata
- `sdk_temp/`: extracted SDK inspection material, not app source
- `sdk_temp.zip`: archive of SDK inspection material
- `compile_error.txt`: old compile log, now stale
- `room_api.txt`, `room_member.txt`, `event_item.txt`: SDK API inspection notes
- `halo_bug_audit.md`, `halo_audit_plan.md`: previous audit/planning documents
- `filelist.txt`, `reslist.txt`: utility artifacts, not part of app execution

## 8. Current health check

Verified on 2026-05-15:

- `:app:compileDebugKotlin` succeeded
- `:app:testDebugUnitTest` succeeded

This means the project is currently buildable, even though the repo still contains an older failing compile log in `compile_error.txt`.

## 9. Main structural problems causing confusion

### Problem A: live code and old audit artifacts are mixed together

The repo root contains multiple historical debugging files that make it look more broken than it currently is.

### Problem B: some features are real, others are UI shells

The app mixes:

- real Matrix + Room-backed flows
- first-launch seeded sample data
- mock-only UI components
- placeholder custom-event models

That makes it hard to tell whether a broken behavior is:

- a sync bug
- missing feature implementation
- mock UI that never had backend support

### Problem C: seeded local data hides backend truth

`MockDataSeeder` inserts starter users/posts/stories/chat rooms into Room on first launch. This is useful for demos, but it can hide whether Matrix sync is truly working.

### Problem D: social features are unevenly implemented

Chat is the most backend-connected area.
Feed and stories are moderately connected.
Comments, reactions, activity, and profile sync are still incomplete.

## 10. Best guess at where your current problems are coming from

If the team feels the project is “messy” or “unreliable,” the most likely causes are:

1. Room is showing seeded/local cache data, so the UI can look healthy even when Matrix wiring is incomplete.
2. Some screens are fully data-driven, while others are still mock or placeholder implementations.
3. Old logs and audit files in the repo root make it hard to tell what is still broken versus what was already fixed.
4. Matrix custom-event support exists at the schema level, but not every event type has a full ingestion/render/update loop.
5. The project is a single app module, so unrelated concerns live close together and feel more tangled than they really are.

## 11. Recommended cleanup order

1. Separate runtime code from audit/debug artifacts.
2. Decide whether `MockDataSeeder` should stay, become debug-only, or be removed.
3. Label every feature as one of:
   - production-connected
   - partially connected
   - UI-only placeholder
4. Finish or disable the incomplete social paths:
   - comments
   - reactions
   - activity
   - profile event sync
5. Add one architecture/status doc per feature so the app’s real state stays visible.
