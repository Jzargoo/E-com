**User Endpoints:**

1. **Product Service**
    - `GET /products/{id}`
    - `POST /products`
    - `PUT /products/{id}`
    - `GET /categories`
    - `POST /categories`
    - `DELETE /products/{id}`
2. **Catalog Service** (service with 2 reading models: Elasticsearch (Id + name) for string full-search + MongoDB (id + category + rate))
    - `GET /catalog/daily?count=*` – Endpoint for recommended products, e.g., select a random category every day like “The Day of Electronics” and return products from this category.
    - `GET /catalog?q={query}&filters={filterDto}`
3. **Cart Service**
    - `POST /cart`
    - `PUT /add/product/{cartId}`
    - `GET /cart/{cartId}`
    - `DELETE /cart/{cartId}`
    - `DELETE /cart/{productId}/{cartId}`
4. **Pricing Service**
    - `GET /pricing/{productId}`
    - `POST /pricing/apply-promo`
    - `POST /pricing/update` (internal)
5. **Inventory Service**
    - `GET /inventory/{productId}`
6. **Order Service**
    - `GET /orders/{id}`
    - `GET /orders/user/{userId}`
    - `PUT /orders/{id}/status`
7. **User Service**
    - `GET /users/{id}` – get user information
    - `PUT /users` – update information not related to Keycloak, e.g., icon
8. **Notification Service**
    - `GET /notifications/user/{userId}` – get user notifications
9. **Delivery Service**
    - `GET /delivery/{orderId}` – get delivery status
    - `POST /delivery/update` – update delivery status
10. **Reaction Service**
    - `POST /reactions` – add review and rating
    - `GET /reactions/product/{productId}` – get product reviews
    - `GET /reactions/user/{userId}` – get user reviews
    - `PUT /reactions/{id}` – update review/rating
11. **Statistical / Analytics Service**
    - `GET /analytics/product/{productId}` – product statistics (views, purchases, add-to-cart) only for products by shops
    - `GET /analytics/user/{userId}` – user behavior
12. **Shop Service**
- `GET /shops/{shopId}` – get shop information
- `PUT /shops/{shopId}` – update shop information
- `DELETE /shops/{shopId}` – delete shop and its products

Asynch paths (connections between kafka handlers):

Place order (orchestration):

Cart service → product service (check existing, in-variants) → inventory service (check availability) → payment service → order service → {notification service; statistical analysis}  → delivery service → notification service

Create a product (orchestration):

product service + grpc (media) → media service(approve) → inventory service → shop service → notification service

Update a product (choreography):

product service → {catalog service; pricing service}

availability of a product (event):

inventory service → product service.
product service → catalog service

Add to catalog(event):

reaction service → catalog service.

How media service works:

The Media Service utilizes a high-throughput, low-latency local object storage (such as a local MinIO instance) 
as its Primary Ingestion Buffer. When a client uploads a file, the Media Service immediately generates a permanent,
unique identifier (UUID) and writes the raw bytes directly to this primary storage. 
Concurrently, it publishes an initial event to the ingestion Kafka topic: “File X is available in Primary Storage”. 
This decoupled design ensures sub-millisecond API responses, hiding cloud network latency from the end-user.


Downstream persistent cloud storages ( local long-term archives) run autonomous worker threads. 
Each storage type operates within its own independent Kafka Consumer Group, 
allowing them to track their read-offsets completely isolated from one another.

The fastest worker to process the ingestion event downloads the asset from the Primary Storage and 
persists it to its respective cloud bucket. 
Immediately following a successful write, this fast worker invokes a deletion command on the 
Primary Storage to keep the ingestion buffer compact and performant. 
Finally, it broadcasts a "gossip" event to the P2P Replication topic: “Storage [Azure] now hosts File X”


Slower, rate-limited, or recovering workers will eventually process the ingestion event, 
attempt to fetch the file from the Primary Storage, and encounter an expected 404 Not Found error 
due to the fast worker's cleanup. This is a non-breaking, standard operational routine.

Instead of throwing a critical exception, the worker emits a warning log and shifts its focus 
to the P2P Replication topic. By reading the gossip log, it discovers alternative peer sources 
(e.g., “Storage [Azure] hosts File X”). The worker then executes an Idempotency Check against 
its own local registry: if the file is missing, it bypasses the deleted primary storage entirely 
and replicates the bytes directly from the active peer node.