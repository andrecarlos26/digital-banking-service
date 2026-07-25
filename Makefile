.PHONY: run test verify package up down logs

run:
	./mvnw spring-boot:run

test:
	./mvnw test

verify:
	./mvnw clean verify

package:
	./mvnw clean package

up:
	docker compose up --build -d

down:
	docker compose down

logs:
	docker compose logs -f api
