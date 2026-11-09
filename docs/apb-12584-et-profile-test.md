# APB-12584 External Test profile verification

This runbook proves that the new API Platform organisation profiles are pulled into Agents External Stubs (AES) in External Test (ET), then validates Agent Authorisation API V3 after it is enabled. It covers organisations created through both the Developer Hub UI and the Create Test User API.

Complete the pre-V3 gate before enabling V3, then continue with the automated
V3 matrix and acceptance pack in the later sections.

The Scala utility is test-only code. It is run with sbt and is not packaged into or deployed with AES. It never prints passwords, client secrets or access tokens.

Every stateful command requires an explicit `--work-dir`. This prevents a tester from accidentally using credentials or evidence from another Jira run. The examples below use:

```text
$PWD/.local/et-testing/APB-12584
```

The tool resolves and prints the absolute work directory and configuration path before doing anything. If a different private directory is used, supply that same directory to every command.

The tool is self-guiding: every successful command prints the exact next action. Its output labels steps as `[BROWSER REQUIRED]`, `[CLI AUTOMATED]`, `[DATABASE ACCESS REQUIRED]`, or `[MANUAL CONFIRMATION REQUIRED]`. Browser and database steps cannot be completed by the Scala tool; it explains precisely what the tester must do and how to resume the automated flow.

## Profiles under test

Create each organisation with all of these profiles:

- `mtd-vat` — supplies the VRN and VAT registration date used for the supported V2 pull-through trigger
- `mtd-income-tax` — supplies the MTDITID and retains coverage of the established profile; it is not the acceptance trigger
- `capital-gains-tax-uk-property`
- `plastic-packaging-tax`
- `pillar-2`
- `trusts`
- `country-by-country-reporting`

The expected AES enrolments are:

| Service | Identifier |
|---|---|
| `HMCE-VATDEC-ORG` | `VATRegNo` |
| `HMRC-MTD-VAT` | `VRN` |
| `HMRC-MTD-IT` | `MTDITID` |
| `HMRC-CGT-PD` | `CGTPDRef` |
| `HMRC-PPT-ORG` | `EtmpRegistrationNumber` |
| `HMRC-PILLAR2-ORG` | `PLRID` |
| `HMRC-CBC-ORG` | `cbcId` and `UTR` |
| `HMRC-TERS-ORG` | `SAUTR` |
| `HMRC-TERSNT-ORG` | `URN` |

## 1. Check out the test branch

Clone the repository if necessary, then run:

```bash
git fetch origin
git switch APB-12584-et-test-tool
git pull --ff-only origin APB-12584-et-test-tool
```

Check Java and sbt:

```bash
java -version
sbt --version
```

The repository currently uses Java 21 and Scala 3. Use the repository's normal development environment if either command is unavailable.

Display the command overview:

```bash
sbt "Test / runMain uk.gov.hmrc.agentsexternalstubs.tools.EtProfileTestTool help"
```

## 2. Obtain sandbox application credentials

This is a manual prerequisite. The Scala utility cannot create or retrieve Developer Hub credentials.

Display the same credential instructions in the terminal at any time:

```bash
sbt "Test / runMain uk.gov.hmrc.agentsexternalstubs.tools.EtProfileTestTool credentials-help"
```

Complete these steps:

1. Open the [Developer Hub sign-in page](https://developer.service.hmrc.gov.uk/developer/login).
2. If you already have a Developer Hub account, sign in with it. Do not create a duplicate account.
3. If you do not have an account, select **Register for an account**, complete registration and activate the account using the instructions sent by Developer Hub. Then return to the sign-in page and sign in.
4. Open **Applications** after signing in.
5. Select the sandbox application intended for APB-12584 testing. If no suitable application exists, create a dedicated sandbox application using the normal team process.
6. Open the application's credentials page and copy its client ID into approved temporary secret storage.
7. Obtain a current client secret from approved secret storage. If none is available, generate a new client secret in Developer Hub. Do not delete or rotate a secret already used by somebody else without coordinating with its owner.
8. Confirm the application is subscribed to **Create Test User API 1.0**.
9. Confirm the application is subscribed to **Agent Authorisation API 2.0**.
10. Before the V3 phase, confirm it is also subscribed to **Agent Authorisation API 3.0**.
11. Confirm `https://www.example.com/auth-redirect` is registered as a redirect URI, or note the registered alternative that will be used for this run.

At the end of this step the tester must have these three values, but must not put them in Git, Jira, screenshots or chat:

```text
CLIENT_ID
CLIENT_SECRET
REDIRECT_URI
```

HMRC's supporting documentation is available under [testing in the sandbox](https://developer.service.hmrc.gov.uk/api-documentation/docs/testing) and [credentials](https://developer.service.hmrc.gov.uk/api-documentation/docs/authorisation/credentials).

## 3. Initialise the private work directory

Run:

```bash
sbt "Test / runMain uk.gov.hmrc.agentsexternalstubs.tools.EtProfileTestTool init-config --work-dir $PWD/.local/et-testing/APB-12584"
```

This creates:

```text
.local/et-testing/APB-12584/
├── operator.env
└── raw/
```

The directories are restricted to the current user and `operator.env` is created with private permissions. The command refuses to overwrite an existing configuration.

Open `.local/et-testing/APB-12584/operator.env` in a local editor and set the values obtained in step 2:

```text
CLIENT_ID=<sandbox application client ID>
CLIENT_SECRET=<current sandbox application client secret>
REDIRECT_URI=https://www.example.com/auth-redirect
```

If the application uses a different registered redirect URI, enter that exact value instead.

Confirm that Git ignores the file:

```bash
git check-ignore -v .local/et-testing/APB-12584/operator.env
```

The tool reads credentials only from the protected `operator.env` file and
never prints their values.

## 4. Validate the configuration and credentials

Run:

```bash
sbt "Test / runMain uk.gov.hmrc.agentsexternalstubs.tools.EtProfileTestTool check-config --work-dir $PWD/.local/et-testing/APB-12584"
```

Expected result:

```text
OAuth client_credentials: PASS (HTTP 200)
Configuration and Developer Hub credentials: PASS
```

The command checks that the required configuration values exist and requests an application token from the sandbox OAuth endpoint. It does not print any credential or token value.

If validation returns HTTP `401`, the client ID or secret may be wrong, deleted or expired. Re-run `credentials-help`, check the selected sandbox application, and update `operator.env` with a current secret.

## 5. Run the ET profile preflight

```bash
sbt "Test / runMain uk.gov.hmrc.agentsexternalstubs.tools.EtProfileTestTool preflight --work-dir $PWD/.local/et-testing/APB-12584"
```

Expected results:

```text
OAuth client_credentials: PASS (HTTP 200)
capital-gains-tax-uk-property                    PASS
plastic-packaging-tax                            PASS
pillar-2                                         PASS
trusts                                           PASS
country-by-country-reporting                     PASS
```

Stop with a NO-GO if any profile is absent. HTTP `403` normally means the application is not subscribed to Create Test User API 1.0.

## 6. Create the UI organisation

Display the guided instructions with the active work directory:

```bash
sbt "Test / runMain uk.gov.hmrc.agentsexternalstubs.tools.EtProfileTestTool ui-org-help --work-dir $PWD/.local/et-testing/APB-12584"
```

Open the sandbox Create Test User page:

```text
https://developer.service.hmrc.gov.uk/api-test-user
```

Sign in to Developer Hub if prompted, select **Organisation**, and click **Create** once. There is no profile selector in this UI. It automatically requests every service currently available for an organisation. Keep the generated result page open until it has been saved.

In the browser, use **Save Page As** and select **Webpage, HTML Only**. Save the page as:

```text
.local/et-testing/APB-12584/raw/ui-organisation.html
```

Protect the saved page:

```bash
chmod 600 .local/et-testing/APB-12584/raw/ui-organisation.html
```

Extract the credentials and required identifiers into protected JSON and validate them:

```bash
sbt "Test / runMain uk.gov.hmrc.agentsexternalstubs.tools.EtProfileTestTool capture-ui-html --work-dir $PWD/.local/et-testing/APB-12584"
```

The command reads `.local/et-testing/APB-12584/raw/ui-organisation.html` and writes `.local/et-testing/APB-12584/raw/ui-organisation.json` without printing any extracted value. Every required field must report `PASS`. If the saved Developer Hub result omits a required field, stop and record a NO-GO defect.

## 7. Create a test agent

```bash
sbt "Test / runMain uk.gov.hmrc.agentsexternalstubs.tools.EtProfileTestTool create-agent --work-dir $PWD/.local/et-testing/APB-12584"
```

Expected result:

```text
OAuth client_credentials: PASS (HTTP 200)
Create agent test user: PASS (HTTP 201)
Agent credentials and ARN saved privately to:
  /absolute/work-directory/raw/agent.json
Sensitive values were not printed.
```

The credentials are in `.local/et-testing/APB-12584/raw/agent.json`. Open that file only in a local editor when the next step needs them.

## 8. Authorise the test agent

Print the prepared OAuth URL:

```bash
sbt "Test / runMain uk.gov.hmrc.agentsexternalstubs.tools.EtProfileTestTool oauth-url --work-dir $PWD/.local/et-testing/APB-12584"
```

Open the printed URL in a browser. Sign in with the `userId` and `password` from `agent.json`, approve access if prompted, and wait for the redirect. The URL will start with:

```text
https://www.example.com/auth-redirect?code=...
```

Copy only the value after `code=` and before the next `&`. Save it as the only line in:

```text
.local/et-testing/APB-12584/raw/oauth-code.txt
```

Then run:

```bash
chmod 600 .local/et-testing/APB-12584/raw/oauth-code.txt
sbt "Test / runMain uk.gov.hmrc.agentsexternalstubs.tools.EtProfileTestTool exchange-code --work-dir $PWD/.local/et-testing/APB-12584"
rm .local/et-testing/APB-12584/raw/oauth-code.txt
```

Expected results include `OAuth authorization_code exchange: PASS (HTTP 200)` and the exact private path of `agent-token.json`. The token value is not printed. If the exchange fails, obtain a fresh code; OAuth codes are short-lived and single-use. Confirm that the redirect URI exactly matches the application configuration.

## 9. Pull the UI organisation into AES

```bash
sbt "Test / runMain uk.gov.hmrc.agentsexternalstubs.tools.EtProfileTestTool v2-invite ui mtd-vat --work-dir $PWD/.local/et-testing/APB-12584"
```

Expected result:

```text
V2 MTD-VAT invitation for ui organisation: PASS (HTTP 204)
```

The V2 MTD-VAT invitation uses the Organisation's VRN and VAT registration date. It is the supported acceptance trigger which causes AES to retrieve the whole API Platform test user, including the new profiles. Do not substitute `mtd-it` for this acceptance step: Organisation-by-NINO is a separate unresolved contract question. The tool retains `mtd-it` only for explicitly requested diagnostics.

## 10. Verify the UI organisation in AES

Display the query and expected enrolment checklist:

```bash
sbt "Test / runMain uk.gov.hmrc.agentsexternalstubs.tools.EtProfileTestTool mongo-help ui --work-dir $PWD/.local/et-testing/APB-12584"
```

Use the Platform Tools Jenkins job:

```text
https://build.tax.service.gov.uk/job/PlatformTools/job/query-mongo/
```

Select **Build with Parameters** and set:

```text
ENVIRONMENT = externaltest
REPLICA_SET_CHOICE = protected
```

The `mongo-help` command reads `userId` from the private organisation JSON and writes a protected, ready-to-paste query to:

```text
.local/et-testing/APB-12584/raw/ui-mongo-query.js
```

Open that file in a local editor and copy its one line into `QUERY`. The tool does not print the generated user ID. Do not add a `use` command or a leading dot.

If `externaltest` is not offered or Jenkins denies access, stop and ask a team member with External Test `query-mongo` permission to run the generated query. Do not use another environment or a write-capable route as a substitute.

Interpret the query output separately from the Jenkins build status:

- `user: null` means the organisation is absent from AES: record **FAIL** and **NO-GO**.
- `group: null` means the AES group is absent: record **FAIL** and **NO-GO**.
- `group.affinityGroup` must be `Organisation`.
- A returned user and group mean continue with the service and identifier checks.
- `Finished: SUCCESS` means only that Jenkins executed the query; it does not mean the APB-12584 assertion passed.

The generated query emits one line beginning `APB12584_RESULT=`. Copy the
complete Jenkins console output into:

```text
.local/et-testing/APB-12584/raw/ui-mongo-console.txt
```

Then protect, extract and validate it:

```bash
chmod 600 .local/et-testing/APB-12584/raw/ui-mongo-console.txt
sbt "Test / runMain uk.gov.hmrc.agentsexternalstubs.tools.EtProfileTestTool capture-mongo ui --work-dir $PWD/.local/et-testing/APB-12584"
```

The tool locates the marked JSON itself, checks the Organisation affinity, all
nine services and the private PPT, CBC and Pillar 2 known facts, then writes a
protected `ui-mongo-result.json`. Do not manually reconstruct the JSON.

Confirm every service and identifier in the table at the top of this document. Values must match the Developer Hub result. CBC is special: the public result supplies `cbcId`, while AES should complete its enrolment with a `UTR`.

For diagnosis, the user can also be viewed at:

```text
https://test-www.tax.service.gov.uk/agents-external-stubs/user?userId=<UI_USER_ID>
```

The browser page and AES REST Query page are useful cross-checks but do not replace the Jira-required Mongo evidence. Do not paste a complete Mongo result into Git, Jira or chat; record only the sanitised checks described later in this guide.

## 11. Create and validate the API organisation

```bash
sbt "Test / runMain uk.gov.hmrc.agentsexternalstubs.tools.EtProfileTestTool create-api-org --work-dir $PWD/.local/et-testing/APB-12584"
```

Expected result starts with:

```text
OAuth client_credentials: PASS (HTTP 200)
Create organisation through API: PASS (HTTP 201)
```

Every identifier field must also report `PASS`. The tool prints the exact private path of `api-organisation.json` without printing any sensitive values.

## 12. Pull the API organisation into AES

The agent access token may expire. Refresh it without repeating the browser journey:

```bash
sbt "Test / runMain uk.gov.hmrc.agentsexternalstubs.tools.EtProfileTestTool refresh-token --work-dir $PWD/.local/et-testing/APB-12584"
```

If refresh itself fails, the command explains how to repeat step 8 safely.

```bash
sbt "Test / runMain uk.gov.hmrc.agentsexternalstubs.tools.EtProfileTestTool v2-invite api mtd-vat --work-dir $PWD/.local/et-testing/APB-12584"
```

Expected result:

```text
V2 MTD-VAT invitation for api organisation: PASS (HTTP 204)
```

## 13. Verify the API organisation in AES

Display the API organisation query and checklist:

```bash
sbt "Test / runMain uk.gov.hmrc.agentsexternalstubs.tools.EtProfileTestTool mongo-help api --work-dir $PWD/.local/et-testing/APB-12584"
```

Run `mongo-help api`, then execute its generated query in the same Jenkins job.
Save the complete console output as:

```text
.local/et-testing/APB-12584/raw/api-mongo-console.txt
```

Extract and validate it:

```bash
chmod 600 .local/et-testing/APB-12584/raw/api-mongo-console.txt
sbt "Test / runMain uk.gov.hmrc.agentsexternalstubs.tools.EtProfileTestTool capture-mongo api --work-dir $PWD/.local/et-testing/APB-12584"
```

All checks must pass. The resulting protected `api-mongo-result.json` supplies
the exact private known facts used by the automated V3 matrix.

## 14. Record the pre-V3 decision

Record only sanitised evidence:

- execution date and tester
- relevant deployed versions or commit references
- HTTP statuses
- all expected service and identifier names
- masked identifiers, for example the first four and last three characters
- pass/fail for UI and API separately
- defect references and retest results

Never record passwords, client secrets, access tokens, OAuth codes, bearer values, session IDs or complete raw Mongo documents.

Select **GO** only when both organisations meet all of these conditions:

- creation succeeded
- every generated identifier was present and correctly formed
- the V2 MTD-VAT invitation returned `204`
- AES contains every expected service and identifier
- AES values match the creation result
- both taxable and non-taxable Trusts enrolments exist
- CBC contains `cbcId` and the AES-completed `UTR`

Otherwise select **NO-GO**, keep V3 disabled, and raise a focused defect.

## 15. Enable and confirm V3

Proceed only after the pre-V3 decision is **GO**:

1. Merge and deploy the approved External Test configuration with
   `features.enable-v3=true`.
2. Restart or redeploy Agent Authorisation API so it reads the new setting.
3. Confirm the V3 API definition is published in External Test.
4. Confirm the sandbox application is subscribed to Agent Authorisation API
   3.0.
5. Confirm compatible Agent Client Relationships and Agents External Stubs
   versions are deployed.

These are deployment and browser checks. The Scala tool deliberately cannot
change an environment or subscribe an application.

## 16. Run the automated V3 Organisation and endpoint matrix

Run:

```bash
sbt "Test / runMain uk.gov.hmrc.agentsexternalstubs.tools.EtProfileTestTool v3-matrix api --work-dir $PWD/.local/et-testing/APB-12584"
```

The command automatically:

1. validates the saved API organisation and Mongo evidence;
2. refreshes the agent OAuth token without printing it;
3. tests MTD-VAT, taxable Trusts, non-taxable Trusts, CGT, PPT, UK CBC and
   Pillar 2;
4. performs create, get, cancel and final-get for every scenario;
5. tests list invitations, absent relationship checking and idempotent
   deauthorisation;
6. confirms every cancelled invitation finishes with status `Cancelled`;
7. writes private requests and responses under `raw/`; and
8. writes a sanitised summary to `v3-matrix-summary.json` in the work directory.

The terminal and sanitised summary contain only scenario names, HTTP statuses,
invitation prefixes, final statuses and PASS/FAIL. They do not contain client
identifiers, known facts, invitation IDs, credentials or tokens.

If any row fails, do not rerun blindly: inspect its protected response locally.
A pending invitation left by an interrupted run can cause a duplicate error;
cancel it or use a fresh organisation before repeating the matrix.

## 17. Run the authoritative V3 acceptance pack

The ET operator tool intentionally does not duplicate the comprehensive error
matrix. In `agent-authorisation-api-acceptance-tests`, run the non-default V3
pack using that repository's README and required Service Manager services:

```bash
./run-v3-tests.sh local false
```

This covers malformed payloads and identifiers, unsupported services, known
fact failures, duplicates, invalid invitation states, authentication, scopes,
ARN and agency-permission boundaries. Both the ET tool result and the
acceptance pack must be green before final sign-off.

## 18. Re-test with V3 disabled

After the enabled-state evidence is complete:

1. deploy the approved External Test configuration with
   `features.enable-v3=false`;
2. restart or redeploy Agent Authorisation API so it reads the changed value;
3. run the non-mutating compatibility check:

```bash
sbt "Test / runMain uk.gov.hmrc.agentsexternalstubs.tools.EtProfileTestTool v3-disabled-check api --work-dir $PWD/.local/et-testing/APB-12584"
```

The command refreshes OAuth, calls a V3-only relationship route and requires
AAA's exact HTTP 404 `StandardNotFound` response. It then calls the V2
invitations endpoint and requires HTTP 200. It does not create, cancel or alter
an invitation or relationship. Private responses remain under `raw/`; the
shareable result is `v3-disabled-summary.json` in the work directory.

If the V3 check returns HTTP 200, V3 is still enabled in the running AAA
instance. If V2 does not return HTTP 200, do not treat the disabled-state test
as successful: investigate the V2 regression or token/application setup.

## 19. Record the final decision

Attach or copy only the sanitised `v3-matrix-summary.json` and
`v3-disabled-summary.json` content after reviewing it. Record deployed
versions, both feature-flag states, the acceptance job result, defect and
retest references, and the final GO/NO-GO decision. Never attach anything
from the `raw` directory.

## Repeat-run cleanup

Use a fresh work directory or delete obsolete raw files before a repeat run. Use fresh organisations to avoid duplicate invitations. Delete OAuth code files immediately after exchange and delete token files when no more calls are needed. Retain raw evidence only as long as required by the ticket.
