## API Platform Test User payload

Payload format for `POST /agents-external-stubs/users/api-platform`

### Individual

    {
      "userId" : "apitestuser",
      "userFullName" : "API Test User",
      "emailAddress" : "user@api.gov.uk",
      "affinityGroup" : "Individual",
      "saUtr" : "6233991105",
      "nino" : "WZ587341D",
      "individualDetails" : {
        "firstName" : "Test",
        "lastName" : "User",
        "dateOfBirth" : "1972-12-23",
        "address" : {
          "line1" : "11 High Lane",
          "line2" : "Croydon",
          "postcode" : "CR12 3XZ"
        }
      },
      "services" : [ "self-assessment" ]
    }
    
### Organisation

    {
      "userId" : "apitestuser",
      "userFullName" : "API Test User",
      "emailAddress" : "user@api.gov.uk",
      "affinityGroup" : "Organisation",
      "ctUtr" : "6233991105",
      "eoriNumber" : "WZ587341975322",
      "vrn" : "233991153",
      "organisationDetails" : {
        "name" : "Test Organisation User",
        "address" : {
          "line1" : "11 High Lane",
          "line2" : "Croydon",
          "postcode" : "CR12 3XZ"
        }
      },
      "services" : [ "corporation-tax", "customs-services", "mtd-vat", "submit-vat-returns" ]
    }
    
### Agent

    {
      "userId" : "apitestuser",
      "userFullName" : "API Test User",
      "emailAddress" : "user@api.gov.uk",
      "affinityGroup" : "Agent",
      "arn" : "VARN2339911",
      "services" : [ "agent-services" ]
    }

## Agent Services Account fields

The API Platform test-user response can also contain the following fields. Posting the response to
`POST /agents-external-stubs/users/api-platform` imports the corresponding enrolments and creates the local records used
by the stubbed customer-lookup endpoints.

Field | Affinity group | Enrolment
---|---|---
`pillar2Id` | Organisation | `HMRC-PILLAR2-ORG` / `PLRID`
`capitalGainsTaxOnUKPropertyReference` | Individual or Organisation | `HMRC-CGT-PD` / `CGTPDRef`
`plasticPackagingTaxRegistrationNumber` | Individual or Organisation | `HMRC-PPT-ORG` / `EtmpRegistrationNumber`
`plasticPackagingTaxRegistrationDate` | Individual or Organisation | Preserved as the PPT subscription `dateOfApplication`
`pillar2RegistrationDate` | Organisation | Preserved as the Pillar 2 UPE `registrationDate`
`countryByCountryReportingId` | Organisation | `HMRC-CBC-ORG` / `cbcId`
`emailAddress` | Organisation with CBC | Preserved as the CBC subscription primary-contact email
`trustsUniqueTaxpayerReference` | Organisation | `HMRC-TERS-ORG` / `SAUTR`
`trustsUniqueReferenceNumber` | Organisation | `HMRC-TERSNT-ORG` / `URN`

The public test-user response deliberately omits the second identifier required by the UK CBC enrolment. The stubs
generate that UTR locally so the imported user has a complete `HMRC-CBC-ORG` enrolment.

## Missing-record fallback

Customer lookup remains local-first. When the record is missing, the stubs can query API Platform Test User and retry
for identifiers supported by its fetch API: NINO, SA UTR, VRN, Pillar 2 ID, CGT property-disposal reference, PPT
reference, CBC ID, taxable Trust UTR and non-taxable Trust URN.

PPT and Pillar 2 registration dates from the test-user response are retained unchanged in the generated AES records so
they can be used as invitation known facts. For CBC, the test-user `emailAddress` becomes the subscription
primary-contact email and the matching invitation known fact; when it is absent, AES retains its generated-email
fallback. CBC lookup also supports backend calls without a resolvable user session: it first searches existing records
across planets, then imports the API Platform test user into the default planet when no record exists. A missing upstream
test user remains a not-found result; the stubs do not fabricate a customer merely because an identifier has a valid
format.
