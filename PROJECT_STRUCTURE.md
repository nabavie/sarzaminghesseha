# Project Structure — سرزمین قصه‌ها (Land of Tales)

A Persian-language, RTL, children's audio-storytelling platform. Storytellers upload or record voice
tales; admins review and approve them before publication; listeners browse, search, listen, rate,
comment, and save tales.

**Stack:** Spring Boot 3.3 (Java 21) · Server-rendered Thymeleaf + Bootstrap 5 RTL · MySQL via
Spring Data JPA · Spring Security (form login, sessions, BCrypt) · Maven

This document walks through every directory and file and explains what the code does.

---

## Table of contents

1. [Root directory](#1-root-directory)
2. [Java source — `src/main/java/com/example/myapp/`](#2-java-source)
   - [`MyappApplication.java`](#21-myappapplicationjava)
   - [`model/` — JPA entities](#22-model--jpa-entities)
   - [`repository/` — Spring Data repositories](#23-repository--spring-data-repositories)
   - [`service/` — business logic](#24-service--business-logic)
   - [`controller/` — web layer](#25-controller--web-layer)
   - [`dto/` — form objects](#26-dto--form-objects)
   - [`config/` — configuration & seeding](#27-config--configuration--seeding)
   - [`util/` — Persian date helper](#28-util--persian-date-helper)
3. [Resources — `src/main/resources/`](#3-resources)
   - [`application.properties`](#31-applicationproperties)
   - [`templates/` — Thymeleaf views](#32-templates--thymeleaf-views)
   - [`static/` — CSS, JS, fonts, vendor assets](#33-static--css-js-fonts-vendor-assets)
   - [`seed/audio/` — bundled tale narrations](#34-seedaudio--bundled-tale-narrations)
4. [Tests — `src/test/`](#4-tests)
5. [Runtime directories](#5-runtime-directories)
6. [How the pieces fit together (request flows)](#6-how-the-pieces-fit-together)
7. [Database schema summary](#7-database-schema-summary)
8. [Endpoint reference](#8-endpoint-reference)

---

## 1. Root directory

| Path | Purpose |
|---|---|
| `pom.xml` | Maven build file. Parent `spring-boot-starter-parent 3.3.0`, Java 21. Dependencies: `spring-boot-starter-web` (MVC + embedded Tomcat), `spring-boot-starter-thymeleaf`, `spring-boot-starter-data-jpa` + `mysql-connector-j`, `spring-boot-starter-validation` (bean validation on forms), `spring-boot-starter-security` + `thymeleaf-extras-springsecurity6` (the `sec:authorize` attributes in templates), `com.github.mfathi91:persian-date-time` (Jalali calendar used by `PersianDateUtil`), `spring-boot-devtools` (dev hot restart), `spring-boot-starter-test`. Single build plugin: `spring-boot-maven-plugin`. |
| `CLAUDE.md` | Guidance file for Claude Code (project conventions, commands, architecture notes). |
| `src/` | All source code and resources (detailed below). |
| `target/` | Maven build output (compiled classes, packaged jar). Generated; not hand-edited. |
| `uploads/` | Runtime upload storage (`audio/`, `covers/`, `avatars/`) created at startup. The DB stores only filenames; the bytes live here. |
| `.idea/` | IntelliJ IDEA project metadata (editor-only, not part of the app). |

**Common commands:** run with `mvn spring-boot:run` (port **8081**), build with `mvn clean package`,
test with `mvn test` (needs local MySQL).

---

## 2. Java source

All application code lives under `src/main/java/com/example/myapp/` in a classic layered Spring MVC
layout. (The `com.example.myapp` package name was kept from the original demo app; the brand
"سرزمین قصه‌ها" is applied in UI text only.)

```
com.example.myapp
├── MyappApplication.java      ← entry point
├── model/                     ← JPA entities + status enums
├── repository/                ← Spring Data JPA interfaces (one per entity)
├── service/                   ← business logic, storage, security helpers
├── controller/                ← @Controller classes (server-rendered pages)
│   └── api/                   ← the one @RestController (progress JSON API)
├── dto/                       ← form-backing beans with validation
├── config/                    ← security, seeding, global model attributes
└── util/                      ← Jalali date/digit formatting
```

### 2.1 `MyappApplication.java`

The standard Spring Boot entry point: `@SpringBootApplication` + `main()` calling
`SpringApplication.run`. Component scanning picks up everything under `com.example.myapp`.

### 2.2 `model/` — JPA entities

Eight entities and three enums. Conventions used throughout: numeric identity PKs
(`GenerationType.IDENTITY`), `Instant createdAt` timestamps, `FetchType.LAZY` on all `@ManyToOne`
relations, and unique constraints where a pair must be unique.

#### `User.java` → table `users`
The account entity for all three roles.
- `username` (unique, ≤50), `passwordHash` (BCrypt), `displayName` (≤100), `avatarPath`
  (filename of the uploaded avatar under `uploads/avatars/`, nullable).
- `recoveryCodeHash` — BCrypt hash of the one-time password-recovery code; `null` until issued.
- `roles` — a `Set<Role>` stored via `@ElementCollection` in join table `user_roles`
  (`EnumType.STRING`, eager-fetched so authorities are always available). Helper `hasRole(Role)`.
- `enabled` — admins can deactivate accounts; checked at login (post-auth, see `SecurityConfig`).

#### `Role.java`
Plain enum: `LISTENER`, `STORYTELLER`, `ADMIN`. Everyone gets LISTENER at registration; the signup
checkbox or a dashboard request grants STORYTELLER immediately; ADMIN comes only from seeding.

#### `Tale.java` → table `tales`
The core content entity — one narrated story.
- `title` (≤150), `description` (`TEXT`), `storyteller` (`@ManyToOne User`, required).
- `categories` — `@ManyToMany` through join table `tale_categories` (a `LinkedHashSet` to keep
  insertion order).
- `audioPath` (required; UUID filename under `uploads/audio/`), `audioContentType` (MIME type kept
  so streaming can send the right `Content-Type`), `coverPath` (optional cover image),
  `durationSeconds` (nullable — filled lazily by the progress API once a browser reports it).
- `status` — `TaleStatus` enum, defaults to `PENDING`; `reviewNote` (≤500, the admin's note on
  approve/reject); `createdAt`, `approvedAt`.

#### `TaleStatus.java`
Enum `PENDING` / `APPROVED` / `REJECTED`. Each constant carries a `persianName`
(e.g. «در انتظار بررسی») and a Bootstrap `badgeClass` (`warning`/`success`/`danger`) so templates
can render status badges directly without switch logic.

#### `Category.java` → table `categories`
Just `id` + unique `name`. Seven Persian categories are seeded by `DataSeeder`.

#### `Comment.java` → table `comments`
Comments on a tale with **one level of reply nesting**:
- `tale`, `author` (both required `@ManyToOne`), `content` (≤1000).
- `parent` — self-referencing FK; `null` for top-level comments.
- `replies` — `@OneToMany(mappedBy = "parent", cascade = REMOVE, orphanRemoval = true)`, ordered
  by `createdAt`, so deleting a top-level comment removes its replies too.

#### `Rating.java` → table `ratings`
1–5 star rating; unique constraint on `(tale_id, user_id)` so each user rates a tale at most once
(re-rating updates the row). `updatedAt` matters: the monthly top-5 query counts *rating activity*
by `updatedAt`.

#### `Favorite.java` → table `favorites`
A user's saved tale; unique on `(user_id, tale_id)`. Toggled on/off from the tale page.

#### `ListeningProgress.java` → table `listening_progress`
Per user+tale playback state, unique on `(user_id, tale_id)`:
- `seconds` — current resume position (player seeks here on return).
- `listenedSeconds` — **the furthest position ever reached; grows monotonically**, so replays and
  rewinds never re-count. This is the "time spent listening" figure on the dashboard.
- `duration` (nullable), `finished`, `updatedAt`.
- `getPercent()` — derived 0–100 value for dashboard progress bars (100 when `finished`).

#### `StorytellerRequest.java` → table `storyteller_requests` (+ `RequestStatus.java`)
Record of a "become a storyteller" request: `user`, optional `message` (≤500), `status`
(`RequestStatus` enum, same persianName/badgeClass pattern as `TaleStatus`), `adminNote`,
`createdAt`, `decidedAt`. Note: dashboard requests are **auto-approved** and stored only as
history for `/admin/requests` (see `StorytellerRequestService`).

### 2.3 `repository/` — Spring Data repositories

One `JpaRepository` interface per entity; almost all queries are derived from method names.
The notable ones:

| File | Highlights |
|---|---|
| `UserRepository` | `findByUsername`, `existsByUsername`. |
| `TaleRepository` | `search(status, q, categoryId, pageable)` — a JPQL `@Query` powering the public catalog: filters approved tales, matches `q` against title *or* description with `LIKE %q%` (empty `q` matches all), and optionally requires membership in a category via a subquery over the join collection (`:categoryId IN (SELECT c.id FROM t.categories c)`). Also `findByStatusOrderByCreatedAtDesc` (paged), `findByStorytellerOrderByCreatedAtDesc`, `findByCategoriesContaining` (used when deleting a category), `countByStatus` (admin dashboard counters). |
| `RatingRepository` | `averageByTale` (`COALESCE(AVG(stars), 0)`), `countByTale`, `findByTaleAndUser`, and `topRatedSince(from, status, pageable)` — groups ratings updated since `from` by tale, ordered by `AVG(stars) DESC, COUNT DESC`; this is the home page's monthly top-5. |
| `CommentRepository` | `findByTaleAndParentIsNullOrderByCreatedAtAsc` (top-level comments; replies come through the entity's `replies` list), `findAllByOrderByCreatedAtDesc` (paged, for admin moderation), `countByTale`. |
| `FavoriteRepository` | `findByUserAndTale`, `existsByUserAndTale`, `findByUserOrderByCreatedAtDesc`. |
| `ListeningProgressRepository` | `findByUserAndTale`, `findByUserOrderByUpdatedAtDesc`. |
| `CategoryRepository` | `findByName`, `findAllByOrderByNameAsc`. |
| `StorytellerRequestRepository` | latest per user, pending list/count, full history list. |

### 2.4 `service/` — business logic

Thin services, one per aggregate, plus storage/security helpers. Write methods are
`@Transactional`; user-facing failures are thrown as `IllegalArgumentException` **with Persian
messages** that controllers surface as form errors.

#### `TaleService.java`
CRUD-ish wrapper over `TaleRepository`: `searchApproved` (trims `q`, builds the `PageRequest`),
`findByStatus`, `countByStatus`, `save`, and the review actions `approve(tale, note)` /
`reject(tale, note)` which set status, normalize the note (blank → `null`), and set/clear
`approvedAt`.

#### `FileStorageService.java`
All disk I/O for uploads. Root comes from `FileStorageProperties` (`app.upload.dir`, default
`uploads/`, resolved to an absolute normalized path).
- `@PostConstruct init()` creates `uploads/audio`, `uploads/covers`, `uploads/avatars`.
- `storeAudio` / `storeCover` / `storeAvatar` → shared `store()` which **whitelists extensions**
  (audio: `mp3, m4a, ogg, webm, wav`; images: `jpg, jpeg, png, webp`), rejects anything else with a
  Persian `IllegalArgumentException`, and saves under a random `UUID.extension` filename (so
  original names never touch the filesystem).
- `storeStream` — raw-stream variant used by `TaleSeeder` to copy classpath seed audio.
- `load(subdir, filename)` — returns a `FileSystemResource` or `null`; despite filenames being
  server-generated UUIDs it still guards against path traversal (`path.startsWith(root)`).
- `delete` — same traversal guard; used when replacing audio/covers/avatars.

#### `MediaTokenService.java` (bean name `"mediaToken"`)
Short-lived **HMAC-SHA256 tokens for the audio streaming endpoint**, so tale audio has no stable
shareable/hotlinkable URL.
- Token format: `<expiryEpochSeconds>.<base64url HMAC of "taleId:expiry">`, valid **6 hours**.
- Secret from `app.media.token-secret`; if blank, a random 32-byte secret is generated per boot
  (restart just means pages mint fresh tokens).
- `issue(taleId)` is called **from templates** via `${@mediaToken.issue(tale.id)}` right next to
  the `<audio>` element; `isValid(taleId, token)` checks expiry then compares HMACs with the
  constant-time `MessageDigest.isEqual`.
- Explicitly documented limitation: this stops link-sharing, not a determined user capturing audio
  they're allowed to hear.

#### `PasswordRecoveryService.java`
Password recovery **without e-mail infrastructure**, via a one-time recovery code:
- `issueCode(user)` — generates a 16-char code from an alphabet with no `0/O/1/I/L` (unambiguous
  when hand-copied), grouped `XXXX-XXXX-XXXX-XXXX`; stores only its **BCrypt hash**; returns the
  plain code — the single moment it is visible.
- `resetPassword(username, code, newPassword)` — throttled per username (max 5 failures per 15
  minutes, in-memory deque of timestamps); verifies the code against the hash, sets the new
  password, **consumes** the code and issues + returns a fresh one (shown once).
- All failures throw `IllegalArgumentException` with a Persian message; username/code mismatch is
  a single generic error (no account probing).

#### `LoginAttemptService.java`
In-memory login brute-force protection (resets on restart). Failures are tracked **per client IP
and per target username** — catching both one attacker spraying many accounts and a distributed
attack on one account. **10 failures within 1 minute → 10-minute ban** for that key. An
`@EventListener` on `AuthenticationSuccessEvent` wipes failure history for the IP/username on
successful login (but does not lift active bans). Used by `LoginFailureHandler` (records) and
`LoginRateLimitFilter` (enforces).

#### `ProgressService.java`
Persists playback state. `record(user, tale, seconds, duration, finished)` upserts the
`ListeningProgress` row: sets the resume position, and advances `listenedSeconds` only if the new
position is further than any previously reached (clamped to duration) — the monotonic-growth rule
that makes "total listening time" replay-proof. `finished` marks the row done and snaps
`listenedSeconds` to the full duration. `totalListenedSeconds(user)` sums across tales.

#### `RatingService.java`
`rate()` clamps stars to 1–5 and upserts the user's rating (bumping `updatedAt`); `average`,
`count`, `userStars`; `topOfMonth(limit)` uses `PersianDateUtil.startOfCurrentJalaliMonth()` — the
top-5 window is the current **Jalali** month, not the Gregorian one.

#### `CommentService.java`
`topLevelForTale`, paged `findAll` for admin, `delete`, and `add(tale, author, parent, content)`
which enforces the one-level nesting rule: a reply to a reply is re-attached to the top-level
parent.

#### `FavoriteService.java`
`isFavorite`, `findByUser`, and `toggle(user, tale)` which adds or removes the favorite and
returns the new state.

#### `CategoryService.java`
List/find/create plus `delete(id)`, which first detaches the category from all tales (clearing
join-table rows) so the delete isn't blocked by foreign keys.

#### `StorytellerRequestService.java`
`submit(user, message)` — the immediate-grant flow: if the user isn't already a storyteller, it
stores an **auto-approved** `StorytellerRequest` (adminNote «تأیید خودکار») for the admin history
page and adds the STORYTELLER role right away. This is safe because every tale still requires
admin approval before publication. Also `approve`/`reject` for genuinely pending requests, and
query helpers (`latestForUser`, `pending`, `all`, `pendingCount`).

#### `UserService.java`
Registration (BCrypt-encodes the password, always adds LISTENER, adds STORYTELLER if the checkbox
was ticked), profile updates, role grants, `passwordMatches`, `changePassword`.

#### `CustomUserDetailsService.java`
Spring Security bridge: loads a `User` by username and maps it to Spring's `UserDetails` with
authorities `ROLE_<name>` for each `Role` and `disabled(!user.isEnabled())`.

### 2.5 `controller/` — web layer

All controllers are server-rendered `@Controller`s returning template names, **except**
`controller/api/ProgressApiController` (the one `@RestController`). Mutations are POSTs with CSRF
enabled; feedback flows via `RedirectAttributes` flash attributes rendered as Bootstrap alerts.

#### Public / listener controllers

- **`HomeController`** — `GET /`: builds the landing page. Fetches the 8 latest approved tales and
  the top-5 of the current Jalali month; if fewer than 5 tales have ratings this month, the list
  is padded with the latest approved tales (no duplicates). Model: `topTales`, `topMonthName`,
  `latestTales`, `categories`.

- **`TaleController`** —
  - `GET /tales`: public catalog with search (`q`), category filter, and pagination (12 per page).
    Computes a **page-number window** (current ±2) so huge catalogs don't render hundreds of links.
  - `GET /tales/{id}`: detail page. Non-approved tales 404 unless the viewer is the owner or an
    admin (`canPreview`). Model includes average rating and count, top-level comments, and — for
    logged-in users — `isFavorite`, `userStars`, and `resumeSeconds` for the player.

- **`MediaController`** — binary responses:
  - `GET /tales/{id}/audio?t=<token>`: streams the narration as `ResponseEntity<Resource>` —
    returning a `Resource` lets Spring MVC honour **HTTP Range requests** automatically, so
    seeking works in the browser player. Three gates, all failing as plain 404: the tale must
    exist, `canListen` must pass (approved → public; otherwise owner/admin only), and the
    short-lived HMAC token must be valid. Response headers: the tale's stored content type,
    `Accept-Ranges: bytes`, `Content-Disposition: inline`, `Cache-Control: no-store`.
  - `GET /media/covers/{filename}` and `GET /media/avatars/{filename}`: serve images with a 7-day
    cache and a content type inferred from the extension.

- **`RatingController`** — `POST /tales/{id}/rating`: upserts the current user's star rating on an
  approved tale, then redirects back to the detail page.

- **`FavoriteController`** — `POST /tales/{id}/favorite`: toggles the favorite.

- **`CommentController`** — `POST /tales/{id}/comments`: validates non-empty and ≤1000 chars
  (errors as flash messages, redirecting to the `#comments` anchor), resolves `parentId` only if
  that comment belongs to the same tale, and delegates to `CommentService.add`.

- **`AuthController`** —
  - `GET /login`, `GET /register`: render the auth pages, but **redirect already-authenticated
    users to `/dashboard`**.
  - `POST /register`: adds manual checks on top of bean validation (username uniqueness, password
    confirmation match), registers the user, then issues a recovery code and passes it as a
    **flash attribute** to…
  - `GET /register/recovery-code`: the one-time display of the recovery code. Because it is
    flash-scoped, refreshing the page loses it by design; without a code in the model it redirects
    to `/login`.
  - `GET|POST /forgot-password`: username + recovery code + new password. On success the fresh
    replacement code is shown on the same recovery-code page (`afterReset` flag).

- **`DashboardController`** (`/dashboard`, authenticated) —
  - `GET /dashboard`: the listener dashboard — favorites, in-progress and finished listening lists
    (filtered to approved tales), total listened seconds, storyteller status, latest request.
  - `GET /dashboard/profile` + `POST /dashboard/profile`: display name + avatar upload (new avatar
    stored first, then the old file deleted).
  - `POST /dashboard/profile/password`: change password — requires the current password, applies
    the same policy as registration.
  - `POST /dashboard/storyteller-request`: the immediate STORYTELLER grant. After
    `requestService.submit(...)` succeeds it calls `refreshAuthorities(...)`, which reloads the
    user's authorities via `CustomUserDetailsService`, swaps a rebuilt `Authentication` into the
    `SecurityContext`, **and writes the context back into the HTTP session** — so the new role
    works without logging out and back in.
  - `POST /dashboard/recovery-code`: regenerate the recovery code (shown once via flash).

- **`controller/api/ProgressApiController`** — `POST /api/progress` (JSON, called by `player.js`
  with the CSRF header). Body record: `{taleId, seconds, duration, finished}`. Validates inputs,
  requires an approved tale, records progress, and **opportunistically stores the tale's duration**
  (reported by the browser's audio metadata) if the tale doesn't have one yet.

#### Storyteller controller

- **`StorytellerController`** (`/storyteller`, requires STORYTELLER role) —
  - `GET /storyteller/tales`: the storyteller's own tales with status badges.
  - `GET /storyteller/tales/new` + `POST /storyteller/tales`: create form/submit. Audio is
    required on create (checked in the controller since `MultipartFile` can't carry a `@NotNull`
    that distinguishes empty uploads). Storage errors (`IllegalArgumentException` from the
    extension whitelist) come back as global form errors.
  - `GET /storyteller/tales/{id}/edit` + `POST /storyteller/tales/{id}`: edit — **only for the
    owner and only while the tale is not approved** (`editableTale` 404s otherwise). Any edit
    resets the tale to `PENDING` and clears the review note/approval date, so it goes back through
    review.
  - `applyForm(...)` — shared mapping: trims title/description, resolves category IDs to entities,
    and on new audio/cover stores the new file *first*, then deletes the old one, and resets
    `durationSeconds` (a new recording has a new length).

#### Admin controllers (`/admin/**`, require ADMIN role)

- **`AdminController`** — `GET /admin`: dashboard with counters (pending/approved/rejected tales,
  users, pending requests).
- **`AdminTaleController`** (`/admin/tales`) — status-filtered paged list (default `PENDING`, 15
  per page), `GET /{id}` review page (admins can listen to pending audio there — `canListen`
  allows it), and `POST /{id}/approve` / `POST /{id}/reject` with an optional note.
- **`AdminUserController`** (`/admin/users`) — paged user list (20/page, newest first) and
  `POST /{id}/toggle-enabled` to disable/re-enable accounts. **Self-disable is blocked.** Existing
  sessions are *not* invalidated; the ban takes effect at next login.
- **`AdminCategoryController`** (`/admin/categories`) — list, create (blank/duplicate checked with
  flash errors), delete.
- **`AdminCommentController`** (`/admin/comments`) — paged moderation list, delete (replies are
  removed via the cascade).
- **`AdminRequestController`** (`/admin/requests`) — full history of storyteller requests, plus
  approve/reject for pending ones (approval grants the role via the service).

### 2.6 `dto/` — form objects

Form-backing beans with Jakarta Bean Validation annotations; **all messages are Persian** and are
rendered by the templates next to the fields.

| File | Fields & rules |
|---|---|
| `RegistrationForm` | `displayName` (required, ≤100); `username` (required, `^[a-zA-Z0-9_]{3,30}$`); `password` (required, 8–100 chars, must contain at least one letter **and** one digit via `^(?=.*\p{L})(?=.*\d).*$`); `confirmPassword` (match checked in the controller); `storyteller` checkbox. |
| `TaleForm` | `title` (required, ≤150); `description` (required, ≤4000); `categoryIds` (`@NotEmpty`); `audio` (`MultipartFile` — required on create, optional on edit, enforced in the controller); `cover` (optional). |
| `ForgotPasswordForm` | `username`, `recoveryCode`, `newPassword` (same password policy), `confirmPassword`. |
| `ChangePasswordForm` | `currentPassword`, `newPassword` (same policy), `confirmPassword`. |

### 2.7 `config/` — configuration & seeding

#### `SecurityConfig.java`
The heart of authentication/authorization. Three beans:

- `PasswordEncoder` — BCrypt.
- `DaoAuthenticationProvider` — customized so the **`enabled` check runs *after* password
  verification** (post-auth) instead of Spring's default pre-auth check. Consequence:
  `DisabledException` can only mean "correct password on a deactivated account", so
  `/login?disabled` is safe to show openly, while wrong passwords on *any* account (existing,
  disabled, or nonexistent) all read as a generic `/login?error` — no account probing.
- `SecurityFilterChain` — the access rules:
  - **Public:** `/`, `/register`, `/register/recovery-code`, `/login`, `/forgot-password`,
    `/error`, static assets (`/css/**`, `/js/**`, `/vendor/**`, `/fonts/**`), `/media/**`, and
    GET `/tales`, `/tales/*`, `/tales/*/audio` (the audio endpoint itself blocks non-approved
    tales for anyone but owner/admin).
  - `/storyteller/**` → `ROLE_STORYTELLER`; `/admin/**` → `ROLE_ADMIN`; everything else
    authenticated.
  - Form login at `/login`, success → `/dashboard`, failures routed through
    `LoginFailureHandler`; logout at `/logout` → `/?loggedout`.
  - `LoginRateLimitFilter` is registered **before** `UsernamePasswordAuthenticationFilter`, so
    banned IPs/usernames are turned away before authentication runs.
  - CSRF stays on (default); the layout exposes `_csrf` meta tags for JS fetch calls.

#### `LoginFailureHandler.java` (`@Component`)
Routes login failures: `DisabledException` → `/login?disabled` (not counted as an attack — the
password was right); anything else records a failure in `LoginAttemptService` and redirects to
`/login?banned` (if the failure just triggered a ban) or `/login?error`.

#### `LoginRateLimitFilter.java` (plain class, *not* `@Component`)
A `OncePerRequestFilter` that rejects `POST /login` from banned IPs/usernames with
`/login?banned` — **even with correct credentials** — while the ban lasts. Deliberately registered
manually inside the security chain (not component-scanned) to keep it out of the servlet
container's global filter chain.

#### `DataSeeder.java` (`CommandLineRunner`, `@Order(1)`)
Runs at every startup, idempotently:
- Seeds the admin account (`admin` / `admin123`) and a test admin (`testadmin` / `Qesse1234`),
  both from `app.seed.*` properties, only if the usernames don't exist yet.
- Seeds 7 default Persian categories (محلی، افسانه، لالایی، پندآموز، حیوانات، ماجراجویی، شعر و ترانه) by name.

#### `TaleSeeder.java` (`CommandLineRunner`, `@Order(2)` — after `DataSeeder`, needs the categories)
Seeds real playable content so the app isn't empty on first boot:
- Creates the storyteller user `qessegoo` / `Qessegoo1234` if missing.
- Defines 8 classic Persian folk tales (کدو قلقله‌زن، شنگول و منگول، ماه‌پیشونی، نخودی، عمو نوروز،
  حسن کچل، خاله سوسکه، طوطی و بازرگان) as an inline `record SeedTale` list with titles,
  descriptions, durations, and category names.
- For each: skips if a tale with the same **title** already exists (that's the idempotency key);
  otherwise copies the mp3 from `classpath:seed/audio/` into `uploads/audio/` via
  `FileStorageService.storeStream` and saves the tale as `APPROVED`.
- The narrations were generated with edge-tts `fa-IR` voices (public-domain folklore).

#### `GlobalModelAttributes.java` (`@ControllerAdvice`)
Adds `currentUser` (the full `User` entity, or `null` for anonymous) to **every** template model,
resolved from the `Authentication` name. This is what the navbar uses for the avatar/display name.

#### `FileStorageProperties.java`
`@ConfigurationProperties(prefix = "app.upload")` — just the `dir` property (default `uploads`).

### 2.8 `util/` — Persian date helper

#### `PersianDateUtil.java` (`@Component("persianDate")`)
Jalali calendar + Persian digit formatting, used directly from templates as
`${@persianDate.format(...)}`, `${@persianDate.digits(...)}`, `${@persianDate.duration(...)}`.
- `format(Instant)` — e.g. «۱۲ تیر ۱۴۰۵» (converts via the `persian-date-time` library, Tehran
  timezone).
- `startOfCurrentJalaliMonth()` — the `Instant` at the start of day 1 of the current Jalali month;
  the window for the home page's monthly top-5.
- `currentJalaliMonthName()` — e.g. «تیر».
- `duration(Object seconds)` — human Persian duration like «۵ دقیقه و ۳۰ ثانیه» (hours/minutes/
  seconds, seconds dropped once hours appear).
- `digits(Object)` — converts ASCII digits to Persian digits. Deliberately kept as a **single
  `Object` overload**: SpEL cannot resolve method overloads, so one polymorphic method it is.

---

## 3. Resources

### 3.1 `application.properties`

| Setting | Meaning |
|---|---|
| `server.port=8081` | App port. |
| `server.forward-headers-strategy=native` | Behind nginx, Tomcat's `RemoteIpValve` substitutes the real client IP from `X-Forwarded-For` (needed for login rate-limiting) — only honored when the direct peer is a private/loopback address, so external clients can't spoof it. The required nginx `proxy_set_header` lines are documented in comments. |
| `spring.datasource.*` | MySQL at `localhost:3306/myapp_db`, `createDatabaseIfNotExist=true`, and crucially `characterEncoding=UTF-8&connectionCollation=utf8mb4_persian_ci` for Persian text. ⚠️ Contains a plaintext local root password — local-only config; don't commit real credentials or reuse elsewhere. |
| `spring.jpa.hibernate.ddl-auto=update` | Schema is evolved by Hibernate; there are **no migration scripts**. |
| `spring.thymeleaf.cache=false` | Template hot reload in dev. |
| `spring.servlet.multipart.max-file-size=100MB` (request 120MB) | Voice tales can be long recordings. |
| `app.upload.dir=uploads` | Upload root (see `FileStorageProperties`). |
| `app.seed.*` | Seed admin usernames/passwords (dev defaults — change in production). |
| `app.media.token-secret=` | Audio-token HMAC secret; blank → random per boot. Set a fixed value if tokens must survive restarts. |

### 3.2 `templates/` — Thymeleaf views

All pages are `lang="fa" dir="rtl"`, UI text is hardcoded Persian by design (no i18n bundles).
Every page composes fragments from `fragments/layout.html`.

#### `fragments/layout.html`
Four fragments used by every page via `th:replace="~{fragments/layout :: …}"`:
- `head(title)` — meta/viewport, the **`_csrf` / `_csrf_header` meta tags** (consumed by
  `player.js` for the fetch call), a title that appends «| سرزمین قصه‌ها», and the three local
  stylesheets (Bootstrap RTL, Vazirmatn, `app.css`).
- `navbar` — brand, links gated by `sec:authorize` (`داشبورد من` when authenticated, `قصه‌گویی` for
  STORYTELLER, `مدیریت` for ADMIN), login/register buttons for guests, and for logged-in users the
  avatar + display name (from the global `currentUser`) plus a POST logout form.
- `footer` — brand + tagline.
- `scripts` — the vendored Bootstrap JS bundle.

#### Page templates by directory

| Directory | Files | Rendered by |
|---|---|---|
| `home/` | `index.html` — hero, monthly top-5 («برترین‌های <month>»), latest tales, category chips. | `HomeController` |
| `tales/` | `list.html` — search box, category filter, paginated card grid with the page window; `detail.html` — cover, description, the `<audio id="talePlayer">` element (src minted with `${@mediaToken.issue(tale.id)}`, `controlslist="nodownload"`, data attributes for tale id / resume position / auth flag), star rating form, favorite toggle, comments with one-level replies. | `TaleController` |
| `auth/` | `login.html` (renders the `?error` / `?disabled` / `?banned` / `?loggedout` states), `register.html` (with the live password checklist), `recovery-code.html` (one-time code display, print/copy hints), `forgot-password.html`. | `AuthController` |
| `dashboard/` | `index.html` — favorites, in-progress (progress bars via `getPercent()`), finished list, total listening time via `${@persianDate.duration(...)}`, storyteller request/status block; `profile.html` — display name + avatar form, change-password form (with checklist), recovery-code regeneration. | `DashboardController` |
| `storyteller/` | `tales.html` — the storyteller's tales with status badges and review notes; `form.html` — create/edit form with the **file-or-record** audio input (`recorder.js`) and category checkboxes styled as Bootstrap `btn-check` buttons. ⚠️ Those checkbox groups must **not** use `th:field` — it injects a hidden `_name` input between the checkbox and its label, breaking the `.btn-check:checked + .btn` sibling selector; plain `name` + `th:checked` is used instead. | `StorytellerController` |
| `admin/` | `index.html` (counter cards), `tales.html` (status-tabbed review queue), `tale-review.html` (listen + approve/reject with note), `users.html` (enable/disable), `categories.html`, `comments.html`, `requests.html`. | Admin controllers |
| `error/` | `403.html`, `404.html` — friendly Persian error pages picked up by Spring Boot's error-view convention. | Spring Boot |

Template gotcha (documented in CLAUDE.md): `#fields.hasGlobalErrors()` / `#fields.globalErrors()`
take no arguments and must live inside the `th:object` form.

### 3.3 `static/` — CSS, JS, fonts, vendor assets

All assets are **vendored locally** (Iranian CDN reliability) — the app makes zero runtime CDN
requests.

- **`css/app.css`** (~300 lines) — the visual theme on top of Bootstrap: CSS variables for the
  palette (night purple `--night`, moon yellow `--moon`, cream background, teal accent), Vazirmatn
  as the body font, sticky-footer flex layout, and component styles (gradient navbar, tale cards,
  star rating, record button pulse animation, progress bars, footer).
- **`js/player.js`** — enhances `#talePlayer` on the tale detail page. On `loadedmetadata` it
  seeks to the saved resume position (skipped when within the last 5 seconds). For authenticated
  users it POSTs progress to `/api/progress` — throttled to every 10s of `timeupdate`, plus on
  `pause`, plus on `ended` (with `finished: true` and `seconds: 0` so the next session starts from
  the beginning) — attaching the CSRF header read from the layout's meta tags. Failures are
  swallowed: progress is best-effort.
- **`js/recorder.js`** — in-browser voice recording for the storyteller form. Uses
  `getUserMedia` + `MediaRecorder` (picking the first supported of `audio/webm`, `audio/mp4`,
  `audio/ogg`), shows a live timer in Persian digits, and on stop wraps the chunks in a `File` and
  **injects it into the form's real `<input type="file">` via `DataTransfer`** — so the normal
  multipart submit carries the recording with no extra upload endpoint. Also renders a preview
  player and Persian error messages (no mic permission / unsupported browser).
- **`js/password-rules.js`** — the live password checklist: any input with
  `data-password-rules="<boxId>"` gets its three rules (≥8 chars, contains a letter `\p{L}`,
  contains a digit) checked on every keystroke, toggling ✔/✖ marks and success/muted classes on
  the matching `[data-rule=…]` items. Used on register, forgot-password, and change-password.
- **`fonts/vazirmatn.css` + `fonts/vazirmatn/*.woff2`** — the Vazirmatn Persian typeface
  (Regular/Medium/Bold) with local `@font-face` rules.
- **`vendor/bootstrap/`** — `bootstrap.rtl.min.css` (the RTL build) and
  `bootstrap.bundle.min.js` (JS + Popper).

### 3.4 `seed/audio/` — bundled tale narrations

Eight mp3 narrations matching `TaleSeeder`'s list (`kadu-qelqelezan.mp3`, `shangul-mangul.mp3`,
`mah-pishooni.mp3`, `nokhodi.mp3`, `amoo-norooz.mp3`, `hasan-kachal.mp3`, `khale-sooske.mp3`,
`tooti-bazargan.mp3`), generated with edge-tts `fa-IR` voices. They ship inside the jar (classpath)
and are copied into `uploads/audio/` on first boot.

---

## 4. Tests

`src/test/java/com/example/myapp/MyappApplicationTests.java` — a single `@SpringBootTest`
`contextLoads()` smoke test verifying the full application context (all beans, JPA mappings, and
security config) starts. Because it boots the real context, it **needs local MySQL running**.
Run with `mvn test` or `mvn test -Dtest=MyappApplicationTests`.

---

## 5. Runtime directories

- **`uploads/`** — created at startup relative to the working directory (configurable via
  `app.upload.dir`): `audio/` (tale narrations), `covers/` (tale cover images), `avatars/`
  (profile pictures). Everything is stored under random UUID filenames; the database stores only
  the filename, never a path or the original name.
- **`target/`** — Maven output: compiled classes, copied resources, surefire reports, and the
  executable `myapp-0.0.1-SNAPSHOT.jar`.

---

## 6. How the pieces fit together

**Publish a tale (storyteller):**
`storyteller/form.html` (upload a file *or* record via `recorder.js`) → `POST /storyteller/tales`
→ `TaleForm` validation → `FileStorageService.storeAudio` (extension whitelist, UUID name) →
`Tale` saved as `PENDING` → appears in the admin queue → admin listens on
`/admin/tales/{id}` and approves → `status=APPROVED`, `approvedAt` set → tale is public. Any later
edit by the owner resets it to `PENDING`.

**Listen with resume (listener):**
`GET /tales/{id}` puts `resumeSeconds` and a fresh 6-hour HMAC token into the page → the browser
requests `/tales/{id}/audio?t=…`; Spring serves the `Resource` with Range support so seeking works
→ `player.js` seeks to the resume point and posts progress every ~10s to `POST /api/progress`
(CSRF header from the layout's meta tags) → `ProgressService` upserts the row, advancing
`listenedSeconds` monotonically → the dashboard shows progress bars and replay-proof total
listening time.

**Login protection chain:**
`POST /login` → `LoginRateLimitFilter` (banned IP/username → `/login?banned`, even with the right
password) → `DaoAuthenticationProvider` (password check first, `enabled` check **after**) →
success: `AuthenticationSuccessEvent` clears failure history → failure: `LoginFailureHandler`
sends `/login?disabled` for correct-password-but-disabled, otherwise records the failure
(10 in 1 minute → 10-minute ban) and sends `/login?error` or `/login?banned`.

**Password recovery:**
Registration → `PasswordRecoveryService.issueCode` → code shown exactly once (flash-scoped
`/register/recovery-code`) → later, `/forgot-password` with username + code + new password →
throttled verification against the BCrypt hash → password set, old code consumed, fresh code
shown once.

---

## 7. Database schema summary

Schema is generated by Hibernate (`ddl-auto=update`); no migrations. Connection collation is
`utf8mb4_persian_ci`.

| Table | From entity | Key columns / constraints |
|---|---|---|
| `users` | `User` | `username` unique; `password_hash`; `recovery_code_hash`; `enabled` |
| `user_roles` | `User.roles` | element collection: `user_id` + `role` string |
| `tales` | `Tale` | `storyteller_id` FK; `audio_path`; `status`; `review_note`; `approved_at` |
| `tale_categories` | `Tale.categories` | join table `tale_id` × `category_id` |
| `categories` | `Category` | `name` unique |
| `comments` | `Comment` | `tale_id`, `author_id`, self-FK `parent_id`; replies cascade-delete |
| `ratings` | `Rating` | unique `(tale_id, user_id)`; `stars` 1–5; `updated_at` drives monthly top-5 |
| `favorites` | `Favorite` | unique `(user_id, tale_id)` |
| `listening_progress` | `ListeningProgress` | unique `(user_id, tale_id)`; `seconds` (resume) vs `listened_seconds` (monotonic) |
| `storyteller_requests` | `StorytellerRequest` | `user_id` FK; `status`; `admin_note`; `decided_at` |

---

## 8. Endpoint reference

| Method & path | Access | Controller |
|---|---|---|
| `GET /` | public | `HomeController` |
| `GET /tales`, `GET /tales/{id}` | public (non-approved: owner/admin) | `TaleController` |
| `GET /tales/{id}/audio?t=` | public + valid token (non-approved: owner/admin) | `MediaController` |
| `GET /media/covers/{f}`, `GET /media/avatars/{f}` | public | `MediaController` |
| `GET/POST /login`, `/register`, `GET /register/recovery-code`, `GET/POST /forgot-password`, `POST /logout` | public | `AuthController` / Spring Security |
| `POST /tales/{id}/rating`, `/favorite`, `/comments` | authenticated | `RatingController`, `FavoriteController`, `CommentController` |
| `POST /api/progress` | authenticated (JSON + CSRF header) | `ProgressApiController` |
| `GET /dashboard`, `GET/POST /dashboard/profile`, `POST /dashboard/profile/password`, `POST /dashboard/storyteller-request`, `POST /dashboard/recovery-code` | authenticated | `DashboardController` |
| `GET /storyteller/tales`, `GET /storyteller/tales/new`, `POST /storyteller/tales`, `GET /storyteller/tales/{id}/edit`, `POST /storyteller/tales/{id}` | ROLE_STORYTELLER | `StorytellerController` |
| `GET /admin` | ROLE_ADMIN | `AdminController` |
| `GET /admin/tales[?status,page]`, `GET /admin/tales/{id}`, `POST /admin/tales/{id}/approve\|reject` | ROLE_ADMIN | `AdminTaleController` |
| `GET /admin/users`, `POST /admin/users/{id}/toggle-enabled` | ROLE_ADMIN | `AdminUserController` |
| `GET/POST /admin/categories`, `POST /admin/categories/{id}/delete` | ROLE_ADMIN | `AdminCategoryController` |
| `GET /admin/comments`, `POST /admin/comments/{id}/delete` | ROLE_ADMIN | `AdminCommentController` |
| `GET /admin/requests`, `POST /admin/requests/{id}/approve\|reject` | ROLE_ADMIN | `AdminRequestController` |

**Seeded accounts:** `admin` / `admin123`, `testadmin` / `Qesse1234` (admins), `qessegoo` /
`Qessegoo1234` (storyteller with the 8 seeded tales).
