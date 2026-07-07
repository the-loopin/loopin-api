import { group, sleep } from 'k6';
import exec from 'k6/execution';
import { config } from './config.js';
import { get, post, expectStatus, jsonHeaders, parseJson } from './http.js';

function randomItem(items) {
  return items[Math.floor(Math.random() * items.length)];
}

function thinkTime(minSeconds = 0.4, maxSeconds = 1.6) {
  sleep(Math.random() * (maxSeconds - minSeconds) + minSeconds);
}

export function discoverEventId(response) {
  const body = parseJson(response, {});
  const content = Array.isArray(body.content) ? body.content : [];
  if (!content.length) {
    return '';
  }

  return content[0].id || content[0].publicId || '';
}

export function publicDiscoveryFlow(state) {
  group('public discovery', () => {
    const list = get('events_list', '/events', {
      flow: 'public',
      query: {
        page: Math.floor(Math.random() * 3),
        size: 20,
        sort: 'startDateTime,asc',
      },
    });
    expectStatus(list, 'events_list', 200);

    thinkTime();

    const filtered = get('events_filtered', '/events', {
      flow: 'public',
      query: {
        page: 0,
        size: 10,
        category: randomItem(['TECH', 'SOCIAL', 'EDUCATION', 'SPORT', 'STARTUP']),
        isFree: Math.random() > 0.5,
      },
    });
    expectStatus(filtered, 'events_filtered', 200);

    if (state.publicEventId) {
      thinkTime();
      const detail = get('event_detail', `/events/${state.publicEventId}`, {
        flow: 'public',
      });
      expectStatus(detail, 'event_detail', 200);
    }

    if (config.publicGroupId) {
      thinkTime();
      const groupDetail = get('group_detail', `/groups/${config.publicGroupId}`, {
        flow: 'public',
      });
      expectStatus(groupDetail, 'group_detail', 200);
    }
  });
}

export function authenticatedProfileFlow(state) {
  if (!state.authToken) {
    return;
  }

  const headers = jsonHeaders(state.authToken);

  group('authenticated profile', () => {
    const userMe = get('users_me', '/users/me', {
      flow: 'authenticated',
      headers,
    });
    expectStatus(userMe, 'users_me', 200);

    thinkTime();

    const profile = get('profile_me', '/me', {
      flow: 'authenticated',
      headers,
    });
    expectStatus(profile, 'profile_me', 200);

    const interests = get('profile_interests', '/me/interests', {
      flow: 'authenticated',
      headers,
    });
    expectStatus(interests, 'profile_interests', 200);

    const badges = get('profile_badges', '/me/badges', {
      flow: 'authenticated',
      headers,
    });
    expectStatus(badges, 'profile_badges', 200);
  });
}

export function authenticatedEventsFlow(state) {
  if (!state.authToken) {
    return;
  }

  const headers = jsonHeaders(state.authToken);

  group('authenticated events', () => {
    const recommended = get('events_recommended', '/events/recommended', {
      flow: 'authenticated',
      headers,
      query: { limit: 10 },
    });
    expectStatus(recommended, 'events_recommended', 200);
  });
}

export function authenticatedMessagingFlow(state) {
  if (!state.authToken || !config.authGroupId) {
    return;
  }

  group('authenticated messaging', () => {
    const messages = get('group_messages', `/groups/${config.authGroupId}/messages`, {
      flow: 'authenticated',
      headers: jsonHeaders(state.authToken),
    });
    expectStatus(messages, 'group_messages', 200);
  });
}

export function guardedWriteFlow(state) {
  if (!config.runMutatingFlows || !state.authToken) {
    return;
  }

  const now = Date.now();
  const startDateTime = new Date(now + 24 * 60 * 60 * 1000).toISOString().slice(0, 19);
  const endDateTime = new Date(now + 26 * 60 * 60 * 1000).toISOString().slice(0, 19);
  const sequence = `${exec.vu.idInTest}-${exec.scenario.iterationInTest}`;

  group('guarded writes', () => {
    const created = post('event_create', '/events', {
      title: `k6 perf event ${sequence}`,
      description: 'Created by opt-in non-production k6 performance tests.',
      type: 'EVENT',
      category: 'TECH',
      city: 'Baku',
      address: 'k6 local test address',
      startDateTime,
      endDateTime,
      isFree: true,
      organizerName: 'Loopin k6',
      status: 'DRAFT',
      interestIds: [],
    }, {
      flow: 'mutating',
      headers: jsonHeaders(state.authToken),
    });

    expectStatus(created, 'event_create', 201);
  });
}

export function runReadOnlyFlows(state) {
  publicDiscoveryFlow(state);
  authenticatedProfileFlow(state);
  authenticatedEventsFlow(state);
  authenticatedMessagingFlow(state);
}

export function runFullFlowSet(state) {
  runReadOnlyFlows(state);
  guardedWriteFlow(state);
}
