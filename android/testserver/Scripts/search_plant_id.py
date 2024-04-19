#!/usr/bin/env python3

import os
import sys
import sqlite3
import cgitb
import cgi
import json
import urllib

def do_html(column_names, data):
    print("""
Content-Type: text/html

<!DOCTYPE html>
<html lang="en">
<head>
<meta charset="UTF-8">
<meta name="viewport" content="width=device-width, initial-scale=1.0">
<title>Simple HTML Table with Images</title>
<style>
table {
    width: 100%;
    border-collapse: collapse;
}
th, td {
    padding: 8px;
    text-align: left;
    border-bottom: 1px solid #ddd;
}
th {
    background-color: #f2f2f2;
}
img {
    width: 50px;
    height: 50px;
}
</style>
</head>
<body>

<h2>Simple HTML Table with Images</h2>

<table>
    """)

    print("  <tr>\n")
    for col in column_names:
        print("    <th>%s</th>\n"%col)
    print("  </tr>\n")
    # Prepare JSON object
    # json_data = []
    for row in data:
        # row_dict = {}
        # for i, value in enumerate(row):
        #     row_dict[column_names[i]] = value
        # json_data.append(row_dict)
        print("  <tr>\n")
        for i,value in enumerate(row):
            if i == 3:
                print("    <th><img src='/%s'></th>\n"%value)
            else:
                print("    <th>%s</th>\n"%value)
        print("  </tr>\n")

    # Dump JSON plaintext
    # json_text = json.dumps(json_data)
    # print(json_text)

    print("""
</table>

</body>
</html>
    """)

def do_json(column_names, data):
    # Prepare JSON object
    row_dict = {}
    for i, value in enumerate(data):
        row_dict[column_names[i]] = value
    print("Content-Type: application/json\n")
    print(json.dumps(row_dict))


cgitb.enable() #disable in prod !!!

# Get the request method (GET or POST)
# request_method = os.environ.get('REQUEST_METHOD', '')
# content_length = int(os.environ.get('CONTENT_LENGTH', 0))
# request_body = sys.stdin.read(content_length)
query_string = os.environ.get('QUERY_STRING', '')
params = urllib.parse.parse_qs(query_string)
uuid = params.get('uuid', [''])[0]

conn = sqlite3.connect('/srv/http/db/database.db')
cursor = conn.cursor()
SQLrequest = f"""
SELECT PLANTS.*, VARIETIES.NAME AS varietyName, VARIETIES.PHOTO_URL AS photoUrl
FROM PLANTS
LEFT JOIN VARIETIES ON PLANTS.VARIETY = VARIETIES.ID
WHERE PLANTS.UUID = '{uuid}'
"""

cursor.execute(SQLrequest)
data = cursor.fetchall()
conn.close()

if len(data) == 0:
    print("Status: 404 Not Found")
    print("Content-Type: application/json\n")
    print("{'reason': 'UUID not found', 'request':'" + SQLrequest + "'}")
elif len(data) > 1:
    print("Status: 500 Internal Server Error")
    print("Content-Type: application/json\n")
    print("{'reason': 'multiple entries found for UUID', 'request':'" + SQLrequest + "'}")
else:
    column_names = [desc[0] for desc in cursor.description]
    do_json(column_names, data[0])
    #do_html(column_names, data)



