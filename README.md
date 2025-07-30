# Distributed File System (DFS)

A scalable, fault-tolerant distributed file system implemented in Java (Spring Boot) and TypeScript/React. DFS supports secure, reliable file storage and management across distributed nodes, implementing advanced features like replication, fault tolerance, dynamic node management, version control, encryption, and both CLI and web-based interfaces.

---

## Features

### Core Features (Server-Side Functionality)
1. **File Operations:**
   - Upload, download, move, rename, and delete files across distributed nodes.
   - Efficient chunked file transfer (1MB chunks) with checksum validation (SHA-256) for data integrity.

2. **Replication:**
   - Configurable replication factor (default: 5 replicas) for reliability.
   - Primary-secondary replication model with dynamic node selection based on health and available space.
   - Supports forced/manual replication for critical files.

3. **Fault Tolerance:**
   - Automatic recovery from node failures using heartbeat-based health monitoring.
   - Scheduled recovery tasks to ensure system stability.
   - Recovery of data stored on failed nodes by leveraging replication.

4. **Node Management:**
   - Dynamic addition and removal of storage nodes.
   - Continuous health monitoring of all nodes using a heartbeat mechanism.
   - View detailed node statuses including node health, available disk space, and active connections.

5. **Encryption:**
   - End-to-end encryption for all file transfers and data storage.
   - Secures data at rest and in transit to ensure confidentiality and integrity.

6. **Version Control:**
   - Create, list, restore, compare, and delete file versions.
   - Unique version identifiers and metadata (e.g., creator, timestamp, and comments).
   - Ability to restore files to any previous version.

7. **Metrics and Monitoring:**
   - Collection of system metrics for node performance and health.
   - Logs for system activities, node events, and replication processes.

8. **API Documentation:**
   - Integrated Swagger UI for exploring and testing RESTful APIs.
   - Easy integration with external systems and tools.

---

### Client Features

#### CLI (Command-Line Interface)
- **File Commands:**
  - Upload, download, delete, move, and rename files.
- **Directory Management:**
  - Create, list, delete, and navigate directories.
- **Node Operations:**
  - Add new nodes, check node health, and recover failed nodes.
- **Version Control:**
  - Create, list, restore, and compare file versions.
- **Replication Commands:**
  - View replication status, force replication for specific files.

#### Web Frontend (React)
- **Authentication:**
  - User registration, login, and session management.
- **File Management:**
  - Upload, download, delete, and manage files through a modern UI.
- **Directory Features:**
  - Create, list, delete, and navigate directories visually.
- **Version Control:**
  - Access version history and restore previous versions.
- **Node and Replication Monitoring:**
  - View node health and replication status via a dashboard.
- **Planned Features:**
  - Secure file sharing between users.
  - Real-time updates for file and node statuses.

---

## Architecture

### Server Components
- **ServerHandler:** Core server logic for handling file and directory operations.
- **DirectoryHandler:** Manages directory-related operations (listing, creation, deletion).
- **NodeManager:** Responsible for tracking, adding, and monitoring storage nodes.
- **ReplicationManager:** Controls replication processes, ensuring data redundancy and reliability.
- **FaultToleranceManager:** Handles detection of node failures and initiates recovery processes.
- **VersionManager:** Manages file versioning metadata and operations.
- **Encryption Layer:** Provides secure storage and transfer mechanisms.
- **Metrics Collector:** Gathers performance data and node health metrics for monitoring.

### Client Components
- **DFSClient (CLI):** Full-featured command-line interface for advanced operations.
- **Web Frontend (React):** User-friendly, browser-based interface for file management and monitoring.

### Models
- **Node:** Represents a distributed storage node, including health and metadata.
- **Command:** Encapsulates operations exchanged between the client and server.
- **FileChunk:** Represents chunked file storage for efficient transfer.
- **Version:** Metadata for versioned files, including creator, timestamp, and version comments.
- **ReplicationStatus:** Tracks replication health and node distribution of files.

---

## Implementation Details

1. **File Transfer and Storage:**
   - Files are divided into 1MB chunks for efficient memory usage.
   - Chunked file transfers allow for resumable uploads and downloads.
   - SHA-256 checksum validation ensures data integrity.

2. **Health Monitoring:**
   - Heartbeat mechanism to check node health every 60 seconds.
   - Automatic detection of failed nodes and initiation of recovery processes.
   - Detailed health reports for all nodes.

3. **Replication Mechanism:**
   - Default replication factor: 3 copies.
   - Automatic replication of files across nodes for fault tolerance.
   - Background replication tasks to maintain data redundancy.

4. **Version Control System:**
   - Allows multiple versions of the same file with unique identifiers.
   - Supports comparison and restoration of previous versions.
   - Metadata includes creator, timestamps, and version comments.

5. **Encryption:**
   - All data is encrypted during storage and transfer.
   - Ensures confidentiality and protection against unauthorized access.

6. **API and Swagger Integration:**
   - Comprehensive REST API for system control and integration.
   - Swagger UI for interactive API exploration and testing.

---

## Installation

### Prerequisites
- **Java 17+**
- **Maven** for building the backend
- **Node.js** and **npm** for the frontend

### Backend Setup
```sh
git clone https://github.com/pranayh24/Distributed-File-System.git
cd Distributed-File-System
mvn clean package
java -jar target/dfs-server.jar
```
- Access Swagger UI for API documentation: `http://localhost:<port>/swagger-ui/`

### Frontend Setup
```sh
cd dfs-frontend
npm install
npm run dev
```
- Access the frontend at: `http://localhost:3000`

---

## Usage

### CLI
- Provides access to all core features including file operations, node management, version control, and replication.
- Run the `DFSClient` executable to open an interactive menu-driven interface.

### Web Interface
- User-friendly platform for managing files, directories, nodes, versions, and replication status.
- Monitor system health and replication processes visually.

### API
- Explore REST endpoints via Swagger UI.
- Integrate with external systems or automation tools.

---

## System Requirements

- Minimum of **three nodes** recommended for proper replication.
- Each node requires sufficient disk space for storage.
- Stable network connectivity between all nodes.
- Synchronized system time across all nodes.

---

## Roadmap
- Complete all frontend features, including secure file sharing and real-time updates.
- Enhance fault tolerance with distributed locking mechanisms.
- Improve security with advanced authentication and access control.
- Add dynamic load balancing across nodes.

---

## Contributors

- [pranayh24](https://github.com/pranayh24)

_Last updated: 2025-07-29_
