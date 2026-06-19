# Guptrix

A secure, modern note-taking and productivity platform built with Next.js, React, Express, TypeScript, and MongoDB.

Guptrix provides authenticated note management, rich content editing, drawing support, secure account management, and cross-device accessibility through both web and Android applications.

---

## Overview

Guptrix is designed to provide a simple yet powerful workspace for creating, organizing, and managing notes.

The platform combines modern frontend technologies with a secure backend architecture, delivering a fast and responsive user experience while maintaining account security through JWT authentication and OTP verification.

---

## Key Features

### Authentication & Security

* JWT-based authentication
* Refresh token support
* OTP verification
* Password reset workflow
* Protected routes
* Session persistence
* Password hashing with bcrypt

### Notes Management

* Create notes
* Edit notes
* Delete notes
* Restore deleted notes
* Trash management
* Rich text content support

### Advanced Editing

* Drawing support
* Excalidraw integration
* Interactive note components
* Rich editor experience

### User Experience

* Dark mode
* Responsive design
* Mobile-friendly interface
* Fast page navigation
* React Query caching
* Zustand state management

### Android Application

* Native Android wrapper
* Android 6.0+ support
* ARM32 support
* ARM64 support
* Universal APK support
* Download handling
* File upload support
* Offline detection
* Fullscreen media support

---

## Technology Stack

### Frontend

* Next.js
* React 19
* Tailwind CSS
* Zustand
* React Query
* Axios
* Radix UI

### Backend

* Node.js
* Express.js
* TypeScript

### Database

* MongoDB
* Mongoose

### Android

* Kotlin
* Android WebView
* Material Design Components

---

## Project Structure

```text
Guptrix/
├── Guptrix_FE/
│   ├── app/
│   ├── components/
│   ├── lib/
│   └── middleware
│
├── Guptrix_BE/
│   ├── src/
│   │   ├── controller/
│   │   ├── models/
│   │   ├── router/
│   │   └── middlewares/
│
└── android-app/
```

---

## Security

The platform implements multiple layers of protection:

* JWT authentication
* Refresh token rotation
* Protected route middleware
* Secure password hashing
* OTP verification
* HTTPS deployment
* Hardened Android WebView configuration
* Restricted file access policies

---

## Encryption & Privacy

User credentials are never stored in plaintext.

Passwords are securely hashed before storage.

Authentication relies on signed JSON Web Tokens and protected backend validation.

Sensitive operations require authenticated access and route-level verification.

---

## Android Release Information

Package Name:

```text
com.guptrix.app
```

Minimum Android Version:

```text
Android 6.0 (API 23)
```

Target Android Version:

```text
Android 14+
```

Supported Architectures:

```text
armeabi-v7a
arm64-v8a
universal
```

---

## Deployment

Frontend can be deployed using:

* Netlify
* Vercel
* Cloudflare Pages

Backend can be deployed using:

* VPS
* Docker
* Railway
* Render
* Cloud Platforms

Database:

* MongoDB Atlas
* Self-hosted MongoDB

---

## Development

Frontend:

```bash
cd Guptrix_FE
npm install
npm run dev
```

Backend:

```bash
cd Guptrix_BE
npm install
npm run dev
```

Android:

```bash
cd android-app
./gradlew assembleDebug
```

Release:

```bash
./gradlew assembleRelease
```

---

## Disclaimer

This software is provided as-is without warranty.

Users and deployers are responsible for securing deployment environments, managing backups, protecting signing keys, and complying with applicable privacy and data-protection regulations.

---

## Author

Ritik Thakur

Website:
https://guptrix.netlify.app

GitHub:

https://github.com/ritikthakur22

---

## Acknowledgements

Built using open-source technologies including:

* Next.js
* React
* Express
* MongoDB
* TypeScript
* Tailwind CSS
* Excalidraw
* Android SDK
* Material Design

