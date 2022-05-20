#!/bin/bash
EMAIL=${1:-dummy@gmail.com}
TOKEN=`python3 getToken.py`
echo -e "----------\nSending add_event request with wrong email:\n"

curl -X POST http://localhost:8080/api/backend/add_event -H "Authorization: Basic $TOKEN" -H "Content-Type: application/json" -d '{"hostEmail":"'$EMAIL'a","startTime":"2022-05-21T20:53:33+00:00","endTime":"2022-05-21T21:53:33+00:00","attendees":[{"email":"attendee@email.com"}]}'

echo -e "\n----------\nSending add_event request with wrong time (endTime<startTime):\n"
curl -X POST http://localhost:8080/api/backend/add_event -H "Authorization: Basic $TOKEN" -H "Content-Type: application/json" -d '{"hostEmail":"'$EMAIL'","startTime":"2022-05-21T20:53:33+00:00","endTime":"2022-05-20T21:53:33+00:00","attendees":[{"email":"attendee@email.com"}]}'

echo -e "\n----------\nSending the correct add_event request:\n"
curl -X POST http://localhost:8080/api/backend/add_event -H "Authorization: Basic $TOKEN" -H "Content-Type: application/json" -d '{"hostEmail":"'$EMAIL'","startTime":"2022-05-21T20:53:33+00:00","endTime":"2022-05-21T21:53:33+00:00","attendees":[{"email":"attendee@email.com"}]}'
