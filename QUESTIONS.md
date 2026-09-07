# Questions

Here we have 3 questions related to the code base for you to answer. It is not about right or wrong, but more about what's the reasoning behind your decisions.

1. In this code base, we have some different implementation strategies when it comes to database access layer and manipulation. If you would maintain this code base, would you refactor any of those? Why?

**Answer:**
```txt
Yes, but only in a small way.

Store and Product use Panache on the entity itself (Active Record). Warehouse uses a domain object plus WarehouseRepository. I would keep that split. Store/Product are simple CRUD, so Panache on the entity is fine and matches the Quarkus quickstart style. Warehouse has real rules (location limits, archive vs replace), so a repository there makes sense.

```
----
2. When it comes to API spec and endpoints handlers, we have an Open API yaml file for the `Warehouse` API from which we generate code, but for the other endpoints - `Product` and `Store` - we just coded directly everything. What would be your thoughts about what are the pros and cons of each approach and what would be your choice?

**Answer:**
```txt
I’d prefer the OpenAPI YAML/code-generation approach for all APIs. Consistent, generates DTOs/interfaces automatically, lower risk of inconsistency in documentation.
Whereas in manual it will be flexible in initial stages but demands more manual work once the project grows.
```
----
3. Given the need to balance thorough testing with time and resource constraints, how would you prioritize and implement tests for this project? Which types of tests would you focus on, and how would you ensure test coverage remains effective over time?

**Answer:**
```txt
Prioritize unit tests on warehouse business rules (create/replace/archive) — highest risk, cheap to run.
Add a few REST smoke tests for main endpoints so APIs still wire correctly.
Skip chasing 100% coverage. Keep tests useful by running the related ones whenever those rules or the OpenAPI change.
```
