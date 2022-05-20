#!/bin/bash
EMAIL=${1:-dummy@gmail.com}
DAY=${2:-2022-05-30}
curl -X GET "http://localhost:8080/api/get_booked?email=$EMAIL&startDate=$DAY"

echo ""
