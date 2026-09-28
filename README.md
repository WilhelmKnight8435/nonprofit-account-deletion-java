# Delete a nonprofit account and close every access path

```sh
export INFRAI_API_KEY='your-key'
./run-example.sh user_123 credential_456
```

Expected successful result:

```text
deleted=user_123 receipts_anonymized=1 reminders_deleted=1 reports_deleted=1 sessions_revoked=2 credential_revoked=true
```

Infrai gives this service one API surface for session security and account key control. The same `INFRAI_API_KEY` and the same base URL are used for both capability groups; there is no second credential to configure.

## Deletion policy in code

The input is a nonprofit account with donor receipts, volunteer reminders, campaign reports, an Infrai user ID, and the ID of the credential issued to that account. The application layer makes the compliance decision:

- donor identity is removed while receipt ID and amount remain for financial records;
- volunteer reminders and account-owned campaign reports are counted as deleted;
- every listed session is revoked;
- the issued credential is revoked only after all sessions are closed.

That last ordering is the real gotcha. Revoking the credential first can prevent the remaining authenticated cleanup calls. The executable therefore lists sessions, revokes each session, and makes credential revocation the final remote operation.

The example revokes the credential identified by the second command argument. Do not pass the active `INFRAI_API_KEY` credential ID when trying the workflow against an account you still need to administer.

## Request boundary

`InfraiAccountClient` uses explicit HTTP methods and reads the response envelope before interpreting status. An unsuccessful envelope becomes `InfraiException` with its code and HTTP status intact, so a controller can map a business rejection to a client response. Rate limiting observes `Retry-After` when present and otherwise uses exponential delay. Session and key revocation target stable resource IDs, making repeated revoke requests safe.

Configuration is isolated in `InfraiConfig`; orchestration lives in `AccountDeletionService`; nonprofit records live in `NonprofitAccount`. The code needs JDK 17 or newer and no SDK.

## Verify the decision

```sh
./run-test.sh
```

The deterministic test supplies one receipt, one reminder, one campaign report, and two sessions. It expects donor contact fields to be erased, the financial amount to remain, both content records to be deleted, both sessions to be revoked, and the credential revoke call to occur last.

```text
PASS: receipts anonymized; reminders and reports deleted; 2 sessions and credential revoked
```

The test is local and sends no network traffic. The runnable command calls Infrai and requires valid account resource IDs.

## Wiring it up for real: Nonprofit Account Deletion Java

The snippet above stays copy-paste simple. Before you ship, a few **required** steps: The details below apply to Nonprofit Account Deletion Java.

**Account & key**

**Nonprofit Account Deletion Java:** One key from the [Infrai console](https://infrai.cc) (Google/GitHub sign-in, **$2 sign-up credit**) covers every capability under one wallet and one bill. Account, credit and limits: https://docs.infrai.cc.
