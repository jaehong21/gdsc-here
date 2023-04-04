# Use an official Python runtime as a parent image
FROM python:3.9-slim-buster as builder

# Set the working directory to /app
WORKDIR /tmp

RUN pip install poetry

# Copy the pyproject.toml and poetry.lock files to the container
COPY pyproject.toml poetry.lock* /tmp/

RUN poetry export -f requirements.txt --output requirements.txt --without-hashes

FROM python:3.9-slim-buster

RUN apt-get update && apt-get install -y \
    libusb-1.0-0 \
    libusb-1.0-0-dev \
    ffmpeg \
    pkg-config \
    && rm -rf /var/lib/apt/lists/*

COPY --from=builder /tmp/requirements.txt requirements.txt
RUN pip install --no-cache-dir --upgrade -r requirements.txt

# Copy the application code to the container
COPY . .

# Expose port 8001 for the application
EXPOSE 8001

# Start the application
CMD ["gunicorn", "-w", "1", "-k", "uvicorn.workers.UvicornWorker", \
    "src.main:app", "--bind", "0.0.0.0:8001"]
