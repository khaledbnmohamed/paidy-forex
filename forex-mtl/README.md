---

# Forex Exchange Rates Service

This project is a Scala-based HTTP API for retrieving foreign exchange (forex) rates

## Overview

The service provides endpoints for retrieving forex exchange rates using data from an external API (such as OneFrame).
It includes components for HTTP routing, rate retrieval logic, caching, and configuration, all built with a functional
programming approach in Scala.

## Getting Started

### Prerequisites

- **Scala** (version used in the project)
- **SBT** - Scala Build Tool
- **Docker** (optional, if ypou want to run the service in a container)

### Installation

1. **Build and Run**:
   ```bash
   sbt compile
   sbt run
   ```

### Running Tests

Run the test suite with:

```bash
sbt test
```

## Project Structure

- `forex.config` - Configuration handling.
- `forex.domain` - Core domain models like `Rate`, `Currency`, `Price`, etc.
- `forex.http` - HTTP routes and protocols.
- `forex.programs` - Business logic for processing rate requests.
- `forex.services` - Contains interpreters for external services, like rate fetching from the OneFrame API.

## Example Usage

### Docker

You can run the service in a Docker container.

```bash
docker-compose up 
```

Or

### Local

Start the server with:

```bash
sbt run
```

Once the server is running, you can make requests to the API. Example:

```bash
curl -X GET "http://localhost:8081/rates?from=USD&to=JPY"
```

Response:

Success:

```json
{
  "from": "USD",
  "to": "JPY",
  "price": 109.5
}
```

#### Error Responses

**Condition:** If token is invalid or missing.

**Code:** `403 Forbidden`

**Content:**

```json
{
  "Missing token"
}
```

**Condition:** If currency is invalid or not in the supported list.

**Code:** `400 Bad Request`

```json
{
  "The system doesn't support this currency yet"
}
```

**Condition:** Other errors

**Code:** 500

```json
{
  "An unexpected error occurred"
}
```

## Technial Decisions

### Assumptions

1. The external service is limited to 1,000 requests per day.
2. The rate provided by the proxy must not be older than 5 minutes.
3. The proxy must be capable of handling at least 10,000 requests per day.

### Design Decisions

1. Using Redis cache to store the rates for 4.5 minutes to make sure we don't hit the limit of 5 minutes
2. [Architecture] Using docker-compose to combine oneframe and redis services to make it easier to run the service.
3.

## Future Improvements

1. **Enhanced Error Handling**: Provide more structured error messages.
2. **Improve system resilliance**: Implement a guard from the 3rd pary API like a circuit breaker for resilient external
   API calls.
3. **Caching Strategies**: Implement caching strategies to reduce load on the OneFrame API and improve response times.
4. **Rate Limiting**: Implement rate limiting to prevent abuse and ensure fair usage of the service.
5. **More Test Coverage**: Increase test coverage with integration tests.
6. **API Documentation**: Generate API documentation using tools like Swagger or OpenAPI.

---


