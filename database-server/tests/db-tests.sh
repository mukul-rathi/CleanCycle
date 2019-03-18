#!/usr/bin/env bash
./wait-for-it.sh endpoint:5000 --timeout=30 -- pytest --cov=app --cov-report html --cov-report term && 
yapf -rd /usr/src/app/  && echo "Code is YAPF formatted." &&
echo "PyLint: " &&
pylint --rcfile=/usr/src/app/.pylintrc /usr/src/app/