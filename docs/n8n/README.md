# Loopin notification workflow

Start the stack with `docker compose up`, open n8n on port 5678, and import
`loopin-notifications.workflow.json`. Activate the workflow, set
`N8N_NOTIFICATIONS_ENABLED=true`, and restart the API.

The API sends every persisted notification through a durable outbox. Delivery is
at-least-once, so downstream workflow steps should deduplicate on `deliveryId` or
the `Idempotency-Key` header.

For non-local deployments, configure Header Auth on the Webhook node to require
`Authorization: Bearer <N8N_NOTIFICATION_WEBHOOK_SECRET>`. Add email, push,
WebSocket, or other workflow branches after the Webhook node and keep the
response node fast so the API can mark the delivery successful.
