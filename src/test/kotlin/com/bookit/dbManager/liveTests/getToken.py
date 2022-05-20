import psycopg2
import yaml
import os
import base64

def main():
    with open("../../../../../../main/resources/application.yml", "r") as f:
        try:
            parsed_yaml=yaml.safe_load(f)
        except yaml.YAMLError as exc:
            print(exc)
            return
    config = parsed_yaml["spring"]["datasource"]

    conn = psycopg2.connect(host="localhost", dbname=config["url"].split("/")[-1], user=config["username"], password=config["password"])
    cur = conn.cursor()

    cur.execute("select * from api_token;")
    print(base64.b64encode(
        bytearray(str.encode(cur.fetchall()[0][2]))
        ).decode())

    conn.close()
    
if __name__=="__main__":
    main()

