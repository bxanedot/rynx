# UI tokens
No measured tokens available. Do not substitute stock Material values.

| Token family | Required measurements | Evidence | Status |
|---|---|---|---|
| Layout | Coordinates, constraints, margins, padding, alignment, breakpoints | Missing | unverified |
| Typography | Font/license, size, weight, line height, tracking, line wrapping | Missing | unverified |
| Geometry | Radii, dimensions, borders, optical alignment | Missing | unverified |
| Color | Light/dark/accent, gradients, alpha, disabled/selected | Missing | unverified |
| Glass | Blur radius, tint, opacity, highlights, backdrop response | Missing | unverified |
| Artwork | Aspect, crop, scale, clipping, shadows | Missing | unverified |
| Interaction | Hit areas, focus, gesture thresholds, scroll and hover | Missing | unverified |

## Implemented proposals (not reference measurements)
- Accent/control selection: #FA2D48.
- Light canvas/surface/text/secondary/divider: #FFFFFF / #F5F5F7 / #18181A / #747478 / #E5E5E8.
- Dark canvas/surface/text/secondary/divider: #171719 / #222225 / #F5F5F7 / #A4A4AA / #353538.
- Platform sans fallback; library heading 32sp semibold; body 14sp; secondary 12sp.
- Desktop adaptation threshold 760dp; sidebar 216dp; player strip 72dp; control target 44dp; original glyph viewport 24 units; displayed glyph 22dp.
These values live in shared/ui and need reference review. No SF font or measured Apple colors are claimed.
