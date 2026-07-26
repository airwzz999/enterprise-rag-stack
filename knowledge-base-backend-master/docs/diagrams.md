# Adding Unified Wrapping of Gateway Response Values and Exceptions - Architecture Diagrams

This document contains the Mermaid syntax for all architecture diagrams in the article, which can be imported into draw.io.

## 1. Technical Architecture Diagram (API Call Flow)

```mermaid
graph TB
    Client[Client Request]

    subgraph Gateway["Spring Cloud Gateway"]
        GlobalLogFilter["GlobalLogFilter<br/>Logs the request"]
        UnifiedResponseFilter["UnifiedResponseFilter<br/>Unifies the response format"]
        GatewayGlobalExceptionHandler["GatewayGlobalExceptionHandler<br/>Gateway exception handling"]
        CorsResponseHeaderFilter["CorsResponseHeaderFilter<br/>Deduplicates CORS headers"]

        GlobalLogFilter --> UnifiedResponseFilter
        UnifiedResponseFilter --> GatewayGlobalExceptionHandler
        CorsResponseHeaderFilter --> UnifiedResponseFilter
    end

    subgraph BackendService["Backend Microservices"]
        GlobalExceptionHandler["GlobalExceptionHandler<br/>Unified exception handling"]
    end

    Client -->|Request| GlobalLogFilter
    GlobalLogFilter -->|Forward| BackendService
    BackendService -->|Response| UnifiedResponseFilter
    UnifiedResponseFilter --> CorsResponseHeaderFilter
    CorsResponseHeaderFilter -->|Response| Client

    style Gateway fill:#e1f5ff
    style BackendService fill:#fff4e6
    style Client fill:#f0f0f0
```

## 2. Full Call Chain Diagram

```mermaid
graph TD
    Client[Client Request]

    subgraph Gateway["Spring Cloud Gateway"]
        GlobalLogFilter1["GlobalLogFilter<br/>Order: -100<br/>Logs the request"]
        Route["Route matching<br/>Forwards to the backend service"]
        UnifiedResponseFilter["UnifiedResponseFilter<br/>Order: -2<br/>Unifies the response format"]
        CorsResponseHeaderFilter["CorsResponseHeaderFilter<br/>Deduplicates CORS headers"]
        GlobalLogFilter2["GlobalLogFilter<br/>Logs the response"]

        GlobalLogFilter1 --> Route
        Route --> UnifiedResponseFilter
        UnifiedResponseFilter --> CorsResponseHeaderFilter
        CorsResponseHeaderFilter --> GlobalLogFilter2
    end

    subgraph BackendService["Backend service kb-user-auth"]
        Controller["Controller<br/>Receives the request"]
        Service["Service<br/>Business logic"]
        Exception["Throws BusinessException"]
        GlobalExceptionHandler["GlobalExceptionHandler<br/>Converts to Result format"]

        Controller --> Service
        Service -->|Business exception| Exception
        Exception --> GlobalExceptionHandler
    end

    subgraph CommonModule["kb-common shared module"]
        Result["Result unified return format"]
        BusinessException["BusinessException<br/>Business exception"]
    end

    Client -->|1. Request| GlobalLogFilter1
    GlobalLogFilter1 -->|2. Route forwarding| Controller
    Service -->|3. Exception| GlobalExceptionHandler
    GlobalExceptionHandler -->|4. Result response| UnifiedResponseFilter
    UnifiedResponseFilter -->|5. Response processing| CorsResponseHeaderFilter
    CorsResponseHeaderFilter -->|6. Final response| GlobalLogFilter2
    GlobalLogFilter2 -->|7. Return| Client

    CommonModule -.->|Dependency| BackendService
    CommonModule -.->|Dependency| Gateway

    style Gateway fill:#e1f5ff
    style BackendService fill:#fff4e6
    style CommonModule fill:#f0fff0
    style Client fill:#f0f0f0
```

## 3. System Layered Architecture Diagram

```mermaid
graph TB
    subgraph ClientLayer["Client Layer"]
        Frontend["Frontend App<br/>Vue3/React"]
        Mobile["Mobile<br/>iOS/Android"]
    end

    subgraph GatewayLayer["Gateway Layer kb-gateway"]
        CorsWebFilter["CORS Filter"]
        GlobalLogFilter["Global Log Filter"]
        UnifiedResponseFilter["Unified Response Filter"]
        GlobalExceptionHandler["Global Exception Handler"]
        RouteLocator["Route Locator"]
    end

    subgraph ServiceLayer["Business Service Layer"]
        UserAuthService["User & Auth Service<br/>kb-user-auth"]
        DocumentService["Document Service<br/>kb-document"]
        FileService["File Service<br/>kb-file"]
    end

    subgraph CommonLayer["Common Layer kb-common"]
        Result["Result unified return"]
        BusinessException["Business exception"]
        AssertUtil["Assertion utility"]
        FeignInterceptor["Feign interceptor"]
    end

    Frontend --> GatewayLayer
    Mobile --> GatewayLayer
    GatewayLayer --> ServiceLayer
    ServiceLayer --> CommonLayer

    style GatewayLayer fill:#e1f5ff
    style ServiceLayer fill:#fff4e6
    style CommonLayer fill:#f0fff0
    style ClientLayer fill:#f0f0f0
```

## 4. Exception Handling Flow Diagram

```mermaid
graph TD
    Start[Request comes in] --> CheckType{Check request type}

    CheckType -->|Internal call<br/>INNER-REQUEST header| InternalException[Internal exception handling]
    CheckType -->|External API call| ExternalException[External exception handling]

    InternalException --> CheckBusiness{Is it a BusinessException?}
    CheckBusiness -->|Yes| InternalBusiness[Return ResponseEntity<br/>Preserving the HTTP status code]
    CheckBusiness -->|No| InternalSystem[Return a 500 error]

    ExternalException --> CheckExceptionType{Determine exception type}

    CheckExceptionType -->|BusinessException| BusinessResult[Return Result.error<br/>Business error code]
    CheckExceptionType -->|AccessDeniedException| ForbiddenResult[Return a 403 error]
    CheckExceptionType -->|ValidationException| ParamResult[Return a parameter error]
    CheckExceptionType -->|Other exceptions| SystemResult[Return a system error]

    InternalBusiness --> End[Response ends]
    InternalSystem --> End
    BusinessResult --> End
    ForbiddenResult --> End
    ParamResult --> End
    SystemResult --> End

    style InternalException fill:#fff4e6
    style ExternalException fill:#e1f5ff
    style End fill:#f0f0f0
```

## 5. Response Handling Flow Diagram

```mermaid
sequenceDiagram
    participant C as Client
    participant G as Gateway
    participant S as Business Service
    participant E as Exception Handler

    C->>G: Send request
    G->>G: GlobalLogFilter logs the request

    G->>S: Route forwards to the business service
    S->>S: Controller processes
    S->>S: Service executes business logic

    alt Business exception
        S->>E: Throw BusinessException
        E->>G: Return Result-format error
    else Normal response
        S->>G: Return business data
    end

    G->>G: UnifiedResponseFilter checks the format
    alt Non-standard format
        G->>G: Wrap into Result format
    end

    G->>G: CorsResponseHeaderFilter deduplicates CORS headers
    G->>G: GlobalLogFilter logs the response

    G->>C: Return the unified-format response
```

## Using These Diagrams in draw.io

### Method 1: Use the Mermaid Plugin
1. Open draw.io
2. Select `Arrange` → `Insert` → `Advanced` → `Mermaid`
3. Paste the Mermaid code above into the dialog
4. Click Insert

### Method 2: Online Conversion
1. Visit https://mermaid.live/
2. Paste the code into the editor
3. Export as SVG/PNG
4. Import the image into draw.io

### Method 3: Command-Line Generation
```bash
# Install mermaid-cli
npm install -g @mermaid-js/mermaid-cli

# Generate the image
mmdc -i diagrams.md -o architecture.png
```

## Diagram Descriptions

- **Technical Architecture Diagram**: shows the overall flow and tech stack of an API call
- **Full Call Chain Diagram**: shows the complete chain from client to response in detail
- **System Layered Architecture Diagram**: shows the system's layered structure and the relationships between layers
- **Exception Handling Flow Diagram**: shows the handling flow for different types of exceptions
- **Response Handling Flow Diagram**: shows the complete request-response process as a sequence diagram
