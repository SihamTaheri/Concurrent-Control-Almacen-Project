# Concurrent-Control-Almacen-Project
Concurrent Control Almacen Project

This project implements a control system for managing inventory operations in a warehouse using Java concurrency features, particularly the JCSP (Java Communicating Sequential Processes) library.

Usage:
------

To use the project:

1. Import the project files into your Java development environment.
2. Ensure you have the JCSP library added to your project dependencies.
3. Compile the Java files.
4. Run the ControlAlmacenCSP class, which serves as the main entry point for the control system.

Features:
---------

- **Concurrent Operations:** The control system handles multiple concurrent operations such as buying, delivering, returning, offering to restock, and restocking products in the warehouse.
- **Communication Channels:** Utilizes JCSP channels for inter-process communication between the client and server components of the control system.
- **Thread Safety:** Ensures thread safety and synchronization of access to shared resources using Java concurrency constructs.
- **Preconditions Handling:** Verifies preconditions before executing operations and throws exceptions if preconditions are not met.
- **Buffering and Queuing:** Implements buffering and queuing mechanisms for managing pending operations during high load or insufficient stock scenarios.
