.PHONY: start stop restart build clean package

start:
	@./start.sh

stop:
	@./stop.sh

restart: stop start

build:
	@echo "Building backend..."
	@mvn package -pl ciff-app -am -DskipTests -q
	@echo "Building frontend..."
	@cd ciff-web && npm run build
	@echo "Build complete."

clean:
	@echo "Cleaning backend..."
	@mvn clean -q
	@echo "Cleaning frontend..."
	@cd ciff-web && rm -rf dist
	@echo "Clean complete."

package: build
	@rm -rf .package && mkdir -p .package/ciff
	@cp ciff-app/target/ciff-app-1.0.0-SNAPSHOT.jar .package/ciff/
	@cp -r ciff-web/dist .package/ciff/ciff-web-dist
	@cp -r deploy .package/ciff/ 2>/dev/null || true
	@tar -czf ciff.tar.gz -C .package ciff
	@rm -rf .package
	@echo "Package created: ciff.tar.gz"
