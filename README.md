# Agents External Stubs

[ ![Download](https://api.bintray.com/packages/hmrc/releases/agents-external-stubs/images/download.svg) ](https://bintray.com/hmrc/releases/agents-external-stubs/_latestVersion)

This microservice is part of Agent Services local testing framework, 
providing dynamic stubs for 3rd party upstream services.

This app SHOULD NOT be run on QA nor Production environment.

## Stubbed API

### [Auth](https://github.com/hmrc/auth/blob/master/README.md)
#### POST /auth/authorise

Feature | What's implemented
-----------|-------------------------- 
predicates | `enrolment`, `authProviders` 
retrievals | `authProviderId`, `credentials`, `authorisedEnrolments`, `allEnrolments`,`affinityGroup`,`confidenceLevel`,`credentialStrength`

## Custom API

#### POST /agents-external-stubs/sign-in 
Authenticate an user

*Payload*

    {"userId": "foo", "plainTextPassword": "password", "providerType": "GovernmentGateway"}

Response | Description
---|---
201| when new authentication created
200| when an existing authentication found
    
#### GET  /agents-external-stubs/session/:authToken
Get current user authentication details

Response | Description
---|---
200| body: `{"userId": "foo", "authToken": "G676JHDJSHJ767676H", "providerType": "GovernmentGateway"}`, `Location` header contains link to get the entity
404| when `authToken` not found
    
#### GET  /agents-external-stubs/users/:userId
Get current user details

Response | Description
---|---
200| `{"userId": "foo", "principalEnrolments": [...], "delegatedEnrolments": [...]}`
404| when userId not found

#### PUT  /agents-external-stubs/users/:userId
Update an existing user.

**WARNING** Payload's `userId` field will not be used to find nor update!

*Payload*
    
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
404| when `userId` not found

#### POST /agents-external-stubs/users/
Create a new user

*Payload same as above*

Response | Description
---|---
201| when user created, `Location` header contains link to get the entity
404| when `userId` not found

## Running the tests

    sbt test it:test

## Running the tests with coverage

    sbt clean coverageOn test it:test coverageReport

## Running the app locally

    sm --start AGENTS_EXTERNAL_STUBS -f

or

    sbt run

It should then be listening on port 9009

    browse http://localhost:9009/

### License


This code is open source software licensed under the [Apache 2.0 License]("http://www.apache.org/licenses/LICENSE-2.0.html")
