# Validação local

Execução em 28/10/2026, Windows com Docker Desktop (engine Linux), JDK Temurin 21 e Maven 3.9.11. Spring Boot 3.5.16. Hardware do host: AMD Ryzen 5 5600G, 12 processadores lógicos; Docker com aproximadamente 7,7 GiB disponíveis. Esses dados contextualizam as verificações e não definem requisitos mínimos ou capacidade de produção.

## Verificações automatizadas

`mvn spotless:apply verify` concluiu com **BUILD SUCCESS**:

- 7 testes unitários: domínio da nota, falha de cache/circuit breaker, contrato HTTP do provedor de IA e estados da análise.
- 4 testes de integração com PostgreSQL 17 e Redis 7.4 reais via Testcontainers: fluxo de avaliação/cache, autorização, validação, OpenAPI, protocolo CSRF com cookie/header e consulta/unicidade de SKU.
- 0 falhas, 0 erros, 0 testes ignorados.
- JaCoCo: **276/306 linhas executadas (90,2%)**, incluindo unitários e integração. Sem meta artificial de cobertura total; relatório gerado em `target/site/jacoco/index.html`.
- Imagem de runtime construída pelo Dockerfile multi-stage com JDK 21 no build e JRE 21 no runtime.

O teste de IA utiliza servidor HTTP local e chave fictícia. Não foi feita chamada paga à OpenAI. O comportamento com uma chave/modelo reais depende da conta e deve ser validado após a configuração.

## Smoke e carga em Docker

Os três containers ficaram saudáveis; Swagger respondeu HTTP 200. `scripts/smoke.ps1` criou um produto, publicou uma avaliação, validou resumo 5/5 e retornou `DISABLED` para IA sem chave.

Cenário `scripts/load.js`, k6 0.57.0, 40 s, rampa de até 10 usuários virtuais e pausa de 200 ms por iteração, contra um único produto com uma avaliação e cache compartilhado:

| Indicador | Resultado |
| --- | --- |
| Requisições | 1.065 |
| Taxa média durante a rampa | 26,60 req/s |
| Erros HTTP | 0% |
| Latência média HTTP | 6,62 ms |
| p95 HTTP | 8,89 ms |
| Maior latência HTTP | 442,79 ms |

Teste repetido após habilitar CSRF, com dataset mínimo, HTTP sem TLS e cache favorável. Não mede capacidade máxima, segurança, comportamento com muitas chaves, dados volumosos ou concorrência de escrita. A taxa é determinada pelo cenário e suas pausas, não é o limite de throughput da API. As metas configuradas de p95 < 500 ms e erros < 1% foram atendidas nesta execução.

Os resultados são uma fotografia desta execução. A CI deve ser consultada para validar o commit atual. Não confunda metas ou resultados deste teste curto com um SLA de produção.
