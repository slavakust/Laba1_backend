#!/usr/bin/env bash

BASE_URL="http://localhost:8080"

curl -i -s "${BASE_URL}/ping"
curl -i -s -X POST "${BASE_URL}/ping"

curl -i -s "${BASE_URL}/cars"
curl -i -s "${BASE_URL}/cars?type=PREMIUM"
curl -i -s "${BASE_URL}/cars?status=AVAILABLE"
curl -i -s "${BASE_URL}/cars?minYear=2023"
curl -i -s "${BASE_URL}/cars?model=Panamera"
curl -i -s "${BASE_URL}/cars?type=COMFORT"
curl -i -s "${BASE_URL}/cars?minYear=invalid"
curl -i -s "${BASE_URL}/cars/car-prem-1"
curl -i -s "${BASE_URL}/cars/car-not-found"

curl -i -s -X POST "${BASE_URL}/cars" -H "Content-Type: application/json" -d '{"id":"car-test-1","brand":"Skoda","model":"Octavia","plateNumber":"E555EE","productionYear":2022,"mileage":10000.0,"type":"ECONOMY"}'
curl -i -s -X POST "${BASE_URL}/cars" -H "Content-Type: application/json" -d '{"brand":"Skoda","model":"Octavia","plateNumber":"E555EE","productionYear":2022,"mileage":10000.0,"type":"ECONOMY"}'
curl -i -s -X POST "${BASE_URL}/cars" -H "Content-Type: application/json" -d '{"id":"car-test-1","brand":"Skoda","model":"Fabia","plateNumber":"X999XX","productionYear":2022,"mileage":10000.0,"type":"ECONOMY"}'
curl -i -s -X POST "${BASE_URL}/cars" -H "Content-Type: application/json" -d '{"brand":"","model":"Fabia","plateNumber":"X999XX","productionYear":2022,"mileage":10000.0,"type":"ECONOMY"}'
curl -i -s -X POST "${BASE_URL}/cars" -H "Content-Type: application/json" -d '{"brand":"Skoda","model":"Fabia","plateNumber":"X999XX","productionYear":2022,"mileage":-50.0,"type":"ECONOMY"}'
curl -i -s -X POST "${BASE_URL}/cars" -H "Content-Type: application/json" -d '{"invalid_json'

curl -i -s -X PUT "${BASE_URL}/cars/car-test-1" -H "Content-Type: application/json" -d '{"model":"Octavia RS","mileage":12500.0}'
curl -i -s -X PUT "${BASE_URL}/cars/car-not-found" -H "Content-Type: application/json" -d '{"model":"Test"}'

curl -i -s "${BASE_URL}/drivers"
curl -i -s "${BASE_URL}/drivers?minAge=25"
curl -i -s "${BASE_URL}/drivers?hasCar=false"
curl -i -s "${BASE_URL}/drivers?hasDeposit=true"
curl -i -s "${BASE_URL}/drivers?minAge=abc"
curl -i -s "${BASE_URL}/drivers/drv-2"
curl -i -s "${BASE_URL}/drivers/drv-ghost"

curl -i -s -X POST "${BASE_URL}/drivers" -H "Content-Type: application/json" -d '{"id":"drv-test-1","name":"Charlie Brown","email":"charlie@test.com","age":26,"experience":4,"hasDeposit":true}'
curl -i -s -X POST "${BASE_URL}/drivers" -H "Content-Type: application/json" -d '{"name":"Duplicate","email":"charlie@test.com","age":26,"experience":4,"hasDeposit":true}'
curl -i -s -X POST "${BASE_URL}/drivers" -H "Content-Type: application/json" -d '{"name":"Bad Email","email":"not-an-email","age":26,"experience":4,"hasDeposit":true}'
curl -i -s -X POST "${BASE_URL}/drivers" -H "Content-Type: application/json" -d '{"name":"Young","email":"young@test.com","age":17,"experience":0,"hasDeposit":false}'
curl -i -s -X POST "${BASE_URL}/drivers" -H "Content-Type: application/json" -d '{"name":"Time Traveler","email":"tt@test.com","age":19,"experience":8,"hasDeposit":false}'

curl -i -s -X PUT "${BASE_URL}/drivers/drv-test-1" -H "Content-Type: application/json" -d '{"experience":5,"hasDeposit":false}'
curl -i -s -X PUT "${BASE_URL}/drivers/drv-ghost" -H "Content-Type: application/json" -d '{"name":"Ghost"}'

curl -i -s -X POST "${BASE_URL}/cars/car-prem-1/rent" -H "Content-Type: application/json" -d '{"driverId":"drv-1"}'
curl -i -s -X POST "${BASE_URL}/cars/car-prem-1/rent" -H "Content-Type: application/json" -d '{"driverId":"drv-test-1"}'
curl -i -s -X POST "${BASE_URL}/cars/car-prem-1/rent" -H "Content-Type: application/json" -d '{"driverId":"drv-2"}'
curl -i -s -X POST "${BASE_URL}/cars/car-prem-1/rent" -H "Content-Type: application/json" -d '{"driverId":"drv-test-1"}'
curl -i -s -X POST "${BASE_URL}/cars/car-eco-1/rent" -H "Content-Type: application/json" -d '{"driverId":"drv-2"}'

curl -i -s -X DELETE "${BASE_URL}/cars/car-prem-1"
curl -i -s -X DELETE "${BASE_URL}/drivers/drv-2"
curl -i -s -X PUT "${BASE_URL}/cars/car-prem-1" -H "Content-Type: application/json" -d '{"model":"New Model"}'

curl -i -s -X POST "${BASE_URL}/cars/car-eco-1/return" -H "Content-Type: application/json" -d '{"hours":2,"distance":40.0}'
curl -i -s -X POST "${BASE_URL}/cars/car-prem-1/return" -H "Content-Type: application/json" -d '{"hours":2,"distance":-5.0}'
curl -i -s -X POST "${BASE_URL}/cars/car-prem-1/return" -H "Content-Type: application/json" -d '{"hours":3,"distance":120.0}'

curl -i -s "${BASE_URL}/cars/car-prem-1"
curl -i -s "${BASE_URL}/drivers/drv-2"

curl -i -s -X DELETE "${BASE_URL}/cars/car-test-1"
curl -i -s "${BASE_URL}/cars/car-test-1"

curl -i -s -X DELETE "${BASE_URL}/drivers/drv-test-1"
curl -i -s "${BASE_URL}/drivers/drv-test-1"

curl -i -s -X PATCH "${BASE_URL}/cars"
curl -i -s -X POST "${BASE_URL}/cars/car-eco-1"
curl -i -s -X POST "${BASE_URL}/cars/car-eco-1/unknown"
