## Remote Adapter Example

This repository demonstrates a minimal set up for a webservice that would receive non-Snowplow payloads from Enrich
and translates into the acceptable format for Snowplow Enrich as the response. 

The goal of this feature is to keep the integrations closer to the Snowplow pipeline and avoiding duplicate parallel pipelines.

### Wehbook vs. Adapter
Snowplow has a large number of webhooks that let you ingest events from various third-party service providers:
see 

If you would like to sponsor a new webhook integration for use by the Snowplow community, please [check that page](https://github.com/snowplow/snowplow/wiki/Setting-up-a-webhook)

But if you need to ingest events from a source where such a public community-wide integration would be inappropriate or impossible, you can implement your own custom integration via an Enrich Remote Adapter. This directory contains a simplified instance of such an adapter. For example:

* Payload with some data-science model fields involved: This is the case with data science models that we have in the organization. First, we need to feed the payload of our custom events to the data model machines and ask their verdict and generate the rawEvent payload based on the schema. The model response can be slow. Here comes the performance. Nowadays the models are written in Python/R so we cannot even put them here in this project too. Even if we could, many organizations have data science models that are tuned and trained based on their sales data and not suitable for others.
* Proprietary software: Usually old-school legacy softwares that nobody dares to touch them because the original owners left the organization long time ago. They just send out some webhook notifications that we need to add other fields to them before putting them in RawEvent formant. Those softwares do not have public audience and organizations cannot share the details, too. But based on the Apache2 license we should.
* Vendor software: There are cases that we cannot publish logic of the schemas or anything related to the vendors. This is totally out of our control. We just bought them to use them internally. This becomes very sensitive to managements when we want to process financial event data. Some companies are not comfortable to share anything about financial systems.


### Specification

* The POST HTTP request sent to an adapter contains the a Json string with these fields: `contentType`, `queryString`, `headers` and `body` (Escaped JSON). For example:

```json
{"queryString": {"abc": 123, "def": "xyz"}, "headers": ["ABC:DEF"], "body": "{\"group\":[{\"response-time\":\"1s\"}]}" , "contentType": "application/json"}
```

* One request can contain several events. That depends on your business-rule. For example, in the example above, the `group` array could have more items and the example code would generate more that one events.

* The body of the HTTP response is expected to be a JSON with either a string field `error` containing the error message if a problem happened on the remote adapter, or a field `events` which is a list of `Map[String, String]`, each map being placed in the parameters of a raw event in the pipeline. This is the output for the example input above:
```json
{"events":[{"e":"ue","abc":"123","tv":"remote-adapter-example-v1.0.0","ue_pr":"{\"schema\":\"iglu:com.snowplowanalytics.snowplow/unstruct_event/jsonschema/1-0-0\",\"data\":{\"schema\":\"iglu:com.snowplowanalytics.snowplow/remote_adapter_example/jsonschema/1-0-0\",\"data\":{\"response-time\":\"1s\"}}}","p":"srv","def":"xyz"}],"error":null}
```


### Running
When run, this tiny webservice will be reachable at the following uri: http://127.0.0.1:8995/sampleRemoteAdapter

You can then configure Enrich to talk to this app whenever it receives notification content at a given url. Simply create a config file to define the remote adapters you want to enable, and tell Enrich about this config file when you start it up.
 
Here is an example config file which will cause Enrich to call this simple remote adapter whenever content is posted to (http://your-collector-url/com.example/v1) :
```
remoteAdapters:[
    {
        vendor: "com.example", # The vendor used in Collector URI
        version: "v1",  # The version used in Collector URI
        url: "http://127.0.0.1:8995/sampleRemoteAdapter", 
        timeout: 1000 # in milli-seconds
    }
]
```

Add this schema to your Iglu Server, so your events do not go to the Enrich Bad Stream:

```json
{
  "title": "Snowplow Remote Adapter Example Event Self-Describing schema",
  "$schema":"http://iglucentral.com/schemas/com.snowplowanalytics.self-desc/schema/jsonschema/1-0-0#",
  "description": "Schema for Schema for transaction event",
  "self": {
    "vendor": "com.snowplowanalytics.snowplow",
    "name": "remote_adapter_example",
    "format": "jsonschema",
    "version": "1-0-0"
  },
  "type": "object",
  "properties": {
    "sampleField":{
      "title": "Sample Field",
      "description": "Just an example",
      "type": "string",
      "examples": ["12345"]
    }
  },
  "minProperties": 1,
  "additionalProperties": true
}
```


### Testing
You can test it with the Curl command below:
```bash
curl -X POST \
  http://localhost:8995/sampleRemoteAdapter \
  -d '{"queryString":{"testQueryString1":"testValue1","testQueryString2":"testValue2"}, "body": "{ \"group\":[{\"field1\":\"This is a test value\"},{\"field2\":\"This is a test value2\"}]}", "contentType":"text/html"}'
```

Or if integrated with your pipeline, send this payload to your collector:

```bash
curl -X POST \
  http://10.99.25.74:8081/com.example/v1 \
  -H 'Content-Type: application/json' \
  -d '{"group":[{"fieldName":"fieldValue"}]}'
```