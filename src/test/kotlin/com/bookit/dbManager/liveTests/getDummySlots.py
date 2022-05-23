import psycopg2
import yaml
import os
import base64
import datetime
import sys

def main(email:str, i:int):
    # Insert 30 records for 30 consecutive days from now
    current = datetime.datetime.utcnow().replace(tzinfo=datetime.timezone.utc)
    msg = dict()
    msg["hostEmail"]=email
    msg["startTime"]=(current+datetime.timedelta(days=i)).strftime('%Y-%m-%dT%H:%M:%S+00')
    msg["endTime"]=(current+datetime.timedelta(days=i,hours=1)).strftime('%Y-%m-%dT%H:%M:%S+00')
    msg["expiration"]=10

    print(str(msg).replace("'","\""))
if __name__=="__main__":
    main(sys.argv[1], int(sys.argv[2]))

