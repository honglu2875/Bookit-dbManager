#!/bin/bash
EMAIL=${1:-dummy@gmail.com}
DAY=${2:-2022-05-30}
TOKEN=`python3 getToken.py`
curl -X GET "http://localhost:8080/api/backend/get_history" -H "Authorization: Basic $TOKEN" -H "Content-Type: application/json" -d '{"email":"'$EMAIL'"}'

echo ""
