#! /bin/bash

PORT=9000

BASE_URL="http://localhost:$PORT/github"

ORG="parse-community"

# Number of stats to get
N="3"

printf "Adding a new org:\n"
curl -X PUT "$BASE_URL/orgs/$ORG"

printf "\nCalling add org:\n"
curl -X PUT "$BASE_URL/orgs/$ORG"

printf "\nCalling list of orgs:\n"
curl "$BASE_URL/orgs"

# Wait for the org to get updated
sleep 10

printf "\nGetting a list of members:\n"
curl "$BASE_URL/orgs/$ORG/members"

printf "\nGetting a list of repos:\n"
curl "$BASE_URL/orgs/$ORG/repos"

printf "\nGetting top $N forks:\n"
curl "$BASE_URL/views/$ORG/top/$N/forks"

printf "\nGetting top $N last_updated:\n"
curl "$BASE_URL/views/$ORG/top/$N/last_updated"

printf "\nGetting top $N open_issues:\n"
curl "$BASE_URL/views/$ORG/top/$N/open_issues"

printf "\nGetting top $N stars:\n"
curl "$BASE_URL/views/$ORG/top/$N/stars"

printf "\nGetting top $N watchers:\n"
curl "$BASE_URL/views/$ORG/top/$N/watchers"

#printf "\nDelete $ORG:\n"
#curl -X DELETE "$BASE_URL/orgs/$ORG"

printf "\nDone\n"