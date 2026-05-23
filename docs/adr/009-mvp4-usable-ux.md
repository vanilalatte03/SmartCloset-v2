# Define MVP4 as Responsive Usable UX

## Status
Accepted

## Context
MVP-3 completed the authenticated-user baseline. Users can sign up, log in, manage their own closet/location/preferences, create recommendations, view history, and mark recommendations as worn.

The remaining product risk is not recommendation scoring depth. The main issue is that a real user still has to understand internal enum values, backend failure codes, separated panels, and score-heavy recommendation output before getting value.

The MVP4 product goal is therefore: after signup or login, a user should succeed with their first outfit recommendation within 2 minutes.

## Decision
MVP4 is a responsive web UX MVP.

- Keep React + Vite + TypeScript as the frontend platform.
- Do not build a native mobile app in MVP4.
- Do not require PWA install, push notification, or app-store release work.
- Reorganize the logged-in app around five product views: Today, Closet, Preferences, Location, History.
- Use desktop sidebar navigation and mobile bottom tab navigation.
- Make Today the default logged-in view.
- Add a first-recommendation readiness checklist using existing API data.
- Add one protected current-weather summary API, `GET /api/weather/current`, so Today can show the user's saved-location weather before recommendation creation.
- Display API enum values as Korean UI labels, color swatches, and material chips.
- Add practical closet management UX for create, edit, and archive using existing clothes APIs.
- Replace raw recommendation business failure codes in the UI with Korean guidance and a direct CTA.
- Present recommendation reasons before score breakdown.

MVP4 does not add public APIs, DB schema, recommendation scoring, or new weather providers. The only backend API addition is the protected current-weather summary endpoint.

## Consequences
- Frontend implementation can proceed without backend schema migration.
- Existing API documentation remains valid, but frontend docs must define how MVP4 maps API data to user-facing UI.
- `GET /api/weather/current` reuses the existing KMA/fallback `WeatherProvider` path and must not create recommendation results or history.
- Recommendation failure codes remain stable API contracts while the UI becomes user-friendly.
- Visual references from external design drafts are reference material only. Project source of truth is the PRD, ADR, and frontend documentation.
- MVP5 can revisit image upload, PWA, native app, AI/GPT recommendations, or external location providers.

## Out of Scope
- Image upload
- Social login
- Password reset
- Email verification
- Refresh token
- External address/map API
- Browser geolocation
- AI/GPT recommendation
- styleTags scoring or recommendation reasons
- Preference normalization tables
- Redis
- AWS deployment and CD automation
