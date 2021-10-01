# Repro for OT issues
Issue: [https://github.com/microsoft/ApplicationInsights-Java/issues/1892](https://github.com/microsoft/ApplicationInsights-Java/issues/1892)


Update a `.env` file - start with the `.env.example`

```
APPLICATIONINSIGHTS_CONNECTION_STRING=InstrumentationKey=<get key>
EH_CONNECTION_STRING="<get root key with listen and send>"
STORAGE_KEY=<get storage account key for below storage account>
STORAGE_ACCOUNT=<put storage account name here>
```

Run with VS Code -- launch the `Spring Boot-EhconsumerApplication<ehconsumer>` configuration via F5...

simple post

```
curl --location --request POST 'http://localhost:8080/messages?message=foobar&traceparent=de32118f-3b03-4c72-803d-1a55f561e8a7' \
--header 'Content-Type: text/plain' \
--data-raw 'asdf'
foobar%              
```


https://github.com/mnadeem/boot-opentelemetry-tempo