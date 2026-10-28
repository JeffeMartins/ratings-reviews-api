import http from 'k6/http';
import { check, sleep } from 'k6';
export const options = {
  stages: [{ duration: '10s', target: 5 }, { duration: '20s', target: 10 }, { duration: '10s', target: 0 }],
  thresholds: { http_req_failed: ['rate<0.01'], http_req_duration: ['p(95)<500'] },
};
export default function () {
  if (!__ENV.PRODUCT_ID) throw new Error('Set PRODUCT_ID to an existing product UUID');
  const response = http.get(`${__ENV.BASE_URL || 'http://host.docker.internal:8080'}/api/v1/products/${__ENV.PRODUCT_ID}/summary`);
  check(response, { 'summary available': r => r.status === 200 });
  sleep(0.2);
}
