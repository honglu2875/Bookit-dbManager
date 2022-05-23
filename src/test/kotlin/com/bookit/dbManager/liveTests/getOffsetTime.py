import datetime
import sys

def main(day:int, minute:int):
    print((datetime.datetime.now()+datetime.timedelta(days=day, minutes=minute)).strftime('%Y-%m-%dT%H:%M:%S+00'))

if __name__=="__main__":
    main(int(sys.argv[1]), int(sys.argv[2]))
