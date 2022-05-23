#!/bin/bash
EMAIL=${1:-dummy@gmail.com}
TOKEN=`python3 getToken.py`
STOKEN=`python3 getScheduleToken.py $EMAIL`
curl -X POST http://localhost:8080/api/backend/$STOKEN/change_available_time -H "Authorization: Basic $TOKEN" -H "Content-Type: application/json" -d '[{"startMinute":100, "endMinute":1000}]'

echo ""
