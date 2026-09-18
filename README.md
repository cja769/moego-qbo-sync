# MoeGo to QuickBooks Sync

Dockerized Spring Boot connector that maps MoeGo daily settlements into the normalized model from `pos-qbo-journal-core`. The initial version uses fixture input and a mock QuickBooks client; it makes no external API calls.

## Run the mock

Clone this repository and `pos-qbo-journal-core` as sibling directories, then run:

```bash
docker compose up --build
```

Production API clients, OAuth, scheduling, checkpoints, and deployment configuration will be added after sandbox credentials are available.
