# Decisões de arquitetura

## 1. Monólito modular e transações locais

Uma API única reduz o custo operacional e permite transações ACID entre produto e avaliação. Pacotes delimitam API, casos de uso, persistência, cache e IA. Serviços externos ficam atrás de adaptadores dedicados. A estrutura é suficiente para este tamanho; interfaces/ports adicionais serão introduzidas quando existirem implementações alternativas ou um domínio que justifique o custo.

PostgreSQL é a fonte de verdade. Flyway versiona o schema. A restrição única `(product_id, author)` resolve concorrência de escrita no banco; uma consulta prévia isolada não seria suficiente. A FK garante integridade referencial e o check da nota protege gravações fora da API.

## 2. Agregação com cache de curta duração

Resumo é uma leitura frequente e tolera atraso pequeno. Cache JSON contém somente um DTO, sem serialização Java nativa. Redis pode ser descartado e reconstruído. O adaptador converte falhas em misses e contabiliza erros; circuit breaker evita insistir em uma dependência com falhas. Uma consulta de existência continua acontecendo em cada leitura de resumo.

Invalidação após commit evita eventos de rollback, mas não é atômica com Redis. Corrida possível: A lê snapshot antigo; B grava/commita/invalida; A grava o snapshot antigo no cache. TTL limita a vida dessa entrada. Para requisitos estritos, use versão de agregados e escrita condicional ou mantenha agregados transacionais no PostgreSQL. Outbox ajuda a garantir entrega de invalidações, mas não resolve sozinha toda corrida de refill.

Não há lock global, single-flight ou cache de IA. Sob um cache miss simultâneo várias requisições podem agregar no banco. Antes de adicionar coordenação distribuída, meça a relação hit/miss, duração do SQL e pressão do pool. Cache não é substituto de planejamento de capacidade.

## 3. Integração de IA isolada

O serviço prepara uma amostra limitada; o gateway encapsula HTTP e parsing. A chamada de rede ocorre fora de transações JPA. O endpoint é POST e restrito ao administrador porque pode gerar custo. Uma única invocação externa, sem retries, tem timeouts e concorrência limitada. O cliente recebe status explícito de indisponibilidade. Testes não precisam de uma chave real.

O prompt delimita reviews como dados, sem ferramentas externas. Respostas continuam probabilísticas e precisam ser tratadas como texto não confiável. Uma evolução relevante é Structured Outputs, com schema, avaliação da qualidade, anonimização de conteúdo livre, persistência versionada da análise e deduplicação idempotente da geração.

## 4. Segurança local explícita

Usuários em memória com senhas aleatórias tornam o clone fácil de demonstrar. Esse mecanismo não oferece cadastro, recuperação de senha, revogação de identidade nem segurança suficiente para exposição pública. A evolução prevista é Resource Server OAuth2/OIDC validando issuer, audience e scopes, com testes de autorização e TLS no gateway.

HTTP Basic pode ser enviado automaticamente pelo browser, portanto stateless não elimina CSRF. Escritas exigem cookie `XSRF-TOKEN` e header `X-XSRF-TOKEN`. O handler aceita o token plain vindo do cookie em headers JavaScript e mantém a representação mascarada em atributos do request; Swagger está configurado para esse protocolo. O cookie é legível por JavaScript para permitir essa integração e não contém uma credencial de identidade.

O rate limit global usa memória limitada e não armazena IPs em um mapa crescente. Não distribui quotas entre réplicas e não diferencia clientes. Um gateway deve impor quotas por identidade e limites de corpo, com proteção a brute force antes da verificação BCrypt.

## 5. Evolução orientada a medições

1. Medir k6 e publicar hardware, dataset, configurações e percentis; não confundir meta com benchmark.
2. Adicionar paginação por cursor para datasets grandes; offset alto e `count(*)` custam trabalho ao banco.
3. Persistir agregados transacionalmente se a cardinalidade/volume justificar; comparar com consulta agregada indexada.
4. Implementar OIDC, limites distribuídos, TLS e backups antes de exposição pública.
5. Adicionar tracing distribuído se houver múltiplos serviços; hoje há métricas e logs de aplicação.
6. Criar usuários reais, edição/versionamento de reviews, moderação e trilha de auditoria conforme novas regras de negócio.
7. Automatizar análise de dependências/imagens e políticas de atualização; Dependabot não substitui um scanner.
