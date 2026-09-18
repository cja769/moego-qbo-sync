# MoeGo to QuickBooks Sync

Dockerized Spring Boot connector that maps MoeGo daily settlements into the normalized model from `pos-qbo-journal-core`. The initial version uses fixture input and a mock QuickBooks client; it makes no external API calls.

## Run the mock

Clone this repository and `pos-qbo-journal-core` as sibling directories, then run:

```bash
docker compose up --build
```

Production API clients, OAuth, scheduling, checkpoints, and deployment configuration will be added after sandbox credentials are available.

## Persistent state

The container stores atomic sync receipts under `SYNC_DATA_DIRECTORY` (`/data` in Compose). Keep the service single-instance and retain the named Docker volume. The shared reconciliation planner supports daily checks plus Sunday, monthly, quarterly, and annual verification windows. Default business time zone is `America/Chicago` and is configurable with `SYNC_TIMEZONE`.
