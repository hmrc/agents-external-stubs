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