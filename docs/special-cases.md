# Special Cases

Special cases feature let you define static responses for some edge-cases, 
they will take precedence over normal response logic from stubs.

### Simple format payload example

    {
      "requestMatch" : {
        "method" : "GET",
        "path" : "/test"
      },
      "response" : {
        "status" : 444
      }
    }
    
### Extended format payload example

    {
      "requestMatch" : {
        "method" : "PUT",
        "path" : "/test"
      },
      "response" : {
        "status" : 400,
        "body": "{\"code\":\"MY_CODE\"}",
        "headers": [
            {"name": "Content-Type", "value":"application/json"}
        ]
      }
    }
    
### Example of get all special-cases response

    [ {
      "requestMatch" : {
        "path" : "/test",
        "method" : "GET"
      },
      "response" : {
        "status" : 444,
        "headers" : [ ]
      },
      "planetId" : "Melmac",
      "id" : "5bc74d911200004600e37b7a"
    }, {
      "requestMatch" : {
        "path" : "/test1",
        "method" : "GET"
      },
      "response" : {
        "status" : 500,
        "headers" : [ ]
      },
      "planetId" : "Melmac",
      "id" : "5bc770901100004a003b0587"
    }, {
      "requestMatch" : {
        "path" : "/test",
        "method" : "PUT"
      },
      "response" : {
        "status" : 400,
        "body" : "{\"code\":\"MY_CODE\"}",
        "headers" : [ {
          "name" : "Content-Type",
          "value" : "application/json"
        } ]
      },
      "planetId" : "Melmac",
      "id" : "5bc7716011000066003b0588"
    } ]