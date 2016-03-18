#!/usr/bin/env bash

set -eu

cd "$(dirname "$0")"

wget -c https://scans.io/data/rapid7/sonar.ssl/20160104/20160104_certs.gz >&2

while IFS= read line
do
    echo "${line#*,}"  |
        sed -e 's/^/-----BEGIN CERTIFICATE-----\n/' -e 's/$/\n-----END CERTIFICATE-----/' |
        fold -w 64 |
        openssl x509 -inform pem -noout -modulus |
        cut -d= -f2-
done < <(zcat 20160104_certs.gz) | grep -v Wrong | sort -u
