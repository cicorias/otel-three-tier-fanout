#!/usr/bin/env bash

# set -eox

MESSAGE="Lorem ipsum $(cat /dev/urandom | tr -dc 'a-zA-Z' | fold -w 20 | head -n 1)"

URL="http://localhost:8080/messages"

echo "sending to host $URL"

curl --location --request POST "${URL}" \
--header "Content-Type: application/json" \
--data-raw "{\"message\":\"$MESSAGE\",\"traceparent\":\"NOTGENERATED\" }"