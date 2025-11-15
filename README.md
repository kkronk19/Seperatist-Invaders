*Separatist Invaders*
A modular, object-oriented Java game inspired by classic arcade shooters

OVERVIEW
Separatist Invaders is a Java-based 2D shooter that launches from a central main entry
point. The program opens with a title screen featuring three options:

<img width="1181" height="668" alt="image" src="https://github.com/user-attachments/assets/d55946d6-c7bf-43aa-a794-6cf632eda2ca" />

Play — currently closes the title panel (gameplay coming soon)

Sandbox Mode — a live testing environment for mechanics, entities, and new features

<img width="1190" height="671" alt="image" src="https://github.com/user-attachments/assets/6bbdf671-fbab-48c8-9289-bdd4119f41f7" />


Exit — closes the program

The purpose of this project is to create a flexible, extensible game architecture that can grow over time with additional content, mechanics, and systems.

Architecture & Design Approach
This project began as an unorganized shell of code. I fully refactored the structure using object-oriented programming (OOP) best practices, with emphasis on:

* Single Responsibility Principle (SRP)
Each class now has one clear purpose (e.g., Player, Enemy, Weapon, InputHandler, SceneManager).

* Open/Closed Principle (OCP)
Systems are open for extension but closed for modification—new weapons, entities, or behaviors can be added without rewriting core logic.

* Modular Packages
The project is divided into logical packages:
core/ – game loop, engine logic
app/ – entry point and scene management
features/ – expandable gameplay mechanics
weapons/ – weapon types, projectile logic
services/ – reusable utility services
ui/ – menus, panels, display logic
input/ – input handling
resources/ – images, audio (future)
This structure makes the code easier to maintain, test, and expand.

Current Features
* Title Screen
Play button
Sandbox button (fully functional)
Exit button

* Sandbox Mode
Live environment for development
Testing area for sprite movement, collisions, behaviors, and weapons
Used for rapid iteration during feature development

* Clean OOP Architecture
Refactored from a single-file mess into clean modular subsystems

******How to Run******
1. Clone the repository:
git clone https://github.com/kkronk19/Seperatist-Invaders.git
2. Open the project in your preferred IDE (VS Code, IntelliJ, Eclipse).
3. Navigate to:
src/app/Main.java
4. Run the main method to start the program.

 Roadmap / Future Development
I plan to continue expanding this project significantly. Upcoming features include:

Gameplay Features

Full playable main mode

Enemy waves, bosses, scoring

Multiple weapon types

Player upgrades

Networking & Co-op

Peer-to-peer or dedicated server gameplay

Co-op missions

Networking using Java sockets or modern APIs (WebSockets, Netty, etc.)

API Integrations

Cloud-saved high scores

Matchmaking or player statistics

Daily challenges

Integration with cyber or security-themed APIs
(I may explore threat-intel or cybersecurity-related gameplay mechanics.)

Technical Expansion

Asset loading system

Better sprite rendering pipeline

Sound/music engine

Physics improvements

AI behaviors for enemies
