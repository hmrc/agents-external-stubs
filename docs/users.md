## Users

Agents External Stubs let you define, authenticate and authorise test users using a dedicated frontend or an API.

### Group and User constraints:

- Group MUST have one and at most one Admin
- Group CAN have at most one Organisation
- Group MAY NOT consist of Assistants only
- Organisation MUST be an Admin in the group
- Agent MAY NOT be in the group with Organisation and Individuals
- All Agents in the group MUST have the same AgentCode

### Minimal user payload:

    { 
      "userId" : "YourUserId", 
      "affinityGroup" : "Individual|Organisation|Agent" 
    }

### Individual user example:

    {
        "userId" : "User003",
        "groupId" : "6V5Y-V0Q8-4Q2N-N4K0",
        "affinityGroup" : "Individual",
        "confidenceLevel" : 50,
        "credentialRole" : "User",
        "nino" : "AB 49 16 66 B",
        "enrolments" : {
          "principal" : [ 
            {
                "key" : "HMRC-MTD-VAT",
                "identifiers" : [ 
                    {
                        "key" : "VRN",
                        "value" : "615743441"
                    }
                ]
            }, 
            {
                "key" : "HMRC-MTD-IT",
                "identifiers" : [ 
                    {
                        "key" : "MTDITID",
                        "value" : "IOKQ81684863073"
                    }
                ]
            }
          ]
        },
        "name" : "Nicholas Brady",
        "dateOfBirth" : "1980-05-15",
        "address" : {
          "line1" : "72 Sidmouth Drive",
          "line2" : "Torquay",
          "postcode" : "TQ19 6KJ",
          "countryCode" : "GB"
        },
        "planetId" : "Alpha"
      
    }
    
### Organisation user example:

    {
        "userId" : "VAT0001",
        "groupId" : "3H2Q-Z6L0-0Z9L-J1B0",
        "affinityGroup" : "Organisation",
        "credentialRole" : "User",
        "enrolments" : {
          "principal" : [ 
            {
                "key" : "HMRC-MTD-VAT",
                "identifiers" : [ 
                    {
                        "key" : "VRN",
                        "value" : "771264037"
                    }
                ]
            }
          ]
        },
        "name" : "Dai-ich",
        "address" : {
           "line1" : "72 Sidmouth Drive",
           "line2" : "Torquay",
           "postcode" : "TQ19 6KJ",
           "countryCode" : "GB"
        },
        "planetId" : "Alpha"
    }
    
### Agent user example
    
    {
        "userId" : "Agent007",
        "groupId" : "3I3R-U5L8-8V0W-A2F0",
        "affinityGroup" : "Agent",
        "credentialRole" : "Admin",
        "enrolments" : {
          "principal" : [ 
            {
                "key" : "HMRC-AS-AGENT",
                "identifiers" : [ 
                    {
                        "key" : "AgentReferenceNumber",
                        "value" : "KARN7447541"
                    }
                ]
            }
          ],
          "delegated" : [ 
            {
                "key" : "HMRC-MTD-IT",
                "identifiers" : [ 
                    {
                        "key" : "MTDITID",
                        "value" : "FBIB94878857408"
                    }
                ]
            }
          ]
        },
        "name" : "Aubrey Levann",
        "agentCode" : "OCILEJ135897",
        "agentFriendlyName" : "Pritchard Professional",
        "agentId" : "451484",
        "address" : {
          "line1" : "72 Sidmouth Drive",
          "line2" : "Torquay",
          "postcode" : "TQ19 6KJ",
          "countryCode" : "GB"
        },
        "planetId" : "Alpha"
    }
    
### Stride user example:
    
    {
        "userId" : "User003",
        "strideRoles" : ["FOO"],
        "planetId" : "Alpha"
      
    }
