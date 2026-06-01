# InternFlow DB Documentation

DB documentation has been consolidated here:

```text
docs/db/DB_SCHEMA.md
```

Production baseline schema for a fresh PostgreSQL database:

```text
db/init_production.sql
```

Do not run `db/init_production.sql` on an existing database with data. Use `db/migrations/*.sql` for upgrades.
