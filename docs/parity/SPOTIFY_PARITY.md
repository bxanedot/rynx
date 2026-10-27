# Spotify parity
Reference behavior is entirely unverified. SPOT-* in FEATURE_MATRIX.md covers the requested feature set, including full playback, account lifecycle, catalog, library, remote devices, visuals and authorized offline use.
No login client ID, redirect registration, native SDK configuration, reference account or playback entitlement supplied. No Spotify runtime implementation present.
Next: observe authorized account flows; map each behavior to current official API/SDK capability and scopes. Shared domain owns account/auth/library/playback state; platform adapters own secure token storage, browser callback and permitted playback SDK lifecycle.
Do not represent catalog access as streaming permission. Do not promise Canvas, Connect, DJ or offline APIs until current official support and eligibility are confirmed. Do not emulate private protocols or bypass DRM. Never log credentials or tokens.
