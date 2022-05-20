#!/bin/bash
EMAIL=${1:-dummy@gmail.com}
TOKEN=`python3 getToken.py`
curl -X POST http://localhost:8080/api/backend/add_schedule_type -H "Authorization: Basic $TOKEN" -H "Content-Type: application/json" -d '{"email":"'$EMAIL'","duration":30,"description":"My schedule type."}'

echo ""
