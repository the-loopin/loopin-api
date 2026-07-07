import http from 'k6/http';
import { check, fail, sleep } from 'k6';
import { resolveAuthToken } from './helpers/auth.js';
import { compactSummary, config, isProductionTarget, sharedThresholds, smokeScenario } from './helpers/config.js';
import { apiUrl, get, expectStatus } from './helpers/http.js';
import { discoverEventId, runFullFlowSet } from './helpers/flows.js';

export const options = {
  scenarios: smokeScenario(),
  thresholds: sharedThresholds(),
  summaryTrendStats: ['avg', 'min', 'med', 'p(90)', 'p(95)', 'p(99)', 'max'],
  userAgent: 'loopin-k6-smoke/1.0',
};

export function setup() {
  if (isProductionTarget() && !config.allowProductionTarget) {
    fail('Refusing to run k6 against TARGET_ENV=production.');
  }

  if (config.runMutatingFlows && isProductionTarget()) {
    fail('RUN_MUTATING_FLOWS is blocked for production targets.');
  }

  const probe = http.get(apiUrl('/events', { page: 0, size: 1 }), {
    timeout: config.timeout,
    tags: { endpoint: 'startup_probe', flow: 'setup' },
  });

  check(probe, {
    'startup probe reachable': (response) => response.status > 0 && response.status < 500,
  });

  const discovery = get('events_setup_discovery', '/events', {
    flow: 'setup',
    query: { page: 0, size: 1 },
  });

  let publicEventId = config.publicEventId;
  if (expectStatus(discovery, 'events_setup_discovery', 200) && !publicEventId) {
    publicEventId = discoverEventId(discovery);
  }

  return {
    authToken: resolveAuthToken(),
    publicEventId,
  };
}

export default function (state) {
  runFullFlowSet(state);
  sleep(Math.random() + 0.25);
}

export function handleSummary(data) {
  return compactSummary(data, 'smoke');
}
