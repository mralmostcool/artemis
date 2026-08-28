---
trigger: always_on
---

- do not use docker compose to build or compile when working with 'frontend', 'nginx', 'redis' or 'postgresql'
- only do it when you are explicitly asked by the user.