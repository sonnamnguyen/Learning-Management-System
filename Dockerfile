# Sử dụng openjdk 21 làm base image
FROM openjdk:21-jdk-slim

# Cập nhật danh sách gói và cài đặt các dependencies
RUN apt-get update && apt-get install -y \
    build-essential \
    gcc \
    wget \
    apt-transport-https \
    gpg \
    libicu-dev \
    && rm -rf /var/lib/apt/lists/*

# Thêm Microsoft package repository
RUN wget https://packages.microsoft.com/config/debian/12/packages-microsoft-prod.deb -O packages-microsoft-prod.deb \
    && dpkg -i packages-microsoft-prod.deb \
    && rm packages-microsoft-prod.deb

RUN wget https://dotnetcli.azureedge.net/dotnet/Sdk/9.0.200/dotnet-sdk-9.0.200-linux-x64.tar.gz -O dotnet-sdk-9.0.200-linux-x64.tar.gz && \
    mkdir -p /usr/share/dotnet && \
    tar -xvf dotnet-sdk-9.0.200-linux-x64.tar.gz -C /usr/share/dotnet && \
    ln -s /usr/share/dotnet/dotnet /usr/local/bin/dotnet && \
    rm dotnet-sdk-9.0.200-linux-x64.tar.gz

# Chỉ định thư mục làm việc trong container
WORKDIR /app

# Sao chép file JAR vào thư mục làm việc
COPY target/LMS2025-1.0-SNAPSHOT.jar app.jar

# Mở cổng cho ứng dụng
EXPOSE 8080

# Lệnh để chạy ứng dụng
ENTRYPOINT ["java", "-jar", "app.jar"]