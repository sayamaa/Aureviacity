# Editable City Data

Primary built-in city/place data is in `cities.json`.

Each city is grouped by `slug` and `name`, then by category. Each place entry keeps the editable fields together:

- `name`
- `description`
- `category`
- `imageUrl`
- `coordinates.latitude`
- `coordinates.longitude`
- `rating`
- `tags`

The frontend still receives the same `CityPage`, `CategoryPage`, and `PlaceCard` data from `PortalService`; the UI layout and routes are unchanged.

Admin-added cities and places are still stored in the H2 database through `ManagedCity` and `ManagedPlace`, then merged by `PortalService`.
