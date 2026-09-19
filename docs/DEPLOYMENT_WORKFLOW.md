# Deployment Workflow

## Environments

| Environment | Git branch | Purpose | Deployment rule |
|---|---|---|---|
| Development | feature branches | Local implementation and tests | Manual/local only |
| Staging | `staging` | Shared integration and manual QA | Deploy automatically from `staging` |
| Production | `master` | Stable public release | Promote only after staging acceptance |

## Promotion flow

```text
feature branch
  -> pull request / review
  -> merge to staging
  -> staging deployment
  -> smoke test and acceptance
  -> merge staging to master
  -> production deployment
```

Do not use a feature branch as a long-lived deployment environment. Feature work should branch from the latest `staging` or `master` according to the change policy, then merge back through review.

## Web staging checks

Current staging web URL:

- `https://studyforielts.onrender.com/`

Current staging BFF URL:

- `https://studyforielts-youtube-bff.onrender.com/`

These URLs must be confirmed in Render before each release because service names and domains may change.

Verify on staging:

1. Web application loads directly.
2. Lesson list request succeeds.
3. Lesson detail request succeeds.
4. Audio URL loads and plays when available.
5. Vocabulary renders.
6. Browser console has no CORS errors.
7. Refreshing a detail route works.
8. Loading, empty, retry, and offline states are usable.
9. Mobile and desktop layouts have no horizontal overflow.
10. Keyboard focus and primary controls are usable.

## Environment configuration

- Staging web builds must point to the staging BFF URL.
- Staging BFF CORS must allow only the staging web origin and explicitly approved local development origins.
- Production secrets must never be copied into repository files or staging logs.
- Record environment variable names and non-secret values in deployment documentation; manage secret values in Render.
- Keep staging data and production data distinguishable. Do not use destructive cleanup scripts against production.

## Release gate

A change may be promoted from `staging` to `master` only when:

- Web typecheck, build, and tests pass.
- Relevant BFF checks pass.
- Android checks pass when shared BFF or Android behavior could be affected.
- Staging smoke checks pass.
- Known limitations are recorded.
- The commit to promote is identified explicitly.

## Rollback

If staging or production is broken:

1. Stop promotion.
2. Identify the last known-good commit.
3. Revert or redeploy that commit through the hosting provider.
4. Record the failure and follow-up fix.
5. Do not force-push shared branches.
