#!/bin/bash
EMAIL=${1:-dummy@gmail.com}
TOKEN=`python3 getToken.py`
curl -X POST http://localhost:8080/api/backend/add_user -H "Authorization: Basic $TOKEN" -H "Content-Type: application/json" -d '{"email":"'$EMAIL'","refreshToken":"bbb","name":"John Doe"}'

echo ""
