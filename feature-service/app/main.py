from fastapi import FastAPI
from app.routers import features

app = FastAPI(title="MandateGuard Feature Service")
app.include_router(features.router, prefix="/features", tags=["features"])
