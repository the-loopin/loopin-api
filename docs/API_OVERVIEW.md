# API Endpoint Reference

All API requests are prefixed with the base path `/api`. Public resource IDs in paths and JSON payloads are UUID strings; internal database IDs are not exposed. Secure endpoints require an `Authorization` header containing a valid JWT:
```http
Authorization: Bearer <your_jwt_token>
```

---

## Authentication

### 1. Google Social Auth
* **Endpoint:** `POST /auth/google`
* **Headers:** `Content-Type: application/json`
* **Request Body:**
  ```json
  {
    "googleId": "103859205847392019485",
    "email": "user@gmail.com",
    "name": "Jane Doe",
    "imageUrl": "https://lh3.googleusercontent.com/a/photo.jpg"
  }
  ```
* **Success Response (200 OK):**
  ```json
  {
    "accessToken": "eyJhbGciOiJIUzI1NiIsIn...",
    "tokenType": "Bearer",
    "expiresIn": 86400000,
    "user": {
      "id": "11111111-1111-4111-8111-111111111111",
      "email": "user@gmail.com",
      "name": "Jane Doe",
      "role": "USER"
    }
  }
  ```

---

##  Users & Registration

### 1. Register Local User
* **Endpoint:** `POST /users/register`
* **Request Body:**
  ```json
  {
    "name": "Alex Smith",
    "email": "alex@example.com",
    "password": "SecurePassword123!"
  }
  ```
* **Success Response (201 Created):**
  ```json
  {
    "id": "22222222-2222-4222-8222-222222222222",
    "name": "Alex Smith",
    "email": "alex@example.com",
    "role": "USER",
    "isActive": true
  }
  ```

### 2. Get My User Info
* **Endpoint:** `GET /users/me`
* **Headers:** `Authorization: Bearer <token>`
* **Success Response (200 OK):**
  ```json
  {
    "id": "22222222-2222-4222-8222-222222222222",
    "name": "Alex Smith",
    "email": "alex@example.com",
    "role": "USER",
    "isActive": true
  }
  ```

---

##  User Profiles & Badges

### 1. Get My Profile Details
* **Endpoint:** `GET /me`
* **Headers:** `Authorization: Bearer <token>`
* **Success Response (200 OK):**
  ```json
  {
    "id": "22222222-2222-4222-8222-222222222222",
    "name": "Alex Smith",
    "city": "San Francisco",
    "bio": "Tech enthusiast and avid hiker."
  }
  ```

### 2. Update My Profile
* **Endpoint:** `PUT /me`
* **Headers:** `Authorization: Bearer <token>`, `Content-Type: application/json`
* **Request Body:**
  ```json
  {
    "name": "Alex Smith Jr.",
    "city": "Oakland",
    "bio": "Event organizer & visual designer."
  }
  ```
* **Success Response (200 OK):**
  ```json
  {
    "id": "22222222-2222-4222-8222-222222222222",
    "name": "Alex Smith Jr.",
    "city": "Oakland",
    "bio": "Event organizer & visual designer."
  }
  ```

### 3. Get My Earned Badges
* **Endpoint:** `GET /me/badges`
* **Headers:** `Authorization: Bearer <token>`
* **Success Response (200 OK):**
  ```json
  [
    "EARLY_ADOPTER",
    "GROUP_LEADER"
  ]
  ```

---

##  Events Discovery

### 1. List Published Events
* **Endpoint:** `GET /events`
* **Query Parameters (Optional):**
  * `type`: Filter by format (`PHYSICAL`, `ONLINE`)
  * `category`: Filter by theme (`TECH`, `MUSIC`, `SPORTS`, `ART`, `FOOD`)
  * `city`: e.g. `San Francisco`
  * `isFree`: `true` or `false`
  * `search`: Matches title/description keywords
  * `startDate` / `endDate`: Filter by ISO date e.g. `2026-07-04`
* **Success Response (200 OK):**
  ```json
  [
    {
      "id": "aaaaaaaa-aaaa-4aaa-8aaa-aaaaaaaaaaaa",
      "title": "Outdoor Rock Concert",
      "description": "Come join us for live music under the stars.",
      "type": "PHYSICAL",
      "category": "MUSIC",
      "city": "Oakland",
      "address": "123 Concert Way",
      "startDateTime": "2026-08-15T19:00:00",
      "endDateTime": "2026-08-15T23:00:00",
      "isFree": false,
      "price": 25.00,
      "organizerName": "Oakland Sounds",
      "imageUrl": "https://picsum.photos/600",
      "status": "PUBLISHED"
    }
  ]
  ```

### 2. Create Event
* **Endpoint:** `POST /events`
* **Headers:** `Authorization: Bearer <token>`
* **Request Body:**
  ```json
  {
    "title": "Board Games Night",
    "description": "Weekly board games gathering at the local cafe.",
    "type": "PHYSICAL",
    "category": "SPORTS",
    "city": "Oakland",
    "address": "456 Tabletop Rd",
    "startDateTime": "2026-07-20T18:00:00",
    "endDateTime": "2026-07-20T22:00:00",
    "isFree": true,
    "price": 0.00,
    "organizerName": "Oakland Gamers Club",
    "imageUrl": "https://picsum.photos/400"
  }
  ```
* **Success Response (201 Created):**
  ```json
  {
    "id": "bbbbbbbb-bbbb-4bbb-8bbb-bbbbbbbbbbbb",
    "title": "Board Games Night",
    ...
    "status": "PUBLISHED"
  }
  ```

---

##  Event Groups

### 1. Create Event Group
* **Endpoint:** `POST /groups`
* **Headers:** `Authorization: Bearer <token>`
* **Request Body:**
  ```json
  {
    "eventId": "aaaaaaaa-aaaa-4aaa-8aaa-aaaaaaaaaaaa",
    "title": "East Bay Concert Buddies",
    "groupSize": "MEDIUM",
    "maxMembers": 8,
    "groupNote": "Be friendly and bring positive vibes!"
  }
  ```
* **Success Response (201 Created):**
  ```json
  {
    "id": "55555555-5555-4555-8555-555555555555",
    "eventId": "aaaaaaaa-aaaa-4aaa-8aaa-aaaaaaaaaaaa",
    "adminId": "22222222-2222-4222-8222-222222222222",
    "title": "East Bay Concert Buddies",
    "groupSize": "MEDIUM",
    "maxMembers": 8,
    "status": "ACTIVE",
    "groupNote": "Be friendly and bring positive vibes!"
  }
  ```

### 2. Update Group Status
* **Endpoint:** `PATCH /groups/{groupId}/status`
* **Headers:** `Authorization: Bearer <token>`
* **Request Body:**
  ```json
  {
    "status": "FULL"
  }
  ```
* **Success Response (200 OK):**
  ```json
  {
    "id": "55555555-5555-4555-8555-555555555555",
    "status": "FULL",
    ...
  }
  ```

---

##  Group Join Requests

### 1. Submit Join Request
* **Endpoint:** `POST /groups/{groupId}/join-requests`
* **Headers:** `Authorization: Bearer <token>`
* **Request Body:**
  ```json
  {
    "message": "Hey! I would love to tag along. I am buying my ticket tonight."
  }
  ```
* **Success Response (201 Created):**
  ```json
  {
    "id": "14141414-1414-4141-8141-141414141414",
    "groupId": "55555555-5555-4555-8555-555555555555",
    "userId": "33333333-3333-4333-8333-333333333333",
    "status": "PENDING",
    "message": "Hey! I would love to tag along. I am buying my ticket tonight.",
    "createdAt": "2026-07-04T23:35:00"
  }
  ```

### 2. Approve Request (Group Admin Only)
* **Endpoint:** `PATCH /groups/{groupId}/join-requests/{requestId}/approve`
* **Headers:** `Authorization: Bearer <token>`
* **Success Response (200 OK):**
  ```json
  {
    "id": "14141414-1414-4141-8141-141414141414",
    "status": "APPROVED",
    ...
  }
  ```

---

##  Administrative Controls (Admin Role Only)

### 1. Get Dashboard Statistics
* **Endpoint:** `GET /admin/dashboard/stats`
* **Headers:** `Authorization: Bearer <admin_token>`
* **Success Response (200 OK):**
  ```json
  {
    "totalUsers": 125,
    "activeUsers": 120,
    "totalEvents": 48,
    "publishedEvents": 42,
    "activeGroups": 18
  }
  ```

