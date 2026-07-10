Loopin serves versioned HTTP endpoints under the `/api/v1` context path. Examples in this document omit the host and show paths relative to `/api/v1`.

Interactive OpenAPI documentation (Swagger UI) is available at `/api/swagger-ui.html` to inspect and test endpoints. To authenticate requests in Swagger UI:
1. Click the **Authorize** button.
2. Enter your JWT Bearer token in the input field.
3. Protected endpoints will automatically include the `Authorization: Bearer <token>` header.

Use JSON for request and response bodies unless noted otherwise:

```http
Content-Type: application/json
```

Protected endpoints require a bearer token:

```http
Authorization: Bearer <token>
```

The examples use placeholder IDs and tokens only. Do not commit real JWTs, Google ID tokens, production hostnames, or credentials.

## Common Conventions

| Item | Convention |
| --- | --- |
| Public resource IDs | UUID strings exposed in API paths and DTOs. |
| Pagination | Spring pageable parameters such as `page`, `size`, and `sort`. |
| Date-time values | ISO-8601 local date-time strings, for example `2026-08-15T10:00:00`. |
| Auth failures | Missing or invalid bearer tokens return `401 Unauthorized`. |
| Authorization failures | Authenticated users without access return `403 Forbidden`. |

## Authentication

### Google Login

`POST /auth/google`

Validates a Google ID token and returns a Loopin JWT.

Request:

```json
{
  "idToken": "<google-id-token>"
}
```

Response `200 OK`:

```json
{
  "token": "<jwt>",
  "email": "alex@example.test",
  "name": "Alex Smith",
  "role": "USER"
}
```

## Users

### Register User

`POST /users/register`

Creates a local user record. This endpoint is public.

Request:

```json
{
  "email": "alex@example.test",
  "name": "Alex Smith"
}
```

Response `201 Created`:

```json
{
  "id": "11111111-1111-4111-8111-111111111111",
  "email": "alex@example.test",
  "name": "Alex Smith",
  "role": "USER"
}
```

### Get Current User

`GET /users/me`

Requires authentication.

Response `200 OK`:

```json
{
  "id": "11111111-1111-4111-8111-111111111111",
  "email": "alex@example.test",
  "name": "Alex Smith",
  "role": "USER"
}
```

### Admin User Operations

The following endpoints require an administrator role:

| Method | Path | Purpose |
| --- | --- | --- |
| `GET` | `/users` | List users. |
| `GET` | `/users/{id}` | Read one user. |
| `PUT` | `/users/{id}/role` | Update user role. |
| `DELETE` | `/users/{id}` | Delete user. |

## User Profile

### Get My Profile

`GET /me`

Response `200 OK`:

```json
{
  "id": "11111111-1111-4111-8111-111111111111",
  "name": "Alex Smith",
  "city": "Baku",
  "bio": "Enjoys startup events and small-group meetups.",
  "interests": []
}
```

### Update My Profile

`PUT /me`

Request:

```json
{
  "name": "Alex Smith",
  "city": "Baku",
  "bio": "Enjoys startup events and small-group meetups."
}
```

Response `200 OK` returns the updated profile.

### My Interests And Badges

| Method | Path | Purpose |
| --- | --- | --- |
| `GET` | `/me/interests` | List current user's interests. |
| `PUT` | `/me/interests` | Replace current user's interests. |
| `GET` | `/me/badges` | List current user's earned badges. |

Update interests request:

```json
{
  "interests": [
    {
      "interestId": "44444444-4444-4444-8444-444444444444",
      "weight": 1.0,
      "source": "USER"
    }
  ]
}
```

## Interests

### List Interests

`GET /interests`

Requires authentication.

Response `200 OK`:

```json
[
  {
    "id": "44444444-4444-4444-8444-444444444444",
    "name": "Technology",
    "slug": "technology",
    "category": "Professional"
  }
]
```

## Events

Event enum values:

| Field | Values |
| --- | --- |
| `type` | `EVENT`, `ACTIVITY` |
| `category` | `TECH`, `STARTUP`, `HR`, `EDUCATION`, `TRAVEL`, `SPORT`, `SOCIAL`, `LANGUAGE`, `CREATIVE`, `OTHER` |
| `status` | `DRAFT`, `PUBLISHED`, `COMPLETED`, `CANCELLED` |

### List Published Events

`GET /events`

This endpoint is public.

Optional query parameters:

| Parameter | Description |
| --- | --- |
| `type` | Filter by `EVENT` or `ACTIVITY`. |
| `category` | Filter by event category. |
| `city` | Filter by city. |
| `isFree` | Filter by free or paid events. |
| `search` | Search title or description. |
| `startDate` | ISO date lower bound, for example `2026-08-01`. |
| `endDate` | ISO date upper bound, for example `2026-08-31`. |
| `page`, `size`, `sort` | Spring pageable controls. |

Response `200 OK` is a Spring page containing event items.

### Get Published Event

`GET /events/{id}`

This endpoint is public.

### Get Recommended Events

`GET /events/recommended?limit=10`

Requires authentication.

### Create Event

`POST /events`

Requires authentication.

Request:

```json
{
  "title": "Founder Coffee Chat",
  "description": "Small-group coffee meetup for early-stage founders.",
  "type": "EVENT",
  "category": "STARTUP",
  "city": "Baku",
  "address": "Nizami Street 10",
  "startDateTime": "2026-08-15T10:00:00",
  "endDateTime": "2026-08-15T12:00:00",
  "isFree": true,
  "price": 0,
  "organizerName": "Loopin Community",
  "imageUrl": "https://example.test/images/founder-coffee.jpg",
  "status": "PUBLISHED",
  "interestIds": [
    "44444444-4444-4444-8444-444444444444"
  ]
}
```

Response `201 Created`:

```json
{
  "id": "55555555-5555-4555-8555-555555555555",
  "title": "Founder Coffee Chat",
  "description": "Small-group coffee meetup for early-stage founders.",
  "type": "EVENT",
  "category": "STARTUP",
  "city": "Baku",
  "address": "Nizami Street 10",
  "startDateTime": "2026-08-15T10:00:00",
  "endDateTime": "2026-08-15T12:00:00",
  "isFree": true,
  "price": 0,
  "organizerName": "Loopin Community",
  "imageUrl": "https://example.test/images/founder-coffee.jpg",
  "status": "PUBLISHED",
  "interests": [],
  "createdAt": "2026-07-07T12:00:00",
  "updatedAt": "2026-07-07T12:00:00"
}
```

### Update Event

`PUT /events/{id}`

Requires authentication and ownership.

### Delete Event

`DELETE /events/{id}`

Requires authentication and ownership. Returns `204 No Content`.

## Groups

Group enum values:

| Field | Values |
| --- | --- |
| `groupSize` | `TWO`, `THREE`, `FOUR`, `FOUR_PLUS` |
| `status` | `OPEN`, `FULL`, `ARCHIVED`, `CANCELLED` |

### Create Group

`POST /groups`

Requires authentication.

Request:

```json
{
  "eventId": "55555555-5555-4555-8555-555555555555",
  "title": "Founder Coffee Table",
  "groupSize": "FOUR_PLUS",
  "maxMembers": 8,
  "groupNote": "Meet near the main entrance five minutes early."
}
```

Response `201 Created`:

```json
{
  "id": "22222222-2222-4222-8222-222222222222",
  "eventId": "55555555-5555-4555-8555-555555555555",
  "adminId": "11111111-1111-4111-8111-111111111111",
  "adminUsername": "Alex Smith",
  "title": "Founder Coffee Table",
  "groupSize": "FOUR_PLUS",
  "maxMembers": 8,
  "status": "OPEN",
  "groupNote": "Meet near the main entrance five minutes early.",
  "memberCount": 1,
  "createdAt": "2026-07-07T12:00:00"
}
```

### Read And Update Groups

| Method | Path | Auth | Purpose |
| --- | --- | --- | --- |
| `GET` | `/groups/{groupId}` | Public | Read one group. |
| `PUT` | `/groups/{groupId}` | Required | Update title, size, max members, and note. |
| `PATCH` | `/groups/{groupId}/status` | Required | Update group status. |

Update group request:

```json
{
  "title": "Founder Coffee Table",
  "groupSize": "FOUR_PLUS",
  "maxMembers": 10,
  "groupNote": "Bring one question for the group."
}
```

Update group status request:

```json
{
  "status": "FULL"
}
```

## Group Members

| Method | Path | Purpose |
| --- | --- | --- |
| `POST` | `/groups/{groupId}/members` | Add a member. |
| `GET` | `/groups/{groupId}/members` | List group members. |
| `GET` | `/groups/{groupId}/members/{userId}` | Read a group member by user ID. |
| `DELETE` | `/groups/{groupId}/members/{userId}` | Remove a member. |

Writes require authentication and appropriate group permissions.

## Group Join Requests

Join request status values are `PENDING`, `APPROVED`, and `REJECTED`.

### Submit Join Request

`POST /groups/{groupId}/join-requests`

Requires authentication.

Request:

```json
{
  "message": "I would like to join and can arrive on time."
}
```

Response `201 Created`:

```json
{
  "id": "33333333-3333-4333-8333-333333333333",
  "groupId": "22222222-2222-4222-8222-222222222222",
  "userId": "11111111-1111-4111-8111-111111111111",
  "status": "PENDING",
  "message": "I would like to join and can arrive on time.",
  "createdAt": "2026-07-07T12:05:00"
}
```

### Manage Join Requests

| Method | Path | Purpose |
| --- | --- | --- |
| `GET` | `/groups/{groupId}/join-requests` | List requests for a group. |
| `GET` | `/groups/{groupId}/join-requests/{requestId}` | Read one request. |
| `GET` | `/me/group-join-requests` | List current user's requests. |
| `PATCH` | `/groups/{groupId}/join-requests/{requestId}/approve` | Approve a request. |
| `PATCH` | `/groups/{groupId}/join-requests/{requestId}/reject` | Reject a request. |
| `DELETE` | `/groups/{groupId}/join-requests/{requestId}` | Delete a request. |

All join request endpoints require authentication. Approval and rejection require group admin permissions.

## Chat

Loopin supports persisted group chat messages through REST and real-time delivery through WebSocket. See [Real-Time Chat](REALTIME_CHAT.md) for the WebSocket protocol.

The REST chat controller currently uses the internal numeric group ID in the path.

### List Group Messages

`GET /groups/{groupId}/messages`

Requires authentication and group membership.

Response `200 OK`:

```json
[
  {
    "id": 1,
    "groupId": 1,
    "senderId": 1,
    "senderName": "Alex Smith",
    "messageText": "Looking forward to meeting everyone.",
    "createdAt": "2026-07-07T12:10:00"
  }
]
```

### Send Group Message

`POST /groups/{groupId}/messages`

Requires authentication and group membership.

Request:

```json
{
  "messageText": "Looking forward to meeting everyone."
}
```

Response `201 Created` returns the persisted message.

## Reports And Moderation

Report target values are `GROUP` and `MESSAGE`. Report status values are `PENDING`, `REVIEWED`, `RESOLVED`, and `DISMISSED`.

### Create Report

`POST /reports`

Requires authentication.

Request:

```json
{
  "targetType": "GROUP",
  "targetId": "22222222-2222-4222-8222-222222222222",
  "reason": "Unsafe coordination",
  "details": "The group note asks users to move the conversation to an unsafe channel."
}
```

Response `201 Created`:

```json
{
  "id": "66666666-6666-4666-8666-666666666666",
  "reporterId": "11111111-1111-4111-8111-111111111111",
  "targetType": "GROUP",
  "targetId": "22222222-2222-4222-8222-222222222222",
  "reason": "Unsafe coordination",
  "details": "The group note asks users to move the conversation to an unsafe channel.",
  "status": "PENDING",
  "createdAt": "2026-07-07T12:15:00",
  "updatedAt": "2026-07-07T12:15:00"
}
```

### Review Reports

`GET /admin/reports?status=PENDING&page=0&size=10`

Requires the `ADMIN` role and returns a Spring page of reports.

### Update Report Status

`PATCH /admin/reports/{id}`

Requires the `ADMIN` role.

Request:

```json
{
  "status": "REVIEWED"
}
```

## Admin

### Dashboard Statistics

`GET /admin/dashboard/stats`

Requires the `ADMIN` role.

Response `200 OK`:

```json
{
  "totalUsers": 125,
  "activeUsers": 120,
  "totalEvents": 48,
  "publishedEvents": 42,
  "activeGroups": 18
}
```

### Admin Users And Events

All endpoints in this table require the `ADMIN` role.

| Method | Path | Purpose |
| --- | --- | --- |
| `GET` | `/admin/users` | List users with pagination. |
| `PUT` | `/admin/users/{id}/role` | Update a user role. |
| `DELETE` | `/admin/users/{id}` | Delete a user. |
| `GET` | `/admin/events` | List events, optionally filtered by `status`. |
| `DELETE` | `/admin/events/{id}` | Delete an event. |
| `GET` | `/admin/moderation/pending` | List content awaiting moderation review. |
| `PATCH` | `/admin/moderation/events/{id}/approve` | Approve a pending event and publish it. |
| `PATCH` | `/admin/moderation/events/{id}/reject` | Reject a pending event; accepts an optional `reason`. |
