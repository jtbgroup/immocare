# Commands used to manage the project

## git

git add * && git commit -a && git push

## deploy locally

```
# deploy backend
make dev-build

# feed database with real data
npm run seed:dev -- --dataset=real

# scratch DB
docker compose -f docker-compose.dev.yml down -v && docker compose -f docker-compose.dev.yml up -d
```

## Feed DB
```
# Real data from root
cd frontend && npm run seed:dev -- --dataset=real && cd ..

# Real data from frontend
npm run seed:dev -- --dataset=real


# Demo data
npm run seed:dev -- --dataset=demo
```

## Logs 

```
docker compose -f docker-compose.dev.yml logs -f backend
docker compose -f docker-compose.dev.yml logs -f frontend
```
