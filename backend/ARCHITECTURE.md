# Malva Pastry Shop - System Architecture

## Overview
This document describes the system architecture, design patterns, and architectural decisions for the Malva Pastry Shop backend.

**Architecture Style:** Layered + Domain-Driven Design (DDD)  
**Deployment:** Monolithic Spring Boot application  
**Strategy:** Dual-channel (Admin SSR + Public REST API)

---

## Table of Contents
1. [High-Level Architecture](#high-level-architecture)
2. [Domain-Driven Design](#domain-driven-design)
3. [Layer Architecture](#layer-architecture)
4. [Package Organization](#package-organization)
5. [Design Patterns](#design-patterns)
6. [Security Architecture](#security-architecture)
7. [API Strategy](#api-strategy)

---

## High-Level Architecture

```mermaid
graph TB
    subgraph "Frontend Layer"
        A1[Admin Panel<br/>Thymeleaf SSR]
        A2[React Storefront<br/>Future]
    end
    
    subgraph "Backend - Spring Boot"
        B1[Admin Controllers<br/>@Controller]
        B2[API Controllers<br/>@RestController]
        
        C1[Storefront Services]
        C2[Inventory Services]
        C3[Sales Services]
        C4[User Service]
        
        D1[JPA Repositories]
        
        E1[Domain Entities]
    end
    
    subgraph "Data Layer"
        F1[(PostgreSQL Database)]
    end
    
    A1 -.HTTP.-> B1
    A2 -.REST API.-> B2
    
    B1 --> C1
    B1 --> C2
    B1 --> C3
    B1 --> C4
    B2 --> C1
    B2 --> C2
    
    C1 --> D1
    C2 --> D1
    C3 --> D1
    C4 --> D1
    
    D1 --> E1
    E1 -.JPA/Hibernate.-> F1
    
    style A1 fill:#e1f5ff
    style A2 fill:#fff4e1
    style B1 fill:#d4edda
    style B2 fill:#fff3cd
    style C1 fill:#cfe2ff
    style C2 fill:#f8d7da
    style F1 fill:#e2e3e5
```

---

## Domain-Driven Design

```mermaid
graph LR
    subgraph "Inventory Context (Core)"
        IN1[Product]
        IN2[Category]
        IN3[Ingredient]
        IN4[ProductIngredient]
        IN5[UnitOfMeasure]
    end

    subgraph "Storefront Context (Public Display)"
        ST1[StorefrontSection]
        ST2[StorefrontSectionProduct]
        ST3[Tag]
        ST4[ProductTag]
    end
    
    subgraph "Auth Context (Shared)"
        AU1[User]
        AU2[Role]
    end

    subgraph "Sales Context (Transactions)"
        SA1[Sale]
        SA2[SaleIngredient]
    end
    
    IN1 -.recipe.-> IN4
    IN4 --> IN3
    IN1 --> IN2
    ST2 --> IN1
    ST2 --> ST1
    ST4 --> IN1
    ST4 --> ST3
    SA1 --> IN1
    IN1 -.created by.-> AU1
    
    style IN1 fill:#f8d7da
    style IN2 fill:#f8d7da
    style IN3 fill:#f8d7da
    style IN4 fill:#f8d7da
    style IN5 fill:#f8d7da
    style ST1 fill:#d1ecf1
    style ST2 fill:#d1ecf1
    style ST3 fill:#d1ecf1
    style ST4 fill:#d1ecf1
    style AU1 fill:#fff3cd
    style AU2 fill:#fff3cd
    style SA1 fill:#e2e3e5
    style SA2 fill:#e2e3e5
```

### Bounded Contexts

#### 1. Inventory Context (Core)
**Purpose:** Product catalog, recipes, and cost management  
**Entities:** Product, Category, Ingredient, ProductIngredient, UnitOfMeasure  
**Services:** ProductService, CategoryService, IngredientService  
**Exposure:** Admin panel + selected data exposed via REST API

**Business Rules:**
- Products have categories for internal organization
- Products implement soft-delete (can be restored)
- Ingredients track unit costs for profit analysis
- ProductIngredient defines recipes (quantity of each ingredient)

#### 2. Storefront Context (Public Display)
**Purpose:** Controls which products and content are displayed on the public frontend  
**Entities:** StorefrontSection, StorefrontSectionProduct, Tag, ProductTag  
**Services:** StorefrontSectionService, TagService  
**Exposure:** Exposed via REST API to React frontend

**Business Rules:**
- StorefrontSections group products for public display
- StorefrontSectionProduct links products to sections with display ordering
- Tags have URL-friendly slugs for filtering
- Cannot delete tag if in use by products

#### 3. Auth Context (Shared)
**Purpose:** Authentication and authorization  
**Entities:** User, Role  
**Services:** UserService (implements UserDetailsService)  
**Exposure:** Admin-only, never public

**Business Rules:**
- Role-based access control (ADMIN, EMPLOYEE)
- BCrypt password hashing
- Session-based authentication for admin panel

#### 4. Sales Context (Transactions)
**Purpose:** Record taking and sales history  
**Entities:** Sale, SaleIngredient  
**Services:** SaleService  
**Exposure:** Admin/Employee internal use

**Business Rules:**
- Sales snapshot product data (name, price) at moment of sale
- Tracks ingredients used per sale

### Cross-Context Relationships

```mermaid
graph LR
    A[Product<br/>Inventory] -->|has recipe| B[ProductIngredient<br/>Inventory]
    B --> C[Ingredient<br/>Inventory]
    A -->|displayed in| E[StorefrontSectionProduct<br/>Storefront]
    E --> F[StorefrontSection<br/>Storefront]
    A -->|tagged via| G[ProductTag<br/>Storefront]
    G --> H[Tag<br/>Storefront]
    A -->|created by| D[User<br/>Auth]
    
    style A fill:#f8d7da
    style B fill:#f8d7da
    style C fill:#f8d7da
    style D fill:#fff3cd
    style E fill:#d1ecf1
    style F fill:#d1ecf1
    style G fill:#d1ecf1
    style H fill:#d1ecf1
```

---

## Layer Architecture

```mermaid
graph TD
    subgraph "Presentation Layer"
        P1[Admin Controllers<br/>Thymeleaf Views]
        P2[API Controllers<br/>JSON Responses]
    end
    
    subgraph "Application Layer"
        A1[Services<br/>Business Logic]
        A2[DTOs<br/>Data Transfer]
    end
    
    subgraph "Domain Layer"
        D1[Entities<br/>Business Rules]
        D2[Value Objects<br/>Enums]
    end
    
    subgraph "Infrastructure Layer"
        I1[Repositories<br/>Data Access]
        I2[Security<br/>Authentication]
        I3[Utils<br/>SlugUtil]
    end
    
    P1 --> A1
    P2 --> A1
    A1 --> D1
    A1 --> I1
    A1 --> A2
    I1 --> D1
    P1 -.uses.-> I2
    P2 -.uses.-> I2
    A1 -.uses.-> I3
    
    style P1 fill:#d4edda
    style P2 fill:#fff3cd
    style A1 fill:#cfe2ff
    style D1 fill:#e2e3e5
    style I1 fill:#f8d7da
```

### Layer Responsibilities

#### Presentation Layer
- **Controllers**: Handle HTTP requests/responses
- **Admin Controllers** (`@Controller`): Return Thymeleaf view names
- **API Controllers** (`@RestController`): Return JSON DTOs
- **Validation**: `@Valid` on request DTOs

#### Application Layer
- **Services**: Business logic, orchestration, transactions
- **DTOs**: Control data shape for different audiences
- **Mappers** (future): Entity ↔ DTO conversion

#### Domain Layer
- **Entities**: Business objects with identity
- **Value Objects**: Immutable objects (enums)
- **Business Rules**: Encoded in entity methods
- **No dependencies**: Pure business logic

#### Infrastructure Layer
- **Repositories**: Database queries via Spring Data JPA
- **Security**: Spring Security configuration
- **Utilities**: Helper classes (e.g., SlugUtil)

---

## Package Organization

```
com.malva_pastry_shop.backend/
│
├── controller/                 # Presentation
│   ├── admin/                  # SSR Controllers (Thymeleaf)
│   └── api/                    # REST Controllers (JSON)
│
├── service/                    # Application
│   ├── storefront/             # Public catalog logic
│   ├── inventory/              # Internal operations
│   └── UserService             # Auth service
│
├── dto/                        # Data Transfer
│   ├── request/                # Input validation
│   └── response/
│       ├── public/             # API responses
│       └── admin/              # Internal reports
│
├── domain/                     # Domain
│   ├── inventory/              # Product, Category, Ingredient, ProductIngredient, UnitOfMeasure
│   ├── storefront/             # StorefrontSection, StorefrontSectionProduct, Tag, ProductTag
│   ├── sales/                  # Sale, SaleIngredient
│   ├── auth/                   # User, Role, RoleType
│   └── common/                 # Base classes (TimestampedEntity, SoftDeletableEntity)
│
├── repository/                 # Infrastructure
├── config/                     # Configuration
│   ├── SecurityConfig
│   └── DataSeeder
│
└── util/                       # Utilities
    └── SlugUtil
```

### Package Naming Conventions

| Package      | Suffix           | Example                              |
| ------------ | ---------------- | ------------------------------------ |
| Entities     | None             | `Product`, `Tag`                     |
| Repositories | `Repository`     | `ProductRepository`                  |
| Services     | `Service`        | `ProductService`                     |
| Controllers  | `Controller`     | `ProductController`                  |
| DTOs         | `DTO`, `Request` | `ProductPublicDTO`, `ProductRequest` |

---


## Security Architecture

```mermaid
graph LR
    A[HTTP Request] --> B{Authenticated?}
    B -->|No| C[Redirect to /login]
    B -->|Yes| D{Authorized?}
    D -->|No| E[403 Forbidden]
    D -->|Yes| F[Controller Method]
    
    F --> G{Has @PreAuthorize?}
    G -->|Yes| H{Role Check}
    G -->|No| I[Execute]
    H -->|Pass| I
    H -->|Fail| E
    
    style C fill:#f8d7da
    style E fill:#f8d7da
    style I fill:#d4edda
```



---

## API Strategy

### Admin Panel (SSR)
- **Technology:** Thymeleaf
- **Audience:** Admin/Employee users
- **Authentication:** Session-based (Spring Security)
- **URL Pattern:** `/products`, `/categories`, `/sales`, `/sections`, etc.

### Public API (REST)
- **Technology:** Spring REST + JSON
- **Audience:** React frontend (customers)
- **Authentication:** None (public read-only)
- **URL Pattern:** `/api/v1/products`, `/api/v1/sections`, `/api/v1/categories`, `/api/v1/tags`


## Technology Stack Alignment

```mermaid
graph TB
    A[Spring Boot 4.0.1] --> B[IoC Container]
    A --> C[Auto-Configuration]
    
    D[Spring MVC] --> E[Admin Controllers]
    D --> F[REST Controllers]
    
    G[Spring Data JPA] --> H[Repositories]
    H --> I[Hibernate 7.2]
    I --> J[PostgreSQL Driver]
    
    K[Spring Security 7.x] --> L[Authentication]
    K --> M[Authorization]
    
    N[Thymeleaf 3.x] --> O[Template Engine]
    
    style A fill:#6db33f
    style D fill:#6db33f
    style G fill:#6db33f
    style K fill:#6db33f
```

## Entity Relationship Diagram

```mermaid
erDiagram
    users ||--o{ products : "creates"
    users }o--|| roles : "has"
    
    categories ||--o{ products : "categorizes"
    products ||--o{ product_ingredients : "contains"
    products ||--o{ product_tags : "tagged_with"
    products ||--o{ storefront_section_products : "displayed_in"
    products ||--o{ sales : "sold_as"
    ingredients ||--o{ product_ingredients : "used_in"
    tags ||--o{ product_tags : "applies_to"
    storefront_sections ||--o{ storefront_section_products : "contains"
    sales ||--o{ sale_ingredients : "uses"
    ingredients ||--o{ sale_ingredients : "consumed_in"
    
    users {
        bigint id PK "Auto-increment"
        varchar_100 name "NOT NULL"
        varchar_100 last_name "NOT NULL"
        varchar_255 email "UNIQUE, NOT NULL"
        varchar_255 password_hash "NOT NULL"
        boolean enabled "DEFAULT true"
        boolean system_admin "DEFAULT false"
        bigint role_id FK "→ roles.id"
        timestamp inserted_at "DEFAULT now()"
        timestamp updated_at "DEFAULT now()"
    }
    
    roles {
        bigint id PK "Auto-increment"
        varchar_50 name "UNIQUE, NOT NULL (ADMIN/EMPLOYEE)"
        varchar_255 description
        timestamp inserted_at "DEFAULT now()"
        timestamp updated_at "DEFAULT now()"
    }
    
    categories {
        bigint id PK "Auto-increment"
        varchar_100 name "NOT NULL"
        text description
        timestamp inserted_at "DEFAULT now()"
        timestamp updated_at "DEFAULT now()"
        timestamp deleted_at "NULL = active"
        bigint deleted_by_id FK "→ users.id"
    }
    
    products {
        bigint id PK "Auto-increment"
        varchar_100 name "NOT NULL"
        text description
        integer preparation_days "≥ 0"
        varchar_500 image_url
        numeric_12_2 base_price "≥ 0.00"
        boolean visible "DEFAULT false"
        bigint user_id FK "→ users.id (creator)"
        bigint category_id FK "→ categories.id"
        timestamp inserted_at "DEFAULT now()"
        timestamp updated_at "DEFAULT now()"
        timestamp deleted_at "NULL = active"
        bigint deleted_by_id FK "→ users.id"
    }
    
    tags {
        bigint id PK "Auto-increment"
        varchar_50 name "NOT NULL"
        varchar_100 slug "UNIQUE, NOT NULL (URL-friendly)"
        varchar_200 description
        timestamp inserted_at "DEFAULT now()"
        timestamp updated_at "DEFAULT now()"
        timestamp deleted_at "NULL = active"
        bigint deleted_by_id FK "→ users.id"
    }
    
    product_tags {
        bigint id PK "Auto-increment"
        bigint product_id FK "→ products.id, NOT NULL"
        bigint tag_id FK "→ tags.id, NOT NULL"
        timestamp inserted_at "DEFAULT now()"
        timestamp updated_at "DEFAULT now()"
    }
    
    ingredients {
        bigint id PK "Auto-increment"
        varchar_100 name "NOT NULL"
        text description
        numeric_10_2 unit_cost "≥ 0.00"
        varchar_50 unit_of_measure "ENUM"
        timestamp inserted_at "DEFAULT now()"
        timestamp updated_at "DEFAULT now()"
        timestamp deleted_at "NULL = active"
        bigint deleted_by_id FK "→ users.id"
    }
    
    product_ingredients {
        bigint id PK "Auto-increment"
        bigint product_id FK "→ products.id, NOT NULL"
        bigint ingredient_id FK "→ ingredients.id, NOT NULL"
        numeric_14_4 quantity "NOT NULL, > 0"
        timestamp inserted_at "DEFAULT now()"
        timestamp updated_at "DEFAULT now()"
    }

    storefront_sections {
        bigint id PK "Auto-increment"
        varchar_100 name "NOT NULL"
        text description
        integer display_order
        boolean visible "DEFAULT false"
        timestamp inserted_at "DEFAULT now()"
        timestamp updated_at "DEFAULT now()"
        timestamp deleted_at "NULL = active"
        bigint deleted_by_id FK "→ users.id"
    }

    storefront_section_products {
        bigint id PK "Auto-increment"
        bigint storefront_section_id FK "→ storefront_sections.id, NOT NULL"
        bigint product_id FK "→ products.id, NOT NULL"
        integer display_order
        timestamp inserted_at "DEFAULT now()"
        timestamp updated_at "DEFAULT now()"
    }

    sales {
        bigint id PK "Auto-increment"
        timestamp sale_date "NOT NULL"
        bigint registered_by_id FK "→ users.id"
        bigint product_id FK "→ products.id (nullable)"
        varchar_100 product_name "Snapshot"
        integer quantity "NOT NULL"
        numeric_12_2 unit_price "Snapshot"
        numeric_12_2 total_amount "Snapshot"
        timestamp inserted_at "DEFAULT now()"
        timestamp updated_at "DEFAULT now()"
    }

    sale_ingredients {
        bigint id PK "Auto-increment"
        bigint sale_id FK "→ sales.id, NOT NULL"
        bigint ingredient_id FK "→ ingredients.id (nullable)"
        varchar_100 ingredient_name "Snapshot"
        numeric_14_4 quantity "NOT NULL"
        numeric_12_2 unit_cost "Snapshot"
        varchar_50 unit_of_measure "Snapshot"
        timestamp inserted_at "DEFAULT now()"
        timestamp updated_at "DEFAULT now()"
    }
```
