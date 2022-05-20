import psycopg2
import yaml
import os
import base64
import datetime
import sys

def main(email:str):
    with open("../../../../../../main/resources/application.yml", "r") as f:
        try:
            parsed_yaml=yaml.safe_load(f)
        except yaml.YAMLError as exc:
            print(exc)
            return
    config = parsed_yaml["spring"]["datasource"]

    conn = psycopg2.connect(host="localhost", dbname=config["url"].split("/")[-1], user=config["username"], password=config["password"])
    cur = conn.cursor()

    # Insert 30 records for 30 consecutive days from now
    current = datetime.datetime.utcnow().replace(tzinfo=datetime.timezone.utc)
    for i in range(30):
        cur.execute(f"insert into booked_slot (id,start_time,end_time,host_email) values (%s, %s, %s, %s);", (i, (current+datetime.timedelta(days=i)).strftime('%Y-%m-%d %H:%M:%S+00'), (current+datetime.timedelta(days=i,hours=1)).strftime('%Y-%m-%d %H:%M:%S+00'), email))

    conn.commit()

    conn.close()
    
if __name__=="__main__":
    main(sys.argv[1])

