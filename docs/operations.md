# Operação local e diagnóstico

## Serviços e credenciais

`scripts/setup.ps1` cria `.env` com três segredos diferentes. Não publique esse arquivo nem saída de `docker compose config`, que pode expandir credenciais. O Compose usa PostgreSQL local com usuário `ratings` e banco `ratings`. Redis não usa senha neste ambiente isolado de demonstração; a porta é publicada apenas em loopback.

Para rodar Java no host (JDK 21), importe variáveis no processo antes de `mvnw.cmd spring-boot:run`:

```powershell
Get-Content .env | ForEach-Object {
    if ($_ -match '^([^#=]+)=(.*)$') { [Environment]::SetEnvironmentVariable($matches[1], $matches[2], 'Process') }
}
docker compose up -d postgres redis
.\mvnw.cmd spring-boot:run
```

O Compose não deve executar `api` ao mesmo tempo que a API no host na porta 8080. PostgreSQL é publicado em `127.0.0.1:15432` para coexistir com instalações na porta padrão 5432; Redis usa `127.0.0.1:6379`. Se houver conflito, altere somente o lado esquerdo dos mappings e as variáveis de conexão do processo Java no host. Entre containers, PostgreSQL continua usando 5432 e Redis, 6379.

## Saúde e métricas

`/actuator/health` inclui dependências e pode retornar 503 se Redis estiver indisponível mesmo que a API consiga ler via fallback. `/actuator/health/liveness` verifica a aplicação; não deve provocar reinício em cascata por falha externa. Readiness usa os grupos padrão do Spring Boot (disponibilidade da aplicação), não comprova acesso a todas as dependências. Healthcheck Docker utiliza liveness; o smoke valida comportamento real.

Métricas Prometheus exigem ADMIN. Explore `http_server_requests_seconds`, `hikaricp_*`, `ratings_cache_total` e `resilience4j_circuitbreaker_*`. Métricas e labels disponíveis dependem dos eventos já observados. Não há collector Prometheus/Grafana no Compose; o endpoint está pronto para coleta.

Logs: `docker compose logs --tail=100 api`. Não habilite wire logging HTTP/SQL em um ambiente com credenciais ou dados reais. A aplicação não registra chaves, credenciais nem corpos de requests intencionalmente.

## Verificar o cache

1. Crie produto e avaliação pelo smoke ou Swagger.
2. Consulte `/products/{id}/summary` duas vezes.
3. Inspecione apenas a chave conhecida:

```powershell
docker compose exec redis redis-cli GET ratings:summary:v1:UUID_DO_PRODUTO
docker compose exec redis redis-cli TTL ratings:summary:v1:UUID_DO_PRODUTO
```

Evite `KEYS *` em ambientes grandes. Delete/crie uma avaliação e observe a invalidação. Para demonstrar fallback: `docker compose stop redis`, consulte o resumo e depois execute `docker compose start redis`. O agregado continuará vindo do PostgreSQL; o circuito Redis poderá levar até 10 segundos para permitir recuperação. Não use o smoke completo durante essa interrupção para avaliar saúde global.

## Dados e mudanças de schema

Dados permanecem no volume nomeado `postgres-data`. Nunca edite uma migration já aplicada: crie `V2__...sql`. Alterar a senha no `.env` só afeta a inicialização de um volume novo; em um volume existente, altere a role PostgreSQL administrativamente e sincronize a configuração. O projeto não automatiza rotação de segredos nem backups.

## IA e falhas

Confira `AI_ENABLED`, chave e modelo no `.env`; recrie `api` após mudanças. `DISABLED` significa configuração ausente/desligada. `NO_REVIEWS` evita uma chamada externa sem dados. `UNAVAILABLE` pode indicar timeout, erro do provedor, circuito aberto, rate limit ou bulkhead saturado. Essa resposta deliberadamente não expõe o erro HTTP bruto do provedor. Para diagnóstico, confira o painel do provedor e as métricas de circuito sem registrar a chave.

`OPENAI_BASE_URL` é uma opção de configuração do processo Java para testes/ambientes controlados; nunca aceite seu valor de um cliente HTTP. O Compose utiliza o host oficial fixo pelo default da aplicação.
