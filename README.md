# Distributed File System (DFS)

A Java-based distributed file system implementation with a modern command-line interface. This system allows for file and directory operations across a network, making it easy to manage files in a distributed environment.

### Important Notice

- This project is currently under development and not yet finished. As a result, users may encounter errors or incomplete features.



## 🌟 Features

### Core Functionality
- **File Operations**
  - Upload files to remote storage
  - Download files from remote storage
  - Move/Rename files and directories

### Directory Management
- Create new directories
- Delete existing directories
- List directory contents with detailed information
  - File/Directory name
  - Type (FILE/DIR)
  - Size (human-readable format)
  - Last modified timestamp

### Modern CLI Interface
- Interactive command-line menu with ASCII borders
- Color-coded output for better readability
- Progress bars for file operations
- Human-readable file sizes
- Command history support
- Error handling with visual feedback

## 🛠️ Technical Details

### System Architecture
- **Client-Server Model**: Implements a distributed architecture
- **Multi-threaded Server**: Handles multiple client connections simultaneously
- **Chunk-based File Transfer**: Efficient handling of large files
- **Checksum Verification**: Ensures data integrity during transfer

### Built With
- Java (100%)
- JLine3 for enhanced CLI capabilities
- Socket Programming for network communication
- Concurrent data structures for thread safety
