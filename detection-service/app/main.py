from fastapi import FastAPI
from app.routers import detection

app = FastAPI(title="MandateGuard Detection Service")
app.include_router(detection.router, prefix="/detect", tags=["detection"])
