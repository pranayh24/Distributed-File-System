# Distributed File System (DFS)

A scalable, fault-tolerant distributed file system implemented in Java with Spring Boot (currently the web-based system is in development and command line interface is functional). This system enables reliable file storage and management across multiple distributed nodes with support for replication, fault tolerance, and version control.

## Features

- **File Operations**: Upload, download, move, and delete files across the distributed system
- **Directory Management**: Create, list, delete, and navigate directory structures
- **Chunked File Transfer**: Efficient handling of large files using 1MB chunks
- **Replication**: Configurable file replication (default: 3 replicas) for data reliability
- **Fault Tolerance**: Automatic recovery from node failures with health monitoring
- **Version Control**: Create, list, restore, compare, and delete file versions
- **Node Management**: Dynamic addition and health monitoring of storage nodes

## Architecture

The DFS consists of the following key components:

### Server Components

- **ServerHandler**: Core server component handling file operations and directory commands
- **DirectoryHandler**: Manages directory operations (listing, creation, deletion)
- **NodeManager**: Tracks and monitors the health of all storage nodes
- **ReplicationManager**: Ensures files are properly replicated across nodes
- **FaultToleranceManager**: Handles recovery operations when nodes fail
- **VersionManager**: Implements version control functionality

### Client Components

- **DFSClient**: Client interface for interaction with the system
- **DirectoryOperations**: Handles directory-related operations from the client
- **VersionOperations**: Manages version control operations from the client

### Models

- **Node**: Represents a storage node in the distributed system
- **Command**: Encapsulates operations for transmission to the server
- **FileChunk**: Represents a chunk of a file for efficient transfer
- **Version**: Contains metadata for versioned files
- **ReplicationStatus**: Tracks the replication state of files across nodes

## Implementation Details

### File Transfer and Storage

Files are transferred in 1MB chunks for efficient memory management, with each chunk individually tracked and validated. This approach allows for:

- Handling large files without excessive memory consumption
- Progress tracking during transfers
- Checksum validation for data integrity using SHA-256
- Resumable transfers in case of interruption

### Replication System

The system implements a configurable replication strategy:

- Default replication factor: 3 copies
- Primary-secondary replication model
- Dynamic node selection based on available space and health
- Background replication for fault tolerance

### Health Monitoring

Node health is continuously monitored:

- Heartbeat mechanism with 60-second timeout
- Automatic detection of node failures
- Health status tracking for all system nodes
- Scheduled health checks

### Version Control

The version control system maintains file history:

- Unique version identifiers
- Creator information and timestamps
- Version comments/descriptions
- Comparison between versions
- Selective restoration of previous versions

## Installation

### Prerequisites

- Java 11 or higher
- Maven for building the project

### Build Steps

1. Clone the repository: git clone https://github.com/pranayh24/Distributed-File-System.git cd Distributed-File-System

2. Build with Maven: mvn clean package

3. The build process will create a JAR file in the `target` directory.

## Usage

### Starting the Server

Run the server with the following command: java -jar target/dfs-server.jar


By default, the server will use the configured port and storage location specified in the application properties.

### Client Operation

The DFS client provides programmatic access to the distributed file system. The main operations include:

#### File Operations
- Upload files to the distributed storage
- Download files from storage to local system
- Delete files from the distributed system
- Move or rename files within the system

#### Directory Operations
- List directory contents
- Create new directories
- Remove directories and their contents
- Navigate the directory structure

#### Version Control
- Create versions of files with comments
- List all versions of a specific file
- Restore files to previous versions
- Compare different versions of a file
- Delete specific versions

#### Node Management
- View system nodes and their status
- Monitor node health
- Check replication status for files
- Force replication when needed

## System Requirements

- Minimum of three nodes recommended for proper replication
- Each node requires sufficient disk space for storage
- Network connectivity between all nodes
- Stable system time across nodes

## Future Enhancements

- Web-based user interface
- Enhanced security with authentication and encryption
- Distributed locking for concurrent file access
- Improved conflict resolution mechanisms
- Dynamic load balancing across nodes

## Contributors

- [pranayh24](https://github.com/pranayh24)

*Last updated: 2025-03-28*
