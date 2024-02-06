#! /bin/bash

cd ../website-backend || exit
poetry run python app/command.py openapi || exit
mv -f ./openapi.json ../website-frontend/src/openapi-codegen
