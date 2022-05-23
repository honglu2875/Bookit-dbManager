#!/bin/bash
EMAIL=${1:-dummy@gmail.com}
TOKEN=`python3 getToken.py`
curl -X GET "http://localhost:8080/api/backend/get_user?email=$EMAIL" -H "Authorization: Basic $TOKEN" -H "Content-Type: application/json" 

echo ""
