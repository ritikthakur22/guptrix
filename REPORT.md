# Guptrix Project Analysis Report

## 1. Framework and Build System
**Frontend:**
- **Framework:** Next.js (App Router), React 19.
- **Build System:** Next.js built-in bundler (`next build`), paired with PostCSS and Tailwind CSS (v4). 

**Backend:**
- **Framework:** Node.js with Express.js (v5).
- **Build System:** Written in TypeScript. Development uses `ts-node` and `nodemon`.

## 2. Architecture
The project follows a decoupled, monorepo-style structure separated into two main directories: `Guptrix_FE` and `Guptrix_BE`.
- **Frontend Architecture:** Utilizes the Next.js App Router (`app` directory). It features a component-driven architecture enriched with state management via `zustand`, data fetching and caching with `@tanstack/react-query`, and accessible UI components powered by Radix UI. Edge middleware (`proxy.ts`) is used for route protection.
- **Backend Architecture:** Follows the classic MVC (Model-View-Controller) pattern. The logic is cleanly separated into `models`, `controller`, `router`, and `middlewares` inside the `Guptrix_BE/src/mvc` directory.

## 3. Routes/Pages
**Public Routes:**
- `/` (Home)
- `/auth/login`
- `/auth/signup`
- `/auth/verify-otp`
- `/auth/forgot-password`
- `/auth/reset-password`

**Private/Protected Routes:**
- `/dashboard`
- `/card`
- `/trash`

## 4. APIs Used
The frontend interacts with the custom backend via Axios. The backend exposes the following main API routes:
- `/api` - General authentication routes (signup, login, OTP).
- `/api/user` - User-related operations.
- `/note` - CRUD operations for notes/cards.
- `/refresh` - JWT refresh token endpoint.

*On the frontend, these API calls are abstracted away in the `Guptrix_FE/app/lib/api/` directory (e.g., `user.ts`, `note.ts`, `auth.ts`).*

## 5. Authentication System
- Uses **JSON Web Tokens (JWT)** for securing APIs.
- Tokens (Access and Refresh) are stored in cookies (`js-cookie` used on the frontend).
- A Next.js Edge Middleware (`proxy.ts`) intercepts navigation. If a user attempts to access private routes without valid tokens, they are redirected to `/auth/login`. If a logged-in user navigates to public auth routes, they are pushed to `/dashboard`.
- Password hashing is implemented on the backend via `bcrypt`.
- Email/OTP verification is utilized during signup and password resets (using `nodemailer`).

## 6. Database Connections
- The backend uses **MongoDB** as its primary database.
- Database interaction is orchestrated using the `mongoose` ODM.
- The connection is initialized in `Guptrix_BE/index.ts` connecting via the `DATABASE_URL` environment variable.

## 7. Payment Integrations
- **None detected**. An analysis of the project's dependencies and source code did not reveal any usage of payment gateways (e.g., Stripe, PayPal, Razorpay).

## 8. Local Storage Usage
`localStorage` is actively utilized on the frontend in the following locations:
- `app/card/_components/Excalidraw.tsx`: Likely to persist drawing canvas states or preferences locally.
- `app/components/ThemeProvider.tsx`: To remember user theme preferences (e.g., light vs dark mode).
- `app/components/ToastProvider.tsx`: Used in conjunction with UI notifications/toasts.
- `app/auth/` (login, signup, reset-password, verify-otp): Likely used to hold temporary user identifiers (like email) across multi-step auth flows before token issuance.

## 9. Service Worker/PWA Setup
- The app possesses a foundational PWA structure via `Guptrix_FE/app/manifest.json`.
- It defines `display: standalone`, theme and background colors, and app icons.
- However, there is no explicit Service Worker (`sw.js`) registration or dedicated PWA bundler plugin (like `next-pwa` or `@serwist/next`) found in `next.config.ts`. It acts primarily as an installable shortcut rather than a fully offline-capable web app.

## 10. Startup and User Interaction Flow
1. **Startup:** Both servers are spun up locally (`Next.js` on port 3000, `Express` on port 4000). The backend establishes a live MongoDB connection.
2. **Access & Middleware:** A user lands on the root `/`. Next.js middleware seamlessly scans their cookies for JWT tokens.
3. **Authentication:** The user registers or logs in via the `/auth` endpoints. `nodemailer` facilitates an OTP sent to the user's email. Post-verification, JWT tokens are granted and lodged in the browser's cookies.
4. **App Interaction:** Navigating to `/dashboard`, the UI fetches data utilizing React Query and Axios. Users can manipulate notes/cards using advanced editors (leveraging `@editorjs` modules and `@excalidraw/excalidraw`). Fluid animations driven by Framer Motion and GSAP enhance the interaction. State updates are localized using Zustand before syncing with the MongoDB backend.

## 11. Files Critical for Android App Conversion
When adapting this web app to an Android App (e.g., via Trusted Web Activities, Capacitor, or a WebView wrapper), the following files are crucial:
- `Guptrix_FE/app/manifest.json`: Defines the mobile identity (name, colors, standalone display mode).
- **Icons:** `Guptrix_FE/app/apple-icon.png`, `favicon.ico`, and `icon1.png` are vital for the app launcher logo.
- `Guptrix_FE/proxy.ts`: The cookie-based middleware mechanism is critical to monitor. Mobile WebViews sometimes handle cookies differently or encounter cross-origin cookie policies that require adjustments.
- *Note:* To achieve a native-feeling Android application, a fully fledged service worker with offline caching strategies would need to be introduced.
