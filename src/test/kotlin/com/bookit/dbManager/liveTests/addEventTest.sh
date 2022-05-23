#!/bin/bash
EMAIL=${1:-dummy@gmail.com}
OFFSETDAY=${2:'1'}
TOKEN=`python3 getToken.py`
START=`python3 getOffsetTime.py 1 10`
END=`python3 getOffsetTime.py 1 40`
EARLYSTART=`python3 getOffsetTime.py -1 0`
EARLYEND=`python3 getOffsetTime.py -1 30`
echo -e "----------\nSending add_event request with wrong email:\n"

curl -X POST http://localhost:8080/api/add_event -H "Authorization: Basic $TOKEN" -H "Content-Type: application/json" -d '{"hostEmail":"'$EMAIL'a","startTime":"'$START'","endTime":"'$END'","attendees":[{"email":"attendee@email.com"}]}'

echo -e "\n----------\nSending add_event request with wrong time (endTime<startTime):\n"
curl -X POST http://localhost:8080/api/add_event -H "Authorization: Basic $TOKEN" -H "Content-Type: application/json" -d '{"hostEmail":"'$EMAIL'","startTime":"'$END'","endTime":"'$START'","attendees":[{"email":"attendee@email.com"}]}'

echo -e "\n----------\nSending add_event request with time earlier than now:\n"
curl -X POST http://localhost:8080/api/add_event -H "Authorization: Basic $TOKEN" -H "Content-Type: application/json" -d '{"hostEmail":"'$EMAIL'","startTime":"'$EARLYSTART'","endTime":"'$EARLYEND'","attendees":[{"email":"attendee@email.com"}]}'

echo -e "\n----------\nSending the correct add_event request:\n"
curl -X POST http://localhost:8080/api/add_event -H "Authorization: Basic $TOKEN" -H "Content-Type: application/json" -d '{"hostEmail":"'$EMAIL'","startTime":"'$START'","endTime":"'$END'","attendees":[{"email":"attendee@email.com"}]}'
