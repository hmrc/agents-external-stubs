# Agents External Stubs

[ ![Download](https://api.bintray.com/packages/hmrc/releases/agents-external-stubs/images/download.svg) ](https://bintray.com/hmrc/releases/agents-external-stubs/_latestVersion)

This microservice is part of Agent Services local testing framework, 
providing dynamic stubs for 3rd party upstream services.

You will have to run [UI stubs](https://github.com/hmrc/agents-external-stubs-frontend) as well in order to be able to sign-in seamlessly into your application.

This app SHOULD NOT be run on QA nor Production environment.

## Table of contents

* [Stubbed APIs](#stubbed_api)
    * [Auth](#stubbed_api_auth)
    * [Citizen Details](#stubbed_api_citizen_details)
    * [User Groups Search](#stubbed_api_users_groups_search)
    * [Enrolment Store Proxy](#stubbed_api_enrolment_store_proxy)
    * [Tax Enrolments](#stubbed_api_tax_enrolments)
    * [DES](#stubbed_api_des)
* [Custom API](#custom_api)
    * [Authentication](#custom_api_authentication)
    * [Users Management](#custom_api_users)
    * [Master Records (DES)](#custom_api_records)
    * [Known Facts](#custom_api_known_facts)

## How requests are handled?

We handle local requests gracefully and do not require existing applications to reconfigure, i.e. we provide necessary TCP proxies:

- listening on 8500 for auth requests
- listening on 9337 for citizen-details requests
- listening on 9984 for users-groups-search requests
- listening on 7775 for enrolment-store-proxy requests
- listening on 9904 for des requests

You can switch this behaviour off by setting `proxies.start` config property to `false`.

## Data Model
Every stubbed user and other data live in some test sandbox (planet). 
You have to declare existing or a new planet whenever you sign-in. Each authenticated session have planetId information. 
Stubbed and custom UIs will consider only users and data assigned to the current planet.

User authentication expires after 15 minutes and so does the bearer token.
All users and other data on each planet are removed after 12h unless marked as permanent.

## Stubbed APIs <a name="stubbed_api"></a>

### [Auth](https://github.com/hmrc/auth) <a name="stubbed_api_auth"></a>

Endpoint | Description
---|---
[`POST /auth/authorise`](https://github.com/hmrc/auth/blob/master/docs/main.md#post-authauthorise) | general authorisation endpoint

Feature | What's implemented
-----------|-------------------------- 
predicates | `enrolment`, `authProviders`, `affinityGroup`, `confidenceLevel`, `credentialStrength`, `nino`, `credentialRole`, `$or`
retrievals | `authProviderId`, `credentials`, `authorisedEnrolments`, `allEnrolments`,`affinityGroup`,`confidenceLevel`,`credentialStrength`, `credentialRole`, `nino`, `groupIdentifier`, `name`, `dateOfBirth`, `agentCode`, `agentInformation`

### [Citizen Details](https://github.com/hmrc/citizen-details) <a name="stubbed_api_citizen_details"></a>

Endpoint | Description
---|---
[`GET /citizen-details/:idName/:taxId`](https://github.com/hmrc/citizen-details#get-citizen-detailsidnametaxid) | citizen information

### [User Groups Search](https://github.com/hmrc/users-groups-search) <a name="stubbed_api_users_groups_search"></a>

Endpoint | Description
---|---
`GET /users-groups-search/users/:userId` | user details
`GET /users-groups-search/groups/:groupId` | group details
`GET /users-groups-search/groups/:groupId/users` | users in the group
`GET /users-groups-search/groups?agentCode=:agentCode&agentId=:agentId` | group having the given agentCode, agentId is ignored

### [Enrolment Store Proxy](https://github.com/hmrc/enrolment-store-proxy) <a name="stubbed_api_enrolment_store_proxy"></a>

Endpoint | Description
---|---
`GET /enrolment-store-proxy/enrolment-store/enrolments/:enrolmentKey/users` | get user ids for the enrolment
`GET /enrolment-store-proxy/enrolment-store/enrolments/:enrolmentKey/groups` | get group ids for the enrolment
`PUT /enrolment-store-proxy/enrolment-store/enrolments/:enrolmentKey` | upsert an (allocated or unallocated) enrolment
`DELETE /enrolment-store-proxy/enrolment-store/enrolments/:enrolmentKey` | remove an unallocated enrolment
`POST /enrolment-store-proxy/enrolment-store/groups/:groupId/enrolments/:enrolmentKey` | allocate an enrolment to a group (agent)
`DELETE /enrolment-store-proxy/enrolment-store/groups/:groupId/enrolments/:enrolmentKey` | de-allocate an enrolment from a group (agent)

### [Tax Enrolments](https://github.com/hmrc/tax-enrolments) <a name="stubbed_api_tax_enrolments"></a>

Endpoint | Description
---|---
`POST /tax-enrolments/groups/:groupId/enrolments/:enrolmentKey` | allocate an enrolment to a group (agent)
`DELETE /tax-enrolments/groups/:groupId/enrolments/:enrolmentKey` | de-allocate an enrolment from a group (agent)
`PUT /tax-enrolments/enrolments/:enrolmentKey` | upsert an (allocated or unallocated) enrolment
`DELETE /tax-enrolments/enrolments/:enrolmentKey` | remove an unallocated enrolment

### DES <a name="stubbed_api_des"></a>

Endpoint | Description
---|---
`POST /registration/relationship` | Provides the ability for an agent to authorise or de-authorise the relationship with a taxpayer, or for a taxpayer to de-authorise the relationship with an agent.
`GET  /registration/relationship` | Provides the ability for a taxpayer (individual or organisation) or their agent to display historical or current relationships.
`GET  /registration/relationship/utr/:utr` | Provides the ability for a taxpayer (individual or organisation) to display current relationship with their agents.
`GET  /registration/relationship/nino/:nino` | ^^ as above
`GET  /registration/business-details/:idType/:idNumber` | Provides the ability for a taxpayer to get the business details associated with the taxpayer.
`GET  /vat/customer/vrn/:vrn/information` | Provides the ability for a taxpayer (Business or Individual) to retrieve a customer data record from the master system for the VAT tax regime.
`GET  /registration/personal-details/arn/:arn` | Provides the ability to request the Agent Record Details (business partner record) associated with the Register Once Number or Agent Register Once Number
`GET  /registration/personal-details/utr/:utr` | ^^ as above
`GET  /registration/agents/utr/:utr` | Provides the ability for an Agent to be subscribed into Agents Services, generating the agent reference number.
`GET  /registration/individual/:idType/:idNumber` | Provides the ability for a Taxpayer (Business or Individual) to register on the master system. In order to utilise this API, the taxpayer must have a valid NINO, UTR or EORI..
`GET  /registration/organisation/:idType/:idNumber` | Provides the ability for a Taxpayer (Business or Individual) to register on the master system. In order to utilise this API, the taxpayer must have a valid NINO, UTR or EORI..

## Custom API <a name="custom_api"></a>

### Authentication <a name="custom_api_authentication"></a>

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

Example (using [httpie](https://httpie.org/)):

    http POST localhost:9009/agents-external-stubs/sign-in userId=Alf planetId=Melmac
    HTTP/1.1 202 Accepted
    Location: /agents-external-stubs/session?authToken=8321db03-ba01-4115-838a-49daab5c6679
    
or    

    curl -v -X POST http://localhost:9009/agents-external-stubs/sign-in -H "Content-Type: application/json" --data '{"userId":"Alf","planetId":"Melmac"}'
    
    
#### GET  /agents-external-stubs/session/:authToken
Get current user authentication details

Response | Description
---|---
200| body: `{"userId": "foo", "authToken": "G676JHDJSHJ767676H", "providerType": "GovernmentGateway", "planetId": "your_test_planetId"}`, `Location` header contains link to get the entity
404| when `authToken` not found

Example (using [httpie](https://httpie.org/)):

    http GET localhost:9009/agents-external-stubs/session?authToken=8321db03-ba01-4115-838a-49daab5c6679
    HTTP/1.1 200 OK
    {
        "_links": [
            {
                "href": "/agents-external-stubs/sign-out",
                "rel": "delete"
            }
        ],
        "authToken": "8321db03-ba01-4115-838a-49daab5c6679",
        "planetId": "Melmac",
        "providerType": "GovernmentGateway",
        "sessionId": "e47f1b42-9616-43da-91f0-734eb958e88d",
        "userId": "Alf"
    }
    
or

    curl -v http://localhost:9009/agents-external-stubs/session?authToken=da7a42f1-7c31-4c0b-b8cb-c9f325457275

#### GET /agents-external-stubs/sign-out
Terminate current authentication and invalidate bearer token.

Response | Description
---|---
204| when success

### Test Users Management <a name="custom_api_users"></a>

#### GET  /agents-external-stubs/users?affinityGroup=X&limit=Y
Get all users available at the current planet (_requires valid bearer token_)

Optional params:
   * affinityGroup: only return users with this affinity
   * agentCode: only return agents with this agentCode
   * limit: max number of users returned, default to 100
   
Response | Description
---|---
200| list of users as [User entity](docs/userEntity.md)
204| empty list of users
    
#### GET  /agents-external-stubs/users/:userId
Get current user details. (_requires valid bearer token_)

Response | Description
---|---
200| [User entity](docs/userEntity.md)
404| when userId not found

#### PUT  /agents-external-stubs/users/:userId
Update an existing user. (_requires valid bearer token_)

**WARNING** Payload's `userId` field will not be used to find nor update!

*Payload*

[User entity](docs/userEntity.md)
     
Response | Description
---|---
202| when user accepted, `Location` header contains link to get the entity
400| when user payload has not passed validation
404| when `userId` not found
409| when user cannot be updated because of a unique constraint violation

#### POST /agents-external-stubs/users/
Create a new user. (_requires valid bearer token_)

*Payload*

[User entity](docs/userEntity.md)

Response | Description
---|---
201| when user created, `Location` header contains link to get the entity
400| when user payload has not passed validation
404| when `userId` not found
409| when `userId` already exists or any other unique constraint violation

Examples:

    http POST localhost:9009/agents-external-stubs/users/ Authorization:"Bearer 7f53d0bb-f15c-4c83-9a0c-057a33caba0a" affinityGroup=Agent
    HTTP/1.1 201 Created
    Location: /agents-external-stubs/users/User239
    
    http POST localhost:9009/agents-external-stubs/users/ Authorization:"Bearer 7f53d0bb-f15c-4c83-9a0c-057a33caba0a" userId=Alf affinityGroup=Agent
    HTTP/1.1 201 Created
    Location: /agents-external-stubs/users/Alf
    
or

    curl -v -X POST -H 'Content-Type: application/json' -H 'Authorization: Bearer 7f53d0bb-f15c-4c83-9a0c-057a33caba0a' --data '{"userId":"Alf","affinityGroup":"Individual"}'

#### DELETE  /agents-external-stubs/users/:userId
Delete user. (_requires valid bearer token_)

Response | Description
---|---
204| user has been removed
404| when userId not found

### Test Master Records Management <a name="custom_api_records"></a>

#### GET /agents-external-stubs/records
Returns all records on the planet grouped by the type, 
e.g. [records response](docs/records.md).

#### GET /agents-external-stubs/records/:recordId
Returns a record by its ID

#### PUT /agents-external-stubs/records/:recordId
Updates the record

#### DELETE /agents-external-stubs/records/:recordId
Removes the record

#### POST /agents-external-stubs/records/business-partner-record
#### GET /agents-external-stubs/records/business-partner-record/generate

#### POST /agents-external-stubs/records/business-details
#### GET /agents-external-stubs/records/business-details/generate

#### POST /agents-external-stubs/records/legacy-agent
#### GET /agents-external-stubs/records/legacy-agent/generate

#### POST /agents-external-stubs/records/legacy-relationship
#### GET /agents-external-stubs/records/legacy-relationship/generate

#### POST /agents-external-stubs/records/vat-customer-information
#### GET /agents-external-stubs/records/vat-customer-information/generate

### Test Known Facts Management <a name="custom_api_known_facts"></a>

#### GET /agents-external-stubs/known-facts/:enrolmentKey
Get known facts (enrolment) information

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
