# Safe Delete

A small full-stack application for safely deleting files on the server. The backend restricts all deletions to a configurable `allowed_root` directory and refuses symlinks, directories, and paths outside the sandbox. The frontend provides a simple UI to submit paths and view per-path results.

## Stack

Spring Boot 3.2 / Java 17 / React 18 / Vite 5

## Layout

```
.
├── backend/
│   ├── pom.xml
│   └── src/
│       └── main/
│           ├── java/com/example/demo/
│           │   ├── DemoApplication.java
│           │   └── controller/ApiController.java
│           └── resources/application.properties
└── frontend/
    ├── index.html
    ├── package.json
    ├── vite.config.js
    └── src/
        ├── App.jsx
        ├── App.css
        └── main.jsx
```

## How to run

### Backend

```bash
cd backend
mvn spring-boot:run
```

The API listens on `http://localhost:8080`. Files may only be deleted from the directory configured via `app.allowed-root` (defaults to `./allowed_root`, resolved relative to the working directory).

### Frontend

```bash
cd frontend
npm install
npm run dev
```

The dev server runs on `http://localhost:5173` and proxies `/api` requests to the backend at `http://localhost:8080`.

## API

### `POST /api/delete`

Deletes one or more files. All paths are validated against `allowed_root` before deletion.

**Request body**

```json
{
  "paths": ["/abs/path/to/file.txt", "/abs/path/to/another.txt"]
}
```

**Response body**

```json
{
  "results": [
    { "path": "/abs/path/to/file.txt", "deleted": true, "reason": "deleted" },
    { "path": "/abs/path/to/another.txt", "deleted": false, "reason": "outside allowed_root" }
  ]
}
```

Each entry contains:

| Field     | Type    | Description                                              |
|-----------|---------|----------------------------------------------------------|
| `path`    | string  | The path as submitted                                    |
| `deleted` | boolean | Whether the file was deleted                             |
| `reason`  | string  | `deleted`, or a refusal reason (see below)               |

Possible refusal reasons: `empty path`, `outside allowed_root`, `symlink refused`, `does not exist`, `not a regular file`, `io error: <message>`.

## Notes

- **Sandboxing:** Only regular files whose normalized absolute path starts with `allowed_root` can be deleted. Symlinks are always refused, even if they point inside the root.
- **No authentication:** The API is unauthenticated. Do not expose it on an untrusted network without adding auth and/or network restrictions.
- **No CORS configuration:** The frontend relies on the Vite dev proxy. For production, serve the built frontend from the same origin as the backend or add explicit CORS configuration.
- **No persistence or audit log:** Deletions are not recorded; results are returned only in the HTTP response.
- **`allowed_root` must exist:** The directory is not created automatically. Create it (or point `app.allowed-root` at an existing directory) before use.
- **Batch semantics:** Each path is processed independently; a failure on one path does not abort the rest of the batch.