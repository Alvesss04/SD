# Distributed Systems (SD) - NOVA FCT 🌐

**Bachelor in Computer Science and Engineering (LEI) | 3rd Year**

This repository contains the projects developed for the Distributed Systems (Sistemas Distribuídos) course at the Faculty of Sciences and Technology of the NOVA University of Lisbon (NOVA FCT).

## ✍️ Authors
- **Tomás Alves** (Student No. 68681)
- **Miguel Carmo** (Student No. 68264)

## 📌 About the Course and Projects

The Distributed Systems course explores the fundamental principles, design patterns, and technologies behind modern distributed architectures, focusing on scalability, fault tolerance, and secure inter-process communication.

The practical evaluation consists of a single large-scale **Distributed Message Delivery System** (similar to an email infrastructure), developed incrementally across two project phases. Users are organized by domains, and they interact with their local domain servers to send and receive messages across different domains.

---

### 🟢 Phase 1: Core System & Fault Tolerance (Project 1)
The first phase focused on establishing the base distributed architecture, enabling cross-domain communication, service discovery, and resilience against temporary network failures.

* **Documentation:**
  * [Project 1 Statement (MD)](./src/Project1/Project1.md)
* **Location:**
  * [Project 1 Code (CODE)](./src/Project1/)

**Key Features:**
* **Service Architecture:** Divided into three main components per domain: *Users Service*, *Messages Service*, and a *Gateway Server (Proxy)*.
* **Dual API Support:** Full interoperability between RESTful APIs (JAX-RS/Jersey) and gRPC/Protobuf services.
* **Multicast Service Discovery:** A decentralized auto-configuration mechanism using IP Multicast (`226.226.226.226:2266`). Servers periodically announce their URIs (`<service>@<domain>\t<uri>`) to dynamically locate peers without hardcoded addresses.
* **Fault Tolerance & Retries:** Robust handling of network partitions. Background executors retry failed cross-domain deliveries for up to 60-90 seconds.
* **Bounce-back Notifications:** If a message cannot be delivered (due to an unknown user or a persistent timeout), the system generates an automatic "FAILED TO SEND" error message back to the sender's inbox.
* **Database Persistence:** Uses Hibernate ORM and HSQLDB to reliably store users and messages.

---

### 🔵 Phase 2: Security & Advanced Features (Project 2)
The second phase builds directly upon the architecture developed in Phase 1, shifting the focus towards securing the distributed system against malicious actors and unauthorized access.

* **Documentation:**
  * [Project 2 Statement (MD)](./src/Project2/Project2.md)
* **Location:**
  * [Project 2 Code (CODE)](./src/Project2/)

**Key Features:**
* **Secure Communication Channels:** Transitioning from unencrypted channels to secure, encrypted communication between clients and servers, as well as server-to-server communication.
* **Authentication & Authorization:** Implementation of robust security mechanisms to ensure that users can only access their own mailboxes and that domains securely authenticate with one another.
* *(Note: Add any other specific features you implemented in Project 2 here, such as replication, consensus algorithms like ZooKeeper/Kafka, or specific cryptographic protocols).*

---

## 🧠 Concepts and Acquired Skills
* **Distributed Architectures:** Designing loosely coupled, domain-based microservices.
* **Inter-Process Communication:** Implementing and integrating both REST and gRPC communication protocols.
* **Fault Tolerance:** Engineering idempotent operations, retry mechanisms, and asynchronous task execution to survive network failures.
* **Service Discovery:** Building UDP Multicast discovery protocols to eliminate hardcoded server addresses.
* **Cybersecurity in Distributed Systems:** Securing APIs, implementing authentication, and establishing encrypted communication channels.
* **Containerization:** Automating deployment and testing using Docker, custom `sdnet` bridge networks, and Maven plugins.

## 📄 License
 
Copyright © 2026 **Alvesss04**. All Rights Reserved.
