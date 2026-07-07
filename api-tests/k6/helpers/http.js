import http from 'k6/http';
import { check } from 'k6';
import { Rate, Trend } from 'k6/metrics';
import { config } from './config.js';

export const endpointErrors = new Rate('api_endpoint_errors');
export const endpointDuration = new Trend('api_endpoint_duration', true);

function encodeQuery(query) {
  const parts = [];

  Object.keys(query || {}).forEach((key) => {
    const value = query[key];
    if (value === undefined || value === null || value === '') {
      return;
    }

    parts.push(`${encodeURIComponent(key)}=${encodeURIComponent(value)}`);
  });

  return parts.length ? `?${parts.join('&')}` : '';
}

export function apiUrl(path, query) {
  const normalizedPath = path.startsWith('/') ? path : `/${path}`;
  return `${config.baseUrl}${config.apiPrefix}${normalizedPath}${encodeQuery(query)}`;
}

export function jsonHeaders(token) {
  const headers = {
    Accept: 'application/json',
    'Content-Type': 'application/json',
  };

  if (token) {
    headers.Authorization = `Bearer ${token}`;
  }

  return headers;
}

export function get(endpoint, path, options = {}) {
  return http.get(apiUrl(path, options.query), {
    headers: options.headers || jsonHeaders(options.token),
    timeout: config.timeout,
    tags: {
      endpoint,
      flow: options.flow || 'unspecified',
    },
  });
}

export function post(endpoint, path, body, options = {}) {
  return http.post(apiUrl(path, options.query), JSON.stringify(body), {
    headers: options.headers || jsonHeaders(options.token),
    timeout: config.timeout,
    tags: {
      endpoint,
      flow: options.flow || 'unspecified',
    },
  });
}

export function expectStatus(response, endpoint, statuses) {
  const accepted = Array.isArray(statuses) ? statuses : [statuses];
  const ok = check(response, {
    [`${endpoint} status ${accepted.join('/')}`]: (res) => accepted.includes(res.status),
  });

  endpointErrors.add(!ok, { endpoint });
  endpointDuration.add(response.timings.duration, { endpoint });
  return ok;
}

export function parseJson(response, fallback = null) {
  try {
    return response.json();
  } catch (_error) {
    return fallback;
  }
}
