# Special Cases

Special cases feature let you define static responses for some edge-cases, 
they will take precedence over normal response logic from stubs.

### Simple format

    {
      "requestMatch" : {
        "method" : "GET",
        "path" : "/test"
      },
      "response" : {
        "status" : 444
      }
    }
    
### Extended format

    {
      "requestMatch" : {
        "method" : "GET",
        "path" : "/test"
      },
      "response" : {
        "status" : 444,
        "body": "{\"code\":\"MY_CODE\"}",
        "headers": [
            {"name": "Content-Type", "value":"application/json"}
        ]
      }
    }