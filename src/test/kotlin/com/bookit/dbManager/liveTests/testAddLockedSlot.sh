#!/bin/bash
EMAIL=${1:-dummy@gmail.com}
TOKEN=`python3 getToken.py`

for i in {1..5}
do
CONTENT=`python3 getDummySlots.py $EMAIL $i`
curl -X POST http://localhost:8080/api/lock_slot -H "Content-Type: application/json" -d "$CONTENT"
done

echo ""
