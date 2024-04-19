#!/usr/bin/env python3

import os
import sys
import sqlite3
import cgitb
import cgi
import json
import urllib


cgitb.enable() #disable in prod !!!

# Get the request method (GET or POST)
request_method = os.environ.get('REQUEST_METHOD', '')
content_length = int(os.environ.get('CONTENT_LENGTH', 0))
request_body = sys.stdin.read(content_length)
# query_string = os.environ.get('QUERY_STRING', '')
# params = urllib.parse.parse_qs(query_string)
# uuid = params.get('uuid', [''])[0]
params = json.loads(request_body);
set_clause = ', '.join([f"{key} = '{value}'" for key, value in params.items() if key != 'ID'])

SQLrequest = f"UPDATE {table_name} SET {set_clause} WHERE ID = '{params['ID']}'"

conn = sqlite3.connect('/srv/http/db/database.db')
cursor = conn.cursor()

try:
    cursor.execute(SQLrequest)
    if cursor.rowcount == 0:
        print("Status: 404 Not Found")
        print("Content-Type: application/json\n")
        print('{"reason": "UUID not found", "SQLquery":"%s"}'%(SQLrequest))
    else:
        conn.commit()
        print("SQL request executed successfully")

except sqlite3.Error as e:
    # Handle other SQLite errors
    print("Status: 500 Internal Server Error")
    print("Content-Type: application/json\n")
    print('{"reason": "%s", "SQLquery":"%s"}'%(str(e), SQLrequest))

finally:
    # Close the database connection
    conn.close()

print("OK")


