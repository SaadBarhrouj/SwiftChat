# SwiftChat - Java Command-Line Chat Application

![Java](https://img.shields.io/badge/Java-≥8-blue?logo=java&logoColor=white)
![MySQL](https://img.shields.io/badge/MySQL-DB-orange?logo=mysql&logoColor=white)
![Status](https://img.shields.io/badge/Status-Development-yellow)

## Overview

SwiftChat is a robust command-line chat application developed in Java. It implements a client-server architecture using TCP sockets for real-time communication. The application supports user authentication, contact management, private messaging, group chats, and file sharing capabilities. Data persistence is handled through a MySQL database.

This project serves as a foundation for a feature-rich chat system, with plans for future enhancements including graphical user interfaces and end-to-end encryption.

## Key Features (Current Implementation)

*   **Client-Server Architecture:** Utilizes Java TCP Sockets (`java.net`) for reliable communication between clients and the server.
*   **Multi-threaded Server:** Handles multiple client connections concurrently using `ClientHandler` threads.
*   **User Authentication:**
    *   Secure Sign Up with email and password validation.
    *   User Login.
*   **Contact Management:**
    *   Add contacts by email.
    *   Assign custom nicknames to contacts.
    *   List contacts with online/offline status.
    *   Delete contacts.
    *   Update contact nicknames.
*   **Private Chat:**
    *   Real-time one-on-one text messaging.
    *   View chat history.
*   **Group Chat:**
    *   Create new groups (user becomes admin).
    *   Join existing public groups by name.
    *   Add/Remove members (Admin only).
    *   List groups the user is a member of.
    *   List members of a specific group with online status and admin indication.
    *   Leave groups.
    *   Real-time group messaging.
    *   View group chat history.
*   **File Transfer:**
    *   Upload files from the client to the server within private or group chats.
    *   Download files shared in chats to the client's Desktop (or home directory).
    *   Attempt to view/open downloaded files automatically on the client.
*   **Message Management:**
    *   Delete messages sent by the user.
    *   View message history with timestamps.
*   **Profile Management:**
    *   Update user's name.
    *   Update user's email address (checks for availability).
    *   Update user's password (with confirmation).
*   **Online Presence:**
    *   Tracks user online/offline status.
    *   Notifies contacts when a user logs out.
*   **Offline Messaging:**
    *   Stores messages/file notifications sent to offline users.
    *   Delivers pending notifications upon user login.
*   **Persistence:**
    *   Uses MySQL database (`swiftchat`) to store user accounts, contacts, groups, messages, and relationships.
    *   Uses Java Serialization as a backup mechanism for user contacts (`.ser` files).
*   **Console Interface:**
    *   Menu-driven navigation using ANSI colors for better readability.
    *   Clear prompts and feedback messages.

## Technology Stack

*   **Language:** Java (Requires JDK 8 or higher)
*   **Networking:** Java TCP Sockets (`java.net`)
*   **Database:** MySQL (using JDBC `java.sql`)
*   **Concurrency:** Java Threads
*   **Persistence:** JDBC, Java Serialization
*   **Utilities:** ANSI escape codes for console colors
