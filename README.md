# Agents External Stubs

[ ![Download](https://api.bintray.com/packages/hmrc/releases/agents-external-stubs/images/download.svg) ](https://bintray.com/hmrc/releases/agents-external-stubs/_latestVersion)

This microservice is part of Agent Services local testing framework, 
providing dynamic stubs for 3rd party upstream services.

You will have to run [UI stubs](https://github.com/hmrc/agents-external-stubs-frontend) as well in order to be able to sign-in seamlessly into your application.

This app SHOULD NOT be run on QA nor Production environment.

## How requests to the stubbed APIs are handled?

To handle requests aimed locally at the stubbed API microservices we provide necessary TCP proxies:

- listening on 8500 for auth requests
- listening on 9337 for citizen-details requests
- listening on 9984 for users-groups-search requests

You can switch this behaviour off by setting `proxies.start` config property to `false`.

## Data Model
Every stubbed user and other data live is some test sandbox (planet). 
You have to declare existing or a new planet whenever you sign-in. Each authenticated session have planetId information. 
Stubbed and custom UIs will consider only users and data assigned to the current planet.

User authentication expires after 15 minutes and so does the bearer token.
All users and other data on each planet are removed after 12h unless marked as permanent.

## Stubbed APIs

### [Auth](https://github.com/hmrc/auth)

Endpoint | Description
---|---
[`POST /auth/authorise`](https://github.com/hmrc/auth/blob/master/docs/main.md#post-authauthorise) | general authorisation endpoint

Feature | What's implemented
-----------|-------------------------- 
predicates | `enrolment`, `authProviders`, `affinityGroup`, `confidenceLevel`, `credentialStrength`, `nino`, `credentialRole`
retrievals | `authProviderId`, `credentials`, `authorisedEnrolments`, `allEnrolments`,`affinityGroup`,`confidenceLevel`,`credentialStrength`, `credentialRole`, `nino`, `groupIdentifier`, `name`, `dateOfBirth`, `agentCode`, `agentInformation`

### [Citizen Details](https://github.com/hmrc/citizen-details)

Endpoint | Description
---|---
[`GET /citizen-details/:idName/:taxId`](https://github.com/hmrc/citizen-details#get-citizen-detailsidnametaxid) | citizen information

### [User Groups Search](https://github.com/hmrc/users-groups-search)

Endpoint | Description
---|---
`GET /users-groups-search/users/:userId` | user details
`GET /users-groups-search/groups/:groupId` | group details
`GET /users-groups-search/groups/:groupId/users` | users in the group
`GET /users-groups-search/groups?agentCode=:agentCode&agentId=:agentId` | group having the given agentCode, agentId is ignored

## Custom API

### Authentication

#### POST /agents-external-stubs/sign-in 
Authenticate an user. 

Returns `Location` header with a link to the authentication details.

*Payload*

    {"userId": "foo", "plainTextPassword": "not_required", "providerType": "GovernmentGateway", "planetId":"your_test_planet"}

Response | Description
---|---
200| when an existing authentication (based on a header) and user found
201| when new authentication and user created
202| when new authentication created for an existing user
    
#### GET  /agents-external-stubs/session/:authToken
Get current user authentication details

Response | Description
---|---
200| body: `{"userId": "foo", "authToken": "G676JHDJSHJ767676H", "providerType": "GovernmentGateway", "planetId": "your_test_planetId"}`, `Location` header contains link to get the entity
404| when `authToken` not found

#### GET /agents-external-stubs/sign-out
Terminate current authentication and invalidate bearer token.

Response | Description
---|---
204| when success

### Test Users Management

#### GET  /agents-external-stubs/users?affinityGroup=X&limit=Y
Get all users available at the current planet (_requires valid bearer token_)

Optional params:
   * affinityGroup: only return users with this affinity
   * limit: max number of users returned, default to 100

Response | Description
---|---
200| list of users as `{ "users": [{ "id": "123", "affinity": "Agent" }, ...] }`
204| empty list of users
    
#### GET  /agents-external-stubs/users/:userId
Get current user details. (_requires valid bearer token_)

Response | Description
---|---
200| user entity details
404| when userId not found

#### PUT  /agents-external-stubs/users/:userId
Update an existing user. (_requires valid bearer token_)

**WARNING** Payload's `userId` field will not be used to find nor update!

*Payload*

User entity, e.g.,
    
    {   
        "userId": "any", 
        "principalEnrolments": [
            { 
                "key": "HMRC-AS-AGENT",
                "identifiers": [
                    {
                        "key": "AgentReferenceNumber",
                        "value": "TARN0000001"
                    }
                ]
            },
            ...
        ], 
        "delegatedEnrolments": [
            { 
                "key": "HMRC-MTD-ID",
                "identifiers": [
                    {
                        "key": "MTDITID",
                        "value": "ABC1234567"
                    }
                ]
            },
            ...
        ]
     }
     
Response | Description
---|---
202| when user accepted, `Location` header contains link to get the entity
400| when user payload has not passed validation
404| when `userId` not found
409| when user cannot be updated because of a unique constraint violation

#### POST /agents-external-stubs/users/
Create a new user. (_requires valid bearer token_)

*Payload same as above*

Response | Description
---|---
201| when user created, `Location` header contains link to get the entity
400| when user payload has not passed validation
404| when `userId` not found
409| when `userId` already exists or any other unique constraint violation

#### DELETE  /agents-external-stubs/users/:userId
Delete user. (_requires valid bearer token_)

Response | Description
---|---
204| user has been removed
404| when userId not found

## Running the tests

    sbt test it:test

## Running the tests with coverage

    sbt clean coverageOn test it:test coverageReport

## Running the app locally

    sm --start AGENTS_EXTERNAL_STUBS -f

or

    sbt run

It should then be listening on ports 9009 and 8500

    browse http://localhost:9009/

### License


This code is open source software licensed under the [Apache 2.0 License]("http://www.apache.org/licenses/LICENSE-2.0.html")
