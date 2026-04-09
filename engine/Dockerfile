# Builds libvalengine.so and exports it on /output for downstream stages.
FROM ubuntu:24.04 AS builder
RUN apt-get update && apt-get install -y --no-install-recommends \
    cmake g++ ninja-build openjdk-21-jdk-headless ca-certificates git \
 && rm -rf /var/lib/apt/lists/*
WORKDIR /src
COPY . .
RUN cmake -B build -G Ninja -DCMAKE_BUILD_TYPE=Release -DVALENGINE_BUILD_TESTS=OFF \
 && cmake --build build --parallel \
 && mkdir /output && cp build/libvalengine.so /output/

FROM scratch AS export
COPY --from=builder /output/ /
