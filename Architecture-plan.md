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

# Media service
The Media Service is responsible for the physical storage and retrieval of media files.
It does not manage product-related metadata, asset versions, or relationships between products and media.
Its primary responsibility is to reliably transfer and persist file data between the application and the configured storage backends.

The Media Service accepts files as a client-side gRPC stream.
Each message contains a portion of the file being uploaded.

When a new upload is initiated, the Media Service first retrieves the metadata required to create the storage session.
It then creates an internal upload session responsible for maintaining the state of the current file transfer.

The gRPC server creates a stream observer associated with this session.
The observer receives incoming file chunks and delegates their processing to the upload session.

The upload session is responsible for the actual file-processing logic, including:

receiving file chunks;
writing chunks to the temporary storage or buffer;
maintaining the state of the current upload;
determining when the complete file has been received;
transferring the completed file to the configured storage backend;
handling failures during the upload;
cleaning up incomplete or failed uploads.

The gRPC server itself does not contain the file-storage logic.
Its responsibility is limited to establishing the upload session, connecting the incoming gRPC stream to the session, and reporting the final result of the operation.

Once the complete file has been successfully persisted, the server completes the gRPC stream.
onCompleted() therefore indicates that the upload operation has been successfully completed.

If an error occurs at any stage of the upload, the session reports the error to the observer.
The observer terminates the gRPC stream with onError().

Consequently, the upload operation has two terminal states:

onCompleted() → the file was successfully persisted
onError(...)  → the file was not successfully persisted

No additional success value is required from the upload operation because successful completion of the RPC itself serves as the confirmation that the file has been stored.

Storage

The Media Service abstracts the underlying storage implementation from the rest of the application.

A file may initially be written to a temporary buffer before being persisted to the final storage backend.
For large files, the service may use multipart storage operations, allowing the file to be persisted in multiple parts without requiring the entire file to be held in memory.

The Media Service is responsible for maintaining consistency between the temporary upload state and the final storage state.

An upload is considered successful only after the service has completed the required storage operation.
If persistence fails, the incomplete file must not be reported as successfully stored.

The Media Service may use different storage implementations without exposing their internal details through the gRPC API.

Upload session

Each upload is represented internally by an independent upload session.

The session isolates the state of one file transfer from other concurrent uploads.
It owns the temporary state required while receiving the file and coordinates the transition from an incomplete upload to a successfully persisted file.

The gRPC observer does not contain the upload business logic itself.
Instead, it acts as an adapter between the gRPC streaming API and the upload session.

Conceptually, the data flow is:

gRPC client
│
│ FileChunk
▼
StreamObserver
│
▼
UploadSession
│
├── temporary buffer
│
└── storage backend

This separation prevents the gRPC layer from becoming coupled to the details of file persistence.

Error handling

Errors occurring during the upload are propagated through the gRPC error channel.

The upload session can notify the observer about an error without requiring the session to depend directly on the surrounding gRPC server implementation.

The observer therefore acts as a small boundary between the storage logic and the transport layer.

If the upload cannot be completed, the observer terminates the RPC with onError().
The caller can then decide how to handle the failure, such as retrying the operation or using its own fallback mechanism.

Completing an upload

Receiving the final chunk does not by itself mean that the upload was successful.

After the final chunk has been received, the upload session must complete the required persistence operations.
Only after these operations succeed is the gRPC request completed.

The lifecycle is therefore:

Receive chunks
│
▼
Temporary storage
│
▼
Complete storage operation
│
├── failure ──────→ onError(...)
│
└── success ──────→ onCompleted()

This guarantees that onCompleted() is not merely an indication that the network stream ended, but an indication that the Media Service has successfully completed its storage operation.

Retrieving files

The Media Service also provides operations for retrieving stored files.

The caller provides the identifier required to locate the file.
The Media Service resolves this identifier through its storage abstraction and streams the file back to the caller.

The file is returned as a stream rather than requiring the complete file to be loaded into memory.

Conceptually:

Storage backend
│
▼
Media Service
│
│ file chunks
▼
gRPC client

The Media Service does not interpret the business meaning of the file.
For example, it does not determine whether a file is an avatar, product image, product video, or another type of product asset.
Such relationships belong to the Product Assets Service.

Responsibility boundaries

The responsibilities of the two services are intentionally separated.

The Product Assets Service manages the business meaning of media:

association between products and assets;
asset metadata;
avatar selection;
asset versions;
asset lifecycle;
fallback records.

The Media Service manages the physical files:

accepting file streams;
persisting file data;
retrieving file data;
managing temporary upload state;
coordinating storage backends;
guaranteeing successful or failed persistence.

The Media Service therefore remains independent of the product domain and can be used for any type of media without knowing why a particular file exists.

## How **media service** works: 

### Description:
The Media Service utilizes a high-throughput, low-latency local object storage (such as a local MinIO instance)
as its Primary Ingestion Buffer. When a client uploads a file, the Media Service immediately generates a permanent,
uri for the file and writes the raw bytes directly to this primary storage. 
Concurrently, it publishes an initial event to the ingestion Kafka topic: “File X is available in Primary Storage”. 

### Downstream persistent storages
Downstream persistent storages (local long-term archives or secondary storages) run 
as virtualProcessors because they consume events from a bus. 
Each storage type operates within its own independent Kafka Consumer Group, 
allowing them to track their read-offsets completely isolated from one another.

### Ingestion event processing
The fastest worker to process the ingestion event downloads the asset from the Primary Storage and 
persists it to its respective cloud bucket. 
Immediately following a successful write, this fast worker invokes a deletion command on the 
Primary Storage to keep the ingestion buffer compact and performant. 
Finally, it broadcasts a "gossip" event to the P2P Replication topic: “Storage [NATIVE_DISK] now hosts File X”

Slower, rate-limited, or recovering workers will eventually process the ingestion event, 
attempt to fetch the file from the Primary Storage, and encounter an expected 404 Not Found error 
due to the fast worker's cleanup. This is a non-breaking, standard operational routine.

### Replication event processing
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