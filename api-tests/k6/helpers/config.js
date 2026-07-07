function envString(name, defaultValue) {
  const value = __ENV[name];
  return value === undefined || value === '' ? defaultValue : value;
}

function envNumber(name, defaultValue) {
  const value = Number(__ENV[name]);
  return Number.isFinite(value) && value > 0 ? value : defaultValue;
}

function envRate(name, defaultValue) {
  const value = Number(__ENV[name]);
  return Number.isFinite(value) && value >= 0 && value <= 1 ? value : defaultValue;
}

function envBoolean(name, defaultValue) {
  const value = __ENV[name];
  if (value === undefined || value === '') {
    return defaultValue;
  }

  return ['1', 'true', 'yes', 'y', 'on'].includes(String(value).toLowerCase());
}

function normalizeBaseUrl(value) {
  return value.replace(/\/+$/, '');
}

function normalizePrefix(value) {
  if (!value) {
    return '';
  }

  const trimmed = value.replace(/^\/+|\/+$/g, '');
  return trimmed ? `/${trimmed}` : '';
}

export const config = {
  baseUrl: normalizeBaseUrl(envString('BASE_URL', 'http://localhost:8080')),
  apiPrefix: normalizePrefix(envString('API_PREFIX', '/api')),
  targetEnv: envString('TARGET_ENV', 'local').toLowerCase(),
  allowProductionTarget: envBoolean('ALLOW_PRODUCTION_TARGET', false),
  timeout: envString('HTTP_TIMEOUT', '10s'),
  authToken: envString('AUTH_TOKEN', ''),
  googleIdToken: envString('GOOGLE_ID_TOKEN', ''),
  requireAuth: envBoolean('REQUIRE_AUTH', false),
  publicEventId: envString('PUBLIC_EVENT_ID', ''),
  publicGroupId: envString('PUBLIC_GROUP_ID', ''),
  authGroupId: envString('AUTH_GROUP_ID', ''),
  runMutatingFlows: envBoolean('RUN_MUTATING_FLOWS', false),
  p95ThresholdMs: envNumber('P95_THRESHOLD_MS', 750),
  p99ThresholdMs: envNumber('P99_THRESHOLD_MS', 1500),
  errorRateThreshold: envRate('ERROR_RATE_THRESHOLD', 0.01),
  checkRateThreshold: envRate('CHECK_RATE_THRESHOLD', 0.95),
  summaryJson: envString('SUMMARY_JSON', ''),
};

export function isProductionTarget() {
  return ['prod', 'production'].includes(config.targetEnv);
}

export function sharedThresholds() {
  return {
    http_req_failed: [`rate<${config.errorRateThreshold}`],
    http_req_duration: [
      `p(95)<${config.p95ThresholdMs}`,
      `p(99)<${config.p99ThresholdMs}`,
    ],
    checks: [`rate>${config.checkRateThreshold}`],
    api_endpoint_errors: [`rate<${config.errorRateThreshold}`],
  };
}

export function smokeScenario() {
  return {
    smoke: {
      executor: 'constant-vus',
      vus: envNumber('SMOKE_VUS', 1),
      duration: envString('SMOKE_DURATION', '1m'),
      tags: { load_profile: 'smoke' },
    },
  };
}

export function loadScenario() {
  const steadyVus = envNumber('LOAD_STEADY_VUS', 20);

  return {
    average_load: {
      executor: 'ramping-vus',
      stages: [
        { duration: envString('LOAD_RAMP_UP', '2m'), target: steadyVus },
        { duration: envString('LOAD_STEADY_DURATION', '5m'), target: steadyVus },
        { duration: envString('LOAD_RAMP_DOWN', '1m'), target: 0 },
      ],
      tags: { load_profile: 'load' },
    },
  };
}

export function stressScenario() {
  const targetVus = envNumber('STRESS_TARGET_VUS', 80);

  return {
    stress: {
      executor: 'ramping-vus',
      stages: [
        { duration: envString('STRESS_RAMP_UP', '4m'), target: targetVus },
        { duration: envString('STRESS_HOLD_DURATION', '8m'), target: targetVus },
        { duration: envString('STRESS_RAMP_DOWN', '2m'), target: 0 },
      ],
      tags: { load_profile: 'stress' },
    },
  };
}

export function spikeScenario() {
  const baselineVus = envNumber('SPIKE_BASELINE_VUS', 5);
  const peakVus = envNumber('SPIKE_PEAK_VUS', 120);

  return {
    spike: {
      executor: 'ramping-vus',
      stages: [
        { duration: envString('SPIKE_WARM_UP', '1m'), target: baselineVus },
        { duration: envString('SPIKE_RAMP_UP', '30s'), target: peakVus },
        { duration: envString('SPIKE_HOLD_DURATION', '1m'), target: peakVus },
        { duration: envString('SPIKE_RAMP_DOWN', '30s'), target: baselineVus },
        { duration: envString('SPIKE_RECOVERY_DURATION', '2m'), target: baselineVus },
        { duration: envString('SPIKE_COOL_DOWN', '30s'), target: 0 },
      ],
      tags: { load_profile: 'spike' },
    },
  };
}

export function compactSummary(data, profileName) {
  function value(name, key) {
    const metric = data.metrics[name];
    if (!metric || !metric.values || metric.values[key] === undefined) {
      return 'n/a';
    }

    return metric.values[key];
  }

  const lines = [
    '',
    'Loopin k6 performance summary',
    `profile: ${profileName}`,
    `target: ${config.targetEnv} (${config.baseUrl}${config.apiPrefix})`,
    `response_time_p95_ms: ${value('http_req_duration', 'p(95)')}`,
    `response_time_p99_ms: ${value('http_req_duration', 'p(99)')}`,
    `error_rate: ${value('http_req_failed', 'rate')}`,
    `throughput_reqs_per_sec: ${value('http_reqs', 'rate')}`,
    `total_requests: ${value('http_reqs', 'count')}`,
    `checks_rate: ${value('checks', 'rate')}`,
    '',
  ];

  const outputs = {
    stdout: `${lines.join('\n')}\n`,
  };

  if (config.summaryJson) {
    outputs[config.summaryJson] = JSON.stringify(data, null, 2);
  }

  return outputs;
}