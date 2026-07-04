# ADR 0004: Use JWT Stateless Auth

## Status
Approved

## Context
The application needs to authenticate users and protect sensitive user actions. Traditional session-based authentication requires keeping state on the server, which complicates horizontal scaling across multiple container nodes and mobile client integrations.

## Decision
We chose stateless token-based authentication using **JSON Web Tokens (JWT)**.

## Consequences
* **Horizontal Scalability:** The backend servers do not need to share session state; each request carries its own authenticated identity in the JWT.
* **Mobile Client Friendliness:** Decoupling session cookies allows native mobile applications to store the token in secure storage and append it as a header.
* **Validation Overhead:** The server must validate the signature of the token on each request, requiring a shared cryptographic secret key.
* **Revocation Limits:** Since the tokens are stateless, they cannot be revoked on-demand without additional architecture (e.g. maintaining a Redis-backed token blacklist or using short-lived access tokens with refresh tokens).
