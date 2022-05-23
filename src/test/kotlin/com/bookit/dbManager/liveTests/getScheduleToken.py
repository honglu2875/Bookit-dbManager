import psycopg2
import yaml
import os
import base64
import sys

def main(email):
    with open("../../../../../../main/resources/application.yml", "r") as f:
        try:
            parsed_yaml=yaml.safe_load(f)
        except yaml.YAMLError as exc:
            print(exc)
            return
    config = parsed_yaml["spring"]["datasource"]

    conn = psycopg2.connect(host="localhost", dbname=config["url"].split("/")[-1], user=config["username"], password=config["password"])
    cur = conn.cursor()

    cur.execute(f"select * from schedule_type where host_email='{email}';")
    print(cur.fetchall()[-1][5])

    conn.close()
    
if __name__=="__main__":
    main(sys.argv[1])

