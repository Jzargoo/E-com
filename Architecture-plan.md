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
3. **Product assets service**
   - `GET /product-assets/{productId}` – get product assets (images, posters to videos)
   - `POST /product-assets/{productId}` – upload product assets (image, video)
   - `DELETE /product-assets/{productId}/{assetId}` – delete product asset
   - `GET /product-assets/{productId/ids` - get product assets 
4. **Media Service**
    - `POST /media` – upload media file
    - `GET /media/{id}` – get media file
    - `DELETE /media/{id}` – delete media file
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
13. **Product assets Service**
    - `GET /assets/{productId}` – get ids of product assets
    - `POST /assets/{productId}` – upload product assets (image, video)
    - `DELETE /assets/{productId}/{assetId}` – delete product asset
    - `GET /assets/{assetId}` – get product asset by id

Asynch paths (connections between kafka handlers):

Place an order (orchestration):

   1. Cart service
   2. product service (check existing, in-variants) 
   3. inventory service (check availability) 
   4. payment service
   5. order service 
   6. {notification service; statistical analysis}  
   7. delivery service
   8. notification service

Create a product (orchestration):

   1. product service
   2. inventory service
   3. product assets service
   4. reaction service
   5. shop service 
   6. notification service

Update a product (choreography):

   1. product service 
   2. {catalog service; pricing service}

availability of a product (event):

   1. inventory service
   2. product service.
   3. catalog service

Add to catalog(event):

reaction service → catalog service.

# How **media service** works: 

## Description:
The Media Service utilizes a high-throughput, low-latency local object storage (such as a local MinIO instance)
as its Primary Ingestion Buffer. When a client uploads a file, the Media Service immediately generates a permanent,
uri for the file and writes the raw bytes directly to this primary storage. 
Concurrently, it publishes an initial event to the ingestion Kafka topic: “File X is available in Primary Storage”. 

## Downstream persistent storages
Downstream persistent storages (local long-term archives or secondary storages) run 
as virtualProcessors because they consume events from a bus. 
Each storage type operates within its own independent Kafka Consumer Group, 
allowing them to track their read-offsets completely isolated from one another.

## Ingestion event processing
The fastest worker to process the ingestion event downloads the asset from the Primary Storage and 
persists it to its respective cloud bucket. 
Immediately following a successful write, this fast worker invokes a deletion command on the 
Primary Storage to keep the ingestion buffer compact and performant. 
Finally, it broadcasts a "gossip" event to the P2P Replication topic: “Storage [NATIVE_DISK] now hosts File X”

Slower, rate-limited, or recovering workers will eventually process the ingestion event, 
attempt to fetch the file from the Primary Storage, and encounter an expected 404 Not Found error 
due to the fast worker's cleanup. This is a non-breaking, standard operational routine.

## Replication event processing
Instead of throwing a critical exception, the worker emits a warning log and shifts its focus 
to the Replication topic. By reading the gossip log, it discovers alternative peer sources 
(e.g., “Storage [NATIVE_DISK] hosts File X”). The worker then executes an Idempotency Check against 
its own local registry: if the file is missing, it bypasses the deleted primary storage entirely 
and replicates the bytes directly from the active peer node 
with registry instance to get an implementation of a specific, related service.

Some services can throw exceptions that indicate that a service is not working properly(e.g. out of memory).
Then it has to delete itself from a registry(Graceful shutdown or graceful cuicide) 
Above all virtual processors  will check if their related service is active or not.




# How the **Product Assets Service** works

The **Product Assets Service** is responsible for managing and serving various media assets associated with products,
such as images, videos, and other multimedia content. It works in conjunction with the **Media Service** to provide efficient storage,
retrieval, and management of these assets.

## Uploading assets

The Product Assets Service receives file upload requests through a reactive API. 
First, it limits the file size to a maximum of a configured number of MB.

The file is then streamed to the Media Service for storage. 
The Media Service handles the actual file storage, generates a version for the uploaded asset, and 
returns the generated identifier to the Product Assets Service.

If the upload fails, the Product Assets Service falls back to local disk storage and creates a fallback entry. 
It then throws an exception indicating that the asset has been stored in the fallback storage.

When the circuit breaker is open, the Product Assets Service does not send requests to the Media Service. 
Instead, it stores the asset in the fallback storage and creates a fallback entry.

When the circuit breaker closes again, the Product Assets Service resumes sending requests to the Media Service and 
removes the corresponding fallback entry.

The circuit breaker state is determined by events emitted by the Resilience4j library.

## Retrieving assets

Accessing an asset by its avatar or ID returns a `Flux` of assets.

The Product Assets Service first checks the Media Service for the requested asset.