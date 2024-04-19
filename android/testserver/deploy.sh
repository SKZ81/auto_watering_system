#!/bin/sh

set -x

sudo mkdir -p /srv/http/db/
sudo mkdir -p /srv/http/cgi-bin/
sudo mkdir -p /srv/http/images/varieties/
sudo cp etc/lighttpd/lighttpd.conf /etc/lighttpd/
sudo cp database.db /srv/http/db/
sudo cp Scripts/* /srv/http/cgi-bin/
sudo cp images/varieties/*.png /srv/http/images/varieties/

sudo systemctl restart lighttpd.service
