#!/bin/bash
EMAIL=${1:-dummy@gmail.com}
TOKEN=`python3 getToken.py`
STOKEN=`python3 getScheduleToken.py $EMAIL`
curl -X DELETE "http://localhost:8080/api/backend/$STOKEN" -H "Authorization: Basic $TOKEN" -H "Content-Type: application/json" 

echo ""
