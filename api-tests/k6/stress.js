import { sleep } from 'k6';
import { setup as smokeSetup } from './smoke.js';
import { compactSummary, sharedThresholds, stressScenario } from './helpers/config.js';
import { publicDiscoveryFlow, authenticatedEventsFlow } from './helpers/flows.js';

export const options = {
  scenarios: stressScenario(),
  thresholds: sharedThresholds(),
  summaryTrendStats: ['avg', 'min', 'med', 'p(90)', 'p(95)', 'p(99)', 'max'],
  userAgent: 'loopin-k6-stress/1.0',
};

export function setup() {
  return smokeSetup();
}

export default function (state) {
  publicDiscoveryFlow(state);
  authenticatedEventsFlow(state);
  sleep(Math.random() + 0.1);
}

export function handleSummary(data) {
  return compactSummary(data, 'stress');
}
