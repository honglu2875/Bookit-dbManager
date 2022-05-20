#!/bin/bash
EMAIL=${1:-dummy@gmail.com}
TOKEN=`python3 getToken.py`
echo -e "----------"
echo -e "Passing the wrong refresh token:\n----------"

curl -X POST http://localhost:8080/api/backend/update_refresh_token -H "Authorization: Basic $TOKEN" -H "Content-Type: application/json" -d '{"email":"'$EMAIL'","refreshToken":"'test'"}'

echo -e "\n----------"

if [[ -z "${RT}" ]]; then
	echo "Need to set environment variable RT=[your refresh token] in order to proceed to the testingof the refresh token."
else
	echo -e "Passing the given refresh token ($RT):\n----------"
	curl -X POST http://localhost:8080/api/backend/update_refresh_token -H "Authorization: Basic $TOKEN" -H "Content-Type: application/json" -d '{"email":"'$EMAIL'","refreshToken":"'$RT'"}'
fi

echo ""
