import { fail } from 'k6';
import { config, isProductionTarget } from './config.js';
import { post, expectStatus, parseJson } from './http.js';

export function resolveAuthToken() {
  if (config.authToken) {
    return config.authToken;
  }

  if (!config.googleIdToken) {
    if (config.requireAuth) {
      fail('Authenticated k6 flow requires AUTH_TOKEN or GOOGLE_ID_TOKEN when REQUIRE_AUTH=true.');
    }

    return '';
  }

  if (isProductionTarget()) {
    fail('Refusing to exchange GOOGLE_ID_TOKEN against TARGET_ENV=production. Use a pre-issued AUTH_TOKEN for read-only diagnostics.');
  }

  const response = post('auth_google', '/auth/google', {
    idToken: config.googleIdToken,
  }, {
    flow: 'setup',
  });

  const ok = expectStatus(response, 'auth_google', 200);
  const body = parseJson(response, {});

  if (!ok || !body.token) {
    fail('Google auth setup did not return a JWT.');
  }

  return body.token;
}
