import { sleep } from 'k6';
import { setup as smokeSetup } from './smoke.js';
import { compactSummary, loadScenario, sharedThresholds } from './helpers/config.js';
import { runReadOnlyFlows } from './helpers/flows.js';

export const options = {
  scenarios: loadScenario(),
  thresholds: sharedThresholds(),
  summaryTrendStats: ['avg', 'min', 'med', 'p(90)', 'p(95)', 'p(99)', 'max'],
  userAgent: 'loopin-k6-load/1.0',
};

export function setup() {
  return smokeSetup();
}

export default function (state) {
  runReadOnlyFlows(state);
  sleep(Math.random() * 1.5 + 0.25);
}

export function handleSummary(data) {
  return compactSummary(data, 'load');
}
