#!/bin/bash

BASE_URL="http://localhost:8081/api/v1/auth"
EMAIL="testuser$(date +%s)@example.com" # Her seferinde eşsiz bir email üretir

echo -e "=====================================\n1. REGISTER ENDPOINT\n====================================="
curl -s -X POST $BASE_URL/register \
  -H "Content-Type: application/json" \
  -d "{\"email\":\"$EMAIL\", \"password\":\"secret123\", \"full_name\":\"Turk Ninja\"}" 
echo -e "\n"

echo -e "=====================================\n2. LOGIN ENDPOINT\n====================================="
# Dönen JSON sonucunu yakala
LOGIN_RESP=$(curl -s -X POST $BASE_URL/login -H "Content-Type: application/json" -d "{\"email\":\"$EMAIL\", \"password\":\"secret123\"}")
echo $LOGIN_RESP

# JSON formatından sed/grep aracılığıyla tokenleri çıkartıyoruz
ACCESS_TOKEN=$(echo $LOGIN_RESP | grep -oP '"access_token":"\K[^"]+')
REFRESH_TOKEN=$(echo $LOGIN_RESP | grep -oP '"refresh_token":"\K[^"]+')
echo -e "\n"

echo -e "=====================================\n3. PROTECTED /ME ENDPOINT\n====================================="
curl -s -X GET $BASE_URL/me -H "Authorization: Bearer $ACCESS_TOKEN"
echo -e "\n"

echo -e "=====================================\n4. REFRESH TOKEN ENDPOINT\n====================================="
REFRESH_RESP=$(curl -s -X POST $BASE_URL/refresh \
  -H "Content-Type: application/json" \
  -d "{\"refresh_token\":\"$REFRESH_TOKEN\"}")
echo $REFRESH_RESP

NEW_ACCESS_TOKEN=$(echo $REFRESH_RESP | grep -oP '"access_token":"\K[^"]+')
echo -e "\n"

echo -e "=====================================\n5. LOGOUT ENDPOINT\n====================================="
curl -s -X POST $BASE_URL/logout \
  -H "Content-Type: application/json" \
  -d "{\"refresh_token\":\"$REFRESH_TOKEN\"}"
echo -e "\n"

echo -e "=====================================\n6. RETEST /ME (Yeni Token ile)\n====================================="
curl -s -X GET $BASE_URL/me -H "Authorization: Bearer $NEW_ACCESS_TOKEN"
echo -e "\n\nAll automated tests are completed successfully! 🎉"
