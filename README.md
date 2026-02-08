# Jobify API

High-performance Job Search and Management API built with **Java 25** and **Spring Boot**.

## 🚀 Features

-   **Advanced Search**:
    -   Full-text search on description literals using **PostgreSQL GIN Index**.
    -   **Infinite Scroll** (Slice) architecture for sub-second response times on large datasets.
    -   Case-insensitive filtering by City, Region, Country, Remote status, and more.
    -   Supports `ILIKE` and `OR` logic for tag-based searching (e.g., "Java", "Golang").
-   **Optimized Performance**:
    -   **L1/L2 Caching**: Local Caffeine cache + Distributed Redis cache.
    -   **Database Tuning**: Custom Expression Indexes for case-insensitive search.
    -   **N+1 Problem Solver**: Optimized Hibernate fetching strategies.
-   **Location Intelligence**: Standardized City, Region, and Country management via `GeoController`.
-   **Observability**: JSON-formatted logging compatible with Google Cloud Logging / Logstash.

## 🛠 Tech Stack

-   **Language:** Java 25
-   **Framework:** Spring Boot 4.x
-   **Database:** PostgreSQL (Supabase recommended)
-   **Cache:** Redis & Caffeine
-   **Documentation:** OpenAPI / Swagger UI
-   **Build Tool:** Maven

## 📦 Prerequisites

-   Java 25 SDK
-   Maven 3.8+
-   PostgreSQL 14+
-   Redis (Optional, fallback to local cache if configured)

## 🏃‍♂️ Getting Started

### 1. Clone the repository
```bash
git clone https://github.com/Arghya721/jobify-api.git
cd jobify-api
```

### 2. Configure Environment
Update `src/main/resources/application.properties` or set environment variables:

```properties
spring.datasource.url=jdbc:postgresql://<HOST>:<PORT>/postgres
spring.datasource.username=<USER>
spring.datasource.password=<PASSWORD>

spring.data.redis.host=localhost
spring.data.redis.port=6379
```

### 3. Database Optimization (CRITICAL)
For the search to work efficiently (sub-second), you **MUST** run the following SQL commands on your PostgreSQL database:

```sql
-- Enable Trigram Extension for partial matching
CREATE EXTENSION IF NOT EXISTS pg_trgm;

-- 1. Create Expression Index for Case-Insensitive Description Search (Huge Performance Boost)
CREATE INDEX idx_job_details_lower_desc_trgm 
ON job_details USING gin (lower(raw_description) gin_trgm_ops);

-- 2. Create Index for Sorting by Posted Date
CREATE INDEX idx_job_details_posted_at 
ON job_details(job_posted_at DESC);
```

### 4. Run the Application
```bash
mvn spring-boot:run
```

The API will start on port `8080` (default).

## 📚 API Documentation

Once the application is running, explore the interactive API docs at:

**Swagger UI:** [http://localhost:8080/swagger-ui.html](http://localhost:8080/swagger-ui.html)

### Key Endpoints:
-   `GET /api/v1/jobs`: Search jobs with filters.
    -   `q`: Keyword search (title).
    -   `description_tags`: List of tags to search in description (e.g., `Java,Remote`).
    -   `sort`: `desc` (default) or `asc`.
    -   `page`, `limit`: Pagination controls.
-   `GET /api/v1/jobs/{id}`: Get detailed job view.
