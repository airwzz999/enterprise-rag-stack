# API Path Mapping Explanation

## Overview

This document explains how frontend API call paths actually map after passing through the gateway.

## Gateway Configuration

```yaml
# kb-gateway application.yml
routes:
  - id: kb-document
    uri: lb://kb-document
    predicates:
      - Path=/api/document/**
    filters:
      - StripPrefix=2  # strips off /api and /document
```

## Path Mapping Rules

### Frontend → Gateway → Backend

| Frontend call | After gateway processing | Actual backend path | Backend controller |
|---------|-----------|------------|--------------|
| `/api/document/categories/tree` | strip `/api/document` | `/categories/tree` | `@RequestMapping("/categories")` |
| `/api/document/categories` | strip `/api/document` | `/categories` | `@RequestMapping("/categories")` |
| `/api/document/categories/123` | strip `/api/document` | `/categories/123` | `@RequestMapping("/categories")` |
| `/api/document/documents` | strip `/api/document` | `/documents` | `@RequestMapping("/documents")` |
| `/api/document/documents/123` | strip `/api/document` | `/documents/123` | `@RequestMapping("/documents")` |
| `/api/document/documents/123/comments` | strip `/api/document` | `/documents/123/comments` | `@RequestMapping("/documents")` |

## Modified Endpoints

### category.service.ts

All category-related endpoints have had the `/document/` prefix added:

```typescript
// ✅ After the change
getCategoryTree: () => http.get<CategoryTree[]>('/document/categories/tree')
getCategory: (id: string) => http.get<DocumentCategory>(`/document/categories/${id}`)
createCategory: (data) => http.post<DocumentCategory>('/document/categories', data)
updateCategory: (id, data) => http.put<DocumentCategory>(`/document/categories/${id}`, data)
deleteCategory: (id: string) => http.delete(`/document/categories/${id}`)
moveCategory: (params) => http.post('/document/categories/move', params)
batchDeleteCategories: (ids) => http.delete('/document/categories/batch', { data: { ids } })
getCategoryDocuments: (categoryId, params) => http.get(`/document/categories/${categoryId}/documents`, { params })
getCategoryStats: () => http.get<Array<{...}>>('/document/categories/stats')
searchCategories: (keyword) => http.get<DocumentCategory[]>('/document/categories/search', { params: { keyword } })
```

### document.service.ts

Document-related endpoints already correctly use the `/document/` prefix (no changes needed):

```typescript
// ✅ Already correct
getDocuments: (filter) => http.get<DocumentListResponse>('/document/documents', { params: filter })
getDocument: (id) => http.get<Document>(`/document/documents/${id}`)
createDocument: (data) => http.post<Document>('/document/documents', data)
updateDocument: (id, data) => http.put<Document>(`/document/documents/${id}`, data)
// ... other document endpoints
```

## Gateway Routing Explanation

### Current Route Configuration

```yaml
# Document service route
- id: kb-document
  uri: lb://kb-document
  predicates:
    - Path=/api/document/**  # matches all requests starting with /api/document
  filters:
    - StripPrefix=2  # strips the first 2 path segments (/api and /document)
```

### Example Request Flow

```
1. Frontend request: GET /api/document/categories/tree

2. Gateway receives it:
   - Matching rule: Path=/api/document/** ✅ matched
   - Extracted path: /api/document/categories/tree
   - StripPrefix=2: strips /api and /document
   - Forwarded path: /categories/tree

3. Backend receives: GET /categories/tree
   - Controller: @RequestMapping("/categories")
   - Method: @GetMapping("/tree")
   - Final match: @RequestMapping("/categories") + @GetMapping("/tree") = /categories/tree ✅
```

## Notes

1. **The frontend must use the full path**: all document- and category-related endpoints must start with `/api/document/`
2. **The gateway must be configured correctly**: make sure the gateway has a `/api/document/**` route configured
3. **Backend controller path**: the backend's @RequestMapping is the path after the prefix has been stripped
4. **StripPrefix count**: StripPrefix=2 means the first 2 path segments are stripped

## Test Verification

### Testing the Category Tree Endpoint

```bash
# Frontend call
GET /api/document/categories/tree

# Gateway processing
Match: Path=/api/document/** ✅
StripPrefix=2 → /categories/tree

# Backend receives
GET /categories/tree
Controller: CategoryController @RequestMapping("/categories")
Method: @GetMapping("/tree")
```

### Testing the Document List Endpoint

```bash
# Frontend call
GET /api/document/documents

# Gateway processing
Match: Path=/api/document/** ✅
StripPrefix=2 → /documents

# Backend receives
GET /documents
Controller: DocumentController @RequestMapping("/documents")
Method: @GetMapping
```
